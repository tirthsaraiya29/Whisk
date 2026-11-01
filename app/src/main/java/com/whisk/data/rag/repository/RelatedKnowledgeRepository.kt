package com.whisk.data.rag.repository

import com.whisk.data.local.NoteDao
import com.whisk.data.local.RelationshipDao
import com.whisk.domain.model.Note
import com.whisk.domain.rag.embedding.EmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RelatedKnowledgeItem(
    val note: Note,
    val score: Float,
    val matchType: String,
    val explanation: String,
)

class RelatedKnowledgeRepository(
    private val noteDao: NoteDao,
    private val relationshipDao: RelationshipDao,
    private val vectorRepository: VectorRepository,
    private val embeddingProvider: EmbeddingProvider,
) {

    suspend fun getRelatedKnowledgeForNote(
        noteId: String,
        limit: Int = 5,
    ): List<RelatedKnowledgeItem> = withContext(Dispatchers.Default) {
        val currentNote = noteDao.getNoteById(noteId)?.toDomain() ?: return@withContext emptyList()
        val allNotes = noteDao.getAllNotesDirect().map { it.toDomain() }.filter { it.id != noteId && !it.isArchived }

        val resultsMap = mutableMapOf<String, RelatedKnowledgeItem>()

        // 1. Check explicit relationships
        val relationships = relationshipDao.getRelationshipsForObjectDirect(noteId)
        for (rel in relationships) {
            val otherId = if (rel.sourceObjectId == noteId) rel.targetObjectId else rel.sourceObjectId
            val otherNote = allNotes.find { it.id == otherId } ?: continue
            resultsMap[otherId] = RelatedKnowledgeItem(
                note = otherNote,
                score = 1.0f,
                matchType = "Explicit Relationship",
                explanation = "Connected via ${rel.relationshipType} relationship",
            )
        }

        // 2. Check semantic similarity using vector embeddings
        val fullText = "${currentNote.title}\n${currentNote.description.orEmpty()}\n${currentNote.body}"
        val queryVector = embeddingProvider.generateEmbedding(fullText)

        val vectorHits = try {
            vectorRepository.searchSimilarChunks(
                queryVector = queryVector,
                modelId = embeddingProvider.modelId,
                topK = limit * 2,
            )
        } catch (_: Exception) {
            emptyList()
        }

        for (hit in vectorHits) {
            val targetId = hit.chunk.sourceObjectId
            if (targetId == noteId || resultsMap.containsKey(targetId)) continue
            val targetNote = allNotes.find { it.id == targetId } ?: continue

            resultsMap[targetId] = RelatedKnowledgeItem(
                note = targetNote,
                score = hit.score,
                matchType = "Semantic Similarity",
                explanation = "High content relevance (${(hit.score * 100).toInt()}% match) in '${hit.chunk.headingPath}'",
            )
        }

        // 3. Shared tags check
        val currentTags = currentNote.tags.toSet()
        if (currentTags.isNotEmpty()) {
            for (note in allNotes) {
                if (resultsMap.containsKey(note.id)) continue
                val shared = note.tags.toSet().intersect(currentTags)
                if (shared.isNotEmpty()) {
                    resultsMap[note.id] = RelatedKnowledgeItem(
                        note = note,
                        score = 0.5f,
                        matchType = "Shared Tags",
                        explanation = "Shares tags: ${shared.joinToString(", ")}",
                    )
                }
            }
        }

        resultsMap.values.sortedByDescending { it.score }.take(limit)
    }
}
