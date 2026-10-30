package com.whisk.ui.screens.notes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.ui.components.DismissibleHintBanner
import com.whisk.ui.components.WhiskTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesListScreen(
    state: NotesUiState,
    onNoteClick: (String) -> Unit,
    onNewNoteClick: () -> Unit,
    onFilterChange: (NoteFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    showHint: Boolean = true,
    onDismissHint: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "WHISK // VAULT",
                subtitle = "${state.notes.size} OBJECTS LOADED",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFilterChange(NoteFilter.All)
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Vault")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dismissible FTUE Hint Banner
            if (showHint) {
                DismissibleHintBanner(
                    text = "TIP: USE [[NOTE TITLE]] TO LINK NOTES AUTOMATICALLY.",
                    onDismiss = onDismissHint
                )
            }

            // Monospaced Terminal Search Field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "> SEARCH OBJECTS OR TAGS...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                singleLine = true
            )

            // Brutalist Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    BrutalistChip(
                        selected = state.activeFilter is NoteFilter.All,
                        label = "ALL_NOTES",
                        onClick = { onFilterChange(NoteFilter.All) }
                    )
                }
                item {
                    BrutalistChip(
                        selected = state.activeFilter is NoteFilter.Favorites,
                        label = "* FAVORITES",
                        onClick = { onFilterChange(NoteFilter.Favorites) }
                    )
                }
                items(state.folders) { folder ->
                    BrutalistChip(
                        selected = (state.activeFilter as? NoteFilter.ByFolder)?.folderName == folder.name,
                        label = "DIR/${folder.name.uppercase()}",
                        onClick = { onFilterChange(NoteFilter.ByFolder(folder.name)) }
                    )
                }
                items(state.tags) { tag ->
                    BrutalistChip(
                        selected = (state.activeFilter as? NoteFilter.ByTag)?.tagName == tag.name,
                        label = "#${tag.name.uppercase()}",
                        onClick = { onFilterChange(NoteFilter.ByTag(tag.name)) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Notes High-Density Edge-to-Edge List
            if (state.notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = if (state.searchQuery.isBlank()) "NO OBJECTS FOUND" else "ZERO SEARCH MATCHES",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PRESS [+] TO INITIALIZE NEW NOTE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteRowCard(note = note, onClick = { onNoteClick(note.id) })
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
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NoteRowCard(
    note: Note,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(note.updatedAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.title.ifBlank { "UNTITLED.MD" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (note.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        val snippet = note.body.lines().take(2).joinToString(" ").replace("#", "").trim()
        if (snippet.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UPDATED $dateStr",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (note.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    note.tags.take(3).forEach { tag ->
                        Text(
                            text = "#${tag.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
