package com.whisk.ui.screens.notes

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyItem
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyType
import com.whisk.ui.components.MarkdownEditor
import com.whisk.ui.components.WhiskTopBar
import com.whisk.domain.model.RelationshipType
import com.whisk.ui.components.RelationshipSection
import com.whisk.ui.components.getIconForObjectType
import com.whisk.ui.theme.DotMatrixFontFamily
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    state: NoteDetailUiState,
    onTitleChange: (String, String) -> Unit,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenAiWithNote: () -> Unit,
    onUpdatePropertyValue: (definitionId: String, textValue: String?, numberValue: Double?, booleanValue: Boolean?, listValue: List<String>) -> Unit,
    onDeletePropertyValue: (definitionId: String) -> Unit,
    onManageProperties: () -> Unit,
    onChangeObjectType: ((String) -> Unit)? = null,
    onAddRelationship: ((targetObjectId: String, type: RelationshipType) -> Unit)? = null,
    onRemoveRelationship: ((relationshipId: String) -> Unit)? = null,
    onQueryObjects: (suspend (String, String?) -> List<Note>)? = null,
    onOpenProjectWorkspace: ((String) -> Unit)? = null,
    onConvertUnlinkedMention: ((matchedText: String, targetTitle: String) -> Unit)? = null,
    onCreateNoteFromBrokenLink: ((targetTitle: String) -> Unit)? = null,
    allNotes: List<Note> = emptyList(),
) {
    val note = state.note ?: return
    val haptic = LocalHapticFeedback.current
    var showBacklinksSheet by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    val folderName = (note.folder ?: "VAULT").ifBlank { "VAULT" }.uppercase()
    val breadcrumbTitle = "$folderName / ${note.title.ifBlank { "UNTITLED.MD" }.uppercase()}"
    val saveText = when (state.saveState) {
        SaveState.SAVING -> "Saving..."
        SaveState.SAVED -> "Saved"
        SaveState.ERROR -> "Error"
        else -> "Saved"
    }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = breadcrumbTitle,
                subtitle = saveText.uppercase(),
                saveStateText = saveText,
                onBackClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackClick()
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        },
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showBacklinksSheet = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Backlinks (${state.backlinks.size})",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenAiWithNote()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Copilot",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteNote()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.onSurface,
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
            // Object Type & Flagship Workspace Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Object Type Selector Dropdown
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showTypeMenu = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = getIconForObjectType(note.typeId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TYPE: ${note.typeId.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        state.objectTypes.forEach { typeDef ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getIconForObjectType(typeDef.id),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = typeDef.name.uppercase(),
                                            fontFamily = DotMatrixFontFamily,
                                            fontWeight = if (typeDef.id == note.typeId) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                },
                                onClick = {
                                    showTypeMenu = false
                                    onChangeObjectType?.invoke(typeDef.id)
                                },
                            )
                        }
                    }
                }

                // Flagship Project Workspace Button (if object type is Project)
                if (note.typeId == "project" && onOpenProjectWorkspace != null) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenProjectWorkspace(note.id)
                        },
                        shape = RoundedCornerShape(0.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            text = "PROJECT WORKSPACE ->",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Note Properties Header Section
            NotePropertiesSection(
                propertyItems = state.propertyItems,
                allDefinitions = state.allDefinitions,
                allNotes = allNotes,
                onUpdatePropertyValue = onUpdatePropertyValue,
                onDeletePropertyValue = onDeletePropertyValue,
                onManageProperties = onManageProperties,
                onOpenNote = onOpenNote,
            )

            // Explicit Object Relationships Section
            if (onAddRelationship != null && onRemoveRelationship != null && onQueryObjects != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                RelationshipSection(
                    relationships = state.relationships,
                    objectTypes = state.objectTypes,
                    onQueryObjects = onQueryObjects,
                    onAddRelationship = onAddRelationship,
                    onRemoveRelationship = onRemoveRelationship,
                    onOpenObject = onOpenNote,
                )
            }

            // Related Knowledge Section
            if (state.relatedKnowledge.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "RELATED KNOWLEDGE // DISCOVERED",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    state.relatedKnowledge.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenNote(item.note.id) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.note.title} [${item.note.typeId.uppercase()}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = DotMatrixFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = item.explanation,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${(item.score * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            MarkdownEditor(
                title = note.title,
                onTitleChange = { newTitle -> onTitleChange(newTitle, note.body) },
                body = note.body,
                onBodyChange = { newBody -> onTitleChange(note.title, newBody) },
                onLinkClick = onOpenNote,
                modifier = Modifier.fillMaxSize(),
                allNotes = allNotes,
            )
        }
    }

    // Backlinks & Mentions Bottom Sheet
    if (showBacklinksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBacklinksSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "CONNECTIONS & MENTIONS // '${note.title.uppercase()}'",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Explicit Backlinks
                    item {
                        Text(
                            text = "EXPLICIT BACKLINKS (${state.backlinks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        if (state.backlinks.isEmpty()) {
                            Text(
                                text = "NO INBOUND BACKLINKS DETECTED.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(state.backlinks) { backlink ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showBacklinksSheet = false
                                    onOpenNote(backlink.sourceNoteId)
                                }
                                .padding(10.dp),
                        ) {
                            Text(
                                text = backlink.sourceNoteTitle.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = DotMatrixFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (backlink.snippet.isNotBlank()) {
                                Text(
                                    text = backlink.snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }

                    // 2. Unlinked Mentions
                    item {
                        Text(
                            text = "UNLINKED MENTIONS (${state.unlinkedMentions.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                        )
                        if (state.unlinkedMentions.isEmpty()) {
                            Text(
                                text = "NO UNLINKED MENTIONS DETECTED.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(state.unlinkedMentions) { mention ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFD97706), RoundedCornerShape(0.dp))
                                .padding(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "IN: ${mention.sourceNoteTitle.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = DotMatrixFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Button(
                                    onClick = {
                                        onConvertUnlinkedMention?.invoke(mention.matchedText, mention.targetNoteTitle)
                                    },
                                    shape = RoundedCornerShape(0.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                ) {
                                    Text(
                                        text = "+ LINK",
                                        fontFamily = DotMatrixFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text(
                                text = mention.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    // 3. Broken Links
                    if (state.brokenLinks.isNotEmpty()) {
                        item {
                            Text(
                                text = "UNRESOLVED / BROKEN LINKS (${state.brokenLinks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                            )
                        }

                        items(state.brokenLinks) { broken ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFDC2626), RoundedCornerShape(0.dp))
                                    .padding(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "[[${broken.targetTitle}]]",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = DotMatrixFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                    )
                                    Button(
                                        onClick = {
                                            onCreateNoteFromBrokenLink?.invoke(broken.targetTitle)
                                        },
                                        shape = RoundedCornerShape(0.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    ) {
                                        Text(
                                            text = "CREATE OBJECT",
                                            fontFamily = DotMatrixFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotePropertiesSection(
    propertyItems: List<NotePropertyItem>,
    allDefinitions: List<PropertyDefinition>,
    allNotes: List<Note>,
    onUpdatePropertyValue: (String, String?, Double?, Boolean?, List<String>) -> Unit,
    onDeletePropertyValue: (String) -> Unit,
    onManageProperties: () -> Unit,
    onOpenNote: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expandedSection by remember { mutableStateOf(true) }
    var showAddMenu by remember { mutableStateOf(false) }

    // Filter properties that are set or available
    val activePropertyItems = propertyItems.filter { it.value != null }
    val availableDefinitions = allDefinitions.filter { def ->
        activePropertyItems.none { it.definition.id == def.id }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Properties Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { expandedSection = !expandedSection },
            ) {
                Icon(
                    imageVector = if (expandedSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "PROPERTIES",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (activePropertyItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${activePropertyItems.size}]",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Add Property Dropdown Menu
                Box {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showAddMenu = true
                        },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Property",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        if (availableDefinitions.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "NO UNASSIGNED PROPERTIES",
                                        fontFamily = DotMatrixFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                onClick = {},
                                enabled = false,
                            )
                        } else {
                            availableDefinitions.forEach { def ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = def.name.uppercase(),
                                                fontFamily = DotMatrixFontFamily,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "[${def.type.name}]",
                                                fontFamily = DotMatrixFontFamily,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        // Initialize default empty value
                                        when (def.type) {
                                            PropertyType.CHECKBOX -> onUpdatePropertyValue(def.id, null, null, false, emptyList())
                                            PropertyType.MULTI_SELECT -> onUpdatePropertyValue(def.id, null, null, null, emptyList())
                                            else -> onUpdatePropertyValue(def.id, "", null, null, emptyList())
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onManageProperties()
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Manage Properties",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Animated Property Items List
        AnimatedVisibility(visible = expandedSection) {
            if (activePropertyItems.isEmpty()) {
                Text(
                    text = "> NO METADATA PROPERTIES ASSIGNED. CLICK '+' TO ADD A PROPERTY.",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    activePropertyItems.forEach { item ->
                        PropertyItemRow(
                            item = item,
                            allNotes = allNotes,
                            onUpdate = { textVal, numVal, boolVal, listVal ->
                                onUpdatePropertyValue(item.definition.id, textVal, numVal, boolVal, listVal)
                            },
                            onDelete = { onDeletePropertyValue(item.definition.id) },
                            onOpenNote = onOpenNote,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyItemRow(
    item: NotePropertyItem,
    allNotes: List<Note>,
    onUpdate: (String?, Double?, Boolean?, List<String>) -> Unit,
    onDelete: () -> Unit,
    onOpenNote: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val def = item.definition
    val value = item.value

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Property Label
        Row(
            modifier = Modifier.width(110.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = def.name,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DotMatrixFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Type-Specific Input Control
        Box(modifier = Modifier.weight(1f)) {
            when (def.type) {
                PropertyType.TEXT -> {
                    var textInput by remember(value?.textValue) { mutableStateOf(value?.textValue ?: "") }
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            onUpdate(it, null, null, emptyList())
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = DotMatrixFontFamily),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PropertyType.NUMBER -> {
                    var numInput by remember(value?.numberValue) { mutableStateOf(value?.numberValue?.toString() ?: "") }
                    var isError by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = numInput,
                        onValueChange = {
                            numInput = it
                            val parsed = it.toDoubleOrNull()
                            if (it.isBlank() || parsed != null) {
                                isError = false
                                onUpdate(null, parsed, null, emptyList())
                            } else {
                                isError = true
                            }
                        },
                        isError = isError,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = DotMatrixFontFamily),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PropertyType.DATE -> {
                    val context = LocalContext.current
                    val currentDateStr = value?.textValue ?: ""

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable {
                                val cal = Calendar.getInstance()
                                val dialog = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val formatted = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                                        onUpdate(formatted, null, null, emptyList())
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH),
                                )
                                dialog.show()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentDateStr.ifBlank { "SELECT DATE..." },
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = if (currentDateStr.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                PropertyType.CHECKBOX -> {
                    val checked = value?.booleanValue ?: false
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = checked,
                            onCheckedChange = { onUpdate(null, null, it, emptyList()) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (checked) "TRUE" else "FALSE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                PropertyType.SELECT -> {
                    var expandedMenu by remember { mutableStateOf(false) }
                    val currentVal = value?.textValue ?: ""

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                .clickable { expandedMenu = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = currentVal.ifBlank { "SELECT OPTION..." },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = DotMatrixFontFamily,
                                    color = if (currentVal.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        ) {
                            def.options.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt,
                                            fontFamily = DotMatrixFontFamily,
                                            fontWeight = if (opt == currentVal) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        expandedMenu = false
                                        onUpdate(opt, null, null, emptyList())
                                    },
                                )
                            }
                        }
                    }
                }

                PropertyType.MULTI_SELECT -> {
                    var showMultiSelectModal by remember { mutableStateOf(false) }
                    val selectedList = value?.listValue ?: emptyList()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { showMultiSelectModal = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        if (selectedList.isEmpty()) {
                            Text(
                                text = "SELECT MULTIPLE OPTIONS...",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = selectedList.joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    if (showMultiSelectModal) {
                        MultiSelectDialog(
                            title = def.name,
                            options = def.options,
                            initialSelected = selectedList,
                            onDismiss = { showMultiSelectModal = false },
                            onConfirm = { updatedSelection ->
                                showMultiSelectModal = false
                                onUpdate(null, null, null, updatedSelection)
                            },
                        )
                    }
                }

                PropertyType.RELATION -> {
                    var showRelationPicker by remember { mutableStateOf(false) }
                    val targetNoteId = value?.textValue
                    val relatedTitle = item.relatedNoteTitle ?: "Select related note..."

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable { showRelationPicker = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = relatedTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = DotMatrixFontFamily,
                                    color = if (targetNoteId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            if (targetNoteId != null) {
                                Text(
                                    text = "[VIEW]",
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onOpenNote(targetNoteId) },
                                )
                            }
                        }
                    }

                    if (showRelationPicker) {
                        RelationNotePickerModal(
                            allNotes = allNotes,
                            onDismiss = { showRelationPicker = false },
                            onSelectNote = { selectedNoteId ->
                                showRelationPicker = false
                                onUpdate(selectedNoteId, null, null, emptyList())
                            },
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDelete()
            },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear Property Value",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun MultiSelectDialog(
    title: String,
    options: List<String>,
    initialSelected: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val selected = remember { mutableStateListOf(*initialSelected.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SELECT OPTIONS // $title",
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
            ) {
                if (options.isEmpty()) {
                    Text(
                        text = "No predefined options configured for this property.",
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    options.forEach { opt ->
                        val isChecked = selected.contains(opt)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selected.remove(opt) else selected.add(opt)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selected.add(opt) else selected.remove(opt)
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = opt,
                                fontFamily = DotMatrixFontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.toList()) },
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("APPLY SELECTION", fontFamily = DotMatrixFontFamily, fontWeight = FontWeight.Bold)
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

@Composable
private fun RelationNotePickerModal(
    allNotes: List<Note>,
    onDismiss: () -> Unit,
    onSelectNote: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(searchQuery, allNotes) {
        if (searchQuery.isBlank()) {
            allNotes
        } else {
            allNotes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.body.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "LINK TO EXISTING NOTE",
                fontFamily = DotMatrixFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search vault notes...", fontFamily = DotMatrixFontFamily) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No matching notes found.",
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                                    .clickable { onSelectNote(note.id) }
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = note.title.ifBlank { "Untitled Note" },
                                    fontFamily = DotMatrixFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
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
