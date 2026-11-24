package com.whisk.ui.screens.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
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
                title = "Whisk Vault",
                subtitle = "${state.notes.size} notes loaded",
                onMenuClick = onMenuClick,
                onCommandPaletteClick = onCommandPaletteClick,
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNewNoteClick()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "+ New Note",
                            style = MaterialTheme.typography.labelSmall,
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.selectedNoteIds.size} notes selected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBatchDelete()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Delete All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onClearMultiSelect()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // FTUE Hint Banner
            if (showHint) {
                DismissibleHintBanner(
                    text = "Tip: Use [[Note Title]] to link notes together seamlessly.",
                    onDismiss = onDismissHint,
                )
            }

            // Search Field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search notes, tags, or content...",
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

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    CategoryChip(
                        selected = state.activeFilter is NoteFilter.All,
                        label = "All Notes",
                        onClick = { onFilterChange(NoteFilter.All) },
                    )
                }
                item {
                    CategoryChip(
                        selected = state.activeFilter is NoteFilter.Favorites,
                        label = "Favorites",
                        onClick = { onFilterChange(NoteFilter.Favorites) },
                    )
                }
                item {
                    CategoryChip(
                        selected = state.activeFilter is NoteFilter.Trash,
                        label = "Trash (${state.trashedNotes.size})",
                        onClick = { onFilterChange(NoteFilter.Trash) },
                    )
                }
                items(state.folders, key = { it.id }) { (_, name) ->
                    CategoryChip(
                        selected = (state.activeFilter as? NoteFilter.ByFolder)?.folderName == name,
                        label = name,
                        onClick = { onFilterChange(NoteFilter.ByFolder(name)) },
                    )
                }
                items(state.tags, key = { it.name }) { (name) ->
                    CategoryChip(
                        selected = (state.activeFilter as? NoteFilter.ByTag)?.tagName == name,
                        label = "#$name",
                        onClick = { onFilterChange(NoteFilter.ByTag(name)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notes List
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
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (state.activeFilter is NoteFilter.Trash) Icons.Default.Delete else Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (state.activeFilter is NoteFilter.Trash) "Trash is empty" else if (state.searchQuery.isBlank()) "No notes in vault" else "No search results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            text = if (state.activeFilter is NoteFilter.Trash) "Deleted notes appear here before 30-day auto-purge" else if (state.searchQuery.isBlank()) "Create your first note to begin building your knowledge vault" else "Try searching for a different keyword or tag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )

                        if (state.activeFilter !is NoteFilter.Trash) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNewNoteClick()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = "+ Create First Note",
                                    style = MaterialTheme.typography.labelLarge,
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Pinned Shelf
                    if (state.activeFilter is NoteFilter.All && state.searchQuery.isBlank() && state.pinnedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "📌 Pinned Notes (${state.pinnedNotes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
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
                        }
                        item {
                            Text(
                                text = "All Notes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
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
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
    val dateFormat = SimpleDateFormat("MMM d, yyyy", androidx.compose.ui.platform.LocalLocale.current.platformLocale)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 12.dp, top = 2.dp)
                        .size(20.dp),
                )
            } else if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp, top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Body snippet
                if (note.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.body.replace(Regex("[#*`_\\[\\]]"), "").trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Badges & Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!note.folder.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = note.folder,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        if (wikilinkCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$wikilinkCount links",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        note.tags.take(2).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD97706),
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = VercelMonoFontFamily,
                            fontSize = 10.sp,
                        )

                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite Note",
                                tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp),
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
    onRestore: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "In trash",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRestore()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Restore",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
