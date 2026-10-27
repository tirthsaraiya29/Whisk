package com.whisk.ui.screens.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.GraphNode
import com.whisk.domain.model.NodeType
import com.whisk.ui.components.WhiskTopBar
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeGraphScreen(
    state: GraphUiState,
    onNodeSelect: (GraphNode?) -> Unit,
    onOpenNote: (String) -> Unit,
    onResetGraph: () -> Unit,
    onToggleOrphans: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val nodeColorNote = MaterialTheme.colorScheme.primary
    val nodeColorTag = Color(0xFFD97706) // Amber Tag
    val edgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val textColor = MaterialTheme.colorScheme.onBackground

    val textMeasurer = rememberTextMeasurer()

    // Calculate initial 2D circle / force-directed positions for nodes
    val nodes = if (state.showOrphans) state.graph.nodes else state.graph.nodes.filter { !it.isOrphan }
    val nodePositions = remember(nodes) {
        val posMap = mutableMapOf<String, Offset>()
        val count = nodes.size
        if (count > 0) {
            val radius = (150f + count * 25f).coerceAtMost(450f)
            nodes.forEachIndexed { i, node ->
                val angle = (2 * Math.PI * i / count).toFloat()
                val x = 500f + radius * cos(angle)
                val y = 600f + radius * sin(angle)
                posMap[node.id] = Offset(x, y)
            }
        }
        posMap
    }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = if (state.isLocalGraph) "Local Graph" else "Knowledge Graph",
                subtitle = "${nodes.size} nodes • ${state.graph.edges.size} connections",
                actions = {
                    IconButton(onClick = onToggleOrphans) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Orphans",
                            tint = if (state.showOrphans) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        onResetGraph()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recenter Graph")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive Graph Canvas with Pan & Zoom
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.4f, 3.5f)
                            offset += pan
                        }
                    }
                    .pointerInput(nodes) {
                        detectTapGestures { tapOffset ->
                            // Find tapped node
                            val transformedTap = (tapOffset - offset) / scale
                            val tappedNode = nodes.find { node ->
                                val pos = nodePositions[node.id] ?: Offset.Zero
                                (pos - transformedTap).getDistance() <= 35f
                            }
                            onNodeSelect(tappedNode)
                        }
                    }
            ) {
                // 1. Draw Edges
                state.graph.edges.forEach { edge ->
                    val fromPos = nodePositions[edge.sourceId]
                    val toPos = nodePositions[edge.targetId]
                    if (fromPos != null && toPos != null) {
                        val start = fromPos * scale + offset
                        val end = toPos * scale + offset
                        drawLine(
                            color = edgeColor,
                            start = start,
                            end = end,
                            strokeWidth = 2f * scale,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 2. Draw Nodes
                nodes.forEach { node ->
                    val basePos = nodePositions[node.id] ?: Offset.Zero
                    val drawPos = basePos * scale + offset
                    val isSelected = state.selectedNode?.id == node.id
                    val color = if (node.type == NodeType.TAG) nodeColorTag else nodeColorNote

                    // Node outer ring
                    if (isSelected) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.3f),
                            radius = 24f * scale,
                            center = drawPos
                        )
                    }

                    // Main node circle
                    drawCircle(
                        color = color,
                        radius = 12f * scale,
                        center = drawPos
                    )

                    // Label text below node
                    val measuredText = textMeasurer.measure(
                        text = node.label,
                        style = androidx.compose.ui.text.TextStyle(
                            color = textColor,
                            fontSize = (11 * scale).sp
                        )
                    )
                    drawText(
                        textLayoutResult = measuredText,
                        topLeft = Offset(drawPos.x - measuredText.size.width / 2, drawPos.y + 16f * scale)
                    )
                }
            }

            // Legend Overlay
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.padding(end = 4.dp)) {
                        drawCircle(color = nodeColorNote, radius = 5f)
                    }
                    Text("Note", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 12.dp))
                    Canvas(modifier = Modifier.padding(end = 4.dp)) {
                        drawCircle(color = nodeColorTag, radius = 5f)
                    }
                    Text("Tag", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    // Node Inspector Sheet
    if (state.selectedNode != null) {
        val selected = state.selectedNode
        ModalBottomSheet(onDismissRequest = { onNodeSelect(null) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Type: ${selected.type.name} • ${if (selected.isOrphan) "Orphan Note" else "Linked Node"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (selected.noteId != null) {
                    Button(
                        onClick = {
                            val id = selected.noteId
                            onNodeSelect(null)
                            onOpenNote(id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Open Note")
                    }
                }
            }
        }
    }
}
