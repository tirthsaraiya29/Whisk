package com.whisk.domain.rag.retrieval

import com.whisk.data.local.NoteDao
import com.whisk.data.rag.repository.VectorRepository
import com.whisk.domain.rag.chunking.DocumentChunker
import com.whisk.domain.rag.embedding.EmbeddingProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class RetrievalFilter(
    val typeId: String? = null,
    val projectRef: String? = null,
    val maxChunksPerObject: Int = 3,
    val minRrfScore: Float = 0.005f,
)

sealed class SearchResult {
    data class Success(
        val chunks: List<RetrievedChunk>,
        val isDegraded: Boolean = false,
        val degradedReason: String? = null,
    ) : SearchResult()

    data class Failure(
        val error: Throwable,
        val message: String,
    ) : SearchResult()
}

class HybridSearchEngine(
    private val noteDao: NoteDao,
    private val vectorRepository: VectorRepository,
    private val embeddingProvider: EmbeddingProvider,
) {

    suspend fun search(
        query: String,
        topK: Int = 10,
        filter: RetrievalFilter = RetrievalFilter(),
    ): SearchResult = withContext(Dispatchers.Default) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext SearchResult.Success(emptyList())

        var ftsError: Throwable? = null
        var vectorError: Throwable? = null

        // 1. Keyword search via FTS
        val ftsNotes = try {
            noteDao.searchNotesFts(cleanQuery).first()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            ftsError = e
            emptyList()
        }

        val ftsChunks = mutableListOf<RetrievedChunk>()
        ftsNotes.forEachIndexed { rankIdx, note ->
            if (filter.typeId != null && note.typeId != filter.typeId) return@forEachIndexed
            val existingChunks = vectorRepository.getChunksForObject(note.id)
            if (existingChunks.isNotEmpty()) {
                existingChunks.take(filter.maxChunksPerObject).forEach { chunk ->
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
            } else {
                ftsChunks.add(
                    RetrievedChunk(
                        chunkId = "fts_${note.id}",
                        sourceObjectId = note.id,
                        sourceObjectType = note.typeId,
                        sourceTitle = note.title,
                        headingPath = "",
                        content = note.body.take(500),
                        rrfScore = 0f,
                        ftsRank = rankIdx + 1,
                    ),
                )
            }
        }

        // 2. Local semantic vector search
        var vectorResults = emptyList<com.whisk.data.rag.repository.VectorSearchResult>()
        try {
            val queryVector = embeddingProvider.generateEmbedding(cleanQuery)
            vectorResults = vectorRepository.searchSimilarChunks(
                queryVector = queryVector,
                modelId = embeddingProvider.modelId,
                topK = topK * 2,
                typeIdFilter = filter.typeId,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            vectorError = e
        }

        if (ftsError != null && vectorError != null) {
            val combinedError = vectorError ?: ftsError ?: IllegalStateException("Search error")
            return@withContext SearchResult.Failure(
                error = combinedError,
                message = "Hybrid search failed (FTS: ${ftsError?.message}, Vector: ${vectorError?.message})",
            )
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
        val candidateChunks = mutableListOf<RetrievedChunk>()

        fusedList.sortedByDescending { it.rrfScore }.forEach { chunk ->
            if (chunk.rrfScore < filter.minRrfScore) return@forEach
            val currentCount = objectChunkCounts[chunk.sourceObjectId] ?: 0
            if (currentCount < filter.maxChunksPerObject) {
                objectChunkCounts[chunk.sourceObjectId] = currentCount + 1
                candidateChunks.add(chunk)
            }
        }

        // 5. On-Device Cross-Encoder Reranking Stage (Term affinity, heading bonus, semantic fusion)
        val queryTerms = cleanQuery.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        val rerankedChunks = if (queryTerms.isNotEmpty() && candidateChunks.isNotEmpty()) {
            candidateChunks.map { chunk ->
                val textLower = chunk.content.lowercase()
                val titleLower = chunk.sourceTitle.lowercase()
                val pathLower = chunk.headingPath.lowercase()

                var matches = 0
                for (term in queryTerms) {
                    if (titleLower.contains(term)) matches += 2
                    if (pathLower.contains(term)) matches += 1
                    if (textLower.contains(term)) matches += 1
                }

                val exactMatchBonus = if (cleanQuery.length > 3 && textLower.contains(cleanQuery.lowercase())) 0.25f else 0.0f
                val termCoverage = (matches.toFloat() / (queryTerms.size * 2)).coerceIn(0f, 1f)
                val baseScore = chunk.rrfScore
                val semanticScore = chunk.vectorScore ?: 0f

                val crossEncoderScore = (baseScore * 0.45f) + (termCoverage * 0.35f) + (semanticScore * 0.20f) + exactMatchBonus
                chunk.copy(rrfScore = crossEncoderScore)
            }.sortedByDescending { it.rrfScore }
        } else {
            candidateChunks
        }

        val isDegraded = vectorError != null
        val degradedReason = if (vectorError != null) "Vector embedding search unavailable: ${vectorError.message}. Keyword FTS fallback used." else null

        SearchResult.Success(
            chunks = rerankedChunks.take(topK),
            isDegraded = isDegraded,
            degradedReason = degradedReason,
        )
    }
}
