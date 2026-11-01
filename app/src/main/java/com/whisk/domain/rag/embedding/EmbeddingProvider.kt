package com.whisk.domain.rag.embedding

interface EmbeddingProvider {
    val modelId: String
    val dimensions: Int

    suspend fun generateEmbedding(text: String): FloatArray
    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray>
}
