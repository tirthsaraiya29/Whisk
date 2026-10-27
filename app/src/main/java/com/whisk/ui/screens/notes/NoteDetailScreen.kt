package com.whisk.ui.screens.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisk.domain.model.Backlink
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
    onOpenAiWithNote: () -> Unit
) {
    val note = state.note ?: return
    var showBacklinksSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = note.title.ifBlank { "Editing Note" },
                subtitle = when (state.saveState) {
                    SaveState.SAVING -> "Saving..."
                    SaveState.SAVED -> "Saved"
                    SaveState.ERROR -> "Save Error"
                    else -> "Autosaved"
                },
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showBacklinksSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = "Backlinks (${state.backlinks.size})",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenAiWithNote) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Copilot",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteNote) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MarkdownEditor(
                title = note.title,
                onTitleChange = { newTitle -> onTitleChange(newTitle, note.body) },
                body = note.body,
                onBodyChange = { newBody -> onTitleChange(note.title, newBody) },
                onLinkClick = onOpenNote,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Backlinks Bottom Sheet Inspector
    if (showBacklinksSheet) {
        ModalBottomSheet(onDismissRequest = { showBacklinksSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Backlinks (${state.backlinks.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Notes mentioning '${note.title}'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (state.backlinks.isEmpty()) {
                    Text(
                        text = "No other notes link to this note yet. Use [[${note.title}]] in another note to create a link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.backlinks) { backlink ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBacklinksSheet = false
                                        onOpenNote(backlink.sourceNoteId)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = backlink.sourceNoteTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (backlink.snippet.isNotBlank()) {
                                        Text(
                                            text = backlink.snippet,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
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
