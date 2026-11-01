package com.whisk.ui.screens.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.PropertyRepository
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PropertyManagementUiState(
    val properties: List<PropertyDefinition> = emptyList(),
    val showCreateDialog: Boolean = false,
    val editingProperty: PropertyDefinition? = null,
    val deleteConfirmationProperty: PropertyDefinition? = null,
)

class PropertyManagementViewModel(
    private val propertyRepository: PropertyRepository,
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    private val _editingProperty = MutableStateFlow<PropertyDefinition?>(null)
    private val _deleteConfirmationProperty = MutableStateFlow<PropertyDefinition?>(null)

    val uiState: StateFlow<PropertyManagementUiState> = combine(
        propertyRepository.allPropertyDefinitions,
        _showCreateDialog,
        _editingProperty,
        _deleteConfirmationProperty,
    ) { properties, showCreate, editing, deleteConfirm ->
        PropertyManagementUiState(
            properties = properties,
            showCreateDialog = showCreate,
            editingProperty = editing,
            deleteConfirmationProperty = deleteConfirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PropertyManagementUiState())

    init {
        viewModelScope.launch {
            propertyRepository.seedDefaultPropertiesIfEmpty()
        }
    }

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun openEditDialog(property: PropertyDefinition) {
        _editingProperty.value = property
    }

    fun closeEditDialog() {
        _editingProperty.value = null
    }

    fun openDeleteConfirmation(property: PropertyDefinition) {
        _deleteConfirmationProperty.value = property
    }

    fun closeDeleteConfirmation() {
        _deleteConfirmationProperty.value = null
    }

    fun createProperty(
        name: String,
        type: PropertyType,
        description: String?,
        options: List<String>,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val nextPosition = (uiState.value.properties.maxOfOrNull { it.position } ?: -1) + 1
            val newProperty = PropertyDefinition(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                description = description?.trim()?.ifBlank { null },
                options = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
                position = nextPosition,
                isArchived = false,
            )
            propertyRepository.savePropertyDefinition(newProperty)
            _showCreateDialog.value = false
        }
    }

    fun updateProperty(
        id: String,
        name: String,
        type: PropertyType,
        description: String?,
        options: List<String>,
        isArchived: Boolean,
    ) {
        if (name.isBlank()) return
        val current = uiState.value.properties.find { it.id == id } ?: return
        viewModelScope.launch {
            val updated = current.copy(
                name = name.trim(),
                type = type,
                description = description?.trim()?.ifBlank { null },
                options = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
                isArchived = isArchived,
                updatedAt = System.currentTimeMillis(),
            )
            propertyRepository.savePropertyDefinition(updated)
            _editingProperty.value = null
        }
    }

    fun deleteProperty(id: String) {
        viewModelScope.launch {
            propertyRepository.deletePropertyDefinition(id)
            _deleteConfirmationProperty.value = null
        }
    }

    fun toggleArchive(property: PropertyDefinition) {
        viewModelScope.launch {
            propertyRepository.savePropertyDefinition(
                property.copy(
                    isArchived = !property.isArchived,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
