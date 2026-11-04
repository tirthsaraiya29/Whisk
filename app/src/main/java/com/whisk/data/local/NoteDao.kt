package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title = :title LIMIT 1")
    suspend fun getNoteByTitle(title: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isArchived = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folder = :folder AND isArchived = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>>

    @Suppress("Unused", "UNUSED_PARAMETER")
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Suppress("Unused", "UNUSED_PARAMETER")
    @Query(
        """
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query AND notes.isArchived = 0 AND notes.deletedAt IS NULL
        ORDER BY notes.isPinned DESC, notes.updatedAt DESC
    """,
    )
    fun searchNotesFts(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String): Int

    @Query("UPDATE notes SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteNote(id: String, timestamp: Long): Int

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: String): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :olderThanTimestamp")
    suspend fun purgeOldTrashedNotes(olderThanTimestamp: Long): Int

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun togglePin(id: String, isPinned: Boolean): Int

    @Query("UPDATE notes SET accentColor = :accentColor WHERE id = :id")
    suspend fun updateAccentColor(id: String, accentColor: String?): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 0 AND deletedAt IS NULL")
    suspend fun getNoteCount(): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL")
    suspend fun getAllNotesDirect(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE typeId = :typeId AND isArchived = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByTypeId(typeId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE (:typeId IS NULL OR typeId = :typeId) AND (:query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND isArchived = 0 AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC LIMIT :limit")
    suspend fun getObjectsForPicker(query: String, typeId: String?, limit: Int = 50): List<NoteEntity>

    @Query("UPDATE notes SET typeId = :typeId WHERE id = :id")
    suspend fun updateObjectType(id: String, typeId: String): Int
}
