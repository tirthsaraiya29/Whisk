package com.whisk.ui.components.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.ui.components.getIconForObjectType
import com.whisk.ui.theme.DotMatrixFontFamily
import com.whisk.ui.theme.VercelMonoFontFamily

@Composable
fun BoardView(
    notes: List<Note>,
    onOpenNote: (String) -> Unit,
    onMoveNoteToGroup: (noteId: String, newGroup: String) -> Unit,
    onCreateInGroup: (group: String) -> Unit,
    modifier: Modifier = Modifier,
    columns: List<String> = listOf("Draft", "In Progress", "In Review", "Done", "Unassigned"),
) {
    val scrollState = rememberScrollState()

    // Group notes by status (or group property)
    val groupedNotes: Map<String, List<Note>> = remember(notes, columns) {
        val map = columns.associateWith { mutableListOf<Note>() }.toMutableMap()
        if (!map.containsKey("Unassigned")) map["Unassigned"] = mutableListOf()

        notes.forEach { note ->
            val status = note.status?.trim()
            val matchedCol = columns.firstOrNull { it.equals(status, ignoreCase = true) }
            if (matchedCol != null) {
                map[matchedCol]?.add(note)
            } else {
                map["Unassigned"]?.add(note)
            }
        }
        map
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        columns.forEach { colName ->
            val colNotes = groupedNotes[colName] ?: emptyList()

            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
                    .padding(10.dp),
            ) {
                // Column Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = colName.uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[${colNotes.size}]",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = { onCreateInGroup(colName) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add in column",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Cards List inside Column
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(colNotes, key = { it.id }) { note ->
                        BoardCard(
                            note = note,
                            columns = columns,
                            currentColumn = colName,
                            onOpen = { onOpenNote(note.id) },
                            onMove = { newCol -> onMoveNoteToGroup(note.id, newCol) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardCard(
    note: Note,
    columns: List<String>,
    currentColumn: String,
    onOpen: () -> Unit,
    onMove: (String) -> Unit,
) {
    val currentIdx = columns.indexOf(currentColumn)
    val prevCol = if (currentIdx > 0) columns[currentIdx - 1] else null
    val nextCol = if (currentIdx >= 0 && currentIdx < columns.size - 1) columns[currentIdx + 1] else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(2.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Icon & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getIconForObjectType(note.typeId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = VercelMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!note.description.isNullOrBlank()) {
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Quick Column Move Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (prevCol != null) {
                    IconButton(
                        onClick = { onMove(prevCol) },
                        modifier = Modifier.size(22.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Move to $prevCol",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(22.dp))
                }

                Text(
                    text = note.typeId.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (nextCol != null) {
                    IconButton(
                        onClick = { onMove(nextCol) },
                        modifier = Modifier.size(22.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Move to $nextCol",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(22.dp))
                }
            }
        }
    }
}
