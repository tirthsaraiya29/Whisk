package com.whisk.data.rag.repository

import com.whisk.data.local.rag.DocumentChunkDao
import com.whisk.data.local.rag.IndexingStateDao
import com.whisk.data.local.rag.IndexingStateEntity
import kotlinx.coroutines.flow.Flow

class IndexingRepository(
    private val indexingStateDao: IndexingStateDao,
    private val chunkDao: DocumentChunkDao,
) {

    val indexedCount: Flow<Int> = indexingStateDao.getIndexedCountFlow()
    val pendingCount: Flow<Int> = indexingStateDao.getPendingCountFlow()
    val failedCount: Flow<Int> = indexingStateDao.getFailedCountFlow()
    val totalChunks: Flow<Int> = chunkDao.getTotalChunkCountFlow()
    val lastIndexedAt: Flow<Long?> = indexingStateDao.getLastIndexedTimeFlow()

    suspend fun getIndexingState(objectId: String): IndexingStateEntity? {
        return indexingStateDao.getIndexingState(objectId)
    }

    suspend fun getAllIndexingStates(): List<IndexingStateEntity> {
        return indexingStateDao.getAllIndexingStatesDirect()
    }

    suspend fun saveIndexingState(state: IndexingStateEntity) {
        indexingStateDao.insertIndexingState(state)
    }

    suspend fun deleteIndexingState(objectId: String) {
        indexingStateDao.deleteIndexingState(objectId)
    }

    suspend fun clearAllState() {
        indexingStateDao.deleteAllIndexingStates()
        chunkDao.deleteAllChunks()
    }
}
