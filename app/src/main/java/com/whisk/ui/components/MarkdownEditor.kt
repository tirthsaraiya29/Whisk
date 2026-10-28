package com.whisk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

    val wordCount = remember(body) {
        if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(body) { body.length }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Formatting Quick Buttons (when in EDIT mode)
            if (mode == EditorMode.EDIT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = { onBodyChange("$body\n# ") }) {
                        Text("H1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onBodyChange("$body\n## ") }) {
                        Text("H2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onBodyChange("$body**bold**") }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                    }
                    IconButton(onClick = { onBodyChange("$body*italic*") }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                    }
                    IconButton(onClick = { onBodyChange("$body\n- ") }) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "List")
                    }
                    IconButton(onClick = { onBodyChange("$body\n> ") }) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Quote")
                    }
                    IconButton(onClick = { onBodyChange("$body\n```kotlin\n\n```") }) {
                        Icon(Icons.Default.Code, contentDescription = "Code block")
                    }
                    IconButton(onClick = { onBodyChange("$body[[Note Title]]") }) {
                        Icon(Icons.Default.Link, contentDescription = "Internal link")
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

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
                    value = body,
                    onValueChange = onBodyChange,
                    placeholder = { Text("Write notes in Markdown or code...", style = MaterialTheme.typography.bodyLarge) },
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

    lines.forEachIndexed { index, line ->
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
            }
            inCodeBlock -> {
                codeBuilder.append(line).append("\n")
            }
            line.startsWith("# ") -> {
                Text(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            line.startsWith("## ") -> {
                Text(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            line.startsWith("### ") -> {
                Text(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
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
                    Text(
                        text = line.removePrefix("> "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            line.trim() == "---" || line.trim() == "***" -> {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
            line.startsWith("- [ ]") || line.startsWith("- [x]") || line.startsWith("- [X]") -> {
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
                            updatedLines[index] = "$newCheckStr $taskText"
                            onBodyChange(updatedLines.joinToString("\n"))
                        },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    RenderLineWithLinks(taskText, onLinkClick)
                }
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)) {
                    Text("• ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.weight(1f)) {
                        RenderLineWithLinks(line.substring(2), onLinkClick)
                    }
                }
            }
            line.isBlank() -> {
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> {
                RenderLineWithLinks(line, onLinkClick)
            }
        }
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        CodeBlockView(code = codeBuilder.toString().trimEnd(), language = codeLang)
    }
}

@Composable
fun RenderLineWithLinks(
    text: String,
    onLinkClick: (String) -> Unit
) {
    val linkRegex = Regex("\\[\\[(.*?)\\]\\]")
    val matches = linkRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    } else {
        val primaryColor = MaterialTheme.colorScheme.primary
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground

        val annotatedString = remember(text, primaryColor, onBackgroundColor) {
            buildAnnotatedString {
                var lastIndex = 0
                matches.forEach { match ->
                    val start = match.range.first
                    val end = match.range.last
                    if (start > lastIndex) {
                        append(text.substring(lastIndex, start))
                    }
                    val linkTitle = match.groupValues[1]
                    val linkStart = length
                    append(" [[ $linkTitle ]] ")
                    val linkEnd = length

                    addStyle(
                        style = SpanStyle(
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                            background = primaryColor.copy(alpha = 0.15f)
                        ),
                        start = linkStart,
                        end = linkEnd
                    )
                    addStringAnnotation(
                        tag = "NOTE_LINK",
                        annotation = linkTitle,
                        start = linkStart,
                        end = linkEnd
                    )
                    lastIndex = end + 1
                }
                if (lastIndex < text.length) {
                    append(text.substring(lastIndex))
                }
            }
        }

        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(color = onBackgroundColor),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "NOTE_LINK", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onLinkClick(annotation.item)
                    }
            }
        )
    }
}
