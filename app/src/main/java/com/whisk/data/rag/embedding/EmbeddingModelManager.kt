package com.whisk.data.rag.embedding

import android.content.Context
import ai.onnxruntime.OrtEnvironment
import com.whisk.data.rag.embedding.tokenizer.BertWordPieceTokenizer
import com.whisk.domain.rag.embedding.EmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.math.sqrt

enum class ModelLifecycleState {
    NOT_INSTALLED,
    DOWNLOADING,
    LOADING,
    READY,
    ERROR,
    INCOMPATIBLE,
}

data class EmbeddingModelStatus(
    val modelId: String = "whisk-minilm-l6-v2-384d-v1",
    val dimensions: Int = 384,
    val state: ModelLifecycleState = ModelLifecycleState.NOT_INSTALLED,
    val statusText: String = "Initializing model engine...",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
) {
    val isDownloaded: Boolean get() = state == ModelLifecycleState.READY || state == ModelLifecycleState.LOADING
    val isLoaded: Boolean get() = state == ModelLifecycleState.READY
}

class EmbeddingModelManager(private val context: Context? = null) {

    private val _status = MutableStateFlow(EmbeddingModelStatus())
    val status: StateFlow<EmbeddingModelStatus> = _status.asStateFlow()

    private val modelFileName = "all-MiniLM-L6-v2-384d.onnx"
    private val initMutex = Mutex()

    private var tokenizer: BertWordPieceTokenizer? = null
    private var inferenceEngine: OnnxEmbeddingInferenceEngine? = null

    suspend fun initializeModel(): Boolean = initMutex.withLock {
        if (_status.value.state == ModelLifecycleState.READY && inferenceEngine != null) {
            return true
        }

        _status.value = _status.value.copy(
            state = ModelLifecycleState.LOADING,
            statusText = "Loading MiniLM ONNX model & tokenizer...",
        )

        withContext(Dispatchers.Default) {
            try {
                // 1. Load Tokenizer vocabulary
                val vocabLines = try {
                    context?.assets?.open("models/vocab.txt")?.use { it.bufferedReader().readLines() }
                } catch (_: Exception) {
                    null
                } ?: try {
                    Thread.currentThread().contextClassLoader?.getResourceAsStream("models/vocab.txt")?.use { it.bufferedReader().readLines() }
                        ?: EmbeddingModelManager::class.java.classLoader?.getResourceAsStream("models/vocab.txt")?.use { it.bufferedReader().readLines() }
                        ?: EmbeddingModelManager::class.java.getResourceAsStream("/models/vocab.txt")?.use { it.bufferedReader().readLines() }
                        ?: EmbeddingModelManager::class.java.getResourceAsStream("/assets/models/vocab.txt")?.use { it.bufferedReader().readLines() }
                } catch (_: Exception) {
                    null
                } ?: run {
                    val candidateVocabFiles = listOf(
                        File("src/main/assets/models/vocab.txt"),
                        File("app/src/main/assets/models/vocab.txt"),
                    )
                    candidateVocabFiles.firstOrNull { it.exists() && it.length() > 0 }?.readLines()
                } ?: BertWordPieceTokenizer.defaultVocabList()

                val loadedTokenizer = BertWordPieceTokenizer(
                    if (vocabLines.isNotEmpty()) vocabLines else BertWordPieceTokenizer.defaultVocabList(),
                )
                tokenizer = loadedTokenizer

                // 2. Load Model Bytes
                val modelBytes: ByteArray? = try {
                    context?.assets?.open("models/$modelFileName")?.use { it.readBytes() }
                } catch (_: Exception) {
                    null
                } ?: try {
                    Thread.currentThread().contextClassLoader?.getResourceAsStream("models/$modelFileName")?.use { it.readBytes() }
                        ?: EmbeddingModelManager::class.java.classLoader?.getResourceAsStream("models/$modelFileName")?.use { it.readBytes() }
                        ?: EmbeddingModelManager::class.java.getResourceAsStream("/models/$modelFileName")?.use { it.readBytes() }
                        ?: EmbeddingModelManager::class.java.getResourceAsStream("/assets/models/$modelFileName")?.use { it.readBytes() }
                } catch (_: Exception) {
                    null
                } ?: run {
                    val candidateFiles = listOf(
                        File("src/main/assets/models/$modelFileName"),
                        File("app/src/main/assets/models/$modelFileName"),
                        getModelFile(),
                    )
                    candidateFiles.firstOrNull { it.exists() && it.length() > 0 }?.readBytes()
                }

                if (modelBytes == null || modelBytes.isEmpty()) {
                    _status.value = _status.value.copy(
                        state = ModelLifecycleState.ERROR,
                        statusText = "Pretrained MiniLM ONNX model file '$modelFileName' not found in assets or storage.",
                    )
                    return@withContext false
                }

                val env = OrtEnvironment.getEnvironment()
                val session = env.createSession(modelBytes)
                val engine = OnnxEmbeddingInferenceEngine(session, env, _status.value.dimensions)

                // 3. Perform test inference with genuine model
                val testTokenization = loadedTokenizer.tokenize("Whisk semantic vector search test")
                val testVector = engine.runInference(testTokenization)

                // 4. Validate output dimensionality & numerical sanity
                if (validateVector(testVector, _status.value.dimensions)) {
                    inferenceEngine = engine
                    _status.value = _status.value.copy(
                        state = ModelLifecycleState.READY,
                        statusText = "MiniLM 384d ONNX Model Ready (On-Device Inference)",
                        downloadedBytes = modelBytes.size.toLong(),
                        totalBytes = modelBytes.size.toLong(),
                    )
                    true
                } else {
                    engine.close()
                    _status.value = _status.value.copy(
                        state = ModelLifecycleState.INCOMPATIBLE,
                        statusText = "Model output failed numerical validation",
                    )
                    false
                }
            } catch (e: Exception) {
                _status.value = _status.value.copy(
                    state = ModelLifecycleState.ERROR,
                    statusText = "Model initialization error: ${e.message}",
                )
                false
            }
        }
    }

    private fun validateVector(vector: FloatArray, expectedDimensions: Int): Boolean {
        if (vector.size != expectedDimensions) return false
        var sumSq = 0.0f
        for (v in vector) {
            if (v.isNaN() || v.isInfinite()) return false
            sumSq += v * v
        }
        val norm = sqrt(sumSq)
        return norm > 1e-4f
    }

    fun getModelFile(): File {
        val extDir = context?.getExternalFilesDir(null)
        val extFile = File(extDir, modelFileName)
        if (extFile.exists() && extFile.length() > 0) return extFile

        val intFile = File(context?.filesDir, modelFileName)
        if (intFile.exists() && intFile.length() > 0) return intFile

        return extFile
    }

    fun getActiveProvider(): EmbeddingProvider {
        return LocalEmbeddingProvider(
            inferenceEngine = inferenceEngine,
            tokenizer = tokenizer,
            modelFile = getModelFile(),
            modelId = _status.value.modelId,
            dimensions = _status.value.dimensions,
            isModelReady = _status.value.state == ModelLifecycleState.READY,
        )
    }

    fun getDelegatingProvider(): EmbeddingProvider {
        return object : EmbeddingProvider {
            override val modelId: String get() = getActiveProvider().modelId
            override val dimensions: Int get() = getActiveProvider().dimensions
            override suspend fun generateEmbedding(text: String): FloatArray =
                getActiveProvider().generateEmbedding(text)
            override suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> =
                getActiveProvider().generateEmbeddings(texts)
        }
    }

    fun checkModelStatus() {
        val file = getModelFile()
        if (file.exists()) {
            _status.value = _status.value.copy(
                downloadedBytes = file.length(),
                totalBytes = file.length(),
            )
        }
    }

    companion object {
        fun computeSha256(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            return hash.joinToString("") { "%02x".format(it) }
        }
    }
}
