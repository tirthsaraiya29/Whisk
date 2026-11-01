package com.whisk.domain.rag.chunking

data class ChunkingConfig(
    val maxChunkSizeChars: Int = 800,
    val minChunkSizeChars: Int = 50,
    val chunkOverlapChars: Int = 100,
)

data class DocumentChunk(
    val id: String,
    val sourceObjectId: String,
    val sourceObjectType: String,
    val sourceTitle: String,
    val headingPath: String,
    val content: String,
    val contentHash: String,
    val sequence: Int,
    val startOffset: Int,
    val endOffset: Int,
    val updatedAt: Long,
)
