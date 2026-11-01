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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.whisk.domain.model.DatabaseView
import com.whisk.domain.model.ViewType
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.components.views.BoardView
import com.whisk.ui.components.views.CalendarView
import com.whisk.ui.components.views.GalleryView
import com.whisk.ui.components.views.ListView
import com.whisk.ui.components.views.TableView
import com.whisk.ui.theme.DotMatrixFontFamily

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
                title = "DATABASE VIEWS",
                subtitle = "${state.filteredNotes.size} OBJECTS • VIEW: ${activeView.name.uppercase()}",
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // View Selector Dropdown
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                            .clickable { showViewDropdown = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "VIEW: ${activeView.name.uppercase()} [${activeView.viewType.name}]",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
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
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        state.availableViews.forEach { view ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${view.name.uppercase()} [${view.viewType.name}]",
                                        fontFamily = DotMatrixFontFamily,
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

                // Add New View Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCreateDialog = true
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create View",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Render active view representation
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (activeView.viewType) {
                    ViewType.TABLE -> {
                        TableView(
                            notes = state.filteredNotes,
                            propertyDefinitions = state.propertyDefinitions,
                            notePropertyValues = state.notePropertyValues,
                            visiblePropertyIds = activeView.visiblePropertyIds,
                            onOpenNote = onOpenNote,
                            onUpdatePropertyValue = onUpdatePropertyValue,
                        )
                    }
                    ViewType.LIST -> {
                        ListView(
                            notes = state.filteredNotes,
                            onOpenNote = onOpenNote,
                        )
                    }
                    ViewType.GALLERY -> {
                        GalleryView(
                            notes = state.filteredNotes,
                            onOpenNote = onOpenNote,
                        )
                    }
                    ViewType.BOARD -> {
                        BoardView(
                            notes = state.filteredNotes,
                            onOpenNote = onOpenNote,
                            onMoveNoteToGroup = onMoveNoteToGroup,
                            onCreateInGroup = onCreateInGroup,
                        )
                    }
                    ViewType.CALENDAR -> {
                        CalendarView(
                            notes = state.filteredNotes,
                            datePropertyId = activeView.datePropertyId,
                            notePropertyValues = state.notePropertyValues,
                            onOpenNote = onOpenNote,
                            onCreateOnDate = onCreateOnDate,
                        )
                    }
                }
            }
        }
    }

    // Create View Dialog
    if (showCreateDialog) {
        var viewName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(ViewType.TABLE) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "CREATE SAVED VIEW",
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = viewName,
                        onValueChange = { viewName = it },
                        placeholder = { Text("View Name...", fontFamily = DotMatrixFontFamily) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = DotMatrixFontFamily),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "VIEW REPRESENTATION TYPE:",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ViewType.entries.forEach { type ->
                            val isSelected = selectedType == type
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                    .clickable { selectedType = type }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = DotMatrixFontFamily,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
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
                            onCreateView(viewName.trim(), selectedType)
                            showCreateDialog = false
                        }
                    },
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("CREATE VIEW", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateDialog = false },
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
