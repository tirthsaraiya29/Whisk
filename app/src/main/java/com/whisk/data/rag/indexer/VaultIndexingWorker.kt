package com.whisk.data.rag.indexer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whisk.data.local.WhiskDatabase
import com.whisk.data.rag.embedding.EmbeddingModelManager
import com.whisk.data.rag.repository.IndexingRepository
import com.whisk.data.rag.repository.VectorRepository

class VaultIndexingWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = WhiskDatabase.getDatabase(applicationContext)
            val noteDao = db.noteDao()
            val relationshipDao = db.relationshipDao()
            val chunkDao = db.documentChunkDao()
            val vectorDao = db.embeddingVectorDao()
            val indexingStateDao = db.indexingStateDao()

            val vectorRepository = VectorRepository(chunkDao, vectorDao)
            val indexingRepository = IndexingRepository(indexingStateDao, chunkDao)
            val embeddingModelManager = EmbeddingModelManager(applicationContext)

            val indexer = DocumentIndexer(
                noteDao = noteDao,
                relationshipDao = relationshipDao,
                vectorRepository = vectorRepository,
                indexingRepository = indexingRepository,
                embeddingModelManager = embeddingModelManager,
            )

            indexer.indexWorkspace()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueueIndexing(context: Context) {
            val request = OneTimeWorkRequestBuilder<VaultIndexingWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
