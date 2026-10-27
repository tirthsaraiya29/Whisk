package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasDao {
    @Query("SELECT * FROM canvases ORDER BY updatedAt DESC")
    fun getAllCanvases(): Flow<List<CanvasEntity>>

    @Query("SELECT * FROM canvases WHERE id = :id LIMIT 1")
    suspend fun getCanvasById(id: String): CanvasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvas(canvas: CanvasEntity)

    @Query("DELETE FROM canvases WHERE id = :id")
    suspend fun deleteCanvas(id: String): Int
}
