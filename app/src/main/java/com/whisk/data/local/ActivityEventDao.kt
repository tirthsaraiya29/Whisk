package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {
    @Query("SELECT * FROM activity_events WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun getEventsForDate(dateKey: String): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_events WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    suspend fun getEventsForDateDirect(dateKey: String): List<ActivityEventEntity>

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_events")
    suspend fun getAllEventsDirect(): List<ActivityEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEventEntity)

    @Query("DELETE FROM activity_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteEventsOlderThan(cutoffTimestamp: Long): Int
}
