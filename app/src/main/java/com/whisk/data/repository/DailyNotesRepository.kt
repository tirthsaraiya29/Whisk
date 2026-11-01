package com.whisk.data.repository

import androidx.room.withTransaction
import com.whisk.data.local.ActivityEventDao
import com.whisk.data.local.ActivityEventEntity
import com.whisk.data.local.DailyNoteDao
import com.whisk.data.local.DailyNoteMetadataEntity
import com.whisk.data.local.NoteDao
import com.whisk.data.local.NoteEntity
import com.whisk.data.local.TaskDao
import com.whisk.data.local.WhiskDatabase
import com.whisk.domain.model.ActivityEvent
import com.whisk.domain.model.ActivityType
import com.whisk.domain.model.Note
import com.whisk.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class DailyNotesRepository(
    private val database: WhiskDatabase,
    private val dailyNoteDao: DailyNoteDao,
    private val activityEventDao: ActivityEventDao,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
) {

    val defaultTemplate = """
# Daily Journal

## Goals

## Notes and Ideas

## Tasks

## What I Learned

## Reflection
    """.trimIndent()

    fun getTodayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    suspend fun getOrCreateDailyNote(
        dateKey: String = getTodayDateKey(),
        templateMarkdown: String = defaultTemplate,
    ): Note {
        // 1. Check if canonical daily note exists for dateKey
        val existingNote = dailyNoteDao.getNoteForDateKey(dateKey)
        if (existingNote != null) {
            return existingNote.toDomain()
        }

        // 2. Atomic creation of new Daily Note
        return database.withTransaction {
            val doubleCheck = dailyNoteDao.getNoteForDateKey(dateKey)
            if (doubleCheck != null) {
                return@withTransaction doubleCheck.toDomain()
            }

            val newNoteId = "daily_note_$dateKey"
            val title = formatDailyNoteTitle(dateKey)

            val noteEntity = NoteEntity(
                id = newNoteId,
                title = title,
                body = templateMarkdown,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                folder = "DAILY",
                tags = "daily-note",
                isFavorite = false,
                isArchived = false,
                sourceMetadata = null,
                repoRef = null,
                aliases = dateKey,
                status = "DONE",
                typeId = "note",
            )
            noteDao.insertNote(noteEntity)

            val metadata = DailyNoteMetadataEntity(
                noteId = newNoteId,
                dateKey = dateKey,
                timezone = TimeZone.getDefault().id,
                createdAt = System.currentTimeMillis(),
            )
            dailyNoteDao.insertDailyNoteMetadata(metadata)

            logActivity(
                eventType = ActivityType.NOTE_CREATED,
                dateKey = dateKey,
                relatedObjectId = newNoteId,
                relatedObjectTitle = title,
                metadata = "Created canonical daily note for $dateKey",
            )

            noteEntity.toDomain()
        }
    }

    fun observeDailyNote(dateKey: String): Flow<Note?> {
        return dailyNoteDao.observeNoteForDateKey(dateKey).map { it?.toDomain() }
    }

    fun getDailyActivityEvents(dateKey: String): Flow<List<ActivityEvent>> {
        return activityEventDao.getEventsForDate(dateKey).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getDailyTasks(): List<TaskItem> {
        val allTasks = taskDao.getAllTasksDirect()
        return allTasks.map { it.toDomain() }
    }

    suspend fun logActivity(
        eventType: ActivityType,
        dateKey: String = getTodayDateKey(),
        relatedObjectId: String? = null,
        relatedObjectTitle: String? = null,
        metadata: String? = null,
    ) {
        val event = ActivityEventEntity(
            id = UUID.randomUUID().toString(),
            eventType = eventType.name,
            timestamp = System.currentTimeMillis(),
            dateKey = dateKey,
            relatedObjectId = relatedObjectId,
            relatedObjectTitle = relatedObjectTitle,
            metadata = metadata,
        )
        activityEventDao.insertEvent(event)
    }

    private fun formatDailyNoteTitle(dateKey: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = parser.parse(dateKey) ?: return dateKey
            val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        } catch (_: Exception) {
            dateKey
        }
    }
}
