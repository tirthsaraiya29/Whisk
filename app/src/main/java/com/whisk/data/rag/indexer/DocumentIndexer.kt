package com.whisk.data.rag.indexer

import com.whisk.data.local.NoteDao
import com.whisk.data.local.RelationshipDao
import com.whisk.data.local.rag.DocumentChunkEntity
import com.whisk.data.local.rag.EmbeddingVectorEntity
import com.whisk.data.local.rag.IndexingStateEntity
import com.whisk.data.local.rag.IndexingStatus
import com.whisk.data.rag.embedding.EmbeddingModelManager
import com.whisk.data.rag.repository.IndexingRepository
import com.whisk.data.rag.repository.VectorRepository
import com.whisk.domain.rag.chunking.DocumentChunker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class IndexingProgress(
    val isIndexing: Boolean = false,
    val totalEligible: Int = 0,
    val indexedCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val currentObjectTitle: String = "",
    val lastErrorText: String? = null,
)

class DocumentIndexer(
    private val noteDao: NoteDao,
    private val relationshipDao: RelationshipDao,
    private val vectorRepository: VectorRepository,
    private val indexingRepository: IndexingRepository,
    private val embeddingModelManager: EmbeddingModelManager,
) {

    private val _progress = MutableStateFlow(IndexingProgress())
    val progress: StateFlow<IndexingProgress> = _progress.asStateFlow()

    suspend fun indexWorkspace(
        forceReindex: Boolean = false,
        onProgress: ((indexed: Int, total: Int) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
        if (_progress.value.isIndexing) return@withContext

        _progress.value = _progress.value.copy(
            isIndexing = true,
            lastErrorText = null,
        )

        try {
            val allNotes = noteDao.getAllNotesDirect().filter { !it.isArchived }
            val total = allNotes.size

            _progress.value = _progress.value.copy(totalEligible = total)

            if (forceReindex) {
                vectorRepository.clearAll()
                indexingRepository.clearAllState()
            }

            val embeddingProvider = embeddingModelManager.getActiveProvider()
            val currentModelId = embeddingProvider.modelId

            var processedCount = 0

            for (note in allNotes) {
                _progress.value = _progress.value.copy(currentObjectTitle = note.title)

                val fullTextForHash = "${note.title}\n${note.description.orEmpty()}\n${note.body}\n${note.tags}"
                val currentHash = DocumentChunker.computeHash(fullTextForHash)

                val existingState = indexingRepository.getIndexingState(note.id)

                if (!forceReindex && existingState != null && existingState.contentHash == currentHash && existingState.status == IndexingStatus.INDEXED) {
                    processedCount++
                    onProgress?.invoke(processedCount, total)
                    _progress.value = _progress.value.copy(indexedCount = processedCount)
                    continue
                }

                // Index or re-index this object
                try {
                    // Remove old chunks/vectors first
                    vectorRepository.deleteObjectVectors(note.id)

                    // Fetch relationship context if any
                    val relationships = relationshipDao.getRelationshipsForObjectDirect(note.id)
                    val relSnippet = if (relationships.isNotEmpty()) {
                        "\nRelationships: " + relationships.joinToString(", ") { "${it.relationshipType} -> ${it.targetObjectId}" }
                    } else ""

                    val chunks = DocumentChunker.chunkDocument(
                        objectId = note.id,
                        objectType = note.typeId,
                        title = note.title,
                        body = note.body + relSnippet,
                        description = note.description,
                        updatedAt = note.updatedAt,
                    )

                    if (chunks.isNotEmpty()) {
                        val chunkTexts = chunks.map { it.content }
                        val floatVectors = embeddingProvider.generateEmbeddings(chunkTexts)

                        val chunkEntities = chunks.map { chunk ->
                            DocumentChunkEntity(
                                id = chunk.id,
                                sourceObjectId = chunk.sourceObjectId,
                                sourceObjectType = chunk.sourceObjectType,
                                sourceTitle = chunk.sourceTitle,
                                headingPath = chunk.headingPath,
                                content = chunk.content,
                                contentHash = chunk.contentHash,
                                sequence = chunk.sequence,
                                startOffset = chunk.startOffset,
                                endOffset = chunk.endOffset,
                                updatedAt = chunk.updatedAt,
                            )
                        }

                        val vectorEntities = chunks.mapIndexed { idx, chunk ->
                            EmbeddingVectorEntity.fromFloatArray(
                                chunkId = chunk.id,
                                sourceObjectId = chunk.sourceObjectId,
                                modelId = currentModelId,
                                floatArray = floatVectors[idx],
                                contentHash = chunk.contentHash,
                                updatedAt = chunk.updatedAt,
                            )
                        }

                        vectorRepository.saveChunkVectors(chunkEntities, vectorEntities)
                    }

                    indexingRepository.saveIndexingState(
                        IndexingStateEntity(
                            objectId = note.id,
                            objectType = note.typeId,
                            contentHash = currentHash,
                            status = IndexingStatus.INDEXED,
                            chunkCount = chunks.size,
                            lastIndexedAt = System.currentTimeMillis(),
                        ),
                    )

                    processedCount++
                    onProgress?.invoke(processedCount, total)
                    _progress.value = _progress.value.copy(indexedCount = processedCount)
                } catch (e: Exception) {
                    indexingRepository.saveIndexingState(
                        IndexingStateEntity(
                            objectId = note.id,
                            objectType = note.typeId,
                            contentHash = currentHash,
                            status = IndexingStatus.FAILED,
                            chunkCount = 0,
                            lastIndexedAt = System.currentTimeMillis(),
                            failureReason = e.message,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            _progress.value = _progress.value.copy(lastErrorText = e.message)
        } finally {
            _progress.value = _progress.value.copy(
                isIndexing = false,
                currentObjectTitle = "",
            )
        }
    }

    suspend fun clearIndex() = withContext(Dispatchers.IO) {
        vectorRepository.clearAll()
        indexingRepository.clearAllState()
        _progress.value = IndexingProgress()
    }
}
