package com.whisk.data.repository

import com.whisk.domain.model.Note
import com.whisk.domain.model.UnlinkedMention
import java.util.UUID

object UnlinkedMentionDetector {

    fun findUnlinkedMentions(
        sourceNote: Note,
        allNotes: List<Note>,
    ): List<UnlinkedMention> {
        val body = sourceNote.body
        if (body.isBlank()) return emptyList()

        // Strip code blocks and explicit wikilinks to avoid false positives inside code or existing links
        val cleanBody = stripCodeAndLinks(body)

        val mentions = mutableListOf<UnlinkedMention>()

        for (target in allNotes) {
            // Ignore self
            if (target.id == sourceNote.id) continue

            // Candidate strings: title and non-empty aliases
            val candidates = mutableListOf(target.title)
            candidates.addAll(target.aliases.filter { it.isNotBlank() })

            for (candidate in candidates) {
                if (candidate.length < 2) continue // Skip single char titles/aliases to avoid noise

                val pattern = Regex("\\b${Regex.escape(candidate)}\\b", RegexOption.IGNORE_CASE)
                val matches = pattern.findAll(cleanBody)

                for (match in matches) {
                    val start = (match.range.first - 30).coerceAtLeast(0)
                    val end = (match.range.last + 30).coerceAtMost(cleanBody.length)
                    val snippet = "..." + cleanBody.substring(start, end).replace("\n", " ") + "..."

                    // Ensure we don't add duplicate mentions for the same target and matched snippet
                    val isDuplicate = mentions.any { it.targetNoteId == target.id && it.snippet == snippet }
                    if (!isDuplicate) {
                        mentions.add(
                            UnlinkedMention(
                                id = UUID.randomUUID().toString(),
                                sourceNoteId = sourceNote.id,
                                sourceNoteTitle = sourceNote.title,
                                targetNoteId = target.id,
                                targetNoteTitle = target.title,
                                matchedText = match.value,
                                snippet = snippet,
                                updatedAt = sourceNote.updatedAt,
                            ),
                        )
                    }
                }
            }
        }

        return mentions.sortedByDescending { it.updatedAt }
    }

    fun convertMentionToWikilink(
        body: String,
        matchedText: String,
        targetTitle: String,
    ): String {
        val pattern = Regex("\\b${Regex.escape(matchedText)}\\b", RegexOption.IGNORE_CASE)
        val replacement = if (matchedText.equals(targetTitle, ignoreCase = true)) {
            "[[$targetTitle]]"
        } else {
            "[[$targetTitle|$matchedText]]"
        }

        return pattern.replaceFirst(body, replacement)
    }

    private fun stripCodeAndLinks(text: String): String {
        // 1. Strip code blocks ``` ... ```
        var result = text.replace(Regex("```[\\s\\S]*?```"), " ")
        // 2. Strip inline code ` ... `
        result = result.replace(Regex("`[^`]*`"), " ")
        // 3. Strip existing wikilinks [[ ... ]]
        result = result.replace(Regex("\\[\\[[^\\]]*\\]\\]"), " ")
        return result
    }
}
