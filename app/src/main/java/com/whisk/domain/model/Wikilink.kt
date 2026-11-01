package com.whisk.domain.model

data class ParsedWikilink(
    val rawText: String,
    val targetTitle: String,
    val alias: String?,
    val isBroken: Boolean = false,
    val targetNoteId: String? = null,
)

data class UnlinkedMention(
    val id: String,
    val sourceNoteId: String,
    val sourceNoteTitle: String,
    val targetNoteId: String,
    val targetNoteTitle: String,
    val matchedText: String,
    val snippet: String,
    val updatedAt: Long,
)

data class BrokenLinkItem(
    val id: String,
    val sourceNoteId: String,
    val sourceNoteTitle: String,
    val rawWikilinkText: String,
    val targetTitle: String,
    val updatedAt: Long,
)
