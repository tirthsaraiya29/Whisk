package com.whisk.data.local.rag

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_chunks",
    indices = [
        Index(value = ["sourceObjectId"]),
        Index(value = ["sourceObjectType"]),
    ],
)
data class DocumentChunkEntity(
    @PrimaryKey val id: String,
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
