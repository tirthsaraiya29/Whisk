package com.whisk.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class CardType {
    NOTE_CARD, NEW_NOTE, TEXT_BLOCK
}

@Serializable
data class CanvasCard(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String? = null,
    val title: String,
    val content: String,
    val x: Float,
    val y: Float,
    val width: Float = 220f,
    val height: Float = 140f,
    val type: CardType = CardType.NOTE_CARD,
    val colorHex: String? = null,
)

@Serializable
data class CanvasConnection(
    val id: String = UUID.randomUUID().toString(),
    val fromCardId: String,
    val toCardId: String,
    val label: String? = null,
)

@Serializable
data class SpatialCanvas(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val cards: List<CanvasCard> = emptyList(),
    val connections: List<CanvasConnection> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
)
