package com.whisk.ui.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.whisk.domain.model.CanvasCard
import com.whisk.domain.model.Note
import com.whisk.ui.components.WhiskTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialCanvasScreen(
    state: CanvasUiState,
    onAddTextCard: () -> Unit,
    onAddNoteCard: (Note) -> Unit,
    onUpdateCardPos: (String, Float, Float) -> Unit,
    onDeleteCard: (String) -> Unit,
    onOpenNote: (String) -> Unit
) {
    var showNotePickerSheet by remember { mutableStateOf(false) }
    var canvasScale by remember { mutableFloatStateOf(1f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = state.currentCanvas.title,
                subtitle = "${state.currentCanvas.cards.size} spatial cards • Zoom: ${(canvasScale * 100).toInt()}%",
                actions = {
                    IconButton(onClick = {
                        canvasScale = 1f
                        canvasOffset = Offset.Zero
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = { showNotePickerSheet = true },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add Note to Canvas")
                }

                FloatingActionButton(
                    onClick = onAddTextCard,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Text Card")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        canvasScale = (canvasScale * zoom).coerceIn(0.4f, 3.0f)
                        canvasOffset += pan
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = canvasScale
                        scaleY = canvasScale
                        translationX = canvasOffset.x
                        translationY = canvasOffset.y
                    }
            ) {
                val strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

                // Background Canvas for Inter-card Connection Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    state.currentCanvas.connections.forEach { conn ->
                        val fromCard = state.currentCanvas.cards.find { it.id == conn.fromCardId }
                        val toCard = state.currentCanvas.cards.find { it.id == conn.toCardId }
                        if (fromCard != null && toCard != null) {
                            drawLine(
                                color = strokeColor,
                                start = Offset(fromCard.x + 110f, fromCard.y + 60f),
                                end = Offset(toCard.x + 110f, toCard.y + 60f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                if (state.currentCanvas.cards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Spatial Canvas is empty.\nTap + or Note icon to place cards.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Render Spatial Cards
                    state.currentCanvas.cards.forEach { card ->
                        CanvasCardItem(
                            card = card,
                            onDrag = { dx, dy ->
                                onUpdateCardPos(card.id, card.x + dx / canvasScale, card.y + dy / canvasScale)
                            },
                            onDelete = { onDeleteCard(card.id) },
                            onOpenNote = {
                                val nId = card.noteId
                                if (nId != null) onOpenNote(nId)
                            }
                        )
                    }
                }
            }
        }
    }

    // Vault Note Selection Sheet
    if (showNotePickerSheet) {
        ModalBottomSheet(onDismissRequest = { showNotePickerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Vault Note to Spatial Canvas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.allNotes.isEmpty()) {
                    Text(
                        text = "No notes in vault. Create notes first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.allNotes, key = { it.id }) { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddNoteCard(note)
                                        showNotePickerSheet = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val excerpt = note.body.take(80).replace("\n", " ")
                                    if (excerpt.isNotBlank()) {
                                        Text(
                                            text = excerpt,
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

@Composable
fun CanvasCardItem(
    card: CanvasCard,
    onDrag: (Float, Float) -> Unit,
    onDelete: () -> Unit,
    onOpenNote: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(card.x.toInt(), card.y.toInt()) }
            .size(card.width.dp, card.height.dp)
            .pointerInput(card.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onOpenNote() },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete card",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = card.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}
