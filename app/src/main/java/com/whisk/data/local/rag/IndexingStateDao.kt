package com.whisk.data.local.rag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IndexingStateDao {
    @Query("SELECT * FROM indexing_states WHERE objectId = :objectId LIMIT 1")
    suspend fun getIndexingState(objectId: String): IndexingStateEntity?

    @Query("SELECT * FROM indexing_states")
    suspend fun getAllIndexingStates(): List<IndexingStateEntity>

    @Query("SELECT * FROM indexing_states")
    fun getAllIndexingStatesFlow(): Flow<List<IndexingStateEntity>>

    @Query("SELECT COUNT(*) FROM indexing_states WHERE status = 'INDEXED'")
    suspend fun getIndexedCount(): Int

    @Query("SELECT COUNT(*) FROM indexing_states WHERE status = 'INDEXED'")
    fun getIndexedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM indexing_states WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM indexing_states WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM indexing_states WHERE status = 'FAILED'")
    suspend fun getFailedCount(): Int

    @Query("SELECT COUNT(*) FROM indexing_states WHERE status = 'FAILED'")
    fun getFailedCountFlow(): Flow<Int>

    @Query("SELECT MAX(lastIndexedAt) FROM indexing_states WHERE status = 'INDEXED'")
    fun getLastIndexedTimeFlow(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndexingState(state: IndexingStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndexingStates(states: List<IndexingStateEntity>)

    @Query("DELETE FROM indexing_states WHERE objectId = :objectId")
    suspend fun deleteIndexingState(objectId: String): Int

    @Query("DELETE FROM indexing_states")
    suspend fun deleteAllIndexingStates(): Int
}
