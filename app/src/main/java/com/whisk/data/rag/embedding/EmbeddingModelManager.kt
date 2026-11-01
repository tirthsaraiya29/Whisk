package com.whisk.data.rag.embedding

import android.content.Context
import com.whisk.domain.rag.embedding.EmbeddingProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class EmbeddingModelStatus(
    val modelId: String = "whisk-local-minilm-v1",
    val dimensions: Int = 384,
    val isDownloaded: Boolean = true,
    val isLoaded: Boolean = true,
    val statusText: String = "Local MiniLM 384d embedding model ready (Offline)",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
)

class EmbeddingModelManager(private val context: Context) {

    private val _status = MutableStateFlow(EmbeddingModelStatus())
    val status: StateFlow<EmbeddingModelStatus> = _status.asStateFlow()

    private val modelFileName = "all-MiniLM-L6-v2-384d.tflite"

    fun getModelFile(): File {
        val extDir = context.getExternalFilesDir(null)
        val extFile = File(extDir, modelFileName)
        if (extFile.exists() && extFile.length() > 0) return extFile

        val intFile = File(context.filesDir, modelFileName)
        if (intFile.exists() && intFile.length() > 0) return intFile

        return extFile
    }

    fun getActiveProvider(): EmbeddingProvider {
        val file = getModelFile()
        return LocalEmbeddingProvider(
            modelFile = if (file.exists()) file else null,
            modelId = _status.value.modelId,
            dimensions = _status.value.dimensions,
        )
    }

    fun checkModelStatus() {
        val file = getModelFile()
        if (file.exists()) {
            _status.value = _status.value.copy(
                isDownloaded = true,
                isLoaded = true,
                statusText = "Custom MiniLM Model loaded (${file.length() / (1024 * 1024)} MB)",
                downloadedBytes = file.length(),
                totalBytes = file.length(),
            )
        } else {
            _status.value = _status.value.copy(
                isDownloaded = true, // Embedded local fallback provider is always ready
                isLoaded = true,
                statusText = "Local On-Device 384d Embedder active",
            )
        }
    }
}
