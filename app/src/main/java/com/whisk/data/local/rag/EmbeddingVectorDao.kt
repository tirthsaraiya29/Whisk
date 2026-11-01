package com.whisk.data.local.rag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmbeddingVectorDao {
    @Query("SELECT * FROM embedding_vectors WHERE chunkId = :chunkId LIMIT 1")
    suspend fun getVectorByChunkId(chunkId: String): EmbeddingVectorEntity?

    @Query("SELECT * FROM embedding_vectors WHERE modelId = :modelId")
    suspend fun getAllVectorsForModel(modelId: String): List<EmbeddingVectorEntity>

    @Query("SELECT * FROM embedding_vectors WHERE sourceObjectId = :objectId")
    suspend fun getVectorsForObject(objectId: String): List<EmbeddingVectorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVectors(vectors: List<EmbeddingVectorEntity>)

    @Query("DELETE FROM embedding_vectors WHERE sourceObjectId = :objectId")
    suspend fun deleteVectorsForObject(objectId: String): Int

    @Query("DELETE FROM embedding_vectors WHERE modelId = :modelId")
    suspend fun deleteVectorsForModel(modelId: String): Int

    @Query("DELETE FROM embedding_vectors")
    suspend fun deleteAllVectors(): Int
}
