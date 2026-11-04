package com.whisk.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.ui.theme.DotGothicFontFamily
import com.whisk.ui.theme.SpaceMonoFontFamily
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class CanvasTool {
    PAN_NAVIGATE,
    PEN,
    RECTANGLE,
    ELLIPSE,
    ARROW,
    ERASER
}

sealed class DrawingElement {
    data class FreehandStroke(
        val points: List<Offset>,
        val color: Color,
        val strokeWidth: Float,
        val isHighlighter: Boolean = false
    ) : DrawingElement()

    data class Rectangle(
        val topLeft: Offset,
        val size: Size,
        val color: Color,
        val strokeWidth: Float,
        val isFilled: Boolean = false
    ) : DrawingElement()

    data class Ellipse(
        val topLeft: Offset,
        val size: Size,
        val color: Color,
        val strokeWidth: Float,
        val isFilled: Boolean = false
    ) : DrawingElement()

    data class Arrow(
        val start: Offset,
        val end: Offset,
        val color: Color,
        val strokeWidth: Float
    ) : DrawingElement()
}

@Composable
fun ExcalidrawOverlay(
    modifier: Modifier = Modifier,
    activeTool: CanvasTool,
    onToolChange: (CanvasTool) -> Unit,
    elements: MutableList<DrawingElement>,
    onElementsChange: () -> Unit,
    canvasScale: Float = 1f,
    canvasOffset: Offset = Offset.Zero
) {
    val undoStack = remember { mutableStateListOf<DrawingElement>() }
    val redoStack = remember { mutableStateListOf<DrawingElement>() }

    var currentColor by remember { mutableStateOf(Color(0xFF00FFCC)) } // Brutalist Cyber Neon
    var strokeWidth by remember { mutableFloatStateOf(4f) }
    var currentDragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDragEnd by remember { mutableStateOf<Offset?>(null) }
    val currentPoints = remember { mutableStateListOf<Offset>() }

    val brutalistColors = listOf(
        Color(0xFF00FFCC), // Neon Cyan
        Color(0xFFFF0055), // Radical Crimson
        Color(0xFFFFB800), // Amber Terminal
        Color(0xFF00E5FF), // Electric Blue
        Color(0xFFFFFFFF), // Pure White
        Color(0xFF171717)  // Brutal Black
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Drawing Surface
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeTool, currentColor, strokeWidth) {
                    if (activeTool == CanvasTool.PAN_NAVIGATE) return@pointerInput

                    detectDragGestures(
                        onDragStart = { start ->
                            val localStart = (start - canvasOffset) / canvasScale
                            when (activeTool) {
                                CanvasTool.PEN -> {
                                    currentPoints.clear()
                                    currentPoints.add(localStart)
                                }
                                CanvasTool.RECTANGLE, CanvasTool.ELLIPSE, CanvasTool.ARROW -> {
                                    currentDragStart = localStart
                                    currentDragEnd = localStart
                                }
                                CanvasTool.ERASER -> {
                                    // Remove nearest element within hit radius
                                    val hitIndex = elements.indexOfLast { elem ->
                                        when (elem) {
                                            is DrawingElement.FreehandStroke -> elem.points.any { (it - localStart).getDistance() < 24f }
                                            is DrawingElement.Rectangle -> (elem.topLeft - localStart).getDistance() < 40f
                                            is DrawingElement.Ellipse -> (elem.topLeft - localStart).getDistance() < 40f
                                            is DrawingElement.Arrow -> (elem.start - localStart).getDistance() < 30f || (elem.end - localStart).getDistance() < 30f
                                        }
                                    }
                                    if (hitIndex >= 0) {
                                        val removed = elements.removeAt(hitIndex)
                                        undoStack.add(removed)
                                        onElementsChange()
                                    }
                                }
                                else -> Unit
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val localPos = (change.position - canvasOffset) / canvasScale
                            when (activeTool) {
                                CanvasTool.PEN -> {
                                    currentPoints.add(localPos)
                                }
                                CanvasTool.RECTANGLE, CanvasTool.ELLIPSE, CanvasTool.ARROW -> {
                                    currentDragEnd = localPos
                                }
                                CanvasTool.ERASER -> {
                                    val hitIndex = elements.indexOfLast { elem ->
                                        when (elem) {
                                            is DrawingElement.FreehandStroke -> elem.points.any { (it - localPos).getDistance() < 24f }
                                            is DrawingElement.Rectangle -> (elem.topLeft - localPos).getDistance() < 40f
                                            is DrawingElement.Ellipse -> (elem.topLeft - localPos).getDistance() < 40f
                                            is DrawingElement.Arrow -> (elem.start - localPos).getDistance() < 30f || (elem.end - localPos).getDistance() < 30f
                                        }
                                    }
                                    if (hitIndex >= 0) {
                                        val removed = elements.removeAt(hitIndex)
                                        undoStack.add(removed)
                                        onElementsChange()
                                    }
                                }
                                else -> Unit
                            }
                        },
                        onDragEnd = {
                            when (activeTool) {
                                CanvasTool.PEN -> {
                                    if (currentPoints.size > 1) {
                                        val stroke = DrawingElement.FreehandStroke(
                                            points = currentPoints.toList(),
                                            color = currentColor,
                                            strokeWidth = strokeWidth
                                        )
                                        elements.add(stroke)
                                        undoStack.add(stroke)
                                        redoStack.clear()
                                        onElementsChange()
                                    }
                                    currentPoints.clear()
                                }
                                CanvasTool.RECTANGLE -> {
                                    val start = currentDragStart
                                    val end = currentDragEnd
                                    if (start != null && end != null) {
                                        val topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y))
                                        val size = Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y))
                                        val rect = DrawingElement.Rectangle(topLeft, size, currentColor, strokeWidth)
                                        elements.add(rect)
                                        undoStack.add(rect)
                                        redoStack.clear()
                                        onElementsChange()
                                    }
                                    currentDragStart = null
                                    currentDragEnd = null
                                }
                                CanvasTool.ELLIPSE -> {
                                    val start = currentDragStart
                                    val end = currentDragEnd
                                    if (start != null && end != null) {
                                        val topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y))
                                        val size = Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y))
                                        val ellipse = DrawingElement.Ellipse(topLeft, size, currentColor, strokeWidth)
                                        elements.add(ellipse)
                                        undoStack.add(ellipse)
                                        redoStack.clear()
                                        onElementsChange()
                                    }
                                    currentDragStart = null
                                    currentDragEnd = null
                                }
                                CanvasTool.ARROW -> {
                                    val start = currentDragStart
                                    val end = currentDragEnd
                                    if (start != null && end != null && (start - end).getDistance() > 10f) {
                                        val arrow = DrawingElement.Arrow(start, end, currentColor, strokeWidth)
                                        elements.add(arrow)
                                        undoStack.add(arrow)
                                        redoStack.clear()
                                        onElementsChange()
                                    }
                                    currentDragStart = null
                                    currentDragEnd = null
                                }
                                else -> Unit
                            }
                        }
                    )
                }
        ) {
            // Render committed elements
            elements.forEach { elem ->
                drawElement(elem, canvasScale, canvasOffset)
            }

            // Render live dragging elements
            if (activeTool == CanvasTool.PEN && currentPoints.size > 1) {
                val path = Path().apply {
                    val first = currentPoints.first() * canvasScale + canvasOffset
                    moveTo(first.x, first.y)
                    for (i in 1 until currentPoints.size) {
                        val p = currentPoints[i] * canvasScale + canvasOffset
                        lineTo(p.x, p.y)
                    }
                }
                drawPath(
                    path = path,
                    color = currentColor,
                    style = Stroke(width = strokeWidth * canvasScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else if (currentDragStart != null && currentDragEnd != null) {
                val start = currentDragStart!!
                val end = currentDragEnd!!
                when (activeTool) {
                    CanvasTool.RECTANGLE -> {
                        val topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)) * canvasScale + canvasOffset
                        val size = Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y)) * canvasScale
                        drawRect(
                            color = currentColor,
                            topLeft = topLeft,
                            size = size,
                            style = Stroke(width = strokeWidth * canvasScale)
                        )
                    }
                    CanvasTool.ELLIPSE -> {
                        val topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)) * canvasScale + canvasOffset
                        val size = Size(kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y)) * canvasScale
                        drawOval(
                            color = currentColor,
                            topLeft = topLeft,
                            size = size,
                            style = Stroke(width = strokeWidth * canvasScale)
                        )
                    }
                    CanvasTool.ARROW -> {
                        drawArrowLine(
                            start = start * canvasScale + canvasOffset,
                            end = end * canvasScale + canvasOffset,
                            color = currentColor,
                            width = strokeWidth * canvasScale
                        )
                    }
                    else -> Unit
                }
            }
        }

        // Floating Brutalist Toolbar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Tool Selector Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolButton(
                        icon = Icons.Default.PanTool,
                        label = "PAN",
                        isSelected = activeTool == CanvasTool.PAN_NAVIGATE,
                        onClick = { onToolChange(CanvasTool.PAN_NAVIGATE) }
                    )
                    ToolButton(
                        icon = Icons.Default.Edit,
                        label = "PEN",
                        isSelected = activeTool == CanvasTool.PEN,
                        onClick = { onToolChange(CanvasTool.PEN) }
                    )
                    ToolButton(
                        icon = Icons.Default.CropSquare,
                        label = "RECT",
                        isSelected = activeTool == CanvasTool.RECTANGLE,
                        onClick = { onToolChange(CanvasTool.RECTANGLE) }
                    )
                    ToolButton(
                        icon = Icons.Default.RadioButtonUnchecked,
                        label = "OVAL",
                        isSelected = activeTool == CanvasTool.ELLIPSE,
                        onClick = { onToolChange(CanvasTool.ELLIPSE) }
                    )
                    ToolButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = "ARROW",
                        isSelected = activeTool == CanvasTool.ARROW,
                        onClick = { onToolChange(CanvasTool.ARROW) }
                    )
                    ToolButton(
                        icon = Icons.Default.Clear,
                        label = "ERASE",
                        isSelected = activeTool == CanvasTool.ERASER,
                        onClick = { onToolChange(CanvasTool.ERASER) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (elements.isNotEmpty()) {
                                val last = elements.removeAt(elements.size - 1)
                                redoStack.add(last)
                                onElementsChange()
                            }
                        },
                        enabled = elements.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (elements.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }

                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeAt(redoStack.size - 1)
                                elements.add(next)
                                onElementsChange()
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                }

                // Palette & Stroke Row
                if (activeTool != CanvasTool.PAN_NAVIGATE && activeTool != CanvasTool.ERASER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        brutalistColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(color)
                                    .border(
                                        width = if (currentColor == color) 2.dp else 1.dp,
                                        color = if (currentColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                    )
                                    .clickable { currentColor = color }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PX: ${strokeWidth.toInt()}",
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .clickable {
                                    strokeWidth = when (strokeWidth) {
                                        2f -> 4f
                                        4f -> 8f
                                        8f -> 14f
                                        else -> 2f
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "WIDTH ⟳",
                                fontFamily = DotGothicFontFamily,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontFamily = SpaceMonoFontFamily,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawElement(
    elem: DrawingElement,
    canvasScale: Float,
    canvasOffset: Offset
) {
    when (elem) {
        is DrawingElement.FreehandStroke -> {
            if (elem.points.size > 1) {
                val path = Path().apply {
                    val first = elem.points.first() * canvasScale + canvasOffset
                    moveTo(first.x, first.y)
                    for (i in 1 until elem.points.size) {
                        val p = elem.points[i] * canvasScale + canvasOffset
                        lineTo(p.x, p.y)
                    }
                }
                drawPath(
                    path = path,
                    color = elem.color,
                    style = Stroke(
                        width = elem.strokeWidth * canvasScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        is DrawingElement.Rectangle -> {
            val topLeft = elem.topLeft * canvasScale + canvasOffset
            val size = elem.size * canvasScale
            drawRect(
                color = elem.color,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = elem.strokeWidth * canvasScale)
            )
        }
        is DrawingElement.Ellipse -> {
            val topLeft = elem.topLeft * canvasScale + canvasOffset
            val size = elem.size * canvasScale
            drawOval(
                color = elem.color,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = elem.strokeWidth * canvasScale)
            )
        }
        is DrawingElement.Arrow -> {
            drawArrowLine(
                start = elem.start * canvasScale + canvasOffset,
                end = elem.end * canvasScale + canvasOffset,
                color = elem.color,
                width = elem.strokeWidth * canvasScale
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowLine(
    start: Offset,
    end: Offset,
    color: Color,
    width: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = width,
        cap = StrokeCap.Round
    )
    val angle = atan2(end.y - start.y, end.x - start.x)
    val arrowHeadLength = 20f
    val arrowAngle = Math.PI / 6

    val p1 = Offset(
        (end.x - arrowHeadLength * cos(angle - arrowAngle)).toFloat(),
        (end.y - arrowHeadLength * sin(angle - arrowAngle)).toFloat()
    )
    val p2 = Offset(
        (end.x - arrowHeadLength * cos(angle + arrowAngle)).toFloat(),
        (end.y - arrowHeadLength * sin(angle + arrowAngle)).toFloat()
    )

    drawLine(color = color, start = end, end = p1, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color = color, start = end, end = p2, strokeWidth = width, cap = StrokeCap.Round)
}
