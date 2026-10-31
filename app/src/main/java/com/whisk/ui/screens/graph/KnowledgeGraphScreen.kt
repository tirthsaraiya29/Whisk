package com.whisk.ui.screens.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.GraphNode
import com.whisk.domain.model.NodeType
import com.whisk.ui.components.WhiskTopBar
import com.whisk.ui.theme.DotMatrixFontFamily
import com.whisk.ui.theme.VercelMonoFontFamily
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeGraphScreen(
    state: GraphUiState,
    onNodeSelect: (GraphNode?) -> Unit,
    onOpenNote: (String) -> Unit,
    onResetGraph: () -> Unit,
    onToggleOrphans: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var nodeSearchQuery by remember { mutableStateOf("") }

    val nodeColorNote = MaterialTheme.colorScheme.primary
    val nodeColorTag = Color(0xFFD97706)
    val edgeColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onBackground

    val textMeasurer = rememberTextMeasurer()

    // Calculate node degree (number of connected edges)
    val nodeDegrees = remember(state.graph) {
        val degrees = mutableMapOf<String, Int>()
        state.graph.edges.forEach { edge ->
            degrees[edge.sourceId] = (degrees[edge.sourceId] ?: 0) + 1
            degrees[edge.targetId] = (degrees[edge.targetId] ?: 0) + 1
        }
        degrees
    }

    // Filter nodes by orphans and search query
    val nodes = remember(state.graph.nodes, state.showOrphans, nodeSearchQuery) {
        state.graph.nodes.filter { node ->
            val matchesOrphan = state.showOrphans || !node.isOrphan
            val matchesQuery = nodeSearchQuery.isBlank() || node.label.contains(nodeSearchQuery, ignoreCase = true)
            matchesOrphan && matchesQuery
        }
    }

    // Force-directed graph layout calculation (Fruchterman-Reingold algorithm)
    val nodePositions = remember(nodes, state.graph.edges) {
        val posMap = mutableMapOf<String, Offset>()
        val count = nodes.size
        if (count == 0) return@remember posMap

        val goldenAngle = 137.5f * (Math.PI / 180f).toFloat()
        nodes.forEachIndexed { i, node ->
            val r = 90f * kotlin.math.sqrt(i.toFloat() + 1f)
            val theta = i * goldenAngle
            posMap[node.id] = Offset(500f + (r * cos(theta)), 500f + (r * sin(theta)))
        }

        val iterations = 45
        val kRepulsion = 14000f
        val kAttraction = 0.06f

        val edgeList = state.graph.edges.filter { e -> posMap.containsKey(e.sourceId) && posMap.containsKey(e.targetId) }

        repeat(iterations) {
            val disp = mutableMapOf<String, Offset>()
            nodes.forEach { disp[it.id] = Offset.Zero }

            for (i in 0 until count) {
                val u = nodes[i]
                val pU = posMap[u.id] ?: continue
                for (j in i + 1 until count) {
                    val v = nodes[j]
                    val pV = posMap[v.id] ?: continue
                    val delta = pU - pV
                    val dist = delta.getDistance().coerceAtLeast(10f)
                    val force = (delta / dist) * (kRepulsion / (dist * dist))
                    disp[u.id] = (disp[u.id] ?: Offset.Zero) + force
                    disp[v.id] = (disp[v.id] ?: Offset.Zero) - force
                }
            }

            edgeList.forEach { (sourceId, targetId) ->
                val pSrc = posMap[sourceId] ?: return@forEach
                val pTgt = posMap[targetId] ?: return@forEach
                val delta = pSrc - pTgt
                val dist = delta.getDistance().coerceAtLeast(1f)
                val force = (delta / dist) * (kAttraction * dist)
                disp[sourceId] = (disp[sourceId] ?: Offset.Zero) - force
                disp[targetId] = (disp[targetId] ?: Offset.Zero) + force
            }

            nodes.forEach { (id) ->
                val curr = posMap[id] ?: Offset.Zero
                val d = disp[id] ?: Offset.Zero
                val limitedD = Offset(d.x.coerceIn(-25f, 25f), d.y.coerceIn(-25f, 25f))
                posMap[id] = curr + limitedD
            }
        }
        posMap
    }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "KNOWLEDGE GRAPH",
                subtitle = "${nodes.size} NODES • ${state.graph.edges.size} CONNECTIONS",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleOrphans()
                    }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Orphans",
                            tint = if (state.showOrphans) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scale = 1f
                        offset = Offset.Zero
                        onResetGraph()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recenter Graph",
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
                .imePadding()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Node Filter Search Bar
            OutlinedTextField(
                value = nodeSearchQuery,
                onValueChange = { nodeSearchQuery = it },
                placeholder = {
                    Text(
                        "> FILTER GRAPH NODES...",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = DotMatrixFontFamily),
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

            // Legend Status Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(nodeColorNote),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NOTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(nodeColorTag),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TAG",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Interactive Canvas Viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.4f, 3.5f)
                                offset += pan
                            }
                        }
                        .pointerInput(nodes, nodePositions, scale, offset) {
                            detectTapGestures { tapOffset ->
                                var clickedNode: GraphNode? = null
                                nodes.forEach { node ->
                                    val rawPos = nodePositions[node.id] ?: return@forEach
                                    val canvasPos = (rawPos * scale) + offset
                                    if ((canvasPos - tapOffset).getDistance() < 30f * scale) {
                                        clickedNode = node
                                    }
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNodeSelect(clickedNode)
                            }
                        },
                ) {
                    state.graph.edges.forEach { (sourceId, targetId) ->
                        val srcPos = nodePositions[sourceId]
                        val tgtPos = nodePositions[targetId]
                        if (srcPos != null && tgtPos != null) {
                            val p1 = (srcPos * scale) + offset
                            val p2 = (tgtPos * scale) + offset
                            drawLine(
                                color = edgeColor,
                                start = p1,
                                end = p2,
                                strokeWidth = 1.5f * scale,
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    nodes.forEach { node ->
                        val rawPos = nodePositions[node.id] ?: return@forEach
                        val center = (rawPos * scale) + offset
                        val degree = nodeDegrees[node.id] ?: 1
                        val radius = (12f + (degree * 3f)).coerceIn(10f, 26f) * scale
                        val color = if (node.type == NodeType.NOTE) nodeColorNote else nodeColorTag

                        drawCircle(
                            color = color,
                            radius = radius,
                            center = center,
                        )

                        val textLayout = textMeasurer.measure(
                            text = node.label,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = (11.sp.value * scale.coerceIn(0.8f, 1.4f)).sp,
                                color = textColor,
                                fontFamily = VercelMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                center.x - (textLayout.size.width / 2f),
                                center.y + radius + 4f,
                            ),
                        )
                    }
                }
            }
        }
    }

    // Selected Node Details Inspector Sheet
    state.selectedNode?.let { selNode ->
        ModalBottomSheet(
            onDismissRequest = { onNodeSelect(null) },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "NODE // ${selNode.label.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "TYPE: ${selNode.type.name} • CONNECTIONS: ${nodeDegrees[selNode.id] ?: 0}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )

                if (selNode.type == NodeType.NOTE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNodeSelect(null)
                                onOpenNote(selNode.id)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "OPEN NOTE FILE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
