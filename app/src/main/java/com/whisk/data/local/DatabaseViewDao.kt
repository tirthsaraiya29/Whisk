package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseViewDao {
    @Query("SELECT * FROM database_views ORDER BY position ASC, createdAt ASC")
    fun getAllViews(): Flow<List<DatabaseViewEntity>>

    @Query("SELECT * FROM database_views")
    suspend fun getAllViewsDirect(): List<DatabaseViewEntity>

    @Query("SELECT * FROM database_views WHERE targetTypeId = :targetTypeId OR (targetTypeId IS NULL AND :targetTypeId IS NULL) ORDER BY position ASC")
    fun getViewsForTarget(targetTypeId: String?): Flow<List<DatabaseViewEntity>>

    @Query("SELECT * FROM database_views WHERE id = :id LIMIT 1")
    suspend fun getViewById(id: String): DatabaseViewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertView(view: DatabaseViewEntity)

    @Query("DELETE FROM database_views WHERE id = :id")
    suspend fun deleteViewById(id: String): Int
}
