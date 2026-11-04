package com.whisk

import com.whisk.domain.rag.intelligence.KnowledgeAnalysisEngine
import com.whisk.domain.rag.retrieval.RetrievedChunk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeAnalysisEngineTest {

    @Test
    fun testContradictionDetection() = runBlocking {
        val engine = KnowledgeAnalysisEngine(fakeNoteDao())

        val chunkA = RetrievedChunk(
            chunkId = "1",
            sourceObjectId = "n1",
            sourceObjectType = "note",
            sourceTitle = "Note A",
            headingPath = "Config",
            content = "Server port: 8080",
            rrfScore = 0.5f,
        )

        val chunkB = RetrievedChunk(
            chunkId = "2",
            sourceObjectId = "n2",
            sourceObjectType = "note",
            sourceTitle = "Note B",
            headingPath = "Config",
            content = "Server port: 9090",
            rrfScore = 0.5f,
        )

        val contradictions = engine.detectContradictions(listOf(chunkA, chunkB))
        assertTrue(contradictions.isNotEmpty())
        assertEquals("Port Configuration Conflict", contradictions.first().topic)
    }

    private fun fakeNoteDao(): com.whisk.data.local.NoteDao {
        return object : com.whisk.data.local.NoteDao {
            override fun getAllNotes(): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun getNoteById(id: String): com.whisk.data.local.NoteEntity? = null
            override suspend fun getNoteByTitle(title: String): com.whisk.data.local.NoteEntity? = null
            override fun getFavoriteNotes(): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override fun getArchivedNotes(): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override fun getNotesByFolder(folder: String): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override fun searchNotes(query: String): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override fun searchNotesFts(query: String): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun insertNote(note: com.whisk.data.local.NoteEntity) {}
            override suspend fun updateNote(note: com.whisk.data.local.NoteEntity) {}
            override suspend fun deleteNoteById(id: String): Int = 0
            override suspend fun getNoteCount(): Int = 0
            override suspend fun getAllNotesDirect(): List<com.whisk.data.local.NoteEntity> = emptyList()
            override fun getNotesByTypeId(typeId: String): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun getObjectsForPicker(query: String, typeId: String?, limit: Int): List<com.whisk.data.local.NoteEntity> = emptyList()
            override suspend fun updateObjectType(id: String, typeId: String): Int = 0
            override suspend fun softDeleteNote(id: String, timestamp: Long): Int = 1
            override suspend fun restoreNote(id: String): Int = 1
            override fun getTrashedNotes(): kotlinx.coroutines.flow.Flow<List<com.whisk.data.local.NoteEntity>> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun purgeOldTrashedNotes(olderThanTimestamp: Long): Int = 0
            override suspend fun togglePin(id: String, isPinned: Boolean): Int = 1
            override suspend fun updateAccentColor(id: String, accentColor: String?): Int = 1
        }
    }
}
