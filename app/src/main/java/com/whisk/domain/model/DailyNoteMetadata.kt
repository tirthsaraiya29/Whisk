package com.whisk.domain.model

data class DailyNoteMetadata(
    val noteId: String,
    val dateKey: String, // "YYYY-MM-DD"
    val timezone: String = "UTC",
    val createdAt: Long = System.currentTimeMillis(),
)
