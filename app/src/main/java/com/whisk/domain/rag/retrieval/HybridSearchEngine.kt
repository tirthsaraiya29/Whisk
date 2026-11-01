package com.whisk.domain.rag.retrieval

import com.whisk.data.local.NoteDao
import com.whisk.data.rag.repository.VectorRepository
import com.whisk.domain.rag.chunking.DocumentChunker
import com.whisk.domain.rag.embedding.EmbeddingProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class RetrievalFilter(
    val typeId: String? = null,
    val projectRef: String? = null,
    val maxChunksPerObject: Int = 3,
    val minRrfScore: Float = 0.005f,
)

class HybridSearchEngine(
    private val noteDao: NoteDao,
    private val vectorRepository: VectorRepository,
    private val embeddingProvider: EmbeddingProvider,
) {

    suspend fun search(
        query: String,
        topK: Int = 10,
        filter: RetrievalFilter = RetrievalFilter(),
    ): List<RetrievedChunk> = withContext(Dispatchers.Default) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        // 1. Keyword search via FTS
        val ftsNotes = try {
            noteDao.searchNotesFts(cleanQuery).first()
        } catch (_: Exception) {
            emptyList()
        }

        // Chunk FTS notes dynamically or match chunk text
        val ftsChunks = mutableListOf<RetrievedChunk>()
        ftsNotes.forEachIndexed { rankIdx, note ->
            if (filter.typeId != null && note.typeId != filter.typeId) return@forEachIndexed
            val chunks = DocumentChunker.chunkDocument(
                objectId = note.id,
                objectType = note.typeId,
                title = note.title,
                body = note.body,
                description = note.description,
            )
            chunks.take(filter.maxChunksPerObject).forEach { chunk ->
                ftsChunks.add(
                    RetrievedChunk(
                        chunkId = chunk.id,
                        sourceObjectId = chunk.sourceObjectId,
                        sourceObjectType = chunk.sourceObjectType,
                        sourceTitle = chunk.sourceTitle,
                        headingPath = chunk.headingPath,
                        content = chunk.content,
                        rrfScore = 0f,
                        ftsRank = rankIdx + 1,
                    ),
                )
            }
        }

        // 2. Local semantic vector search
        val queryVector = embeddingProvider.generateEmbedding(cleanQuery)
        val vectorResults = try {
            vectorRepository.searchSimilarChunks(
                queryVector = queryVector,
                modelId = embeddingProvider.modelId,
                topK = topK * 2,
                typeIdFilter = filter.typeId,
            )
        } catch (_: Exception) {
            emptyList()
        }

        // 3. Reciprocal Rank Fusion (RRF)
        val scoreMap = mutableMapOf<String, RetrievedChunk>()
        val ftsRanks = mutableMapOf<String, Int>()
        val vectorRanks = mutableMapOf<String, Int>()

        ftsChunks.forEachIndexed { index, chunk ->
            ftsRanks[chunk.chunkId] = index + 1
            scoreMap[chunk.chunkId] = chunk
        }

        vectorResults.forEachIndexed { index, vRes ->
            val chunk = vRes.chunk
            vectorRanks[chunk.id] = index + 1
            if (!scoreMap.containsKey(chunk.id)) {
                scoreMap[chunk.id] = RetrievedChunk(
                    chunkId = chunk.id,
                    sourceObjectId = chunk.sourceObjectId,
                    sourceObjectType = chunk.sourceObjectType,
                    sourceTitle = chunk.sourceTitle,
                    headingPath = chunk.headingPath,
                    content = chunk.content,
                    rrfScore = 0f,
                    vectorScore = vRes.score,
                )
            }
        }

        // Calculate RRF scores: 1 / (k + rank)
        val k = 60f
        val fusedList = scoreMap.map { (chunkId, chunk) ->
            val ftsRank = ftsRanks[chunkId]
            val vectorRank = vectorRanks[chunkId]

            val ftsScore = if (ftsRank != null) 1.0f / (k + ftsRank) else 0.0f
            val vectorScore = if (vectorRank != null) 1.0f / (k + vectorRank) else 0.0f
            val totalRrf = ftsScore + vectorScore

            chunk.copy(
                rrfScore = totalRrf,
                ftsRank = ftsRank,
                vectorRank = vectorRank,
            )
        }

        // 4. Deduplicate & apply chunk limit per object
        val objectChunkCounts = mutableMapOf<String, Int>()
        val finalChunks = mutableListOf<RetrievedChunk>()

        fusedList.sortedByDescending { it.rrfScore }.forEach { chunk ->
            if (chunk.rrfScore < filter.minRrfScore) return@forEach
            val currentCount = objectChunkCounts[chunk.sourceObjectId] ?: 0
            if (currentCount < filter.maxChunksPerObject) {
                objectChunkCounts[chunk.sourceObjectId] = currentCount + 1
                finalChunks.add(chunk)
            }
        }

        finalChunks.take(topK)
    }
}
