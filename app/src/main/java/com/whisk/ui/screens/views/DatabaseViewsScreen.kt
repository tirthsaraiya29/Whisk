package com.whisk.ui.screens.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.whisk.domain.model.DatabaseView
import com.whisk.domain.model.ViewType
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.components.views.BoardView
import com.whisk.ui.components.views.CalendarView
import com.whisk.ui.components.views.GalleryView
import com.whisk.ui.components.views.ListView
import com.whisk.ui.components.views.TableView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseViewsScreen(
    state: DatabaseViewsUiState,
    onSelectView: (DatabaseView) -> Unit,
    onCreateView: (name: String, viewType: ViewType) -> Unit,
    onDuplicateView: (viewId: String) -> Unit,
    onDeleteView: (viewId: String) -> Unit,
    onOpenNote: (String) -> Unit,
    onUpdatePropertyValue: (noteId: String, definitionId: String, textVal: String?, numVal: Double?, boolVal: Boolean?, listVal: List<String>) -> Unit,
    onMoveNoteToGroup: (noteId: String, newGroup: String) -> Unit,
    onCreateInGroup: (group: String) -> Unit,
    onCreateOnDate: (dateKey: String) -> Unit,
    onMenuClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showViewDropdown by remember { mutableStateOf(false) }

    val activeView = state.activeView ?: DatabaseView(id = "tmp", name = "Vault Views", viewType = ViewType.TABLE)

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "Database Views",
                subtitle = "${state.filteredNotes.size} objects • View: ${activeView.name}",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDuplicateView(activeView.id)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate View",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (state.availableViews.size > 1) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteView(activeView.id)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete View",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
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
            // View Switcher Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // View Selector Dropdown
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showViewDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "${activeView.name} (${activeView.viewType.name})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = showViewDropdown,
                        onDismissRequest = { showViewDropdown = false },
                    ) {
                        state.availableViews.forEach { view ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${view.name} [${view.viewType.name}]",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (view.id == activeView.id) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    showViewDropdown = false
                                    onSelectView(view)
                                },
                            )
                        }
                    }
                }

                // Quick View Type Pills
                state.availableViews.forEach { view ->
                    val isSelected = view.id == activeView.id
                    if (!isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectView(view)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = view.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                // New View Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showCreateDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "New View",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

            // Render Selected Database View Component
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeView.viewType) {
                    ViewType.TABLE -> TableView(
                        notes = state.filteredNotes,
                        propertyDefinitions = state.propertyDefinitions,
                        notePropertyValues = state.notePropertyValues,
                        visiblePropertyIds = emptyList(),
                        onOpenNote = onOpenNote,
                        onUpdatePropertyValue = onUpdatePropertyValue,
                    )
                    ViewType.BOARD -> BoardView(
                        notes = state.filteredNotes,
                        onOpenNote = onOpenNote,
                        onMoveNoteToGroup = onMoveNoteToGroup,
                        onCreateInGroup = onCreateInGroup,
                    )
                    ViewType.CALENDAR -> CalendarView(
                        notes = state.filteredNotes,
                        datePropertyId = null,
                        notePropertyValues = state.notePropertyValues,
                        onOpenNote = onOpenNote,
                        onCreateOnDate = onCreateOnDate,
                    )
                    ViewType.GALLERY -> GalleryView(
                        notes = state.filteredNotes,
                        onOpenNote = onOpenNote,
                    )
                    ViewType.LIST -> ListView(
                        notes = state.filteredNotes,
                        onOpenNote = onOpenNote,
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateViewDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type ->
                onCreateView(name, type)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CreateViewDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, viewType: ViewType) -> Unit,
) {
    var viewName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ViewType.TABLE) }
    var showTypeDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Database View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = viewName,
                    onValueChange = { viewName = it },
                    label = { Text("View Name") },
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
                        ViewType.entries.forEach { type ->
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (viewName.isNotBlank()) {
                        onCreate(viewName.trim(), selectedType)
                    }
                },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Create View")
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
