package com.whisk.ui.screens.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.ui.components.DismissibleHintBanner
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.theme.DotMatrixFontFamily
import com.whisk.ui.theme.VercelMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    state: NotesUiState,
    onNoteClick: (String) -> Unit,
    onNewNoteClick: () -> Unit,
    onFilterChange: (NoteFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTogglePin: (Note) -> Unit = {},
    onToggleFavorite: (Note) -> Unit = {},
    onToggleSelect: (String) -> Unit = {},
    onLongClickNote: (String) -> Unit = {},
    onClearMultiSelect: () -> Unit = {},
    onBatchDelete: () -> Unit = {},
    onRestoreNote: (String) -> Unit = {},
    showHint: Boolean = true,
    onDismissHint: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    onCommandPaletteClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "WHISK // VAULT",
                subtitle = "${state.notes.size} OBJECTS LOADED",
                onMenuClick = onMenuClick,
                onCommandPaletteClick = onCommandPaletteClick,
                actions = {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNewNoteClick()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "+ NEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFilterChange(NoteFilter.All)
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Vault")
                    }
                },
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
            // Batch Multi-Select Header Bar
            if (state.selectedNoteIds.isNotEmpty() || state.isMultiSelectMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.selectedNoteIds.size} OBJECTS SELECTED",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.onPrimary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBatchDelete()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "[TRASH ALL]",
                                fontFamily = DotMatrixFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.onPrimary)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onClearMultiSelect()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "[CANCEL]",
                                fontFamily = DotMatrixFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Dismissible FTUE Hint Banner
            if (showHint) {
                DismissibleHintBanner(
                    text = "TIP: USE [[NOTE TITLE]] TO LINK NOTES AUTOMATICALLY.",
                    onDismiss = onDismissHint,
                )
            }

            // Monospaced Terminal Search Field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "> SEARCH OBJECTS OR TAGS...",
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

            // Brutalist Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    BrutalistChip(
                        selected = state.activeFilter is NoteFilter.All,
                        label = "ALL_NOTES",
                        onClick = { onFilterChange(NoteFilter.All) },
                    )
                }
                item {
                    BrutalistChip(
                        selected = state.activeFilter is NoteFilter.Favorites,
                        label = "* FAVORITES",
                        onClick = { onFilterChange(NoteFilter.Favorites) },
                    )
                }
                item {
                    BrutalistChip(
                        selected = state.activeFilter is NoteFilter.Trash,
                        label = "TRASH (${state.trashedNotes.size})",
                        onClick = { onFilterChange(NoteFilter.Trash) },
                    )
                }
                items(state.folders, key = { it.id }) { (_, name) ->
                    BrutalistChip(
                        selected = (state.activeFilter as? NoteFilter.ByFolder)?.folderName == name,
                        label = "DIR/${name.uppercase()}",
                        onClick = { onFilterChange(NoteFilter.ByFolder(name)) },
                    )
                }
                items(state.tags, key = { it.name }) { (name) ->
                    BrutalistChip(
                        selected = (state.activeFilter as? NoteFilter.ByTag)?.tagName == name,
                        label = "#${name.uppercase()}",
                        onClick = { onFilterChange(NoteFilter.ByTag(name)) },
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Notes High-Density Edge-to-Edge List
            val currentList = if (state.activeFilter is NoteFilter.Trash) state.trashedNotes else state.notes
            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (state.activeFilter is NoteFilter.Trash) Icons.Default.Delete else Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (state.activeFilter is NoteFilter.Trash) "TRASH IS EMPTY" else if (state.searchQuery.isBlank()) "NO OBJECTS IN VAULT" else "ZERO SEARCH MATCHES",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            text = if (state.activeFilter is NoteFilter.Trash) "DELETED NOTES APPEAR HERE BEFORE 30-DAY AUTO-PURGE" else if (state.searchQuery.isBlank()) "INITIALIZE YOUR FIRST NOTE TO BUILD KNOWLEDGE GRAPH" else "TRY FILTERING BY DIFFERENT TAG OR SEARCH QUERY",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        if (state.activeFilter !is NoteFilter.Trash) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(1.dp, MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNewNoteClick()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = "+ CREATE FIRST NOTE",
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp),
                ) {
                    // Pinned Shelf (when viewing all notes and not filtering/searching)
                    if (state.activeFilter is NoteFilter.All && state.searchQuery.isBlank() && state.pinnedNotes.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "📌 PINNED OBJECTS (${state.pinnedNotes.size})",
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                        }
                        items(state.pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                            NoteRowCard(
                                note = note,
                                isSelected = state.selectedNoteIds.contains(note.id),
                                isSelectionMode = state.isMultiSelectMode || state.selectedNoteIds.isNotEmpty(),
                                onClick = {
                                    if (state.isMultiSelectMode || state.selectedNoteIds.isNotEmpty()) {
                                        onToggleSelect(note.id)
                                    } else {
                                        onNoteClick(note.id)
                                    }
                                },
                                onLongClick = { onLongClickNote(note.id) },
                                onTogglePin = { onTogglePin(note) },
                                onToggleFavorite = { onToggleFavorite(note) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "ALL OBJECTS",
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                        }
                    }

                    // Main or Trashed Notes List
                    items(currentList, key = { it.id }) { note ->
                        if (state.activeFilter is NoteFilter.Trash) {
                            TrashedNoteRowCard(
                                note = note,
                                onRestore = { onRestoreNote(note.id) }
                            )
                        } else {
                            NoteRowCard(
                                note = note,
                                isSelected = state.selectedNoteIds.contains(note.id),
                                isSelectionMode = state.isMultiSelectMode || state.selectedNoteIds.isNotEmpty(),
                                onClick = {
                                    if (state.isMultiSelectMode || state.selectedNoteIds.isNotEmpty()) {
                                        onToggleSelect(note.id)
                                    } else {
                                        onNoteClick(note.id)
                                    }
                                },
                                onLongClick = { onLongClickNote(note.id) },
                                onTogglePin = { onTogglePin(note) },
                                onToggleFavorite = { onToggleFavorite(note) }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrutalistChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = DotMatrixFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteRowCard(
    note: Note,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", androidx.compose.ui.platform.LocalLocale.current.platformLocale)
    val dateStr = dateFormat.format(Date(note.updatedAt))

    val wikilinkCount = remember(note.body) {
        if (note.body.isBlank()) 0 else note.body.split("[[").size - 1
    }

    val accentColor = remember(note.accentColor) {
        note.accentColor?.let {
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(84.dp)
                    .background(accentColor)
            )
        }

        // Selection checkbox
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (isSelected) "Selected" else "Not Selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    if (!note.folder.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp),
                        )
                        Text(
                            text = "${note.folder.uppercase()} / ",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Text(
                        text = note.title.ifBlank { "UNTITLED.MD" },
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = VercelMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Direct Pin Toggle
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePin()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Direct Favorite Toggle
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFavorite()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (note.isFavorite) "Unfavorite" else "Favorite",
                            tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            val snippet = note.body.lines().asSequence().take(2).joinToString(" ").replace("#", "").trim()
            if (snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = VercelMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "UPDATED $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (wikilinkCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[$wikilinkCount LINKS]",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (note.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        note.tags.take(3).forEach { tag ->
                            Text(
                                text = "#${tag.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = DotMatrixFontFamily,
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

@Composable
fun TrashedNoteRowCard(
    note: Note,
    onRestore: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", androidx.compose.ui.platform.LocalLocale.current.platformLocale)
    val dateStr = dateFormat.format(Date(note.deletedAt ?: note.updatedAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifBlank { "UNTITLED.MD" },
                style = MaterialTheme.typography.titleMedium,
                fontFamily = VercelMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TRASHED: $dateStr",
                fontFamily = DotMatrixFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }

        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRestore()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "[RESTORE]",
                fontFamily = DotMatrixFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
