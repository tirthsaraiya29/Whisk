package com.whisk.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import com.whisk.domain.model.Tag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val results: List<Note> = emptyList(),
    val selectedTag: String? = null,
    val tags: List<Tag> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)

    private val searchResults = _query
        .debounce(150)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                notesRepository.allNotes
            } else {
                notesRepository.searchNotesFts(query)
            }
        }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _selectedTag,
        searchResults
    ) { query, tag, results ->
        val filtered = results.filter { note ->
            tag == null || note.tags.contains(tag)
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
