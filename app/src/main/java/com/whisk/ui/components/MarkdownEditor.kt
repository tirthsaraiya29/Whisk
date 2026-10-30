package com.whisk.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EditorMode {
    EDIT, PREVIEW, SPLIT
}

class MarkdownVisualTransformation(
    private val primaryColor: Color,
    private val codeColor: Color,
    private val keywordColor: Color,
    private val headerColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        val str = text.text
        if (str.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val lines = str.lines()
        var inCode = false

        val keywords = setOf(
            "fun", "val", "var", "class", "import", "return", "if", "else",
            "function", "let", "const", "def", "select", "from", "where", "async", "await"
        )
        val keywordRegex = Regex("\\b(" + keywords.joinToString("|") + ")\\b", RegexOption.IGNORE_CASE)
        val linkRegex = Regex("\\[\\[(.*?)\\]\\]")
        val headerRegex = Regex("^#{1,3}\\s.*")

        for (match in linkRegex.findAll(str)) {
            val start = match.range.first.coerceIn(0, str.length)
            val end = (match.range.last + 1).coerceIn(0, str.length)
            builder.addStyle(
                SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                start,
                end
            )
        }

        var charOffset = 0
        lines.forEach { line ->
            val lineEnd = (charOffset + line.length).coerceIn(0, str.length)
            val currentOffset = charOffset.coerceIn(0, str.length)

            if (line.trim().startsWith("```")) {
                inCode = !inCode
                builder.addStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor, fontWeight = FontWeight.Bold),
                    currentOffset,
                    lineEnd
                )
            } else if (inCode) {
                builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), currentOffset, lineEnd)
                for (km in keywordRegex.findAll(line)) {
                    val kStart = (currentOffset + km.range.first).coerceIn(0, str.length)
                    val kEnd = (currentOffset + km.range.last + 1).coerceIn(0, str.length)
                    builder.addStyle(
                        SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        kStart,
                        kEnd
                    )
                }
            } else if (headerRegex.matches(line)) {
                builder.addStyle(
                    SpanStyle(color = headerColor, fontWeight = FontWeight.Bold),
                    currentOffset,
                    lineEnd
                )
            }
            charOffset = lineEnd + 1
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun MarkdownEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(EditorMode.EDIT) }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = body, selection = TextRange(body.length)))
    }

    LaunchedEffect(body) {
        if (body != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = body)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val codeColor = MaterialTheme.colorScheme.tertiary
    val keywordColor = Color(0xFFD97706)
    val headerColor = MaterialTheme.colorScheme.primary

    val visualTransformation = remember(primaryColor, codeColor, keywordColor, headerColor) {
        MarkdownVisualTransformation(primaryColor, codeColor, keywordColor, headerColor)
    }

    fun applyFormatting(prefix: String, suffix: String = "") {
        val currentTfv = textFieldValue
        val selection = currentTfv.selection
        val text = currentTfv.text
        val selectedText = if (selection.collapsed) "" else text.substring(selection.start.coerceIn(0, text.length), selection.end.coerceIn(0, text.length))
        val inserted = prefix + selectedText + suffix
        val newText = text.substring(0, selection.start.coerceIn(0, text.length)) + inserted + text.substring(selection.end.coerceIn(0, text.length))
        val newCursor = if (selection.collapsed && suffix.isNotEmpty()) {
            selection.start + prefix.length
        } else {
            selection.start + inserted.length
        }
        val nextTfv = TextFieldValue(text = newText, selection = TextRange(newCursor.coerceIn(0, newText.length)))
        textFieldValue = nextTfv
        onBodyChange(newText)
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val wordCount = remember(body) {
        if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(body) { body.length }
    val lineCount = remember(body) { body.lines().size }

    fun applyFormattingWithHaptic(prefix: String, suffix: String = "") {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        applyFormatting(prefix, suffix)
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Mode Toggle Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${wordCount}W // ${charCount}C // ${lineCount}L",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Brutalist Mode Toggle Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    EditorMode.EDIT to "EDIT",
                    EditorMode.SPLIT to "SPLIT",
                    EditorMode.PREVIEW to "PREVIEW"
                ).forEach { (targetMode, label) ->
                    val isSelected = mode == targetMode
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                mode = targetMode
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // Formatting Scrollable Toolbar
        if (mode == EditorMode.EDIT || mode == EditorMode.SPLIT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { applyFormattingWithHaptic("# ") }) {
                    Text("H1", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { applyFormattingWithHaptic("## ") }) {
                    Text("H2", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { applyFormattingWithHaptic("**", "**") }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = { applyFormattingWithHaptic("*", "*") }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = { applyFormattingWithHaptic("- ") }) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "List")
                }
                IconButton(onClick = { applyFormattingWithHaptic("> [!NOTE] ") }) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Callout Note")
                }
                IconButton(onClick = { applyFormattingWithHaptic("```kotlin\n", "\n```") }) {
                    Icon(Icons.Default.Code, contentDescription = "Code block")
                }
                IconButton(onClick = { applyFormattingWithHaptic("```mermaid\ngraph TD\n    A[Start] --> B[Process]\n```") }) {
                    Icon(Icons.Default.Hub, contentDescription = "Mermaid diagram")
                }
                IconButton(onClick = { applyFormattingWithHaptic("[[", "]]") }) {
                    Icon(Icons.Default.Link, contentDescription = "Internal link")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        }

        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Note Title...", style = MaterialTheme.typography.headlineMedium) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Body Content Rendering according to EditorMode
        when (mode) {
            EditorMode.EDIT -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Line-Number Gutter
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(top = 16.dp, end = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        (1..lineCount.coerceAtMost(100)).forEach { lineNum ->
                            Text(
                                text = "$lineNum",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != body) {
                                onBodyChange(newValue.text)
                            }
                        },
                        placeholder = { Text("Write notes in Markdown, code, or Mermaid diagrams...", style = MaterialTheme.typography.bodyLarge) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                        visualTransformation = visualTransformation,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }
            EditorMode.SPLIT -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Left Editor Column
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != body) {
                                onBodyChange(newValue.text)
                            }
                        },
                        placeholder = { Text("Write notes...", style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        visualTransformation = visualTransformation,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    // Right Preview Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                    ) {
                        RenderMarkdownPreview(body, onLinkClick, onBodyChange)
                    }
                }
            }
            EditorMode.PREVIEW -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    RenderMarkdownPreview(body, onLinkClick, onBodyChange)
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownPreview(
    body: String,
    onLinkClick: (String) -> Unit,
    onBodyChange: (String) -> Unit
) {
    val lines = body.lines()
    var inCodeBlock = false
    var codeLang = ""
    val codeBuilder = StringBuilder()

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        when {
            line.startsWith("```") -> {
                if (inCodeBlock) {
                    CodeBlockView(code = codeBuilder.toString().trimEnd(), language = codeLang)
                    codeBuilder.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeLang = line.removePrefix("```").trim().ifBlank { "code" }
                }
                i++
            }
            inCodeBlock -> {
                codeBuilder.append(line).append("\n")
                i++
            }
            line.startsWith("> [!WARNING]") || line.startsWith("> [!IMPORTANT]") -> {
                val calloutText = line.substringAfter("]").trim()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22D97706))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        RenderFormattedMarkdownLine(text = calloutText, onLinkClick = onLinkClick)
                    }
                }
                i++
            }
            line.startsWith("> [!NOTE]") || line.startsWith("> [!TIP]") -> {
                val calloutText = line.substringAfter("]").trim()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        RenderFormattedMarkdownLine(text = calloutText, onLinkClick = onLinkClick)
                    }
                }
                i++
            }
            line.startsWith("# ") -> {
                RenderFormattedMarkdownLine(
                    text = line.removePrefix("# "),
                    onLinkClick = onLinkClick,
                    textStyle = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(vertical = 6.dp),
                    isHeader = true
                )
                i++
            }
            line.startsWith("## ") -> {
                RenderFormattedMarkdownLine(
                    text = line.removePrefix("## "),
                    onLinkClick = onLinkClick,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                    isHeader = true
                )
                i++
            }
            line.startsWith("### ") -> {
                RenderFormattedMarkdownLine(
                    text = line.removePrefix("### "),
                    onLinkClick = onLinkClick,
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                    isHeader = true
                )
                i++
            }
            line.startsWith("> ") -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RenderFormattedMarkdownLine(
                        text = line.removePrefix("> "),
                        onLinkClick = onLinkClick,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
                i++
            }
            line.trim() == "---" || line.trim() == "***" -> {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                i++
            }
            line.startsWith("- [ ]") || line.startsWith("- [x]") || line.startsWith("- [X]") -> {
                val lineIndex = i
                val isChecked = line.contains("[x]", ignoreCase = true)
                val taskText = line.substringAfter("]").trim()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            val newCheckStr = if (checked) "- [x]" else "- [ ]"
                            val updatedLines = lines.toMutableList()
                            updatedLines[lineIndex] = "$newCheckStr $taskText"
                            onBodyChange(updatedLines.joinToString("\n"))
                        },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    RenderFormattedMarkdownLine(text = taskText, onLinkClick = onLinkClick)
                }
                i++
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                    Text("• ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.weight(1f)) {
                        RenderFormattedMarkdownLine(text = line.substring(2), onLinkClick = onLinkClick)
                    }
                }
                i++
            }
            line.trim().startsWith("|") && line.trim().endsWith("|") -> {
                val tableBlock = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableBlock.add(lines[i])
                    i++
                }
                RenderMarkdownTable(tableLines = tableBlock, onLinkClick = onLinkClick)
            }
            line.isBlank() -> {
                Spacer(modifier = Modifier.height(8.dp))
                i++
            }
            else -> {
                RenderFormattedMarkdownLine(text = line, onLinkClick = onLinkClick)
                i++
            }
        }
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        CodeBlockView(code = codeBuilder.toString().trimEnd(), language = codeLang)
    }
}

@Composable
fun RenderFormattedMarkdownLine(
    text: String,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    isHeader: Boolean = false
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant

    val annotatedString = remember(text, primaryColor, onBackgroundColor, codeBgColor) {
        MarkdownParser.parseInlineMarkdown(text, primaryColor, onBackgroundColor, codeBgColor)
    }

    val style = if (isHeader) textStyle.copy(fontWeight = FontWeight.Bold) else textStyle

    ClickableText(
        text = annotatedString,
        style = style.copy(color = onBackgroundColor),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "NOTE_LINK", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
            annotatedString.getStringAnnotations(tag = "URL_LINK", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
        },
        modifier = modifier
    )
}

@Composable
fun RenderMarkdownTable(
    tableLines: List<String>,
    onLinkClick: (String) -> Unit
) {
    val rows = tableLines.map { line ->
        line.trim().trim('|').split('|').map { it.trim() }
    }.filter { row ->
        !row.all { cell -> cell.all { char -> char == '-' || char == ':' || char == ' ' } }
    }

    if (rows.isEmpty()) return

    val headerRow = rows.firstOrNull()
    val dataRows = if (rows.size > 1) rows.subList(1, rows.size) else emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        if (headerRow != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                headerRow.forEach { cell ->
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        RenderFormattedMarkdownLine(cell, onLinkClick, isHeader = true)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }

        dataRows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { cell ->
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        RenderFormattedMarkdownLine(cell, onLinkClick)
                    }
                }
            }
            if (rowIndex < dataRows.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}
