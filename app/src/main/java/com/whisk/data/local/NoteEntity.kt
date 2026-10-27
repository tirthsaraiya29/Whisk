package com.whisk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whisk.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val folder: String?,
    val tags: String,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val sourceMetadata: String?,
    val repoRef: String?
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            title = title,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt,
            folder = folder,
            tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            isFavorite = isFavorite,
            isArchived = isArchived,
            sourceMetadata = sourceMetadata,
            repoRef = repoRef
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                title = note.title,
                body = note.body,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                folder = note.folder,
                tags = note.tags.joinToString(","),
                isFavorite = note.isFavorite,
                isArchived = note.isArchived,
                sourceMetadata = note.sourceMetadata,
                repoRef = note.repoRef
            )
        }
    }
}
