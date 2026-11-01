package com.whisk

import com.whisk.data.local.ActivityEventEntity
import com.whisk.data.local.DailyNoteMetadataEntity
import com.whisk.data.repository.ActivityEventDto
import com.whisk.data.repository.BackupData
import com.whisk.data.repository.DailyNoteMetadataDto
import com.whisk.domain.model.ActivityEvent
import com.whisk.domain.model.ActivityType
import com.whisk.domain.model.DailyNoteMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyNotesTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testDailyNoteMetadataEntityToDomainRoundtrip() {
        val domain = DailyNoteMetadata(
            noteId = "daily_note_2026-11-02",
            dateKey = "2026-11-02",
            timezone = "America/New_York",
        )

        val entity = DailyNoteMetadataEntity.fromDomain(domain)
        assertEquals("daily_note_2026-11-02", entity.noteId)
        assertEquals("2026-11-02", entity.dateKey)

        val restored = entity.toDomain()
        assertEquals(domain.noteId, restored.noteId)
        assertEquals(domain.dateKey, restored.dateKey)
        assertEquals(domain.timezone, restored.timezone)
    }

    @Test
    fun testActivityEventEntityToDomainRoundtrip() {
        val domain = ActivityEvent(
            id = "event_123",
            eventType = ActivityType.TASK_COMPLETED,
            dateKey = "2026-11-02",
            relatedObjectId = "task_456",
            relatedObjectTitle = "Implement Room migration 6 to 7",
            metadata = "Task checked off in daily notes",
        )

        val entity = ActivityEventEntity.fromDomain(domain)
        assertEquals("TASK_COMPLETED", entity.eventType)
        assertEquals("2026-11-02", entity.dateKey)

        val restored = entity.toDomain()
        assertEquals(ActivityType.TASK_COMPLETED, restored.eventType)
        assertEquals("2026-11-02", restored.dateKey)
        assertEquals("Implement Room migration 6 to 7", restored.relatedObjectTitle)
    }

    @Test
    fun testBackupDataSerializationWithDailyNotesAndActivityEvents() {
        val dailyDto = DailyNoteMetadataDto(
            noteId = "daily_note_2026-11-02",
            dateKey = "2026-11-02",
            timezone = "UTC",
            createdAt = 1000L,
        )

        val activityDto = ActivityEventDto(
            id = "act_1",
            eventType = "NOTE_CREATED",
            timestamp = 1000L,
            dateKey = "2026-11-02",
            relatedObjectId = "daily_note_2026-11-02",
            relatedObjectTitle = "Monday, November 2, 2026",
            metadata = "Created daily note",
        )

        val backup = BackupData(
            dailyNotes = listOf(dailyDto),
            activityEvents = listOf(activityDto),
        )

        val serialized = json.encodeToString(backup)
        assertTrue(serialized.contains("dailyNotes"))
        assertTrue(serialized.contains("activityEvents"))

        val restored = json.decodeFromString<BackupData>(serialized)
        assertEquals(1, restored.dailyNotes.size)
        assertEquals("2026-11-02", restored.dailyNotes[0].dateKey)
        assertEquals(1, restored.activityEvents.size)
        assertEquals("NOTE_CREATED", restored.activityEvents[0].eventType)
    }
}
