package com.whisk.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.NotesRepository
import com.whisk.data.repository.ObjectTypeRepository
import com.whisk.data.repository.PropertyRepository
import com.whisk.domain.model.Note
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyFilter
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
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class SearchUiState(
    val query: String = "",
    val results: List<Note> = emptyList(),
    val selectedTag: String? = null,
    val selectedTypeId: String? = null,
    val tags: List<Tag> = emptyList(),
    val objectTypes: List<ObjectTypeDefinition> = emptyList(),
    val activePropertyFilter: PropertyFilter? = null,
    val availablePropertyDefinitions: List<PropertyDefinition> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val notesRepository: NotesRepository,
    private val propertyRepository: PropertyRepository,
    objectTypeRepository: ObjectTypeRepository? = null,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _selectedTypeId = MutableStateFlow<String?>(null)
    private val _activePropertyFilter = MutableStateFlow<PropertyFilter?>(null)
    private val _propertyDefinitions = MutableStateFlow<List<PropertyDefinition>>(emptyList())
    private val _objectTypes = MutableStateFlow<List<ObjectTypeDefinition>>(emptyList())

    init {
        viewModelScope.launch {
            propertyRepository.nonArchivedPropertyDefinitions.collect { defs ->
                _propertyDefinitions.value = defs
            }
        }
        objectTypeRepository?.let { repo ->
            viewModelScope.launch {
                repo.nonArchivedObjectTypeDefinitions.collect { types ->
                    _objectTypes.value = types
                }
            }
        }
    }

    private val searchResults = _query
        .debounce(150.milliseconds)
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
        _selectedTypeId,
        _activePropertyFilter,
        _propertyDefinitions,
        _objectTypes,
        searchResults,
    ) { flows: Array<Any?> ->
        val query = flows[0] as String
        val tag = flows[1] as String?
        val typeId = flows[2] as String?
        val propFilter = flows[3] as PropertyFilter?
        @Suppress("UNCHECKED_CAST")
        val propDefs = flows[4] as List<PropertyDefinition>
        @Suppress("UNCHECKED_CAST")
        val types = flows[5] as List<ObjectTypeDefinition>
        @Suppress("UNCHECKED_CAST")
        val results = flows[6] as List<Note>

        var filtered = results.filter { note ->
            ((tag == null) || note.tags.contains(tag)) &&
                ((typeId == null) || note.typeId == typeId) &&
                (query.isBlank() || note.title.contains(query, ignoreCase = true) || note.body.contains(query, ignoreCase = true) || note.aliases.any { it.contains(query, ignoreCase = true) })
        }

        if (propFilter != null) {
            val matchingIds = propertyRepository.filterNoteIdsByProperties(listOf(propFilter)).toSet()
            filtered = filtered.filter { matchingIds.contains(it.id) }
        }

        SearchUiState(
            query = query,
            results = filtered,
            selectedTag = tag,
            selectedTypeId = typeId,
            objectTypes = types,
            activePropertyFilter = propFilter,
            availablePropertyDefinitions = propDefs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun selectTypeId(typeId: String?) {
        _selectedTypeId.value = if (_selectedTypeId.value == typeId) null else typeId
    }

    fun setPropertyFilter(filter: PropertyFilter?) {
        _activePropertyFilter.value = filter
    }
}
