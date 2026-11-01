package com.whisk.domain.rag.retrieval

data class RetrievedChunk(
    val chunkId: String,
    val sourceObjectId: String,
    val sourceObjectType: String,
    val sourceTitle: String,
    val headingPath: String,
    val content: String,
    val rrfScore: Float,
    val ftsRank: Int? = null,
    val vectorRank: Int? = null,
    val vectorScore: Float? = null,
) {
    val citationHandle: String
        get() = "$sourceTitle > $headingPath"
}
