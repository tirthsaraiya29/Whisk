package com.whisk.data.repository

import com.whisk.domain.model.Note
import com.whisk.domain.model.ParsedWikilink

object WikilinkParser {

    private val WIKILINK_REGEX = Regex("\\[\\[(.*?)]]")

    fun parseWikilinks(content: String): List<ParsedWikilink> {
        val matches = WIKILINK_REGEX.findAll(content)
        val result = mutableListOf<ParsedWikilink>()

        for (match in matches) {
            val raw = match.groupValues[1].trim()
            if (raw.isBlank()) continue

            val parts = raw.split("|", limit = 2)
            val targetTitle = parts[0].trim()
            val alias = if (parts.size > 1) parts[1].trim() else null

            result.add(
                ParsedWikilink(
                    rawText = match.value,
                    targetTitle = targetTitle,
                    alias = alias,
                ),
            )
        }
        return result
    }

    fun resolveWikilink(
        targetTitleOrAlias: String,
        allNotes: List<Note>,
    ): Note? {
        val clean = targetTitleOrAlias.trim()
        if (clean.isBlank()) return null

        // 1. Exact ID match
        val byId = allNotes.firstOrNull { it.id == clean }
        if (byId != null) return byId

        // 2. Exact Title match (case-insensitive)
        val byTitleExact = allNotes.firstOrNull { it.title.equals(clean, ignoreCase = true) }
        if (byTitleExact != null) return byTitleExact

        // 3. Alias match (case-insensitive)
        val byAlias = allNotes.firstOrNull { note ->
            note.aliases.any { it.equals(clean, ignoreCase = true) }
        }
        if (byAlias != null) return byAlias

        // 4. Fallback: Title contains query
        return allNotes.firstOrNull { it.title.contains(clean, ignoreCase = true) }
    }
}
