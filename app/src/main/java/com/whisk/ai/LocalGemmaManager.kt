package com.whisk.ai

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.LocalModelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class LocalGemmaManager(private val context: Context) : AutoCloseable {

    private val _modelStatus = MutableStateFlow(LocalModelStatus())
    val modelStatus: StateFlow<LocalModelStatus> = _modelStatus.asStateFlow()

    private val modelFileName = "gemma-2b-it-gpu-int4.bin"
    fun getModelFile(): File = File(context.getExternalFilesDir(null) ?: context.filesDir, modelFileName)

    private var llmInference: LlmInference? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        checkModelFile()
    }

    fun checkModelFile() {
        val file = getModelFile()
        if (file.exists() && file.length() > 50_000_000L) {
            val sizeMb = file.length() / (1024 * 1024)
            _modelStatus.value = LocalModelStatus(
                isDownloaded = true,
                downloadProgress = 1.0f,
                statusText = "Gemma 2B LiteRT ready ($sizeMb MB)",
                modelSizeBytes = file.length(),
                downloadedBytes = file.length(),
                totalBytes = file.length(),
                isLoaded = llmInference != null
            )
        } else {
            _modelStatus.value = LocalModelStatus(
                isDownloaded = false,
                downloadProgress = 0f,
                statusText = "Gemma 2B model not downloaded from Hugging Face",
                isLoaded = false
            )
        }
    }

    fun checkStorageAvailable(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    suspend fun downloadModel(config: AiConfig, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val available = checkStorageAvailable()
        if (available < 1_500_000_000L) {
            _modelStatus.value = _modelStatus.value.copy(
                statusText = "Error: Insufficient storage (Requires 1.5 GB free)",
                loadError = "Insufficient storage"
            )
            return@withContext false
        }

        _modelStatus.value = _modelStatus.value.copy(
            statusText = "Connecting to Hugging Face...",
            downloadProgress = 0f
        )

        val targetFile = getModelFile()
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

        try {
            val requestBuilder = Request.Builder().url(config.hfModelUrl).get()
            if (config.hfApiToken.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer ${config.hfApiToken}")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                val errorMsg = "Hugging Face download failed (HTTP ${response.code}): ${response.message}"
                _modelStatus.value = _modelStatus.value.copy(
                    statusText = errorMsg,
                    loadError = errorMsg
                )
                return@withContext false
            }

            val body = response.body
            if (body == null) {
                _modelStatus.value = _modelStatus.value.copy(
                    statusText = "Response body from Hugging Face was empty",
                    loadError = "Empty response body"
                )
                return@withContext false
            }

            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L
            val buffer = ByteArray(64 * 1024)
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L

            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        bytesSinceLastTime += read

                        val now = System.currentTimeMillis()
                        val duration = now - lastTime
                        var speedKbps = 0f
                        if (duration >= 500) {
                            speedKbps = (bytesSinceLastTime / 1024f) / (duration / 1000f)
                            lastTime = now
                            bytesSinceLastTime = 0
                        }

                        val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
                        onProgress(progress)

                        val downloadedMb = bytesDownloaded / (1024 * 1024)
                        val totalMb = if (totalBytes > 0) totalBytes / (1024 * 1024) else 0
                        val statusText = if (totalBytes > 0) {
                            "Downloading Gemma 2B: $downloadedMb / $totalMb MB (${(progress * 100).toInt()}%) • ${speedKbps.toInt()} KB/s"
                        } else {
                            "Downloading Gemma 2B: $downloadedMb MB • ${speedKbps.toInt()} KB/s"
                        }

                        _modelStatus.value = _modelStatus.value.copy(
                            downloadProgress = progress,
                            statusText = statusText,
                            downloadedBytes = bytesDownloaded,
                            totalBytes = totalBytes,
                            downloadSpeedKbps = speedKbps
                        )
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 50_000_000L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                _modelStatus.value = LocalModelStatus(
                    isDownloaded = true,
                    downloadProgress = 1.0f,
                    statusText = "Gemma 2B downloaded & ready (${targetFile.length() / (1024 * 1024)} MB)",
                    modelSizeBytes = targetFile.length(),
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length(),
                    isLoaded = false
                )
                initializeEngine(config)
                true
            } else {
                tempFile.delete()
                _modelStatus.value = _modelStatus.value.copy(
                    statusText = "Downloaded file size too small or incomplete",
                    loadError = "Corrupted download"
                )
                false
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            _modelStatus.value = _modelStatus.value.copy(
                statusText = "Download error: ${e.message}",
                loadError = e.message
            )
            false
        }
    }

    suspend fun initializeEngine(config: AiConfig): Boolean = withContext(Dispatchers.IO) {
        val file = getModelFile()
        if (!file.exists() || file.length() < 50_000_000L) {
            _modelStatus.value = _modelStatus.value.copy(
                isLoaded = false,
                statusText = "Model file missing or incomplete"
            )
            return@withContext false
        }

        try {
            _modelStatus.value = _modelStatus.value.copy(statusText = "Initializing MediaPipe LLM engine...")
            llmInference?.close()

            val backend = if (config.useGpuAccelerator) LlmInference.Backend.GPU else LlmInference.Backend.CPU

            val optionsBuilder = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(config.maxTokens)
                .setPreferredBackend(backend)

            val options = optionsBuilder.build()

            llmInference = try {
                LlmInference.createFromOptions(context, options)
            } catch (e: Exception) {
                if (config.useGpuAccelerator) {
                    // Fallback to CPU backend if GPU fails
                    val cpuOptions = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(file.absolutePath)
                        .setMaxTokens(config.maxTokens)
                        .setPreferredBackend(LlmInference.Backend.CPU)
                        .build()
                    LlmInference.createFromOptions(context, cpuOptions)
                } else {
                    throw e
                }
            }

            _modelStatus.value = _modelStatus.value.copy(
                isLoaded = true,
                statusText = "Gemma 2B LiteRT active (${if (config.useGpuAccelerator) "GPU" else "CPU"})",
                loadError = null
            )
            true
        } catch (e: Exception) {
            _modelStatus.value = _modelStatus.value.copy(
                isLoaded = false,
                statusText = "Engine initialization failed: ${e.message}",
                loadError = e.message
            )
            false
        }
    }

    fun generateStream(prompt: String, contextText: String = "", config: AiConfig = AiConfig()): Flow<String> = callbackFlow {
        val fullPrompt = if (contextText.isNotBlank()) {
            "Context:\n$contextText\n\nUser Question:\n$prompt\n\nAnswer concisely as a senior developer:"
        } else {
            "User Question:\n$prompt\n\nAnswer concisely as a senior developer:"
        }

        if (llmInference == null) {
            val initialized = initializeEngine(config)
            if (!initialized || llmInference == null) {
                val errorMsg = _modelStatus.value.loadError ?: "Local model is not initialized or not downloaded."
                trySend("Error: $errorMsg")
                close()
                return@callbackFlow
            }
        }

        try {
            val activeInference = llmInference
            if (activeInference != null) {
                activeInference.generateResponseAsync(fullPrompt) { partialResult, done ->
                    if (!partialResult.isNullOrEmpty()) {
                        trySend(partialResult)
                    }
                    if (done) {
                        close()
                    }
                }
            } else {
                trySend("Error: LLM Inference instance unavailable.")
                close()
            }
        } catch (e: Exception) {
            trySend("Inference Exception: ${e.message}")
            close()
        }

        awaitClose { }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteModel(): Boolean = withContext(Dispatchers.IO) {
        llmInference?.close()
        llmInference = null

        val file = getModelFile()
        if (file.exists()) {
            file.delete()
        }
        _modelStatus.value = LocalModelStatus(
            isDownloaded = false,
            downloadProgress = 0f,
            statusText = "Model deleted",
            isLoaded = false
        )
        true
    }

    override fun close() {
        llmInference?.close()
        llmInference = null
    }
}
