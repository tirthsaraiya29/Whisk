package com.whisk.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyFilter
import com.whisk.domain.model.PropertyOperator
import com.whisk.domain.model.PropertyType
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.screens.notes.NoteRowCard

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSetPropertyFilter: (PropertyFilter?) -> Unit,
    onNoteClick: (String) -> Unit,
    onSelectType: ((String?) -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val resultCount = state.results.size
    var showFilterModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "Vault Search",
                subtitle = if (state.query.isNotBlank() || state.activePropertyFilter != null) {
                    "$resultCount ${if (resultCount == 1) "match" else "matches"} found"
                } else {
                    "Search notes, tags & metadata"
                },
                onMenuClick = onMenuClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Knowledge Gap Banner
            if (state.knowledgeGaps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Knowledge Gaps Detected (${state.knowledgeGaps.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    state.knowledgeGaps.take(2).forEach { gap ->
                        Text(
                            text = "• [${gap.gapType}] ${gap.objectTitle}: ${gap.suggestionText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Search Input Field
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Search notes, tags, or properties...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQueryChange("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear query")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )

            // Object Type Chips Row
            if (state.objectTypes.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        val isAll = state.selectedTypeId == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAll) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType?.invoke(null)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "All Types",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    items(state.objectTypes) { typeDef ->
                        val isSelected = state.selectedTypeId == typeDef.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType?.invoke(if (isSelected) null else typeDef.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = typeDef.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Property Filter & Quick Suggestion Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.activePropertyFilter == null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFilterModal = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filter by Property",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else {
                    val filterProp = state.availablePropertyDefinitions.find { it.id == state.activePropertyFilter.propertyDefinitionId }
                    val filterName = filterProp?.name ?: "Property"
                    val filterText = "$filterName ${state.activePropertyFilter.operator.name} '${state.activePropertyFilter.value ?: ""}'"

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = filterText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Property Filter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSetPropertyFilter(null)
                                },
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Suggestion chips
                if (state.query.isBlank() && state.activePropertyFilter == null) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Welcome", "Architecture", "Mermaid", "Kotlin", "Todo").forEach { keyword ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onQueryChange(keyword)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = keyword,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Results List
            if (state.results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.query.isBlank() && state.activePropertyFilter == null) "Type to search your vault" else "No matching notes found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.results, key = { it.id }) { note ->
                        NoteRowCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Property Filter Dialog
    if (showFilterModal) {
        PropertyFilterDialog(
            definitions = state.availablePropertyDefinitions,
            onDismiss = { showFilterModal = false },
            onApply = { filter ->
                onSetPropertyFilter(filter)
                showFilterModal = false
            },
        )
    }
}

@Composable
private fun PropertyFilterDialog(
    definitions: List<PropertyDefinition>,
    onDismiss: () -> Unit,
    onApply: (PropertyFilter) -> Unit,
) {
    var selectedDef by remember { mutableStateOf(definitions.firstOrNull()) }
    var selectedOp by remember { mutableStateOf(PropertyOperator.EQUALS) }
    var filterValue by remember { mutableStateOf("") }
    var showDefDropdown by remember { mutableStateOf(false) }
    var showOpDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter by Property",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Property Definition Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDefDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(selectedDef?.name ?: "Select Property", style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showDefDropdown,
                        onDismissRequest = { showDefDropdown = false },
                    ) {
                        definitions.forEach { def ->
                            DropdownMenuItem(
                                text = { Text(def.name, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedDef = def
                                    showDefDropdown = false
                                },
                            )
                        }
                    }
                }

                // Operator Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showOpDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(selectedOp.name, style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showOpDropdown,
                        onDismissRequest = { showOpDropdown = false },
                    ) {
                        PropertyOperator.values().forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op.name, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedOp = op
                                    showOpDropdown = false
                                },
                            )
                        }
                    }
                }

                // Value Text Field
                OutlinedTextField(
                    value = filterValue,
                    onValueChange = { filterValue = it },
                    label = { Text("Filter Value") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val def = selectedDef ?: return@Button
                    onApply(
                        PropertyFilter(
                            propertyDefinitionId = def.id,
                            operator = selectedOp,
                            value = filterValue.ifBlank { null },
                        ),
                    )
                },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Apply Filter")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
