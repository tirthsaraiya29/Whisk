package com.whisk.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.screens.notes.NoteRowCard

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onNoteClick: (String) -> Unit
) {
    val resultCount = state.results.size

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "Search Vault",
                subtitle = if (state.query.isNotBlank()) {
                    "$resultCount ${if (resultCount == 1) "result" else "results"} for '${state.query}'"
                } else {
                    "Instant offline FTS search across notes & tags"
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search titles, body, code, or tags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                singleLine = true
            )

            // Quick Search Suggestion Chips
            if (state.query.isBlank()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SuggestionChip(
                            onClick = { onQueryChange("welcome") },
                            label = { Text("welcome") }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = { onQueryChange("architecture") },
                            label = { Text("architecture") }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = { onQueryChange("mermaid") },
                            label = { Text("mermaid") }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = { onQueryChange("kotlin") },
                            label = { Text("kotlin") }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = { onQueryChange("TODO") },
                            label = { Text("TODO") }
                        )
                    }
                }
            }

            if (state.results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = if (state.query.isBlank()) "Search Vault" else "No matching notes found",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.query.isBlank()) {
                                "Type keywords, tags, or code snippets above"
                            } else {
                                "Try searching for a different term or tag"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.results, key = { it.id }) { note ->
                        NoteRowCard(note = note, onClick = { onNoteClick(note.id) })
                    }
                }
            }
        }
    }
}
