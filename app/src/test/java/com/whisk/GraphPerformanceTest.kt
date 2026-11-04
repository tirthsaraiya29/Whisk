package com.whisk

import com.whisk.data.local.NoteDao
import com.whisk.data.local.NoteEntity
import com.whisk.data.repository.GraphRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.system.measureTimeMillis

class FakeNoteDaoForGraph(private val notes: List<NoteEntity>) : NoteDao {
    override fun getAllNotes(): Flow<List<NoteEntity>> = flowOf(notes)
    override suspend fun getAllNotesDirect(): List<NoteEntity> = notes
    override fun getFavoriteNotes(): Flow<List<NoteEntity>> = flowOf(emptyList())
    override fun getArchivedNotes(): Flow<List<NoteEntity>> = flowOf(emptyList())
    override fun getNotesByFolder(folder: String): Flow<List<NoteEntity>> = flowOf(emptyList())
    override fun getNotesByTypeId(typeId: String): Flow<List<NoteEntity>> = flowOf(emptyList())
    override suspend fun getNoteById(id: String): NoteEntity? = notes.find { it.id == id }
    override suspend fun getNoteByTitle(title: String): NoteEntity? = notes.find { it.title == title }
    override suspend fun getObjectsForPicker(query: String, typeId: String?, limit: Int): List<NoteEntity> = emptyList()
    override suspend fun updateObjectType(id: String, typeId: String): Int = 1
    override suspend fun softDeleteNote(id: String, timestamp: Long): Int = 1
    override suspend fun restoreNote(id: String): Int = 1
    override fun getTrashedNotes(): Flow<List<NoteEntity>> = flowOf(emptyList())
    override suspend fun purgeOldTrashedNotes(olderThanTimestamp: Long): Int = 0
    override suspend fun togglePin(id: String, isPinned: Boolean): Int = 1
    override suspend fun updateAccentColor(id: String, accentColor: String?): Int = 1
    override suspend fun insertNote(note: NoteEntity) {}
    override suspend fun updateNote(note: NoteEntity) {}
    override suspend fun deleteNoteById(id: String): Int = 0
    override suspend fun getNoteCount(): Int = notes.size
    override fun searchNotes(query: String): Flow<List<NoteEntity>> = flowOf(emptyList())
    override fun searchNotesFts(query: String): Flow<List<NoteEntity>> = flowOf(emptyList())
}

@OptIn(ExperimentalCoroutinesApi::class)
class GraphPerformanceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createTestNote(id: String, title: String, body: String): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            body = body,
            createdAt = 1000L,
            updatedAt = 1000L,
            folder = null,
            tags = "",
            isFavorite = false,
            isArchived = false,
            sourceMetadata = null,
            repoRef = null,
        )
    }

    @Test
    fun testGraphConstruction100Notes() = runTest {
        val notes = (1..100).map { i ->
            val target = if (i > 1) "[[Note ${i - 1}]]" else ""
            createTestNote("n_$i", "Note $i", "Body $i $target")
        }

        val repository = GraphRepository(noteDao = FakeNoteDaoForGraph(notes))
        val elapsed = measureTimeMillis {
            val graph = repository.buildGlobalGraph()
            assertEquals(100, graph.nodes.size)
            assertEquals(99, graph.edges.size)
        }

        assertTrue("100 notes graph should take < 500ms, took $elapsed ms", elapsed < 500)
    }

    @Test
    fun testGraphConstruction1000NotesScalability() = runTest {
        val notes = (1..1000).map { i ->
            val target = if (i > 1) "[[Note ${i - 1}]]" else ""
            createTestNote("n_$i", "Note $i", "Body text for note $i with link $target")
        }

        val repository = GraphRepository(noteDao = FakeNoteDaoForGraph(notes))
        val elapsed = measureTimeMillis {
            val graph = repository.buildGlobalGraph()
            assertEquals(1000, graph.nodes.size)
            assertEquals(999, graph.edges.size)
        }

        assertTrue("1000 notes graph should take < 2000ms, took $elapsed ms", elapsed < 2000)
    }

    @Test
    fun testGraphConstruction5000NotesScalability() = runTest {
        val notes = (1..5000).map { i ->
            val target = if (i > 1) "[[Note ${i - 1}]]" else ""
            createTestNote("n_$i", "Note $i", "Body text for note $i with link $target")
        }

        val repository = GraphRepository(noteDao = FakeNoteDaoForGraph(notes))
        val elapsed = measureTimeMillis {
            val graph = repository.buildGlobalGraph()
            assertEquals(5000, graph.nodes.size)
            assertEquals(4999, graph.edges.size)
        }

        assertTrue("5000 notes graph should take < 5000ms, took $elapsed ms", elapsed < 5000)
    }

    @Test
    fun testGraphConstruction10000NotesScalability() = runTest {
        val notes = (1..10000).map { i ->
            val target = if (i > 1) "[[Note ${i - 1}]]" else ""
            createTestNote("n_$i", "Note $i", "Body text for note $i with link $target")
        }

        val repository = GraphRepository(noteDao = FakeNoteDaoForGraph(notes))
        val elapsed = measureTimeMillis {
            val graph = repository.buildGlobalGraph()
            assertEquals(10000, graph.nodes.size)
            assertEquals(9999, graph.edges.size)
        }

        assertTrue("10000 notes graph should take < 10000ms, took $elapsed ms", elapsed < 10000)
    }

    @Test
    fun testOrphanDetectionSinglePass() = runTest {
        val connected1 = createTestNote("c1", "Connected 1", "Links to [[Connected 2]]")
        val connected2 = createTestNote("c2", "Connected 2", "Just text")
        val orphan = createTestNote("o1", "Orphan Note", "No links anywhere")

        val repository = GraphRepository(noteDao = FakeNoteDaoForGraph(listOf(connected1, connected2, orphan)))
        val graph = repository.buildGlobalGraph()

        val orphanNode = graph.nodes.find { it.id == "o1" }
        val connectedNode1 = graph.nodes.find { it.id == "c1" }
        val connectedNode2 = graph.nodes.find { it.id == "c2" }

        assertTrue(orphanNode?.isOrphan == true)
        assertFalse(connectedNode1?.isOrphan == true)
        assertFalse(connectedNode2?.isOrphan == true)
    }
}
