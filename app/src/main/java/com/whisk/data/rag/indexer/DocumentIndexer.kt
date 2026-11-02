package com.whisk.data.rag.indexer

import com.whisk.data.local.NoteDao
import com.whisk.data.local.NotePropertyValueEntity
import com.whisk.data.local.ObjectRelationshipEntity
import com.whisk.data.local.PropertyDao
import com.whisk.data.local.RelationshipDao
import com.whisk.data.local.rag.DocumentChunkEntity
import com.whisk.data.local.rag.EmbeddingVectorEntity
import com.whisk.data.local.rag.IndexingStateEntity
import com.whisk.data.local.rag.IndexingStatus
import com.whisk.data.rag.embedding.EmbeddingModelManager
import com.whisk.data.rag.embedding.LocalEmbeddingProvider
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

data class ReconciliationResult(
    val removedOrphanNotes: Int = 0,
    val removedOrphanChunks: Int = 0,
    val removedOrphanVectors: Int = 0,
)

class DocumentIndexer(
    private val noteDao: NoteDao,
    private val relationshipDao: RelationshipDao,
    private val vectorRepository: VectorRepository,
    private val indexingRepository: IndexingRepository,
    private val embeddingModelManager: EmbeddingModelManager,
    private val propertyDao: PropertyDao? = null,
) {

    private val _progress = MutableStateFlow(IndexingProgress())
    val progress: StateFlow<IndexingProgress> = _progress.asStateFlow()

    companion object {
        const val PIPELINE_VERSION = "v1"

        fun computeFingerprint(
            noteId: String,
            title: String,
            description: String?,
            body: String,
            tags: String,
            typeId: String,
            relationships: List<ObjectRelationshipEntity> = emptyList(),
            propertyValues: List<NotePropertyValueEntity> = emptyList(),
            modelId: String,
            pipelineVersion: String = PIPELINE_VERSION,
            tokenizerVersion: String = "bert-wordpiece-v1",
            dimensions: Int = 384,
        ): String {
            val sortedRels = relationships
                .map { "${it.relationshipType}:${it.targetObjectId}" }
                .sorted()
                .joinToString(";")

            val sortedProps = propertyValues
                .map { "${it.propertyDefinitionId}:${it.textValue.orEmpty()}:${it.numberValue ?: 0}:${it.booleanValue ?: false}:${it.listValueJson.orEmpty()}" }
                .sorted()
                .joinToString(";")

            val rawContent = buildString {
                append("v=").append(pipelineVersion).append("\n")
                append("model=").append(modelId).append("\n")
                append("tok=").append(tokenizerVersion).append("\n")
                append("dim=").append(dimensions).append("\n")
                append("id=").append(noteId).append("\n")
                append("type=").append(typeId).append("\n")
                append("title=").append(title).append("\n")
                append("desc=").append(description.orEmpty()).append("\n")
                append("body=").append(body).append("\n")
                append("tags=").append(tags).append("\n")
                append("rels=").append(sortedRels).append("\n")
                append("props=").append(sortedProps).append("\n")
            }

            return DocumentChunker.computeHash(rawContent)
        }
    }

    suspend fun reconcileOrphans(): ReconciliationResult = withContext(Dispatchers.IO) {
        val activeNotes = noteDao.getAllNotesDirect().filter { !it.isArchived }
        val activeNoteIds = activeNotes.map { it.id }.toSet()

        var removedNotesCount = 0
        var removedChunksCount = 0
        var removedVectorsCount = 0

        // 1. Remove indexing states & vectors for notes that no longer exist or are archived
        val allStates = indexingRepository.getAllIndexingStates()
        for (state in allStates) {
            if (state.objectId !in activeNoteIds) {
                vectorRepository.deleteObjectVectors(state.objectId)
                indexingRepository.deleteIndexingState(state.objectId)
                removedNotesCount++
            }
        }

        // 2. Remove orphaned chunks whose source note doesn't exist
        val allChunks = vectorRepository.getAllChunksDirect()
        val validChunkIds = mutableSetOf<String>()
        for (chunk in allChunks) {
            if (chunk.sourceObjectId !in activeNoteIds) {
                vectorRepository.deleteChunkById(chunk.id)
                removedChunksCount++
            } else {
                validChunkIds.add(chunk.id)
            }
        }

        // 3. Remove orphaned vectors across ALL model IDs
        val allVectors = vectorRepository.getAllVectorsDirect()
        for (vec in allVectors) {
            if (vec.sourceObjectId !in activeNoteIds || vec.chunkId !in validChunkIds) {
                vectorRepository.deleteVectorByChunkId(vec.chunkId)
                removedVectorsCount++
            }
        }

        ReconciliationResult(
            removedOrphanNotes = removedNotesCount,
            removedOrphanChunks = removedChunksCount,
            removedOrphanVectors = removedVectorsCount,
        )
    }

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
            // Clean up any orphans first
            reconcileOrphans()

            // Ensure model is initialized
            var activeProvider = embeddingModelManager.getActiveProvider()
            if (activeProvider is LocalEmbeddingProvider && !activeProvider.isModelReady) {
                embeddingModelManager.initializeModel()
                activeProvider = embeddingModelManager.getActiveProvider()
            }

            if (activeProvider is LocalEmbeddingProvider && !activeProvider.isModelReady) {
                _progress.value = _progress.value.copy(
                    isIndexing = false,
                    lastErrorText = "Local embedding model is not ready for inference.",
                )
                return@withContext
            }

            val allNotes = noteDao.getAllNotesDirect().filter { !it.isArchived }
            val total = allNotes.size

            _progress.value = _progress.value.copy(totalEligible = total)

            val currentModelId = activeProvider.modelId

            if (forceReindex) {
                vectorRepository.clearAll()
                indexingRepository.clearAllState()
            }

            var processedCount = 0

            for (note in allNotes) {
                _progress.value = _progress.value.copy(currentObjectTitle = note.title)

                val relationships = relationshipDao.getRelationshipsForObjectDirect(note.id)
                val propertyValues = propertyDao?.getPropertyValuesForNoteDirect(note.id) ?: emptyList()

                val currentFingerprint = computeFingerprint(
                    noteId = note.id,
                    title = note.title,
                    description = note.description,
                    body = note.body,
                    tags = note.tags,
                    typeId = note.typeId,
                    relationships = relationships,
                    propertyValues = propertyValues,
                    modelId = currentModelId,
                    dimensions = activeProvider.dimensions,
                )

                val existingState = indexingRepository.getIndexingState(note.id)

                if (!forceReindex && existingState != null && existingState.contentHash == currentFingerprint && existingState.status == IndexingStatus.INDEXED) {
                    processedCount++
                    onProgress?.invoke(processedCount, total)
                    _progress.value = _progress.value.copy(indexedCount = processedCount)
                    continue
                }

                try {
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

                    val chunkEntities = mutableListOf<DocumentChunkEntity>()
                    val vectorEntities = mutableListOf<EmbeddingVectorEntity>()

                    if (chunks.isNotEmpty()) {
                        val chunkTexts = chunks.map { it.content }
                        val floatVectors = activeProvider.generateEmbeddings(chunkTexts)

                        chunkEntities.addAll(chunks.map { chunk ->
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
                        })

                        vectorEntities.addAll(chunks.mapIndexed { idx, chunk ->
                            EmbeddingVectorEntity.fromFloatArray(
                                chunkId = chunk.id,
                                sourceObjectId = chunk.sourceObjectId,
                                modelId = currentModelId,
                                floatArray = floatVectors[idx],
                                contentHash = chunk.contentHash,
                                updatedAt = chunk.updatedAt,
                            )
                        })
                    }

                    // Replace vectors atomically ONLY after embedding generation completes successfully
                    vectorRepository.replaceObjectVectorsAtomic(note.id, chunkEntities, vectorEntities)

                    indexingRepository.saveIndexingState(
                        IndexingStateEntity(
                            objectId = note.id,
                            objectType = note.typeId,
                            contentHash = currentFingerprint,
                            status = IndexingStatus.INDEXED,
                            chunkCount = chunks.size,
                            lastIndexedAt = System.currentTimeMillis(),
                        ),
                    )

                    processedCount++
                    onProgress?.invoke(processedCount, total)
                    _progress.value = _progress.value.copy(indexedCount = processedCount)
                } catch (e: Exception) {
                    // Do NOT delete previous valid vectors if generation failed!
                    indexingRepository.saveIndexingState(
                        IndexingStateEntity(
                            objectId = note.id,
                            objectType = note.typeId,
                            contentHash = currentFingerprint,
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
