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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class EditorMode {
    EDIT, PREVIEW
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

    var textFieldValue by remember(body) {
        mutableStateOf(TextFieldValue(text = body, selection = TextRange(body.length)))
    }

    fun applyFormatting(prefix: String, suffix: String = "") {
        val currentTfv = textFieldValue
        val selection = currentTfv.selection
        val text = currentTfv.text
        val selectedText = if (selection.collapsed) "" else text.substring(selection.start, selection.end)
        val inserted = prefix + selectedText + suffix
        val newText = text.substring(0, selection.start) + inserted + text.substring(selection.end)
        val newCursor = if (selection.collapsed && suffix.isNotEmpty()) {
            selection.start + prefix.length
        } else {
            selection.start + inserted.length
        }
        val nextTfv = TextFieldValue(text = newText, selection = TextRange(newCursor))
        textFieldValue = nextTfv
        onBodyChange(newText)
    }

    val wordCount = remember(body) {
        if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(body) { body.length }

    Column(modifier = modifier.fillMaxSize()) {
        // Mode Toggle Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$wordCount words • $charCount chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Mode Toggle Switcher
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(2.dp)
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (mode == EditorMode.EDIT) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { mode = EditorMode.EDIT }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (mode == EditorMode.EDIT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                "Edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mode == EditorMode.EDIT) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (mode == EditorMode.PREVIEW) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { mode = EditorMode.PREVIEW }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (mode == EditorMode.PREVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                "Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mode == EditorMode.PREVIEW) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Formatting Scrollable Toolbar (when in EDIT mode)
        if (mode == EditorMode.EDIT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { applyFormatting("# ") }) {
                    Text("H1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { applyFormatting("## ") }) {
                    Text("H2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { applyFormatting("**", "**") }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = { applyFormatting("*", "*") }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = { applyFormatting("- ") }) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "List")
                }
                IconButton(onClick = { applyFormatting("> ") }) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Quote")
                }
                IconButton(onClick = { applyFormatting("```kotlin\n", "\n```") }) {
                    Icon(Icons.Default.Code, contentDescription = "Code block")
                }
                IconButton(onClick = { applyFormatting("```mermaid\ngraph TD\n    A[Start] --> B[Process]\n```") }) {
                    Icon(Icons.Default.Hub, contentDescription = "Mermaid diagram")
                }
                IconButton(onClick = { applyFormatting("[[", "]]") }) {
                    Icon(Icons.Default.Link, contentDescription = "Internal link")
                }
            }
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

        // Body Content (Edit mode vs Formatted Preview mode)
        if (mode == EditorMode.EDIT) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        if (newValue.text != body) {
                            onBodyChange(newValue.text)
                        }
                    },
                    placeholder = { Text("Write notes in Markdown, code, or Mermaid diagrams...", style = MaterialTheme.typography.bodyLarge) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                // Word and Character counter footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "$wordCount words • $charCount characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Rendered Markdown Preview
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
                    // End code block
                    CodeBlockView(code = codeBuilder.toString().trimEnd(), language = codeLang)
                    codeBuilder.clear()
                    inCodeBlock = false
                } else {
                    // Start code block
                    inCodeBlock = true
                    codeLang = line.removePrefix("```").trim().ifBlank { "code" }
                }
                i++
            }
            inCodeBlock -> {
                codeBuilder.append(line).append("\n")
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
                // Collect table block
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
        // Table Header
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

        // Table Rows
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
