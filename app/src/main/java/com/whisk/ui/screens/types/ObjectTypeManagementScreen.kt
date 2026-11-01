package com.whisk.ui.screens.types

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.data.repository.CustomTypeDeleteAction
import com.whisk.domain.model.BuiltInObjectTypes
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.components.getIconForObjectType
import com.whisk.ui.theme.DotMatrixFontFamily

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
                title = "OBJECT TYPES // REGISTRY",
                subtitle = "BUILT-IN & CUSTOM KNOWLEDGE SCHEMAS",
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.objectTypes, key = { it.id }) { typeDef ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .padding(12.dp),
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
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = typeDef.name.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = DotMatrixFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (typeDef.isBuiltIn) "[BUILT-IN]" else "[CUSTOM]",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = DotMatrixFontFamily,
                                        fontSize = 10.sp,
                                        color = if (typeDef.isBuiltIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                if (!typeDef.description.isNullOrBlank()) {
                                    Text(
                                        text = typeDef.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = DotMatrixFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        if (typeDef.isBuiltIn) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Protected",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenDeleteConfirmation(typeDef)
                                },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Custom Type",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Custom Type Dialog
    if (state.showCreateDialog) {
        CreateCustomTypeDialog(
            onDismiss = onCloseCreateDialog,
            onCreate = { name, icon, desc ->
                onCreateCustomType(name, icon, desc)
            },
        )
    }

    // Delete Custom Type Confirmation Dialog
    val deleteType = state.selectedTypeForDelete
    if (deleteType != null) {
        DeleteCustomTypeDialog(
            typeDef = deleteType,
            availableTypes = state.objectTypes.filter { it.id != deleteType.id },
            onDismiss = onCloseDeleteConfirmation,
            onConfirmDelete = { action, reassignId ->
                onDeleteCustomType(deleteType.id, action, reassignId)
            },
        )
    }
}

@Composable
private fun CreateCustomTypeDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String, description: String?) -> Unit,
) {
    var nameText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("CREATE CUSTOM OBJECT TYPE", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Type Name (e.g. Movies, Research Papers)", fontFamily = DotMatrixFontFamily) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Description", fontFamily = DotMatrixFontFamily) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onCreate(nameText.trim(), "description", descText.ifBlank { null })
                    }
                },
                enabled = nameText.isNotBlank(),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("CREATE TYPE", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(0.dp)) {
                Text("CANCEL", fontFamily = DotMatrixFontFamily)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp),
    )
}

@Composable
private fun DeleteCustomTypeDialog(
    typeDef: ObjectTypeDefinition,
    availableTypes: List<ObjectTypeDefinition>,
    onDismiss: () -> Unit,
    onConfirmDelete: (CustomTypeDeleteAction, String?) -> Unit,
) {
    var selectedAction by remember { mutableStateOf(CustomTypeDeleteAction.CONVERT_TO_NOTES) }
    var selectedReassignType by remember { mutableStateOf(BuiltInObjectTypes.NOTE) }
    var expandedMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("DELETE TYPE: ${typeDef.name.uppercase()}", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "DECIDE WHAT HAPPENS TO EXISTING OBJECTS OF THIS TYPE:",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedAction == CustomTypeDeleteAction.CONVERT_TO_NOTES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { selectedAction = CustomTypeDeleteAction.CONVERT_TO_NOTES }
                            .padding(8.dp),
                    ) {
                        Text("1. Convert objects to Notes", fontFamily = DotMatrixFontFamily, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedAction == CustomTypeDeleteAction.REASSIGN_TYPE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { selectedAction = CustomTypeDeleteAction.REASSIGN_TYPE }
                            .padding(8.dp),
                    ) {
                        Text("2. Reassign to another type", fontFamily = DotMatrixFontFamily, style = MaterialTheme.typography.bodySmall)
                    }

                    if (selectedAction == CustomTypeDeleteAction.REASSIGN_TYPE) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = availableTypes.find { it.id == selectedReassignType }?.name ?: "Notes",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Target Type", fontFamily = DotMatrixFontFamily) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedMenu = true },
                            )

                            DropdownMenu(
                                expanded = expandedMenu,
                                onDismissRequest = { expandedMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                            ) {
                                availableTypes.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.name, fontFamily = DotMatrixFontFamily) },
                                        onClick = {
                                            selectedReassignType = t.id
                                            expandedMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedAction == CustomTypeDeleteAction.ARCHIVE_OBJECTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { selectedAction = CustomTypeDeleteAction.ARCHIVE_OBJECTS }
                            .padding(8.dp),
                    ) {
                        Text("3. Archive objects", fontFamily = DotMatrixFontFamily, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (selectedAction == CustomTypeDeleteAction.DELETE_OBJECTS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { selectedAction = CustomTypeDeleteAction.DELETE_OBJECTS }
                            .padding(8.dp),
                    ) {
                        Text("4. Delete objects", fontFamily = DotMatrixFontFamily, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete(selectedAction, selectedReassignType)
                },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("PROCEED", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(0.dp)) {
                Text("CANCEL", fontFamily = DotMatrixFontFamily)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp),
    )
}
