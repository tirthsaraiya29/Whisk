package com.whisk.ui.screens.types

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.data.repository.CustomTypeDeleteAction
import com.whisk.data.repository.ObjectTypeRepository
import com.whisk.domain.model.ObjectTypeDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ObjectTypeManagementUiState(
    val objectTypes: List<ObjectTypeDefinition> = emptyList(),
    val showCreateDialog: Boolean = false,
    val selectedTypeForDelete: ObjectTypeDefinition? = null,
)

class ObjectTypeManagementViewModel(
    private val objectTypeRepository: ObjectTypeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectTypeManagementUiState())
    val uiState: StateFlow<ObjectTypeManagementUiState> = _uiState.asStateFlow()

    init {
        observeTypes()
    }

    private fun observeTypes() {
        viewModelScope.launch {
            objectTypeRepository.allObjectTypeDefinitions.collect { types ->
                _uiState.value = _uiState.value.copy(objectTypes = types)
            }
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun closeCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createCustomType(name: String, icon: String, description: String?) {
        val newTypeDef = ObjectTypeDefinition(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            icon = icon,
            isBuiltIn = false,
            position = _uiState.value.objectTypes.size,
        )
        viewModelScope.launch {
            objectTypeRepository.saveObjectTypeDefinition(newTypeDef)
            closeCreateDialog()
        }
    }

    fun openDeleteConfirmation(typeDef: ObjectTypeDefinition) {
        if (!typeDef.isBuiltIn) {
            _uiState.value = _uiState.value.copy(selectedTypeForDelete = typeDef)
        }
    }

    fun closeDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(selectedTypeForDelete = null)
    }

    fun deleteCustomType(
        typeId: String,
        action: CustomTypeDeleteAction,
        reassignTypeId: String? = null,
    ) {
        viewModelScope.launch {
            objectTypeRepository.deleteObjectTypeDefinition(typeId, action, reassignTypeId)
            closeDeleteConfirmation()
        }
    }

    fun toggleArchive(typeDef: ObjectTypeDefinition) {
        viewModelScope.launch {
            objectTypeRepository.saveObjectTypeDefinition(typeDef.copy(isArchived = !typeDef.isArchived))
        }
    }
}
