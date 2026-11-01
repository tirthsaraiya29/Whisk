package com.whisk.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_sessions",
    indices = [
        Index(value = ["timestamp"]),
    ],
)
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long,
    val provider: String? = null,
    val model: String? = null,
    val contextRef: String? = null,
)
