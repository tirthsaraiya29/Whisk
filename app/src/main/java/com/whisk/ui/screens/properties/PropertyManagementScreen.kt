package com.whisk.ui.screens.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyType
import com.whisk.ui.components.WhiskTopBar

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
                title = "Property Schemas",
                subtitle = "Workspace Metadata Definitions",
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
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Property",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
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
                            text = "No property definitions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Create custom metadata schemas to structure your notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.properties, key = { it.id }) { prop ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = prop.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = prop.type.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }

                                    if (!prop.description.isNullOrBlank()) {
                                        Text(
                                            text = prop.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }

                                Row {
                                    IconButton(onClick = { onOpenEditDialog(prop) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Property", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onToggleArchive(prop) }) {
                                        Icon(if (prop.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = "Archive Property")
                                    }
                                    IconButton(onClick = { onOpenDeleteConfirmation(prop) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Property", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        PropertyEditDialog(
            title = "Create Property Definition",
            onDismiss = onCloseCreateDialog,
            onSave = { name, type, desc, options ->
                onCreateProperty(name, type, desc, options)
                onCloseCreateDialog()
            },
        )
    }

    if (state.editingProperty != null) {
        val prop = state.editingProperty
        PropertyEditDialog(
            title = "Edit Property Definition",
            initialName = prop.name,
            initialType = prop.type,
            initialDesc = prop.description ?: "",
            initialOptions = prop.options,
            onDismiss = onCloseEditDialog,
            onSave = { name, type, desc, options ->
                onUpdateProperty(prop.id, name, type, desc, options, prop.isArchived)
                onCloseEditDialog()
            },
        )
    }

    if (state.deleteConfirmationProperty != null) {
        val prop = state.deleteConfirmationProperty
        AlertDialog(
            onDismissRequest = onCloseDeleteConfirmation,
            title = { Text("Delete Property Definition") },
            text = { Text("Are you sure you want to delete '${prop.name}'? All assigned values across notes will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProperty(prop.id)
                        onCloseDeleteConfirmation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCloseDeleteConfirmation,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PropertyEditDialog(
    title: String,
    initialName: String = "",
    initialType: PropertyType = PropertyType.TEXT,
    initialDesc: String = "",
    initialOptions: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, type: PropertyType, desc: String?, options: List<String>) -> Unit,
) {
    var nameText by remember { mutableStateOf(initialName) }
    var selectedType by remember { mutableStateOf(initialType) }
    var descText by remember { mutableStateOf(initialDesc) }
    var newOptionText by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf<String>().apply { addAll(initialOptions) } }
    var showTypeDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Property Name") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showTypeDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Type: ${selectedType.name}", style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false },
                    ) {
                        PropertyType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedType = type
                                    showTypeDropdown = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Description (Optional)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (selectedType == PropertyType.SELECT || selectedType == PropertyType.MULTI_SELECT) {
                    Text("Options:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        options.forEach { opt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { options.remove(opt) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text("$opt ✕", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newOptionText,
                            onValueChange = { newOptionText = it },
                            placeholder = { Text("New Option") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (newOptionText.isNotBlank()) {
                                    options.add(newOptionText.trim())
                                    newOptionText = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onSave(nameText.trim(), selectedType, descText.ifBlank { null }, options.toList())
                    }
                },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Save")
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
