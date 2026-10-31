package com.whisk.ui.screens.notes

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

sealed class NoteFilter {
    data object All : NoteFilter()
    data object Favorites : NoteFilter()
    data class ByFolder(val folderName: String) : NoteFilter()
    data class ByTag(val tagName: String) : NoteFilter()
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val activeFilter: NoteFilter = NoteFilter.All,
    val searchQuery: String = "",
)

class NotesViewModel(
    private val notesRepository: NotesRepository,
) : ViewModel() {

    private val _activeFilter = MutableStateFlow<NoteFilter>(NoteFilter.All)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NotesUiState> = combine(
        notesRepository.allNotes,
        notesRepository.folders,
        _activeFilter,
        _searchQuery,
    ) { allNotes, folders, filter, query ->
        // Extract tags dynamically from all vault notes
        val tagCounts = mutableMapOf<String, Int>()
        allNotes.forEach { (_, _, _, _, _, _, tags) ->
            tags.forEach { tag ->
                if (tag.isNotBlank()) {
                    tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                }
            }
        }
        val extractedTags = tagCounts.asSequence().map { (name, count) -> Tag(name = name, noteCount = count) }
            .sortedByDescending { (_, noteCount) -> noteCount }.toList()

        val filtered = allNotes.filter { note ->
            val matchesFilter = when (filter) {
                is NoteFilter.All -> true
                is NoteFilter.Favorites -> note.isFavorite
                is NoteFilter.ByFolder -> note.folder == filter.folderName
                is NoteFilter.ByTag -> note.tags.contains(filter.tagName)
            }
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.body.contains(query, ignoreCase = true) ||
                    note.tags.any { it.contains(query, ignoreCase = true) }

            (matchesFilter) && (matchesQuery)
        }

        NotesUiState(
            notes = filtered,
            folders = folders,
            tags = extractedTags,
            activeFilter = filter,
            searchQuery = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesUiState())

    fun setFilter(filter: NoteFilter) {
        _activeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
