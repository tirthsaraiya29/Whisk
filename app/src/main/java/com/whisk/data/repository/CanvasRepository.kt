package com.whisk.data.repository

import com.whisk.data.local.CanvasDao
import com.whisk.data.local.CanvasEntity
import com.whisk.domain.model.CanvasCard
import com.whisk.domain.model.CanvasConnection
import com.whisk.domain.model.SpatialCanvas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CanvasRepository(private val canvasDao: CanvasDao) {

    private val json = Json { ignoreUnknownKeys = true }

    val allCanvases: Flow<List<SpatialCanvas>> = canvasDao.getAllCanvases().map { entities ->
        entities.map { entity ->
            val cards: List<CanvasCard> = try {
                json.decodeFromString(entity.cardsJson)
            } catch (e: Exception) {
                emptyList()
            }
            val connections: List<CanvasConnection> = try {
                json.decodeFromString(entity.connectionsJson)
            } catch (e: Exception) {
                emptyList()
            }
            SpatialCanvas(
                id = entity.id,
                title = entity.title,
                cards = cards,
                connections = connections,
                updatedAt = entity.updatedAt
            )
        }
    }

    suspend fun getCanvasById(id: String): SpatialCanvas? {
        val entity = canvasDao.getCanvasById(id) ?: return null
        val cards: List<CanvasCard> = try {
            json.decodeFromString(entity.cardsJson)
        } catch (e: Exception) {
            emptyList()
        }
        val connections: List<CanvasConnection> = try {
            json.decodeFromString(entity.connectionsJson)
        } catch (e: Exception) {
            emptyList()
        }
        return SpatialCanvas(
            id = entity.id,
            title = entity.title,
            cards = cards,
            connections = connections,
            updatedAt = entity.updatedAt
        )
    }

    suspend fun saveCanvas(canvas: SpatialCanvas) {
        val cardsJson = json.encodeToString(canvas.cards)
        val connectionsJson = json.encodeToString(canvas.connections)
        val entity = CanvasEntity(
            id = canvas.id,
            title = canvas.title,
            cardsJson = cardsJson,
            connectionsJson = connectionsJson,
            updatedAt = System.currentTimeMillis()
        )
        canvasDao.insertCanvas(entity)
    }

    suspend fun deleteCanvas(id: String) {
        canvasDao.deleteCanvas(id)
    }
}
