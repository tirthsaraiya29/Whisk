package com.whisk.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import com.whisk.domain.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val results: List<Note> = emptyList(),
    val selectedTag: String? = null,
    val tags: List<Tag> = emptyList()
)

class SearchViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _selectedTag,
        notesRepository.allNotes
    ) { query, tag, allNotes ->
        val filtered = allNotes.filter { note ->
            val matchesTag = tag == null || note.tags.contains(tag)
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.body.contains(query, ignoreCase = true) ||
                    note.tags.any { it.contains(query, ignoreCase = true) }
            matchesTag && matchesQuery
        }

        SearchUiState(
            query = query,
            results = filtered,
            selectedTag = tag
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }
}
