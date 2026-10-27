package com.whisk.domain.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val folder: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val sourceMetadata: String? = null,
    val repoRef: String? = null
)

data class NoteLink(
    val sourceNoteId: String,
    val targetTitle: String,
    val targetNoteId: String? = null,
    val isBroken: Boolean = false
)

data class Backlink(
    val sourceNoteId: String,
    val sourceNoteTitle: String,
    val snippet: String,
    val updatedAt: Long
)

data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentId: String? = null,
    val noteCount: Int = 0
)

data class Tag(
    val name: String,
    val noteCount: Int = 0
)
