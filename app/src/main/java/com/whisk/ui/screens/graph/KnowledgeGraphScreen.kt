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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    var showPhysicsSheet by remember { mutableStateOf(false) }

    // Physics configuration sliders
    var kRepulsion by remember { mutableFloatStateOf(14000f) }
    var kAttraction by remember { mutableFloatStateOf(0.06f) }
    var maxNodesLimit by remember { mutableIntStateOf(80) }

    val nodeColorNote = MaterialTheme.colorScheme.primary
    val nodeColorTag = Color(0xFFD97706)
    val nodeColorProject = Color(0xFF00F0FF)
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

    // Filter nodes by orphans, search query, and max limit
    val nodes = remember(state.graph.nodes, state.showOrphans, nodeSearchQuery, maxNodesLimit) {
        state.graph.nodes
            .filter { node ->
                val matchesOrphan = state.showOrphans || !node.isOrphan
                val matchesQuery = nodeSearchQuery.isBlank() || node.label.contains(nodeSearchQuery, ignoreCase = true)
                matchesOrphan && matchesQuery
            }
            .take(maxNodesLimit)
    }

    // Force-directed graph layout calculation (Fruchterman-Reingold algorithm with custom sliders)
    val nodePositions = remember(nodes, state.graph.edges, kRepulsion, kAttraction) {
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
                subtitle = "${nodes.size} NODES • ${state.graph.edges.size} RELATIONS",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showPhysicsSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Graph Physics Tuning",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleOrphans()
                    }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Orphans",
                            tint = if (state.showOrphans) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        )
                    }

                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scale = 1f
                        offset = Offset.Zero
                        onResetGraph()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Canvas Layout")
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
            // Monospace Filter Search Input
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
                    Box(modifier = Modifier.size(8.dp).background(nodeColorNote))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "NOTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(nodeColorTag))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TAG",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(nodeColorProject))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PROJECT",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            // Interactive Canvas Viewport with Damped Zoom & Generous Touch Targets
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
                                // Damped smooth zoom factor for high refresh rate screens
                                val dampedZoom = 1f + (zoom - 1f) * 0.65f
                                scale = (scale * dampedZoom).coerceIn(0.35f, 3.5f)
                                offset += pan
                            }
                        }
                        .pointerInput(nodes, nodePositions, scale, offset) {
                            detectTapGestures { tapOffset ->
                                var clickedNode: GraphNode? = null
                                var closestDist = Float.MAX_VALUE
                                nodes.forEach { node ->
                                    val rawPos = nodePositions[node.id] ?: return@forEach
                                    val canvasPos = (rawPos * scale) + offset
                                    val dist = (canvasPos - tapOffset).getDistance()
                                    val degree = nodeDegrees[node.id] ?: 1
                                    val radius = (12f + (degree * 3f)).coerceIn(10f, 26f) * scale
                                    // Generous touch target box (at least 60px) for high PPI screens
                                    val touchThreshold = maxOf(60f, radius + 25f)
                                    if (dist < touchThreshold && dist < closestDist) {
                                        closestDist = dist
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
                        val color = when {
                            node.label.startsWith("#") || node.type == NodeType.TAG -> nodeColorTag
                            node.label.contains("PROJECT", ignoreCase = true) -> nodeColorProject
                            else -> nodeColorNote
                        }

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

    // Selected Node Detail Modal Sheet
    state.selectedNode?.let { node ->
        ModalBottomSheet(
            onDismissRequest = { onNodeSelect(null) },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "NODE // ${node.label.uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TYPE: ${node.type.name} • CONNECTIONS: ${nodeDegrees[node.id] ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = VercelMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (node.type == NodeType.NOTE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(0.dp))
                            .clickable {
                                onNodeSelect(null)
                                onOpenNote(node.id)
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "> OPEN NOTE IN VAULT",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = DotMatrixFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    // Graph Physics & Performance Tuning Sheet
    if (showPhysicsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhysicsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "GRAPH PHYSICS & SCALE TUNING",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DotMatrixFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Max Node Limit
                Text(
                    text = "MAX VISIBLE NODES: $maxNodesLimit",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Slider(
                    value = maxNodesLimit.toFloat(),
                    onValueChange = { maxNodesLimit = it.toInt() },
                    valueRange = 20f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Repulsion / Charge
                Text(
                    text = "CHARGE REPULSION: ${kRepulsion.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Slider(
                    value = kRepulsion,
                    onValueChange = { kRepulsion = it },
                    valueRange = 5000f..30000f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Attraction / Link Tension
                Text(
                    text = "LINK TENSION: ${(kAttraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = DotMatrixFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Slider(
                    value = kAttraction,
                    onValueChange = { kAttraction = it },
                    valueRange = 0.01f..0.15f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                        .clickable {
                            kRepulsion = 14000f
                            kAttraction = 0.06f
                            maxNodesLimit = 80
                            showPhysicsSheet = false
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "RESET TO DEFAULTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = DotMatrixFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
