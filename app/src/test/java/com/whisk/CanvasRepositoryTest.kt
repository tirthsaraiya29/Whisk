package com.whisk

import com.whisk.domain.model.CanvasCard
import com.whisk.domain.model.CardType
import com.whisk.domain.model.SpatialCanvas
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSpatialCanvasSerialization() {
        val card = CanvasCard(
            id = "c1",
            title = "Idea 1",
            content = "Details",
            x = 100f,
            y = 200f,
            type = CardType.NOTE_CARD,
        )
        val canvas = SpatialCanvas(id = "canvas1", title = "Design Space", cards = listOf(card))

        val cardsJson = json.encodeToString(canvas.cards)
        val decodedCards: List<CanvasCard> = json.decodeFromString(cardsJson)

        assertEquals(1, decodedCards.size)
        assertEquals("Idea 1", decodedCards[0].title)
        assertEquals(100f, decodedCards[0].x)
        assertEquals(200f, decodedCards[0].y)
    }
}
