package com.whisk.data.local.rag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentChunkDao {
    @Query("SELECT * FROM document_chunks WHERE id = :id LIMIT 1")
    suspend fun getChunkById(id: String): DocumentChunkEntity?

    @Query("SELECT * FROM document_chunks WHERE sourceObjectId = :objectId ORDER BY sequence ASC")
    suspend fun getChunksForObject(objectId: String): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE id IN (:chunkIds)")
    suspend fun getChunksByIds(chunkIds: List<String>): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE sourceObjectType = :objectType")
    suspend fun getChunksByObjectType(objectType: String): List<DocumentChunkEntity>

    @Query("SELECT * FROM document_chunks")
    suspend fun getAllChunksDirect(): List<DocumentChunkEntity>

    @Query("SELECT COUNT(*) FROM document_chunks")
    suspend fun getTotalChunkCount(): Int

    @Query("SELECT COUNT(*) FROM document_chunks")
    fun getTotalChunkCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)

    @Query("DELETE FROM document_chunks WHERE sourceObjectId = :objectId")
    suspend fun deleteChunksForObject(objectId: String): Int

    @Query("DELETE FROM document_chunks WHERE id = :id")
    suspend fun deleteChunkById(id: String): Int

    @Query("DELETE FROM document_chunks")
    suspend fun deleteAllChunks(): Int
}
