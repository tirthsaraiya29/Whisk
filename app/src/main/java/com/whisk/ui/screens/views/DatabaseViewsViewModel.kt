package com.whisk.ui.screens.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.DatabaseViewRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.data.repository.PropertyRepository
import com.whisk.data.repository.ViewEngine
import com.whisk.domain.model.DatabaseView
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyValue
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.ViewType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class DatabaseViewsUiState(
    val availableViews: List<DatabaseView> = emptyList(),
    val activeView: DatabaseView? = null,
    val notes: List<Note> = emptyList(),
    val propertyDefinitions: List<PropertyDefinition> = emptyList(),
    val notePropertyValues: Map<String, List<NotePropertyValue>> = emptyMap(),
    val filteredNotes: List<Note> = emptyList(),
)

class DatabaseViewsViewModel(
    private val databaseViewRepository: DatabaseViewRepository,
    private val notesRepository: NotesRepository,
    private val propertyRepository: PropertyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DatabaseViewsUiState())
    val uiState: StateFlow<DatabaseViewsUiState> = _uiState.asStateFlow()

    init {
        seedAndObserveViews()
        observeNotesAndProperties()
    }

    private fun seedAndObserveViews() {
        viewModelScope.launch {
            databaseViewRepository.seedDefaultViewsIfEmpty()
            databaseViewRepository.allViews.collect { views ->
                val currentActive = _uiState.value.activeView
                val selectedActive = views.find { it.id == currentActive?.id } ?: views.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    availableViews = views,
                    activeView = selectedActive,
                )
                reapplyViewEngine()
            }
        }
    }

    private fun observeNotesAndProperties() {
        viewModelScope.launch {
            notesRepository.allNotes.collect { notesList ->
                _uiState.value = _uiState.value.copy(notes = notesList)
                reapplyViewEngine()
            }
        }
        viewModelScope.launch {
            propertyRepository.nonArchivedPropertyDefinitions.collect { defs ->
                _uiState.value = _uiState.value.copy(propertyDefinitions = defs)
                reapplyViewEngine()
            }
        }
    }

    fun selectView(view: DatabaseView) {
        _uiState.value = _uiState.value.copy(activeView = view)
        reapplyViewEngine()
    }

    fun createView(name: String, viewType: ViewType, targetTypeId: String? = null) {
        viewModelScope.launch {
            val newView = DatabaseView(
                id = UUID.randomUUID().toString(),
                name = name,
                viewType = viewType,
                targetTypeId = targetTypeId,
                position = _uiState.value.availableViews.size,
            )
            databaseViewRepository.saveView(newView)
            selectView(newView)
        }
    }

    fun duplicateView(viewId: String) {
        viewModelScope.launch {
            val dup = databaseViewRepository.duplicateView(viewId)
            if (dup != null) selectView(dup)
        }
    }

    fun deleteView(viewId: String) {
        viewModelScope.launch {
            databaseViewRepository.deleteView(viewId)
        }
    }

    fun updatePropertyValue(
        noteId: String,
        definitionId: String,
        textVal: String?,
        numVal: Double?,
        boolVal: Boolean?,
        listVal: List<String>,
    ) {
        viewModelScope.launch {
            val existingValues = _uiState.value.notePropertyValues[noteId] ?: emptyList()
            val existingItem = existingValues.find { it.propertyDefinitionId == definitionId }
            val valueId = existingItem?.id ?: UUID.randomUUID().toString()

            val newValue = NotePropertyValue(
                id = valueId,
                noteId = noteId,
                propertyDefinitionId = definitionId,
                textValue = textVal,
                numberValue = numVal,
                booleanValue = boolVal,
                listValue = listVal,
            )
            propertyRepository.saveNotePropertyValue(newValue)
        }
    }

    fun updateNoteGroupStatus(noteId: String, newStatus: String) {
        viewModelScope.launch {
            val note = notesRepository.getNoteById(noteId) ?: return@launch
            val updated = note.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            notesRepository.saveNote(updated)
        }
    }

    fun createNoteInGroup(status: String) {
        viewModelScope.launch {
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                title = "Untitled $status Note",
                body = "",
                status = status,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            notesRepository.saveNote(newNote)
        }
    }

    fun createNoteOnDate(dateKey: String) {
        viewModelScope.launch {
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                title = "Note for $dateKey",
                body = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            notesRepository.saveNote(newNote)
        }
    }

    private fun reapplyViewEngine() {
        val state = _uiState.value
        val activeView = state.activeView ?: return

        val propDefsMap = state.propertyDefinitions.associateBy { it.id }
        val filtered = ViewEngine.applyViewQuery(
            notes = state.notes,
            propertyValues = state.notePropertyValues,
            propertyDefinitions = propDefsMap,
            view = activeView,
        )
        _uiState.value = state.copy(filteredNotes = filtered)
    }
}
