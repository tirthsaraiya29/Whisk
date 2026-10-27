package com.whisk.ui.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.whisk.domain.model.CanvasCard
import com.whisk.domain.model.Note
import com.whisk.ui.components.WhiskTopBar

@Composable
fun SpatialCanvasScreen(
    state: CanvasUiState,
    onAddTextCard: () -> Unit,
    onAddNoteCard: (Note) -> Unit,
    onUpdateCardPos: (String, Float, Float) -> Unit,
    onDeleteCard: (String) -> Unit,
    onOpenNote: (String) -> Unit
) {
    Scaffold(
        topBar = {
            WhiskTopBar(
                title = state.currentCanvas.title,
                subtitle = "${state.currentCanvas.cards.size} cards • Spatial Layout"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTextCard,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Text Card")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background Canvas for Connection Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                state.currentCanvas.connections.forEach { conn ->
                    val fromCard = state.currentCanvas.cards.find { it.id == conn.fromCardId }
                    val toCard = state.currentCanvas.cards.find { it.id == conn.toCardId }
                    if (fromCard != null && toCard != null) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.Gray,
                            start = Offset(fromCard.x + 100f, fromCard.y + 60f),
                            end = Offset(toCard.x + 100f, toCard.y + 60f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Render Spatial Cards
            state.currentCanvas.cards.forEach { card ->
                CanvasCardItem(
                    card = card,
                    onDrag = { dx, dy ->
                        onUpdateCardPos(card.id, card.x + dx, card.y + dy)
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
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
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
                        Icon(Icons.Default.Delete, contentDescription = "Delete card", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
