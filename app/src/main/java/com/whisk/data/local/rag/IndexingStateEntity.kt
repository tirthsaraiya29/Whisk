package com.whisk.data.local.rag

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IndexingStatus {
    PENDING,
    INDEXED,
    FAILED,
}

@Entity(tableName = "indexing_states")
data class IndexingStateEntity(
    @PrimaryKey val objectId: String,
    val objectType: String,
    val contentHash: String,
    val status: IndexingStatus,
    val chunkCount: Int,
    val lastIndexedAt: Long,
    val failureReason: String? = null,
)
