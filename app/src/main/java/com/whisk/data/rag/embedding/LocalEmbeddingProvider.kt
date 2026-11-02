package com.whisk.data.rag.embedding

import com.whisk.data.rag.embedding.tokenizer.BertWordPieceTokenizer
import com.whisk.domain.rag.embedding.EmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

class LocalEmbeddingProvider(
    private val inferenceEngine: OnnxEmbeddingInferenceEngine? = null,
    private val tokenizer: BertWordPieceTokenizer? = null,
    private val modelFile: File? = null,
    override val modelId: String = "whisk-minilm-l6-v2-384d-v1",
    override val dimensions: Int = 384,
    val isModelReady: Boolean = false,
) : EmbeddingProvider {

    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return@withContext FloatArray(dimensions)
        }

        if (inferenceEngine != null && tokenizer != null && isModelReady) {
            val tokenResult = tokenizer.tokenize(cleanText)
            return@withContext inferenceEngine.runInference(tokenResult)
        }

        throw IllegalStateException(
            "Local ONNX Embedding Model is not ready (State: NOT_READY). " +
                "Ensure EmbeddingModelManager.initializeModel() has been completed before generating embeddings.",
        )
    }

    override suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        texts.map { text ->
            generateEmbedding(text)
        }
    }

    companion object {
        fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
            if (v1.size != v2.size || v1.isEmpty()) return 0.0f
            var dot = 0.0f
            var norm1 = 0.0f
            var norm2 = 0.0f
            for (i in v1.indices) {
                dot += v1[i] * v2[i]
                norm1 += v1[i] * v1[i]
                norm2 += v2[i] * v2[i]
            }
            val denom = (sqrt(norm1) * sqrt(norm2))
            return if (denom > 1e-6f) (dot / denom).coerceIn(-1.0f, 1.0f) else 0.0f
        }
    }
}
