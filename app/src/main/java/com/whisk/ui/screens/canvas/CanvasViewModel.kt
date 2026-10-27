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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class CanvasUiState(
    val currentCanvas: SpatialCanvas = SpatialCanvas(title = "Idea Canvas"),
    val allNotes: List<Note> = emptyList(),
    val selectedCardId: String? = null
)

class CanvasViewModel(
    private val canvasRepository: CanvasRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _currentCanvas = MutableStateFlow(SpatialCanvas(title = "Idea Canvas"))
    private val _selectedCardId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CanvasUiState> = combine(
        _currentCanvas,
        notesRepository.allNotes,
        _selectedCardId
    ) { canvas, notes, selectedId ->
        CanvasUiState(
            currentCanvas = canvas,
            allNotes = notes,
            selectedCardId = selectedId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CanvasUiState())

    fun addNoteCard(note: Note, x: Float = 100f, y: Float = 100f) {
        val newCard = CanvasCard(
            id = UUID.randomUUID().toString(),
            noteId = note.id,
            title = note.title,
            content = note.body.take(120),
            x = x,
            y = y,
            type = CardType.NOTE_CARD
        )
        val updatedCards = _currentCanvas.value.cards + newCard
        val updated = _currentCanvas.value.copy(cards = updatedCards)
        _currentCanvas.value = updated
        saveCanvas(updated)
    }

    fun addTextCard(title: String, content: String, x: Float = 150f, y: Float = 150f) {
        val newCard = CanvasCard(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            x = x,
            y = y,
            type = CardType.TEXT_BLOCK
        )
        val updatedCards = _currentCanvas.value.cards + newCard
        val updated = _currentCanvas.value.copy(cards = updatedCards)
        _currentCanvas.value = updated
        saveCanvas(updated)
    }

    fun updateCardPosition(cardId: String, newX: Float, newY: Float) {
        val updatedCards = _currentCanvas.value.cards.map { card ->
            if (card.id == cardId) card.copy(x = newX, y = newY) else card
        }
        val updated = _currentCanvas.value.copy(cards = updatedCards)
        _currentCanvas.value = updated
        saveCanvas(updated)
    }

    fun connectCards(fromCardId: String, toCardId: String) {
        val newConn = CanvasConnection(
            id = UUID.randomUUID().toString(),
            fromCardId = fromCardId,
            toCardId = toCardId
        )
        val updatedConns = _currentCanvas.value.connections + newConn
        val updated = _currentCanvas.value.copy(connections = updatedConns)
        _currentCanvas.value = updated
        saveCanvas(updated)
    }

    fun deleteCard(cardId: String) {
        val updatedCards = _currentCanvas.value.cards.filter { it.id != cardId }
        val updatedConns = _currentCanvas.value.connections.filter { it.fromCardId != cardId && it.toCardId != cardId }
        val updated = _currentCanvas.value.copy(cards = updatedCards, connections = updatedConns)
        _currentCanvas.value = updated
        saveCanvas(updated)
    }

    fun selectCard(cardId: String?) {
        _selectedCardId.value = cardId
    }

    private fun saveCanvas(canvas: SpatialCanvas) {
        viewModelScope.launch {
            canvasRepository.saveCanvas(canvas)
        }
    }
}
