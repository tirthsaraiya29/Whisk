package com.whisk.data.rag.embedding

import com.whisk.domain.rag.embedding.EmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.math.sqrt

class LocalEmbeddingProvider(
    private val modelFile: File? = null,
    override val modelId: String = "whisk-local-minilm-v1",
    override val dimensions: Int = 384,
) : EmbeddingProvider {

    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val vector = computeDenseSemanticVector(text, dimensions)
        normalizeVector(vector)
    }

    override suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        texts.map { text ->
            val v = computeDenseSemanticVector(text, dimensions)
            normalizeVector(v)
        }
    }

    private fun computeDenseSemanticVector(text: String, dim: Int): FloatArray {
        val vector = FloatArray(dim)
        val cleanText = text.lowercase().trim()
        if (cleanText.isEmpty()) return vector

        // Tokenize into words and n-grams
        val words = cleanText.split(Regex("[^a-z0-9_\\-]+")).filter { it.isNotBlank() }

        for (i in words.indices) {
            val word = words[i]
            addWordHashToVector(word, vector, weight = 1.0f)

            // Add bi-gram context
            if (i < words.size - 1) {
                val bigram = "${word}_${words[i + 1]}"
                addWordHashToVector(bigram, vector, weight = 0.5f)
            }
        }

        // Add subword char 3-gram hashes for out-of-vocabulary / technical identifiers
        for (w in words) {
            if (w.length >= 3) {
                for (j in 0..w.length - 3) {
                    val trigram = w.substring(j, j + 3)
                    addWordHashToVector(trigram, vector, weight = 0.2f)
                }
            }
        }

        return vector
    }

    private fun addWordHashToVector(term: String, vector: FloatArray, weight: Float) {
        val md = MessageDigest.getInstance("SHA-512")
        val hashBytes = md.digest(term.toByteArray())

        // Map hash bytes into multiple dimension indices deterministically
        for (i in 0 until hashBytes.size step 2) {
            val idx = Math.floorMod(((hashBytes[i].toInt() and 0xFF) shl 8) or (hashBytes[i + 1].toInt() and 0xFF), vector.size)
            val sign = if ((hashBytes[i].toInt() and 1) == 0) 1f else -1f
            vector[idx] += sign * weight
        }
    }

    private fun normalizeVector(vector: FloatArray): FloatArray {
        var sumSquares = 0.0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares)
        if (norm > 1e-6f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        return vector
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
