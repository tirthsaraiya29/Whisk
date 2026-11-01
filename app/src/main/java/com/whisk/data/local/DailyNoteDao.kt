package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNoteDao {
    @Query("SELECT * FROM daily_note_metadata WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyNoteMetadata(dateKey: String): DailyNoteMetadataEntity?

    @Query("SELECT notes.* FROM notes JOIN daily_note_metadata ON notes.id = daily_note_metadata.noteId WHERE daily_note_metadata.dateKey = :dateKey LIMIT 1")
    suspend fun getNoteForDateKey(dateKey: String): NoteEntity?

    @Query("SELECT notes.* FROM notes JOIN daily_note_metadata ON notes.id = daily_note_metadata.noteId WHERE daily_note_metadata.dateKey = :dateKey LIMIT 1")
    fun observeNoteForDateKey(dateKey: String): Flow<NoteEntity?>

    @Query("SELECT * FROM daily_note_metadata ORDER BY dateKey DESC")
    fun getAllDailyNotes(): Flow<List<DailyNoteMetadataEntity>>

    @Query("SELECT * FROM daily_note_metadata")
    suspend fun getAllDailyNotesDirect(): List<DailyNoteMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyNoteMetadata(metadata: DailyNoteMetadataEntity): Long
}
