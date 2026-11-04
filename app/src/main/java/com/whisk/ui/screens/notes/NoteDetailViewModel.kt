package com.whisk.ui.screens.notes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.rag.repository.RelatedKnowledgeItem
import com.whisk.data.rag.repository.RelatedKnowledgeRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.data.repository.ObjectTypeRepository
import com.whisk.data.repository.PropertyRepository
import com.whisk.data.repository.RelationshipRepository
import com.whisk.domain.model.Backlink
import com.whisk.domain.model.BrokenLinkItem
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyItem
import com.whisk.domain.model.NotePropertyValue
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.RelationshipDetail
import com.whisk.domain.model.RelationshipType
import com.whisk.domain.model.UnlinkedMention
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

enum class SaveState {
    IDLE, SAVING, SAVED, ERROR
}

@Immutable
data class NoteDetailUiState(
    val note: Note? = null,
    val backlinks: List<Backlink> = emptyList(),
    val unlinkedMentions: List<UnlinkedMention> = emptyList(),
    val brokenLinks: List<BrokenLinkItem> = emptyList(),
    val relatedKnowledge: List<RelatedKnowledgeItem> = emptyList(),
    val saveState: SaveState = SaveState.IDLE,
    val isDeleted: Boolean = false,
    val isBiometricLocked: Boolean = false,
    val propertyItems: List<NotePropertyItem> = emptyList(),
    val allDefinitions: List<PropertyDefinition> = emptyList(),
    val objectTypes: List<ObjectTypeDefinition> = emptyList(),
    val relationships: List<RelationshipDetail> = emptyList(),
)

class NoteDetailViewModel(
    private val notesRepository: NotesRepository,
    private val propertyRepository: PropertyRepository,
    private val objectTypeRepository: ObjectTypeRepository? = null,
    private val relationshipRepository: RelationshipRepository? = null,
    private val relatedKnowledgeRepository: RelatedKnowledgeRepository? = null,
    private val noteId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    private val saveMutex = Mutex()
    private var autoSaveJob: Job? = null

    init {
        loadNote()
        observeProperties()
        observeObjectTypesAndRelationships()
    }

    fun loadNote() {
        viewModelScope.launch {
            val note = notesRepository.getNoteById(noteId)
            if (note != null) {
                val backlinks = notesRepository.getBacklinksForNote(note.title, note.id, note.aliases)
                val unlinkedMentions = notesRepository.getUnlinkedMentionsForNote(note)
                val brokenLinks = notesRepository.getBrokenLinks().filter { it.sourceNoteId == note.id }
                val related = relatedKnowledgeRepository?.getRelatedKnowledgeForNote(note.id, limit = 5) ?: emptyList()
                val isLocked = note.tags.any { it.equals("locked", ignoreCase = true) || it.equals("secret", ignoreCase = true) }

                _uiState.update { current ->
                    current.copy(
                        note = note,
                        backlinks = backlinks,
                        unlinkedMentions = unlinkedMentions,
                        brokenLinks = brokenLinks,
                        relatedKnowledge = related,
                        isBiometricLocked = isLocked,
                    )
                }
            }
        }
    }

    fun unlockBiometric() {
        _uiState.update { it.copy(isBiometricLocked = false) }
    }

    fun convertUnlinkedMention(matchedText: String, targetTitle: String) {
        val currentNote = _uiState.value.note ?: return
        viewModelScope.launch {
            notesRepository.convertUnlinkedMentionToLink(currentNote.id, matchedText, targetTitle)
            loadNote()
        }
    }

    fun createNoteFromBrokenLink(targetTitle: String, onNoteCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newNote = notesRepository.createNoteFromBrokenLink(targetTitle)
            loadNote()
            onNoteCreated(newNote.id)
        }
    }

    private fun observeProperties() {
        viewModelScope.launch {
            propertyRepository.getPropertyItemsForNote(noteId).collect { items ->
                _uiState.update { it.copy(propertyItems = items) }
            }
        }
        viewModelScope.launch {
            propertyRepository.nonArchivedPropertyDefinitions.collect { defs ->
                _uiState.update { it.copy(allDefinitions = defs) }
            }
        }
    }

    private fun observeObjectTypesAndRelationships() {
        objectTypeRepository?.let { repo ->
            viewModelScope.launch {
                repo.nonArchivedObjectTypeDefinitions.collect { types ->
                    _uiState.update { it.copy(objectTypes = types) }
                }
            }
        }
        relationshipRepository?.let { repo ->
            viewModelScope.launch {
                repo.getRelationshipsForObject(noteId).collect { rels ->
                    _uiState.update { it.copy(relationships = rels) }
                }
            }
        }
    }

    fun changeObjectType(newTypeId: String) {
        val current = _uiState.value.note ?: return
        val updated = current.copy(typeId = newTypeId, updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(note = updated) }
        viewModelScope.launch {
            saveMutex.withLock {
                notesRepository.saveNote(updated)
            }
        }
    }

    fun addRelationship(targetObjectId: String, relationshipType: RelationshipType) {
        relationshipRepository?.let { repo ->
            viewModelScope.launch {
                repo.addRelationship(noteId, targetObjectId, relationshipType)
            }
        }
    }

    fun removeRelationship(relationshipId: String) {
        relationshipRepository?.let { repo ->
            viewModelScope.launch {
                repo.removeRelationship(relationshipId)
            }
        }
    }

    suspend fun queryObjectsForPicker(query: String, typeId: String?): List<Note> {
        return notesRepository.getObjectsForPicker(query, typeId)
    }

    fun updatePropertyValue(
        definitionId: String,
        textValue: String? = null,
        numberValue: Double? = null,
        booleanValue: Boolean? = null,
        listValue: List<String> = emptyList(),
    ) {
        viewModelScope.launch {
            val existingItem = _uiState.value.propertyItems.find { it.definition.id == definitionId }
            val valueId = existingItem?.value?.id ?: UUID.randomUUID().toString()

            val newValue = NotePropertyValue(
                id = valueId,
                noteId = noteId,
                propertyDefinitionId = definitionId,
                textValue = textValue,
                numberValue = numberValue,
                booleanValue = booleanValue,
                listValue = listValue,
            )
            propertyRepository.saveNotePropertyValue(newValue)
        }
    }

    fun deletePropertyValue(definitionId: String) {
        viewModelScope.launch {
            propertyRepository.deleteNotePropertyValue(noteId, definitionId)
        }
    }

    fun updateContent(title: String, body: String) {
        val currentNote = _uiState.value.note ?: return
        val updated = currentNote.copy(
            title = title,
            body = body,
            updatedAt = System.currentTimeMillis(),
        )
        _uiState.update {
            it.copy(note = updated, saveState = SaveState.SAVING)
        }

        // Debounced atomic autosave
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500.milliseconds)
            try {
                saveMutex.withLock {
                    notesRepository.saveNote(updated)
                }
                val backlinks = notesRepository.getBacklinksForNote(updated.title, updated.id)
                _uiState.update {
                    it.copy(backlinks = backlinks, saveState = SaveState.SAVED)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(saveState = SaveState.ERROR) }
            }
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value.note ?: return
        val updated = current.copy(isFavorite = !current.isFavorite)
        _uiState.update { it.copy(note = updated) }
        viewModelScope.launch {
            saveMutex.withLock {
                notesRepository.saveNote(updated)
            }
        }
    }

    fun togglePin() {
        val current = _uiState.value.note ?: return
        val updated = current.copy(isPinned = !current.isPinned)
        _uiState.update { it.copy(note = updated) }
        viewModelScope.launch {
            saveMutex.withLock {
                notesRepository.togglePin(current.id, updated.isPinned)
            }
        }
    }

    fun updateAccentColor(color: String?) {
        val current = _uiState.value.note ?: return
        val updated = current.copy(accentColor = color)
        _uiState.update { it.copy(note = updated) }
        viewModelScope.launch {
            saveMutex.withLock {
                notesRepository.updateAccentColor(current.id, color)
            }
        }
    }

    fun duplicateNote(onDuplicated: (String) -> Unit) {
        viewModelScope.launch {
            val duplicated = notesRepository.duplicateNote(noteId)
            if (duplicated != null) {
                onDuplicated(duplicated.id)
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            saveMutex.withLock {
                notesRepository.deleteNote(noteId)
            }
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun softDeleteNote() {
        viewModelScope.launch {
            saveMutex.withLock {
                notesRepository.softDeleteNote(noteId)
            }
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun flushPendingSave() {
        autoSaveJob?.cancel()
        val note = _uiState.value.note ?: return
        if (_uiState.value.saveState == SaveState.SAVING) {
            // Use independent IO scope + NonCancellable so back-navigation cannot abort the save
            CoroutineScope(Dispatchers.IO + NonCancellable).launch {
                try {
                    saveMutex.withLock {
                        notesRepository.saveNote(note)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCleared() {
        flushPendingSave()
    }
}
