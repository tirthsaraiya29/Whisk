package com.whisk.vault

data class VaultUsageStats(
    val totalNotes: Int,
    val totalSizeBytes: Long,
    val lastModified: Long,
    val lastAccessed: Long
)
