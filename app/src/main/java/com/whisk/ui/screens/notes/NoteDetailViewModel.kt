package com.whisk.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Backlink
import com.whisk.domain.model.Note
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SaveState {
    IDLE, SAVING, SAVED, ERROR
}

data class NoteDetailUiState(
    val note: Note? = null,
    val backlinks: List<Backlink> = emptyList(),
    val saveState: SaveState = SaveState.IDLE,
    val isDeleted: Boolean = false
)

class NoteDetailViewModel(
    private val notesRepository: NotesRepository,
    private val noteId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        loadNote()
    }

    fun loadNote() {
        viewModelScope.launch {
            val note = notesRepository.getNoteById(noteId)
            if (note != null) {
                val backlinks = notesRepository.getBacklinksForNote(note.title, note.id)
                _uiState.value = _uiState.value.copy(
                    note = note,
                    backlinks = backlinks
                )
            }
        }
    }

    fun updateContent(title: String, body: String) {
        val currentNote = _uiState.value.note ?: return
        val updated = currentNote.copy(
            title = title,
            body = body,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(
            note = updated,
            saveState = SaveState.SAVING
        )

        // Debounced autosave
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(600)
            try {
                notesRepository.saveNote(updated)
                val backlinks = notesRepository.getBacklinksForNote(updated.title, updated.id)
                _uiState.value = _uiState.value.copy(
                    backlinks = backlinks,
                    saveState = SaveState.SAVED
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saveState = SaveState.ERROR)
            }
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value.note ?: return
        val updated = current.copy(isFavorite = !current.isFavorite)
        _uiState.value = _uiState.value.copy(note = updated)
        viewModelScope.launch {
            notesRepository.saveNote(updated)
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }
    }

    fun flushPendingSave() {
        autoSaveJob?.cancel()
        val note = _uiState.value.note ?: return
        if (_uiState.value.saveState == SaveState.SAVING) {
            viewModelScope.launch {
                try {
                    notesRepository.saveNote(note)
                    val backlinks = notesRepository.getBacklinksForNote(note.title, note.id)
                    _uiState.value = _uiState.value.copy(
                        backlinks = backlinks,
                        saveState = SaveState.SAVED
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(saveState = SaveState.ERROR)
                }
            }
        }
    }

    override fun onCleared() {
        flushPendingSave()
        super.onCleared()
    }
}
