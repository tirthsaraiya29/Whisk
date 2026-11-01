package com.whisk.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.whisk.domain.model.DailyNoteMetadata

@Entity(
    tableName = "daily_note_metadata",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["dateKey"], unique = true),
        Index(value = ["noteId"]),
    ],
)
data class DailyNoteMetadataEntity(
    @PrimaryKey val noteId: String,
    val dateKey: String, // "YYYY-MM-DD"
    val timezone: String,
    val createdAt: Long,
) {
    fun toDomain(): DailyNoteMetadata {
        return DailyNoteMetadata(
            noteId = noteId,
            dateKey = dateKey,
            timezone = timezone,
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomain(domain: DailyNoteMetadata): DailyNoteMetadataEntity {
            return DailyNoteMetadataEntity(
                noteId = domain.noteId,
                dateKey = domain.dateKey,
                timezone = domain.timezone,
                createdAt = domain.createdAt,
            )
        }
    }
}
