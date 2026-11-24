package com.whisk.ui.screens.types

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whisk.data.repository.CustomTypeDeleteAction
import com.whisk.domain.model.BuiltInObjectTypes
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.components.getIconForObjectType

@Composable
fun ObjectTypeManagementScreen(
    state: ObjectTypeManagementUiState,
    onBackClick: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onCloseCreateDialog: () -> Unit,
    onCreateCustomType: (name: String, icon: String, description: String?) -> Unit,
    onOpenDeleteConfirmation: (ObjectTypeDefinition) -> Unit,
    onCloseDeleteConfirmation: () -> Unit,
    onDeleteCustomType: (typeId: String, action: CustomTypeDeleteAction, reassignTypeId: String?) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "Object Types Registry",
                subtitle = "Built-in & Custom Knowledge Schemas",
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenCreateDialog()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Custom Object Type",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.objectTypes, key = { it.id }) { typeDef ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = getIconForObjectType(typeDef.id),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = typeDef.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (typeDef.isBuiltIn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = if (typeDef.isBuiltIn) "Built-in" else "Custom",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (typeDef.isBuiltIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                    }
                                    if (!typeDef.description.isNullOrBlank()) {
                                        Text(
                                            text = typeDef.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                            }

                            if (!typeDef.isBuiltIn) {
                                IconButton(onClick = { onOpenDeleteConfirmation(typeDef) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Custom Type",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Built-in Locked",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateObjectTypeDialog(
            onDismiss = onCloseCreateDialog,
            onCreate = { name, icon, desc ->
                onCreateCustomType(name, icon, desc)
                onCloseCreateDialog()
            },
        )
    }

    if (state.selectedTypeForDelete != null) {
        val typeDef = state.selectedTypeForDelete
        DeleteObjectTypeDialog(
            typeDef = typeDef,
            allTypes = state.objectTypes,
            onDismiss = onCloseDeleteConfirmation,
            onConfirm = { action, reassignTypeId ->
                onDeleteCustomType(typeDef.id, action, reassignTypeId)
                onCloseDeleteConfirmation()
            },
        )
    }
}

@Composable
private fun CreateObjectTypeDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String, description: String?) -> Unit,
) {
    var nameText by remember { mutableStateOf("") }
    var iconText by remember { mutableStateOf("description") }
    var descText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Custom Object Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Type Name") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Description (Optional)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onCreate(nameText.trim(), iconText.ifBlank { "description" }, descText.ifBlank { null })
                    }
                },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun DeleteObjectTypeDialog(
    typeDef: ObjectTypeDefinition,
    allTypes: List<ObjectTypeDefinition>,
    onDismiss: () -> Unit,
    onConfirm: (action: CustomTypeDeleteAction, reassignTypeId: String?) -> Unit,
) {
    var selectedAction by remember { mutableStateOf(CustomTypeDeleteAction.CONVERT_TO_NOTES) }
    var selectedReassignType by remember { mutableStateOf(allTypes.firstOrNull { it.id == BuiltInObjectTypes.NOTE } ?: allTypes.firstOrNull()) }
    var showReassignDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Custom Type '${typeDef.name}'", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Choose action for existing objects assigned to this type:", style = MaterialTheme.typography.bodyMedium)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedAction = CustomTypeDeleteAction.CONVERT_TO_NOTES }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (selectedAction == CustomTypeDeleteAction.CONVERT_TO_NOTES) "✓ Convert to standard notes" else "  Convert to standard notes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedAction == CustomTypeDeleteAction.CONVERT_TO_NOTES) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedAction = CustomTypeDeleteAction.REASSIGN_TYPE }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (selectedAction == CustomTypeDeleteAction.REASSIGN_TYPE) "✓ Reassign to type" else "  Reassign to type",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedAction == CustomTypeDeleteAction.REASSIGN_TYPE) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                if (selectedAction == CustomTypeDeleteAction.REASSIGN_TYPE) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showReassignDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(selectedReassignType?.name ?: "Select Type", style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showReassignDropdown,
                            onDismissRequest = { showReassignDropdown = false },
                        ) {
                            allTypes.filter { it.id != typeDef.id }.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text(target.name, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedReassignType = target
                                        showReassignDropdown = false
                                    },
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedAction = CustomTypeDeleteAction.DELETE_OBJECTS }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (selectedAction == CustomTypeDeleteAction.DELETE_OBJECTS) "✓ Delete all objects" else "  Delete all objects",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedAction == CustomTypeDeleteAction.DELETE_OBJECTS) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedAction, selectedReassignType?.id)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAction == CustomTypeDeleteAction.DELETE_OBJECTS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Confirm Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
