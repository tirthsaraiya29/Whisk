package com.whisk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvases")
data class CanvasEntity(
    @PrimaryKey val id: String,
    val title: String,
    val cardsJson: String,
    val connectionsJson: String,
    val updatedAt: Long,
)
