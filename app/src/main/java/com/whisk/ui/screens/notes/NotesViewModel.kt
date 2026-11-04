package com.whisk.ui.screens.notes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Folder
import com.whisk.domain.model.Note
import com.whisk.domain.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class NoteFilter {
    data object All : NoteFilter()
    data object Favorites : NoteFilter()
    data object Trash : NoteFilter()
    data class ByFolder(val folderName: String) : NoteFilter()
    data class ByTag(val tagName: String) : NoteFilter()
}

@Immutable
data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val pinnedNotes: List<Note> = emptyList(),
    val trashedNotes: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val activeFilter: NoteFilter = NoteFilter.All,
    val searchQuery: String = "",
    val selectedNoteIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
)

class NotesViewModel(
    private val notesRepository: NotesRepository,
) : ViewModel() {

    private val _activeFilter = MutableStateFlow<NoteFilter>(NoteFilter.All)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isMultiSelectMode = MutableStateFlow(false)

    init {
        // Auto-purge trash older than 30 days on startup
        viewModelScope.launch {
            notesRepository.purgeOldTrash(30)
        }
    }

    val uiState: StateFlow<NotesUiState> = combine(
        notesRepository.allNotes,
        notesRepository.trashedNotes,
        notesRepository.folders,
        _activeFilter,
        _searchQuery,
        _selectedNoteIds,
        _isMultiSelectMode,
    ) { allNotes, trashedNotes, folders, filter, query, selectedIds, isMultiSelect ->
        // Extract tags dynamically from all vault notes
        val tagCounts = mutableMapOf<String, Int>()
        allNotes.forEach { note ->
            note.tags.forEach { tag ->
                if (tag.isNotBlank()) {
                    tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                }
            }
        }
        val extractedTags = tagCounts.asSequence().map { (name, count) -> Tag(name = name, noteCount = count) }
            .sortedByDescending { it.noteCount }.toList()

        val sourceList = if (filter is NoteFilter.Trash) trashedNotes else allNotes

        val filtered = sourceList.filter { note ->
            val matchesFilter = when (filter) {
                is NoteFilter.All -> true
                is NoteFilter.Favorites -> note.isFavorite
                is NoteFilter.Trash -> true
                is NoteFilter.ByFolder -> note.folder == filter.folderName
                is NoteFilter.ByTag -> note.tags.contains(filter.tagName)
            }
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.body.contains(query, ignoreCase = true) ||
                    note.tags.any { it.contains(query, ignoreCase = true) }

            matchesFilter && matchesQuery
        }

        val pinned = if (filter !is NoteFilter.Trash && query.isBlank()) {
            allNotes.filter { it.isPinned }
        } else emptyList()

        NotesUiState(
            notes = filtered,
            pinnedNotes = pinned,
            trashedNotes = trashedNotes,
            folders = folders,
            tags = extractedTags,
            activeFilter = filter,
            searchQuery = query,
            selectedNoteIds = selectedIds,
            isMultiSelectMode = isMultiSelect,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesUiState())

    fun setFilter(filter: NoteFilter) {
        _activeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleNoteSelection(noteId: String) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _selectedNoteIds.value = current
        if (current.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    fun enterMultiSelectMode(initialNoteId: String) {
        _isMultiSelectMode.value = true
        _selectedNoteIds.value = setOf(initialNoteId)
    }

    fun clearMultiSelect() {
        _isMultiSelectMode.value = false
        _selectedNoteIds.value = emptySet()
    }

    fun batchDeleteSelected() {
        val ids = _selectedNoteIds.value
        viewModelScope.launch {
            ids.forEach { notesRepository.softDeleteNote(it) }
            clearMultiSelect()
        }
    }

    fun batchTagSelected(tag: String) {
        val ids = _selectedNoteIds.value
        viewModelScope.launch {
            ids.forEach { id ->
                val note = notesRepository.getNoteById(id)
                if (note != null && !note.tags.contains(tag)) {
                    val updated = note.copy(tags = note.tags + tag, updatedAt = System.currentTimeMillis())
                    notesRepository.saveNote(updated)
                }
            }
            clearMultiSelect()
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isFavorite = !note.isFavorite, updatedAt = System.currentTimeMillis())
            notesRepository.saveNote(updated)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            notesRepository.togglePin(note.id, !note.isPinned)
        }
    }

    fun softDelete(noteId: String) {
        viewModelScope.launch {
            notesRepository.softDeleteNote(noteId)
        }
    }

    fun restore(noteId: String) {
        viewModelScope.launch {
            notesRepository.restoreNote(noteId)
        }
    }

    suspend fun createNewNote(): String {
        val newNote = Note(
            title = "Untitled Note",
            body = "",
            createdAt = System.currentTimeMillis(),
        )
        notesRepository.saveNote(newNote)
        return newNote.id
    }
}
