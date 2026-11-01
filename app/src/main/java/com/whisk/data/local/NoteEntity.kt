package com.whisk.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.whisk.domain.model.Note

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["typeId"]),
    ],
)
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
    val repoRef: String?,
    @ColumnInfo(defaultValue = "''") val aliases: String = "",
    @ColumnInfo(defaultValue = "'DRAFT'") val status: String? = "DRAFT",
    @ColumnInfo(defaultValue = "'note'") val typeId: String = "note",
    @ColumnInfo(defaultValue = "NULL") val description: String? = null,
    @ColumnInfo(defaultValue = "NULL") val icon: String? = null,
    @ColumnInfo(defaultValue = "NULL") val coverImage: String? = null,
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            title = title,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt,
            folder = folder,
            tags = if (tags.isBlank()) emptyList() else tags.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList(),
            isFavorite = isFavorite,
            isArchived = isArchived,
            sourceMetadata = sourceMetadata,
            repoRef = repoRef,
            aliases = if (aliases.isBlank()) emptyList() else aliases.split(",").asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList(),
            status = status,
            typeId = typeId,
            description = description,
            icon = icon,
            coverImage = coverImage,
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
                repoRef = note.repoRef,
                aliases = note.aliases.joinToString(","),
                status = note.status,
                typeId = note.typeId,
                description = note.description,
                icon = note.icon,
                coverImage = note.coverImage,
            )
        }
    }
}
