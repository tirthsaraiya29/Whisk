package com.whisk.domain.rag.intelligence

import com.whisk.data.local.NoteDao
import com.whisk.domain.model.Note
import com.whisk.domain.rag.retrieval.RetrievedChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class KnowledgeGapSuggestion(
    val objectId: String,
    val objectTitle: String,
    val objectType: String,
    val gapType: String,
    val suggestionText: String,
)

data class KnowledgeContradictionSuggestion(
    val topic: String,
    val sourceAId: String,
    val sourceATitle: String,
    val excerptA: String,
    val sourceBId: String,
    val sourceBTitle: String,
    val excerptB: String,
    val explanation: String,
)

data class AutoLinkSuggestion(
    val sourceNoteId: String,
    val sourceNoteTitle: String,
    val targetNoteId: String,
    val targetNoteTitle: String,
    val matchedText: String,
    val confidence: Float,
    val reason: String,
)

class KnowledgeAnalysisEngine(
    private val noteDao: NoteDao,
) {

    suspend fun detectKnowledgeGaps(): List<KnowledgeGapSuggestion> = withContext(Dispatchers.Default) {
        val notes = noteDao.getAllNotesDirect().map { it.toDomain() }.filter { !it.isArchived }
        val suggestions = mutableListOf<KnowledgeGapSuggestion>()

        for (note in notes) {
            // Check empty body or minimal documentation
            if (note.body.trim().length < 50) {
                suggestions.add(
                    KnowledgeGapSuggestion(
                        objectId = note.id,
                        objectTitle = note.title,
                        objectType = note.typeId,
                        gapType = "Minimal Content",
                        suggestionText = "Object '${note.title}' has very brief content. Consider expanding details or adding architecture documentation.",
                    ),
                )
            }

            // Projects or Repositories without description
            if ((note.typeId.lowercase() in listOf("project", "repository", "book", "course")) && note.description.isNullOrBlank()) {
                suggestions.add(
                    KnowledgeGapSuggestion(
                        objectId = note.id,
                        objectTitle = note.title,
                        objectType = note.typeId,
                        gapType = "Missing Description",
                        suggestionText = "${note.typeId.replaceFirstChar { it.uppercase() }} '${note.title}' lacks a summary description.",
                    ),
                )
            }
        }

        suggestions
    }

    suspend fun detectContradictions(chunks: List<RetrievedChunk>): List<KnowledgeContradictionSuggestion> = withContext(Dispatchers.Default) {
        val contradictions = mutableListOf<KnowledgeContradictionSuggestion>()
        if (chunks.size < 2) return@withContext contradictions

        // Group chunks by topic/heading or compare pairs
        for (i in chunks.indices) {
            for (j in i + 1 until chunks.size) {
                val cA = chunks[i]
                val cB = chunks[j]
                if (cA.sourceObjectId == cB.sourceObjectId) continue

                // Check for status or numerical conflict pattern
                val textA = cA.content.lowercase()
                val textB = cB.content.lowercase()

                if ((textA.contains("port") && textB.contains("port")) && textA != textB) {
                    val portRegex = Regex("port\\s*[:=]?\\s*(\\d+)")
                    val matchA = portRegex.find(textA)?.groupValues?.get(1)
                    val matchB = portRegex.find(textB)?.groupValues?.get(1)
                    if (matchA != null && matchB != null && matchA != matchB) {
                        contradictions.add(
                            KnowledgeContradictionSuggestion(
                                topic = "Port Configuration Conflict",
                                sourceAId = cA.sourceObjectId,
                                sourceATitle = cA.sourceTitle,
                                excerptA = cA.content.take(120),
                                sourceBId = cB.sourceObjectId,
                                sourceBTitle = cB.sourceTitle,
                                excerptB = cB.content.take(120),
                                explanation = "Found conflicting port declarations ($matchA in ${cA.sourceTitle} vs $matchB in ${cB.sourceTitle})",
                            ),
                        )
                    }
                }
            }
        }

        contradictions
    }

    suspend fun suggestAutoLinks(currentNote: Note): List<AutoLinkSuggestion> = withContext(Dispatchers.Default) {
        val allNotes = noteDao.getAllNotesDirect().map { it.toDomain() }.filter { it.id != currentNote.id && !it.isArchived }
        val suggestions = mutableListOf<AutoLinkSuggestion>()

        for (other in allNotes) {
            if (other.title.isBlank() || other.title.length < 3) continue
            // Check if current note contains unlinked title mention
            val titlePattern = Regex("\\b${Regex.escape(other.title)}\\b", RegexOption.IGNORE_CASE)
            val match = titlePattern.find(currentNote.body)
            val isAlreadyLinked = currentNote.body.contains("[[${other.title}]]", ignoreCase = true)

            if (match != null && !isAlreadyLinked) {
                suggestions.add(
                    AutoLinkSuggestion(
                        sourceNoteId = currentNote.id,
                        sourceNoteTitle = currentNote.title,
                        targetNoteId = other.id,
                        targetNoteTitle = other.title,
                        matchedText = match.value,
                        confidence = 0.9f,
                        reason = "Found unlinked mention of '${other.title}' in text.",
                    ),
                )
            }
        }

        suggestions.take(10)
    }
}
