package com.whisk

import com.whisk.data.rag.embedding.EmbeddingModelManager
import com.whisk.data.rag.embedding.LocalEmbeddingProvider
import com.whisk.data.rag.embedding.ModelLifecycleState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingProviderTest {

    @Test
    fun testGenerateEmbeddingDimensionAndNormalization() = runBlocking {
        val manager = EmbeddingModelManager()
        val initSuccess = manager.initializeModel()
        assertTrue("Model initialization should succeed. Status: ${manager.status.value.statusText}", initSuccess)
        assertEquals(ModelLifecycleState.READY, manager.status.value.state)

        val provider = manager.getActiveProvider()
        val text = "Whisk Android RAG architecture"
        val embedding = provider.generateEmbedding(text)

        assertEquals(384, embedding.size)

        var normSq = 0.0f
        for (v in embedding) {
            normSq += v * v
        }
        val norm = Math.sqrt(normSq.toDouble()).toFloat()
        assertEquals(1.0f, norm, 0.01f)
    }

    @Test
    fun testCosineSimilarityRelatedVsUnrelated() = runBlocking {
        val manager = EmbeddingModelManager()
        manager.initializeModel()
        val provider = manager.getActiveProvider()

        val vText = provider.generateEmbedding("Android networking TLS mDNS architecture")
        val vSimilar = provider.generateEmbedding("Android network setup TLS local discovery")
        val vDifferent = provider.generateEmbedding("Baking chocolate cake recipe oven temperature")

        val simRelated = LocalEmbeddingProvider.cosineSimilarity(vText, vSimilar)
        val simUnrelated = LocalEmbeddingProvider.cosineSimilarity(vText, vDifferent)

        assertTrue(
            "Expected simRelated ($simRelated) > simUnrelated ($simUnrelated)",
            simRelated > simUnrelated,
        )
    }

    @Test
    fun testSecuringAndroidAppVsProtectingMobileAppSimilarity() = runBlocking {
        val manager = EmbeddingModelManager()
        manager.initializeModel()
        val provider = manager.getActiveProvider()

        val vQuery1 = provider.generateEmbedding("How do I secure an Android application?")
        val vQuery2 = provider.generateEmbedding("What are the best ways to protect a mobile app?")
        val vUnrelated = provider.generateEmbedding("Baking chocolate cake recipe in the kitchen")

        val simRelated = LocalEmbeddingProvider.cosineSimilarity(vQuery1, vQuery2)
        val simUnrelated = LocalEmbeddingProvider.cosineSimilarity(vQuery1, vUnrelated)

        assertTrue("Expected related similarity ($simRelated) > unrelated ($simUnrelated)", simRelated > simUnrelated)
    }
}
