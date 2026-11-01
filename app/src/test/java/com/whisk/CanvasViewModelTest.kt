package com.whisk

import com.whisk.data.local.CanvasDao
import com.whisk.data.local.CanvasEntity
import com.whisk.data.local.FolderDao
import com.whisk.data.local.FolderEntity
import com.whisk.data.local.NoteDao
import com.whisk.data.local.NoteEntity
import com.whisk.data.local.TaskDao
import com.whisk.data.local.TaskEntity
import com.whisk.data.repository.CanvasRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.SpatialCanvas
import com.whisk.ui.screens.canvas.CanvasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class FakeCanvasDao : CanvasDao {
    private val _canvases = MutableStateFlow<Map<String, CanvasEntity>>(emptyMap())

    override fun getAllCanvases(): Flow<List<CanvasEntity>> {
        return _canvases.map { map ->
            map.values.sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun getCanvasById(id: String): CanvasEntity? {
        return _canvases.value[id]
    }

    override suspend fun insertCanvas(canvas: CanvasEntity) {
        _canvases.value = _canvases.value + (canvas.id to canvas)
    }

    override suspend fun deleteCanvas(id: String): Int {
        val existed = _canvases.value.containsKey(id)
        _canvases.value = _canvases.value - id
        return if (existed) 1 else 0
    }
}

class FakeNoteDao : NoteDao {
    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    override fun getAllNotes(): Flow<List<NoteEntity>> = _notes
    override suspend fun getNoteById(id: String): NoteEntity? = _notes.value.find { it.id == id }
    override suspend fun getNoteByTitle(title: String): NoteEntity? = _notes.value.find { it.title == title }
    override fun getFavoriteNotes(): Flow<List<NoteEntity>> = _notes.map { list -> list.filter { it.isFavorite } }
    override fun getArchivedNotes(): Flow<List<NoteEntity>> = _notes.map { list -> list.filter { it.isArchived } }
    override fun getNotesByFolder(folder: String): Flow<List<NoteEntity>> = _notes.map { list -> list.filter { it.folder == folder } }
    override fun searchNotes(query: String): Flow<List<NoteEntity>> = _notes
    override fun searchNotesFts(query: String): Flow<List<NoteEntity>> = _notes
    override suspend fun insertNote(note: NoteEntity) {
        _notes.value = _notes.value.filter { it.id != note.id } + note
    }
    override suspend fun updateNote(note: NoteEntity) {
        insertNote(note)
    }
    override suspend fun deleteNoteById(id: String): Int {
        val existed = _notes.value.any { it.id == id }
        _notes.value = _notes.value.filter { it.id != id }
        return if (existed) 1 else 0
    }
    override suspend fun getNoteCount(): Int = _notes.value.size
    override suspend fun getAllNotesDirect(): List<NoteEntity> = _notes.value
}

class FakeFolderDao : FolderDao {
    private val _folders = MutableStateFlow<List<FolderEntity>>(emptyList())
    override fun getAllFolders(): Flow<List<FolderEntity>> = _folders
    override suspend fun insertFolder(folder: FolderEntity) {
        _folders.value = _folders.value + folder
    }
    override suspend fun deleteFolder(id: String): Int {
        val existed = _folders.value.any { it.id == id }
        _folders.value = _folders.value.filter { it.id != id }
        return if (existed) 1 else 0
    }
}

class FakeTaskDao : TaskDao {
    private val _tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    override fun getAllTasks(): Flow<List<TaskEntity>> = _tasks
    override suspend fun getTasksForNote(noteId: String): List<TaskEntity> = _tasks.value.filter { it.noteId == noteId }
    override suspend fun insertTasks(tasks: List<TaskEntity>) {
        _tasks.value = _tasks.value + tasks
    }
    override suspend fun deleteTasksForNote(noteId: String): Int {
        val count = _tasks.value.count { it.noteId == noteId }
        _tasks.value = _tasks.value.filter { it.noteId != noteId }
        return count
    }
    override suspend fun getTaskById(id: String): TaskEntity? = _tasks.value.find { it.id == id }
    override suspend fun updateTaskStatus(id: String, isCompleted: Boolean): Int {
        val existed = _tasks.value.any { it.id == id }
        _tasks.value = _tasks.value.map { if (it.id == id) it.copy(isCompleted = isCompleted) else it }
        return if (existed) 1 else 0
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createNotesRepository(): NotesRepository {
        return NotesRepository(FakeNoteDao(), FakeFolderDao(), FakeTaskDao())
    }

    @Test
    fun testCanvasPersistenceAcrossViewModelRecreation() = runTest {
        val canvasDao = FakeCanvasDao()
        val canvasRepository = CanvasRepository(canvasDao)
        val notesRepository = createNotesRepository()

        // Step 1: Create initial ViewModel instance (ViewModel 1)
        val viewModel1 = CanvasViewModel(canvasRepository, notesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel1.uiState.collect {}
        }

        // Step 2: Add a card to the spatial canvas
        viewModel1.addTextCard("Persistent Card", "Card content", 120f, 220f)
        advanceUntilIdle()

        val state1 = viewModel1.uiState.value
        val savedCanvasId = state1.selectedCanvasId
        assertNotNull(savedCanvasId)

        val currentCanvas1 = state1.currentCanvas
        assertNotNull(currentCanvas1)
        assertEquals(1, currentCanvas1!!.cards.size)
        assertEquals("Persistent Card", currentCanvas1.cards[0].title)

        // Step 3: Destroy/clear ViewModel 1
        viewModel1.onCleared()

        // Step 4: Recreate ViewModel instance (ViewModel 2) using the SAME repository
        val viewModel2 = CanvasViewModel(canvasRepository, notesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel2.uiState.collect {}
        }
        advanceUntilIdle()

        // Step 5: Verify ViewModel 2 restored the canvas and cards
        val state2 = viewModel2.uiState.value
        assertFalse(state2.isLoading)
        assertNull(state2.errorMessage)
        assertEquals(savedCanvasId, state2.selectedCanvasId)

        val restoredCanvas = state2.currentCanvas
        assertNotNull(restoredCanvas)
        assertEquals(1, restoredCanvas!!.cards.size)
        assertEquals("Persistent Card", restoredCanvas.cards[0].title)
        assertEquals("Card content", restoredCanvas.cards[0].content)
        assertEquals(120f, restoredCanvas.cards[0].x)
        assertEquals(220f, restoredCanvas.cards[0].y)
    }

    @Test
    fun testEmptyCanvasStateWhenNoCanvasesExist() = runTest {
        val canvasDao = FakeCanvasDao()
        val canvasRepository = CanvasRepository(canvasDao)
        val notesRepository = createNotesRepository()

        val viewModel = CanvasViewModel(canvasRepository, notesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.currentCanvas)
        assertNull(state.selectedCanvasId)
    }

    @Test
    fun testSelectCanvasLoadsSpecificCanvas() = runTest {
        val canvasDao = FakeCanvasDao()
        val canvasRepository = CanvasRepository(canvasDao)
        val notesRepository = createNotesRepository()

        val canvasA = SpatialCanvas(id = "canvas_A", title = "Canvas Alpha")
        val canvasB = SpatialCanvas(id = "canvas_B", title = "Canvas Beta")
        canvasRepository.saveCanvas(canvasA)
        canvasRepository.saveCanvas(canvasB)

        val viewModel = CanvasViewModel(canvasRepository, notesRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.selectCanvas("canvas_B")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("canvas_B", state.selectedCanvasId)
        assertNotNull(state.currentCanvas)
        assertEquals("Canvas Beta", state.currentCanvas!!.title)
    }
}
