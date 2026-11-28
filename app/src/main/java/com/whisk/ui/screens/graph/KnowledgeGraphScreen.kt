package com.whisk.ui.screens.graph

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
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
    var showGraphSettingsPopover by remember { mutableStateOf(false) }

    // Obsidian Graph Settings Sections State
    var showFiltersSection by remember { mutableStateOf(false) }
    var showGroupsSection by remember { mutableStateOf(false) }
    var showDisplaySection by remember { mutableStateOf(false) }
    var showForcesSection by remember { mutableStateOf(false) }

    var kRepulsion by remember { mutableFloatStateOf(14000f) }
    var kAttraction by remember { mutableFloatStateOf(0.06f) }
    var maxNodesLimit by remember { mutableIntStateOf(80) }

    val nodeColorNote = MaterialTheme.colorScheme.primary
    val nodeColorTag = Color(0xFFD97706)
    val nodeColorProject = Color(0xFF8B5CF6)
    val edgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val textColor = MaterialTheme.colorScheme.onBackground

    val textMeasurer = rememberTextMeasurer()

    val nodeDegrees = remember(state.graph) {
        val degrees = mutableMapOf<String, Int>()
        state.graph.edges.forEach { edge ->
            degrees[edge.sourceId] = (degrees[edge.sourceId] ?: 0) + 1
            degrees[edge.targetId] = (degrees[edge.targetId] ?: 0) + 1
        }
        degrees
    }

    val nodes = remember(state.graph.nodes, state.showOrphans, nodeSearchQuery, maxNodesLimit) {
        state.graph.nodes
            .filter { node ->
                val matchesOrphan = state.showOrphans || !node.isOrphan
                val matchesQuery = nodeSearchQuery.isBlank() || node.label.contains(nodeSearchQuery, ignoreCase = true)
                matchesOrphan && matchesQuery
            }
            .take(maxNodesLimit)
    }

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

        val iterations = 40
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
                val p1 = posMap[sourceId]
                val p2 = posMap[targetId]
                if (p1 != null && p2 != null) {
                    val delta = p1 - p2
                    val dist = delta.getDistance().coerceAtLeast(10f)
                    val force = (delta / dist) * (dist * kAttraction)
                    disp[sourceId] = (disp[sourceId] ?: Offset.Zero) - force
                    disp[targetId] = (disp[targetId] ?: Offset.Zero) + force
                }
            }

            nodes.forEach { node ->
                val p = posMap[node.id] ?: Offset.Zero
                val d = disp[node.id] ?: Offset.Zero
                val limitedD = Offset(
                    d.x.coerceIn(-30f, 30f),
                    d.y.coerceIn(-30f, 30f),
                )
                posMap[node.id] = p + limitedD
            }
        }

        posMap
    }

    Scaffold(
        topBar = {
            WhiskTopBar(
                title = "Graph view",
                subtitle = "${nodes.size} nodes • ${state.graph.edges.size} connections",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showGraphSettingsPopover = !showGraphSettingsPopover
                    }) {
                        Icon(Icons.Default.Tune, contentDescription = "Graph Settings Controls")
                    }

                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scale = 1f
                        offset = Offset.Zero
                        onResetGraph()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Layout")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Dark Graph Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
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
                            fontSize = (12.sp.value * scale.coerceIn(0.8f, 1.4f)).sp,
                            color = textColor,
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

            // Floating Obsidian Graph Settings Control Panel (Screenshot 2)
            if (showGraphSettingsPopover) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .width(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Graph Controls",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        scale = 1f
                                        offset = Offset.Zero
                                        onResetGraph()
                                    },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { showGraphSettingsPopover = false },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

                        // 1. Filters Header Accordion
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showFiltersSection = !showFiltersSection }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Filters", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Icon(if (showFiltersSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        AnimatedVisibility(visible = showFiltersSection) {
                            Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                                OutlinedTextField(
                                    value = nodeSearchQuery,
                                    onValueChange = { nodeSearchQuery = it },
                                    placeholder = { Text("Filter nodes...", style = MaterialTheme.typography.bodySmall) },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Show Orphans", style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = state.showOrphans,
                                        onCheckedChange = { onToggleOrphans() },
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }

                        // 2. Groups Header Accordion
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showGroupsSection = !showGroupsSection }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Groups", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Icon(if (showGroupsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        AnimatedVisibility(visible = showGroupsSection) {
                            Text("Auto-grouped by #tags and note types.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                        }

                        // 3. Display Header Accordion
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showDisplaySection = !showDisplaySection }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Display", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Icon(if (showDisplaySection) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        AnimatedVisibility(visible = showDisplaySection) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Max Nodes: $maxNodesLimit", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = maxNodesLimit.toFloat(),
                                    onValueChange = { maxNodesLimit = it.toInt() },
                                    valueRange = 20f..200f,
                                )
                            }
                        }

                        // 4. Forces Header Accordion
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showForcesSection = !showForcesSection }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Forces", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Icon(if (showForcesSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        AnimatedVisibility(visible = showForcesSection) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Repulsion: ${kRepulsion.toInt()}", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = kRepulsion,
                                    onValueChange = { kRepulsion = it },
                                    valueRange = 5000f..35000f,
                                )
                            }
                        }
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
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = node.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Type: ${node.type.name} • Connections: ${nodeDegrees[node.id] ?: 0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (node.type == NodeType.NOTE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                onNodeSelect(null)
                                onOpenNote(node.id)
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Open Note in Vault",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
