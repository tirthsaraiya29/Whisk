package com.whisk.ui.screens.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateListOf
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
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyType
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.theme.DotMatrixFontFamily

@Composable
fun PropertyManagementScreen(
    state: PropertyManagementUiState,
    onBackClick: () -> Unit,
    onOpenCreateDialog: () -> Unit,
    onCloseCreateDialog: () -> Unit,
    onCreateProperty: (String, PropertyType, String?, List<String>) -> Unit,
    onOpenEditDialog: (PropertyDefinition) -> Unit,
    onCloseEditDialog: () -> Unit,
    onUpdateProperty: (String, String, PropertyType, String?, List<String>, Boolean) -> Unit,
    onOpenDeleteConfirmation: (PropertyDefinition) -> Unit,
    onCloseDeleteConfirmation: () -> Unit,
    onDeleteProperty: (String) -> Unit,
    onToggleArchive: (PropertyDefinition) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "WHISK // PROPERTIES",
                subtitle = "WORKSPACE METADATA SCHEMAS",
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenCreateDialog()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEW PROPERTY",
                        fontFamily = DotMatrixFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (state.properties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO PROPERTY DEFINITIONS",
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "CREATE CUSTOM METADATA SCHEMAS TO STRUCTURE YOUR NOTES",
                            fontFamily = DotMatrixFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 100.dp,
                    ),
                ) {
                    items(state.properties, key = { it.id }) { prop ->
                        PropertyCardRow(
                            property = prop,
                            onEdit = { onOpenEditDialog(prop) },
                            onToggleArchive = { onToggleArchive(prop) },
                            onDelete = { onOpenDeleteConfirmation(prop) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    }
                }
            }
        }
    }

    // Create Property Dialog
    if (state.showCreateDialog) {
        PropertyEditModal(
            title = "NEW PROPERTY DEFINITION",
            initialName = "",
            initialType = PropertyType.TEXT,
            initialDescription = "",
            initialOptions = emptyList(),
            initialIsArchived = false,
            isEditing = false,
            onDismiss = onCloseCreateDialog,
            onSave = { name, type, desc, options, _ ->
                onCreateProperty(name, type, desc, options)
            },
        )
    }

    // Edit Property Dialog
    state.editingProperty?.let { prop ->
        PropertyEditModal(
            title = "EDIT PROPERTY // ${prop.name.uppercase()}",
            initialName = prop.name,
            initialType = prop.type,
            initialDescription = prop.description ?: "",
            initialOptions = prop.options,
            initialIsArchived = prop.isArchived,
            isEditing = true,
            onDismiss = onCloseEditDialog,
            onSave = { name, type, desc, options, archived ->
                onUpdateProperty(prop.id, name, type, desc, options, archived)
            },
        )
    }

    // Delete Confirmation Dialog
    state.deleteConfirmationProperty?.let { prop ->
        AlertDialog(
            onDismissRequest = onCloseDeleteConfirmation,
            title = {
                Text(
                    text = "DELETE PROPERTY // ${prop.name.uppercase()}",
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${prop.name}'? All assigned values for this property across all notes in your vault will be permanently deleted.",
                    fontFamily = DotMatrixFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteProperty(prop.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(0.dp),
                ) {
                    Text("DELETE PERMANENTLY", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCloseDeleteConfirmation,
                    shape = RoundedCornerShape(0.dp),
                ) {
                    Text("CANCEL", fontFamily = DotMatrixFontFamily)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(0.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PropertyCardRow(
    property: PropertyDefinition,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = property.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (property.isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TypeBadge(type = property.type)
                if (property.isArchived) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[ARCHIVED]",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Row {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEdit()
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Property",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleArchive()
                }) {
                    Icon(
                        imageVector = if (property.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = if (property.isArchived) "Unarchive" else "Archive",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Property",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (!property.description.isNullOrBlank()) {
            Text(
                text = property.description,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = DotMatrixFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if ((property.type == PropertyType.SELECT || property.type == PropertyType.MULTI_SELECT) && property.options.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                property.options.forEach { opt ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypeBadge(type: PropertyType) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = DotMatrixFontFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun PropertyEditModal(
    title: String,
    initialName: String,
    initialType: PropertyType,
    initialDescription: String,
    initialOptions: List<String>,
    initialIsArchived: Boolean,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, PropertyType, String?, List<String>, Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var name by remember { mutableStateOf(initialName) }
    var selectedType by remember { mutableStateOf(initialType) }
    var description by remember { mutableStateOf(initialDescription) }
    var isArchived by remember { mutableStateOf(initialIsArchived) }
    val options = remember { mutableStateListOf(*initialOptions.toTypedArray()) }
    var newOptionText by remember { mutableStateOf("") }
    var expandedTypeDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = DotMatrixFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Property Name *", fontFamily = DotMatrixFontFamily) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Type Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Property Type *", fontFamily = DotMatrixFontFamily) },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedTypeDropdown = true },
                    )
                    DropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        PropertyType.entries.forEach { propType ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = propType.name,
                                        fontFamily = DotMatrixFontFamily,
                                        fontWeight = if (propType == selectedType) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    selectedType = propType
                                    expandedTypeDropdown = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)", fontFamily = DotMatrixFontFamily) },
                    singleLine = false,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Options Section for SELECT & MULTI_SELECT
                if (selectedType == PropertyType.SELECT || selectedType == PropertyType.MULTI_SELECT) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Text(
                        text = "PREDEFINED OPTIONS",
                        fontFamily = DotMatrixFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newOptionText,
                            onValueChange = { newOptionText = it },
                            placeholder = { Text("New Option...", fontFamily = DotMatrixFontFamily) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newOptionText.trim()
                                if (trimmed.isNotBlank() && !options.contains(trimmed)) {
                                    options.add(trimmed)
                                    newOptionText = ""
                                }
                            },
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text("+ ADD", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        options.forEachIndexed { index, opt ->
                            Row(
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = DotMatrixFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove option",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { options.removeAt(index) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(name, selectedType, description, options.toList(), isArchived)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = if (isEditing) "SAVE CHANGES" else "CREATE PROPERTY",
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(0.dp),
            ) {
                Text("CANCEL", fontFamily = DotMatrixFontFamily)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp),
    )
}
