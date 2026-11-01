package com.whisk.ui.screens.search

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.whisk.ui.theme.DotMatrixFontFamily

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
                title = "TERMINAL // SEARCH",
                subtitle = if (state.query.isNotBlank() || state.activePropertyFilter != null) {
                    "$resultCount ${if (resultCount == 1) "MATCH" else "MATCHES"} FOUND"
                } else {
                    "OFFLINE FULL-TEXT & PROPERTY SEARCH ENGINE"
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
            // Terminal Input Field
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "> TYPE QUERY OR TAG...",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = DotMatrixFontFamily),
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                singleLine = true,
            )

            // Object Type Chips Row
            if (state.objectTypes.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        val isAll = state.selectedTypeId == null
                        Box(
                            modifier = Modifier
                                .background(if (isAll) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType?.invoke(null)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "ALL OBJECTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = if (isAll) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    items(state.objectTypes) { typeDef ->
                        val isSelected = state.selectedTypeId == typeDef.id
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType?.invoke(if (isSelected) null else typeDef.id)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = typeDef.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Property Filter & Quick Suggestion Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Property Filter Button / Active Chip
                if (state.activePropertyFilter == null) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFilterModal = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ FILTER BY PROPERTY",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else {
                    val filterProp = state.availablePropertyDefinitions.find { it.id == state.activePropertyFilter.propertyDefinitionId }
                    val filterName = filterProp?.name?.uppercase() ?: "PROPERTY"
                    val filterText = "$filterName ${state.activePropertyFilter.operator.name} '${state.activePropertyFilter.value ?: ""}'"

                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = filterText,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Property Filter",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .size(14.dp)
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
                        listOf("WELCOME", "ARCHITECTURE", "MERMAID", "KOTLIN", "TODO").forEach { keyword ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onQueryChange(keyword)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "> $keyword",
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Results Edge-to-Edge List
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
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        Text(
                            text = if (state.query.isBlank() && state.activePropertyFilter == null) "SEARCH VAULT" else "ZERO MATCHES",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (state.query.isBlank() && state.activePropertyFilter == null) {
                                "ENTER KEYWORDS, TAGS, OR PROPERTY FILTERS ABOVE"
                            } else {
                                "MODIFY QUERY PARAMETERS OR PROPERTY FILTER CRITERIA"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                ) {
                    items(state.results, key = { it.id }) { note ->
                        NoteRowCard(note = note, onClick = { onNoteClick(note.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    }
                }
            }
        }
    }

    // Property Filter Builder Modal Dialog
    if (showFilterModal) {
        PropertyFilterBuilderDialog(
            definitions = state.availablePropertyDefinitions,
            onDismiss = { showFilterModal = false },
            onApplyFilter = { filter ->
                showFilterModal = false
                onSetPropertyFilter(filter)
            },
        )
    }
}

@Composable
private fun PropertyFilterBuilderDialog(
    definitions: List<PropertyDefinition>,
    onDismiss: () -> Unit,
    onApplyFilter: (PropertyFilter) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    var selectedProp by remember { mutableStateOf(definitions.firstOrNull()) }
    var selectedOp by remember { mutableStateOf(PropertyOperator.EQUALS) }
    var filterValueText by remember { mutableStateOf("") }

    var expandedPropMenu by remember { mutableStateOf(false) }
    var expandedOpMenu by remember { mutableStateOf(false) }

    // Allowed operators based on property type
    val allowedOperators = remember(selectedProp) {
        when (selectedProp?.type) {
            PropertyType.TEXT -> listOf(PropertyOperator.EQUALS, PropertyOperator.NOT_EQUALS, PropertyOperator.CONTAINS, PropertyOperator.IS_EMPTY, PropertyOperator.IS_NOT_EMPTY)
            PropertyType.NUMBER -> listOf(PropertyOperator.EQUALS, PropertyOperator.NOT_EQUALS, PropertyOperator.GREATER_THAN, PropertyOperator.LESS_THAN, PropertyOperator.IS_EMPTY, PropertyOperator.IS_NOT_EMPTY)
            PropertyType.DATE -> listOf(PropertyOperator.EQUALS, PropertyOperator.BEFORE, PropertyOperator.AFTER, PropertyOperator.IS_EMPTY, PropertyOperator.IS_NOT_EMPTY)
            PropertyType.CHECKBOX -> listOf(PropertyOperator.EQUALS, PropertyOperator.IS_EMPTY, PropertyOperator.IS_NOT_EMPTY)
            PropertyType.SELECT -> listOf(PropertyOperator.EQUALS, PropertyOperator.NOT_EQUALS, PropertyOperator.IS_EMPTY, PropertyOperator.IS_NOT_EMPTY)
            PropertyType.MULTI_SELECT -> listOf(PropertyOperator.CONTAINS, PropertyOperator.IS_EMPTY, PropertyOperator.IS_NOT_EMPTY)
            PropertyType.RELATION -> listOf(PropertyOperator.EQUALS, PropertyOperator.IS_EMPTY, PropertyOperator.IS_NOT_EMPTY)
            null -> listOf(PropertyOperator.EQUALS)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "FILTER BY PROPERTY",
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
                if (definitions.isEmpty()) {
                    Text(
                        text = "No property definitions available. Create properties in Property Management first.",
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Property Picker Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedProp?.name ?: "Select Property",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Property", fontFamily = DotMatrixFontFamily) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedPropMenu = true },
                        )
                        DropdownMenu(
                            expanded = expandedPropMenu,
                            onDismissRequest = { expandedPropMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        ) {
                            definitions.forEach { def ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(def.name, fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("[${def.type.name}]", fontFamily = DotMatrixFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        selectedProp = def
                                        selectedOp = when (def.type) {
                                            PropertyType.MULTI_SELECT -> PropertyOperator.CONTAINS
                                            else -> PropertyOperator.EQUALS
                                        }
                                        filterValueText = ""
                                        expandedPropMenu = false
                                    },
                                )
                            }
                        }
                    }

                    // Operator Picker Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedOp.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Operator", fontFamily = DotMatrixFontFamily) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedOpMenu = true },
                        )
                        DropdownMenu(
                            expanded = expandedOpMenu,
                            onDismissRequest = { expandedOpMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        ) {
                            allowedOperators.forEach { op ->
                                DropdownMenuItem(
                                    text = { Text(op.name, fontFamily = DotMatrixFontFamily) },
                                    onClick = {
                                        selectedOp = op
                                        expandedOpMenu = false
                                    },
                                )
                            }
                        }
                    }

                    // Value Input
                    if (selectedOp != PropertyOperator.IS_EMPTY && selectedOp != PropertyOperator.IS_NOT_EMPTY) {
                        when (selectedProp?.type) {
                            PropertyType.SELECT, PropertyType.MULTI_SELECT -> {
                                var expandedValMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = filterValueText.ifBlank { "Select Value..." },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Target Value", fontFamily = DotMatrixFontFamily) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedValMenu = true },
                                    )
                                    DropdownMenu(
                                        expanded = expandedValMenu,
                                        onDismissRequest = { expandedValMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        selectedProp?.options?.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, fontFamily = DotMatrixFontFamily) },
                                                onClick = {
                                                    filterValueText = opt
                                                    expandedValMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            PropertyType.CHECKBOX -> {
                                var expandedValMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = filterValueText.ifBlank { "true" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Target Value", fontFamily = DotMatrixFontFamily) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedValMenu = true },
                                    )
                                    DropdownMenu(
                                        expanded = expandedValMenu,
                                        onDismissRequest = { expandedValMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                                    ) {
                                        listOf("true", "false").forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, fontFamily = DotMatrixFontFamily) },
                                                onClick = {
                                                    filterValueText = opt
                                                    expandedValMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                OutlinedTextField(
                                    value = filterValueText,
                                    onValueChange = { filterValueText = it },
                                    label = { Text("Filter Value", fontFamily = DotMatrixFontFamily) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
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
                    val prop = selectedProp
                    if (prop != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onApplyFilter(
                            PropertyFilter(
                                propertyDefinitionId = prop.id,
                                operator = selectedOp,
                                value = filterValueText.ifBlank { null },
                            ),
                        )
                    }
                },
                enabled = selectedProp != null,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("APPLY FILTER", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
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
