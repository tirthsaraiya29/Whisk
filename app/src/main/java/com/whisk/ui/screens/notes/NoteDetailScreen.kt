package com.whisk.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whisk.ui.components.MarkdownEditor
import com.whisk.ui.components.WhiskTopBar

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
) {
    val note = state.note ?: return
    val haptic = LocalHapticFeedback.current
    var showBacklinksSheet by remember { mutableStateOf(value = false) }

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
            MarkdownEditor(
                title = note.title,
                onTitleChange = { newTitle -> onTitleChange(newTitle, note.body) },
                body = note.body,
                onBodyChange = { newBody -> onTitleChange(note.title, newBody) },
                onLinkClick = onOpenNote,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // Brutalist Backlinks Inspector Bottom Sheet
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
                    text = "BACKLINKS // [${state.backlinks.size}]",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "OBJECTS LINKING TO '${note.title.uppercase()}'",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                if (state.backlinks.isEmpty()) {
                    Text(
                        text = "NO INBOUND BACKLINKS DETECTED. USE [[${note.title}]] IN OTHER NOTES TO CREATE LINKS.",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = backlink.sourceNoteTitle.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (backlink.snippet.isNotBlank()) {
                                    Text(
                                        text = backlink.snippet,
                                        style = MaterialTheme.typography.bodyMedium,
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
    }
}
