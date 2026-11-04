package com.whisk.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.whisk.data.local.WhiskDatabase
import com.whisk.data.repository.ExportImportRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.data.sync.E2EESyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class CloudBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val destinationUrl = inputData.getString(KEY_DESTINATION_URL)
        val encryptionKey = inputData.getString(KEY_ENCRYPTION_KEY)

        try {
            val database = WhiskDatabase.getDatabase(applicationContext)
            val notesRepository = NotesRepository(
                noteDao = database.noteDao(),
                folderDao = database.folderDao(),
                taskDao = database.taskDao()
            )
            val exportRepo = ExportImportRepository(
                noteDao = database.noteDao(),
                database = database,
                context = applicationContext
            )

            // Generate zip export to cache
            val backupFile = File(applicationContext.cacheDir, "vault_backup_${System.currentTimeMillis()}.zip")
            val outputStream = backupFile.outputStream()
            exportRepo.exportVaultToZip(outputStream)
            outputStream.close()

            val rawBytes = backupFile.readBytes()

            // Optional E2EE encryption if key provided
            val payloadBytes = if (!encryptionKey.isNullOrBlank()) {
                val e2eeEngine = E2EESyncEngine(encryptionKey)
                e2eeEngine.encryptVaultPayload(rawBytes)
            } else {
                rawBytes
            }

            // Upload to cloud endpoint if configured
            if (!destinationUrl.isNullOrBlank()) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val requestBody = payloadBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(destinationUrl)
                    .put(requestBody)
                    .addHeader("X-Whisk-Backup", "true")
                    .addHeader("X-Whisk-Timestamp", System.currentTimeMillis().toString())
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    backupFile.delete()
                    return@withContext Result.retry()
                }
            }

            // Cleanup temp file
            backupFile.delete()

            Result.success(workDataOf("bytesBackedUp" to payloadBytes.size))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "com.whisk.CLOUD_VAULT_BACKUP"
        const val KEY_DESTINATION_URL = "destination_url"
        const val KEY_ENCRYPTION_KEY = "encryption_key"

        fun schedulePeriodicBackup(
            context: Context,
            intervalHours: Long = 24,
            destinationUrl: String? = null,
            encryptionKey: String? = null
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val inputData = workDataOf(
                KEY_DESTINATION_URL to (destinationUrl ?: ""),
                KEY_ENCRYPTION_KEY to (encryptionKey ?: "")
            )

            val backupRequest = PeriodicWorkRequestBuilder<CloudBackupWorker>(
                repeatInterval = intervalHours,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupRequest
            )
        }

        fun cancelPeriodicBackup(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
