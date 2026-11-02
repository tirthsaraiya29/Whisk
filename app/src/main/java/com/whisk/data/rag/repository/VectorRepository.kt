package com.whisk.data.rag.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.whisk.data.local.rag.DocumentChunkDao
import com.whisk.data.local.rag.DocumentChunkEntity
import com.whisk.data.local.rag.EmbeddingVectorDao
import com.whisk.data.local.rag.EmbeddingVectorEntity
import com.whisk.data.rag.embedding.LocalEmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VectorSearchResult(
    val chunk: DocumentChunkEntity,
    val score: Float,
)

class VectorRepository(
    private val chunkDao: DocumentChunkDao,
    private val vectorDao: EmbeddingVectorDao,
    private val database: RoomDatabase? = null,
) {

    suspend fun saveChunkVectors(chunks: List<DocumentChunkEntity>, vectors: List<EmbeddingVectorEntity>) {
        if (database != null) {
            database.withTransaction {
                if (chunks.isNotEmpty()) chunkDao.insertChunks(chunks)
                if (vectors.isNotEmpty()) vectorDao.insertVectors(vectors)
            }
        } else {
            if (chunks.isNotEmpty()) chunkDao.insertChunks(chunks)
            if (vectors.isNotEmpty()) vectorDao.insertVectors(vectors)
        }
    }

    suspend fun replaceObjectVectorsAtomic(
        objectId: String,
        chunks: List<DocumentChunkEntity>,
        vectors: List<EmbeddingVectorEntity>,
    ) {
        if (database != null) {
            database.withTransaction {
                chunkDao.deleteChunksForObject(objectId)
                vectorDao.deleteVectorsForObject(objectId)
                if (chunks.isNotEmpty()) chunkDao.insertChunks(chunks)
                if (vectors.isNotEmpty()) vectorDao.insertVectors(vectors)
            }
        } else {
            chunkDao.deleteChunksForObject(objectId)
            vectorDao.deleteVectorsForObject(objectId)
            if (chunks.isNotEmpty()) chunkDao.insertChunks(chunks)
            if (vectors.isNotEmpty()) vectorDao.insertVectors(vectors)
        }
    }

    suspend fun deleteObjectVectors(objectId: String) {
        if (database != null) {
            database.withTransaction {
                chunkDao.deleteChunksForObject(objectId)
                vectorDao.deleteVectorsForObject(objectId)
            }
        } else {
            chunkDao.deleteChunksForObject(objectId)
            vectorDao.deleteVectorsForObject(objectId)
        }
    }

    suspend fun getAllChunksDirect(): List<DocumentChunkEntity> {
        return chunkDao.getAllChunksDirect()
    }

    suspend fun deleteChunkById(id: String) {
        chunkDao.deleteChunkById(id)
    }

    suspend fun getAllVectorsForModel(modelId: String): List<EmbeddingVectorEntity> {
        return vectorDao.getAllVectorsForModel(modelId)
    }

    suspend fun getAllVectorsDirect(): List<EmbeddingVectorEntity> {
        return vectorDao.getAllVectorsDirect()
    }

    suspend fun deleteVectorByChunkId(chunkId: String) {
        vectorDao.deleteVectorByChunkId(chunkId)
    }

    suspend fun clearAll() {
        if (database != null) {
            database.withTransaction {
                chunkDao.deleteAllChunks()
                vectorDao.deleteAllVectors()
            }
        } else {
            chunkDao.deleteAllChunks()
            vectorDao.deleteAllVectors()
        }
    }

    suspend fun searchSimilarChunks(
        queryVector: FloatArray,
        modelId: String,
        topK: Int = 20,
        typeIdFilter: String? = null,
        minScoreThreshold: Float = 0.1f,
    ): List<VectorSearchResult> = withContext(Dispatchers.Default) {
        val allVectors = vectorDao.getAllVectorsForModel(modelId)
        if (allVectors.isEmpty()) return@withContext emptyList()

        val scoredResults = mutableListOf<VectorSearchResult>()

        // Load chunks map in batch
        val chunkIdsToFetch = allVectors.map { it.chunkId }
        val chunksMap = chunkDao.getChunksByIds(chunkIdsToFetch).associateBy { it.id }

        for (vEntity in allVectors) {
            val chunk = chunksMap[vEntity.chunkId] ?: continue

            if (typeIdFilter != null && chunk.sourceObjectType != typeIdFilter) {
                continue
            }

            val chunkVector = vEntity.toFloatArray()
            val score = LocalEmbeddingProvider.cosineSimilarity(queryVector, chunkVector)

            if (score >= minScoreThreshold) {
                scoredResults.add(VectorSearchResult(chunk = chunk, score = score))
            }
        }

        scoredResults.sortedByDescending { it.score }.take(topK)
    }
}
