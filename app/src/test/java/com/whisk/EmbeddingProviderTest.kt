package com.whisk

import com.whisk.data.rag.embedding.LocalEmbeddingProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingProviderTest {

    @Test
    fun testGenerateEmbeddingDimensionAndNormalization() = runBlocking {
        val provider = LocalEmbeddingProvider(dimensions = 384)
        val text = "Whisk Android RAG architecture"
        val embedding = provider.generateEmbedding(text)

        assertEquals(384, embedding.size)

        // Check L2 norm is ~1.0
        var normSq = 0.0f
        for (v in embedding) {
            normSq += v * v
        }
        val norm = Math.sqrt(normSq.toDouble()).toFloat()
        assertEquals(1.0f, norm, 0.01f)
    }

    @Test
    fun testCosineSimilarityRelatedVsUnrelated() = runBlocking {
        val provider = LocalEmbeddingProvider(dimensions = 384)
        val vText = provider.generateEmbedding("Android networking TLS mDNS architecture")
        val vSimilar = provider.generateEmbedding("Android network setup TLS local discovery")
        val vDifferent = provider.generateEmbedding("Baking chocolate cake recipe oven temperature")

        val simRelated = LocalEmbeddingProvider.cosineSimilarity(vText, vSimilar)
        val simUnrelated = LocalEmbeddingProvider.cosineSimilarity(vText, vDifferent)

        assertTrue(simRelated > simUnrelated)
    }
}
