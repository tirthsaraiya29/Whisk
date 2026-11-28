package com.whisk.ui.screens.notes

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.whisk.domain.model.RelationshipType
import com.whisk.ui.components.EnhancedMarkdownEditor
import com.whisk.ui.components.RelationshipSection
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.components.getIconForObjectType
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
    onTogglePin: () -> Unit = {},
    onDuplicateNote: () -> Unit = {},
    onUpdateAccentColor: (String?) -> Unit = {},
    onToggleBiometricLock: () -> Unit = {},
    onUnlockNote: () -> Unit = {},
    allNotes: List<Note> = emptyList(),
) {
    val note = state.note ?: return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showBacklinksSheet by remember { mutableStateOf(false) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val folderName = (note.folder ?: "Vault").ifBlank { "Vault" }
    val breadcrumbTitle = "$folderName / ${note.title.ifBlank { "Untitled" }}"

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = breadcrumbTitle,
                subtitle = when (state.saveState) {
                    SaveState.SAVING -> "Saving..."
                    SaveState.SAVED -> "Saved"
                    SaveState.ERROR -> "Error saving"
                    else -> "Saved"
                },
                saveStateText = if (state.saveState == SaveState.SAVED) "Saved" else null,
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
                            showInspectorSheet = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Note Metadata & Properties",
                            tint = if (state.propertyItems.any { it.value != null }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

                    // More Menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onTogglePin()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.isBiometricLocked) "Unlock Biometrics" else "Lock with Biometrics") },
                                leadingIcon = { Icon(if (state.isBiometricLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onToggleBiometricLock()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate Note") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onDuplicateNote()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Markdown") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Markdown", note.body)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied Markdown to clipboard", Toast.LENGTH_SHORT).show()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Set Accent Color") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showColorPicker = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Note") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    onDeleteNote()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        if (state.isBiometricLocked && !state.isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Text(
                        text = "Biometrically Encrypted Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = "This document is biometrically locked to protect sensitive vault data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (com.whisk.security.BiometricAuthHelper.isBiometricAvailable(context)) {
                                com.whisk.security.BiometricAuthHelper.promptBiometricAuth(
                                    context = context,
                                    onSuccess = { onUnlockNote() },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() },
                                )
                            } else {
                                Toast.makeText(context, "Biometric authentication is not configured on this device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Unlock with Biometrics")
                    }
                }
            }
        } else {
            // Pure Distraction-Free Obsidian Writing Surface
            EnhancedMarkdownEditor(
                title = note.title,
                onTitleChange = { newTitle -> onTitleChange(newTitle, note.body) },
                body = note.body,
                onBodyChange = { newBody -> onTitleChange(note.title, newBody) },
                onLinkClick = onOpenNote,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                allNotes = allNotes,
            )
        }
    }

    // Metadata Inspector Bottom Sheet
    if (showInspectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInspectorSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Note Properties & Metadata",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    IconButton(
                        onClick = { showInspectorSheet = false },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Inspector")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NotePropertiesSection(
                    propertyItems = state.propertyItems,
                    allDefinitions = state.allDefinitions,
                    allNotes = allNotes,
                    onUpdatePropertyValue = onUpdatePropertyValue,
                    onDeletePropertyValue = onDeletePropertyValue,
                    onManageProperties = onManageProperties,
                    onOpenNote = onOpenNote,
                )

                if (onAddRelationship != null && onRemoveRelationship != null && onQueryObjects != null) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    RelationshipSection(
                        relationships = state.relationships,
                        objectTypes = state.objectTypes,
                        onQueryObjects = onQueryObjects,
                        onAddRelationship = onAddRelationship,
                        onRemoveRelationship = onRemoveRelationship,
                        onOpenObject = onOpenNote,
                    )
                }

                if (state.relatedKnowledge.isNotEmpty()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Discovered Knowledge Connections",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        state.relatedKnowledge.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showInspectorSheet = false
                                        onOpenNote(item.note.id)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.note.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = item.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${(item.score * 100).toInt()}% match",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        val accents = listOf(
            "#7C3AED" to "Obsidian Purple",
            "#3B82F6" to "Whisk Blue",
            "#10B981" to "Emerald Green",
            "#F59E0B" to "Amber",
            "#EF4444" to "Crimson Red",
            null to "Reset Color",
        )
        ModalBottomSheet(
            onDismissRequest = { showColorPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Note Accent Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                accents.forEach { (hex, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onUpdateAccentColor(hex)
                                showColorPicker = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (hex != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(android.graphics.Color.parseColor(hex))),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                            )
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }

    // Backlinks & Mentions Bottom Sheet
    if (showBacklinksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBacklinksSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "Backlinks & Mentions for '${note.title}'",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "Explicit Backlinks (${state.backlinks.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (state.backlinks.isEmpty()) {
                            Text(
                                text = "No inbound backlinks found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(state.backlinks) { backlink ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showBacklinksSheet = false
                                    onOpenNote(backlink.sourceNoteId)
                                }
                                .padding(12.dp),
                        ) {
                            Text(
                                text = backlink.sourceNoteTitle,
                                style = MaterialTheme.typography.bodyMedium,
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

                    item {
                        Text(
                            text = "Unlinked Mentions (${state.unlinkedMentions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (state.unlinkedMentions.isEmpty()) {
                            Text(
                                text = "No unlinked mentions found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(state.unlinkedMentions) { mention ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFD97706).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "In: ${mention.sourceNoteTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Button(
                                    onClick = {
                                        onConvertUnlinkedMention?.invoke(mention.matchedText, mention.targetNoteTitle)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                ) {
                                    Text(
                                        text = "+ Link",
                                        style = MaterialTheme.typography.labelSmall,
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

    val activePropertyItems = propertyItems.filter { it.value != null }
    val availableDefinitions = allDefinitions.filter { def ->
        activePropertyItems.none { it.definition.id == def.id }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
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
                    text = "Properties",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

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
        }

        AnimatedVisibility(visible = expandedSection) {
            if (activePropertyItems.isEmpty()) {
                Text(
                    text = "No custom properties assigned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
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
    val def = item.definition
    val value = item.value

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = def.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(110.dp),
        )

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
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PropertyType.CHECKBOX -> {
                    val checked = value?.booleanValue ?: false
                    Switch(
                        checked = checked,
                        onCheckedChange = { onUpdate(null, null, it, emptyList()) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                else -> {
                    var textInput by remember(value?.textValue) { mutableStateOf(value?.textValue ?: "") }
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            onUpdate(it, null, null, emptyList())
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Property",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
