package com.whisk.ui.screens.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.CanvasRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.CanvasCard
import com.whisk.domain.model.CanvasConnection
import com.whisk.domain.model.CardType
import com.whisk.domain.model.Note
import com.whisk.domain.model.SpatialCanvas
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

data class CanvasUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentCanvas: SpatialCanvas? = null,
    val selectedCanvasId: String? = null,
    val allCanvases: List<SpatialCanvas> = emptyList(),
    val allNotes: List<Note> = emptyList(),
    val selectedCardId: String? = null,
)

class CanvasViewModel(
    private val canvasRepository: CanvasRepository,
    notesRepository: NotesRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(value = true)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _currentCanvas = MutableStateFlow<SpatialCanvas?>(null)
    private val _selectedCanvasId = MutableStateFlow<String?>(null)
    private val _allCanvases = MutableStateFlow<List<SpatialCanvas>>(emptyList())
    private val _selectedCardId = MutableStateFlow<String?>(null)

    private var debouncedSaveJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadCanvases()
    }

    fun loadCanvases() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            canvasRepository.allCanvases
                .catch { e ->
                    _errorMessage.value = e.message ?: "Failed to load spatial canvases"
                    _isLoading.value = false
                }
                .collect { canvases ->
                    _allCanvases.value = canvases
                    _isLoading.value = false

                    if (canvases.isNotEmpty()) {
                        val activeId = _selectedCanvasId.value
                        val matchedCanvas = canvases.find { (id) -> id == activeId }
                        if (matchedCanvas != null) {
                            _currentCanvas.value = matchedCanvas
                        } else {
                            val firstCanvas = canvases.first()
                            _selectedCanvasId.value = firstCanvas.id
                            _currentCanvas.value = firstCanvas
                        }
                    } else {
                        _selectedCanvasId.value = null
                        _currentCanvas.value = null
                    }
                }
        }
    }

    fun selectCanvas(canvasId: String) {
        _selectedCanvasId.value = canvasId
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val canvas = canvasRepository.getCanvasById(canvasId)
                if (canvas != null) {
                    _currentCanvas.value = canvas
                } else {
                    _errorMessage.value = "Canvas not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load requested canvas"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewCanvas(title: String = "Idea Canvas"): SpatialCanvas {
        val newCanvas = SpatialCanvas(title = title)
        _selectedCanvasId.value = newCanvas.id
        _currentCanvas.value = newCanvas
        saveCanvasImmediately(newCanvas)
        return newCanvas
    }

    fun deleteCanvas(canvasId: String) {
        viewModelScope.launch {
            try {
                canvasRepository.deleteCanvas(canvasId)
                if (_selectedCanvasId.value == canvasId) {
                    _selectedCanvasId.value = null
                    _currentCanvas.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete canvas"
            }
        }
    }

    fun addNoteCard(note: Note, x: Float = 100f, y: Float = 100f) {
        val baseCanvas = _currentCanvas.value ?: SpatialCanvas(title = "Idea Canvas")
        val newCard = CanvasCard(
            id = UUID.randomUUID().toString(),
            noteId = note.id,
            title = note.title,
            content = note.body.take(120),
            x = x,
            y = y,
            type = CardType.NOTE_CARD,
        )
        val updatedCards = baseCanvas.cards + newCard
        val updated = baseCanvas.copy(cards = updatedCards, updatedAt = System.currentTimeMillis())
        _selectedCanvasId.value = updated.id
        _currentCanvas.value = updated
        saveCanvasImmediately(updated)
    }

    fun addTextCard(title: String, content: String, x: Float = 150f, y: Float = 150f) {
        val baseCanvas = _currentCanvas.value ?: SpatialCanvas(title = "Idea Canvas")
        val newCard = CanvasCard(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            x = x,
            y = y,
            type = CardType.TEXT_BLOCK,
        )
        val updatedCards = baseCanvas.cards + newCard
        val updated = baseCanvas.copy(cards = updatedCards, updatedAt = System.currentTimeMillis())
        _selectedCanvasId.value = updated.id
        _currentCanvas.value = updated
        saveCanvasImmediately(updated)
    }

    fun updateCardPosition(cardId: String, newX: Float, newY: Float) {
        val current = _currentCanvas.value ?: return
        val updatedCards = current.cards.map { card ->
            if (card.id == cardId) card.copy(x = newX, y = newY) else card
        }
        val updated = current.copy(cards = updatedCards, updatedAt = System.currentTimeMillis())
        _currentCanvas.value = updated
        debounceSaveCanvas(updated)
    }

    fun connectCards(fromCardId: String, toCardId: String) {
        val current = _currentCanvas.value ?: return
        val newConn = CanvasConnection(
            id = UUID.randomUUID().toString(),
            fromCardId = fromCardId,
            toCardId = toCardId,
        )
        val updatedConns = current.connections + newConn
        val updated = current.copy(connections = updatedConns, updatedAt = System.currentTimeMillis())
        _currentCanvas.value = updated
        saveCanvasImmediately(updated)
    }

    fun deleteCard(cardId: String) {
        val current = _currentCanvas.value ?: return
        val updatedCards = current.cards.filter { (id) -> id != cardId }
        val updatedConns = current.connections.filter { (_, fromCardId, toCardId) -> (fromCardId != cardId) && (toCardId != cardId) }
        val updated = current.copy(cards = updatedCards, connections = updatedConns, updatedAt = System.currentTimeMillis())
        _currentCanvas.value = updated
        saveCanvasImmediately(updated)
    }

    fun selectCard(cardId: String?) {
        _selectedCardId.value = cardId
    }

    private fun debounceSaveCanvas(canvas: SpatialCanvas) {
        debouncedSaveJob?.cancel()
        debouncedSaveJob = viewModelScope.launch {
            delay(300.milliseconds)
            canvasRepository.saveCanvas(canvas)
        }
    }

    private fun saveCanvasImmediately(canvas: SpatialCanvas) {
        debouncedSaveJob?.cancel()
        viewModelScope.launch {
            canvasRepository.saveCanvas(canvas)
        }
    }

    public override fun onCleared() {
        debouncedSaveJob?.cancel()
        val current = _currentCanvas.value
        if (current != null) {
            viewModelScope.launch {
                canvasRepository.saveCanvas(current)
            }
        }
    }

    private data class StateBundlePrimary(
        val isLoading: Boolean,
        val errorMessage: String?,
        val currentCanvas: SpatialCanvas?,
        val selectedCanvasId: String?,
    )

    private data class StateBundleSecondary(
        val allCanvases: List<SpatialCanvas>,
        val allNotes: List<Note>,
        val selectedCardId: String?,
    )

    val uiState: StateFlow<CanvasUiState> = combine(
        combine(_isLoading, _errorMessage, _currentCanvas, _selectedCanvasId) { loading, error, canvas, canvasId ->
            StateBundlePrimary(loading, error, canvas, canvasId)
        },
        combine(_allCanvases, notesRepository.allNotes, _selectedCardId) { canvases, notes, cardId ->
            StateBundleSecondary(canvases, notes, cardId)
        },
    ) { primary, secondary ->
        CanvasUiState(
            isLoading = primary.isLoading,
            errorMessage = primary.errorMessage,
            currentCanvas = primary.currentCanvas,
            selectedCanvasId = primary.selectedCanvasId,
            allCanvases = secondary.allCanvases,
            allNotes = secondary.allNotes,
            selectedCardId = secondary.selectedCardId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CanvasUiState())
}
