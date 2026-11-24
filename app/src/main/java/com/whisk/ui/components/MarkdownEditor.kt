package com.whisk.ui.components

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whisk.domain.model.Note
import com.whisk.ui.theme.VercelMonoFontFamily

enum class EditorMode {
    EDIT, PREVIEW, SPLIT
}

class MarkdownVisualTransformation(
    private val primaryColor: Color,
    private val codeColor: Color,
    private val keywordColor: Color,
    private val headerColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        val str = text.text
        if (str.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val lines = str.lines()
        var inCode = false

        val keywords = setOf(
            "fun", "val", "var", "class", "import", "return", "if", "else",
            "function", "let", "const", "def", "select", "from", "where", "async", "await",
        )
        val keywordRegex = Regex("\\b(" + keywords.joinToString("|") + ")\\b", RegexOption.IGNORE_CASE)
        val linkRegex = Regex("\\[\\[(.*?)]]")
        val headerRegex = Regex("^#{1,3}\\s.*")

        for (match in linkRegex.findAll(str)) {
            val start = match.range.first.coerceIn(0, str.length)
            val end = (match.range.last + 1).coerceIn(0, str.length)
            builder.addStyle(
                SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                start,
                end,
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
                    lineEnd,
                )
            } else if (inCode) {
                builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), currentOffset, lineEnd)
                for (km in keywordRegex.findAll(line)) {
                    val kStart = (currentOffset + km.range.first).coerceIn(0, str.length)
                    val kEnd = (currentOffset + km.range.last + 1).coerceIn(0, str.length)
                    builder.addStyle(
                        SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        kStart,
                        kEnd,
                    )
                }
            } else if (headerRegex.matches(line)) {
                builder.addStyle(
                    SpanStyle(color = headerColor, fontWeight = FontWeight.Bold),
                    currentOffset,
                    lineEnd,
                )
            }
            charOffset = lineEnd + 1
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    allNotes: List<Note> = emptyList(),
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

    val haptic = LocalHapticFeedback.current

    val wordCount = remember(body) {
        if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(body) { body.length }
    val lineCount = remember(body) { body.lines().size }
    val readingTimeMinutes = remember(wordCount) { maxOf(1, (wordCount + 100) / 200) }
    var showTocSheet by remember { mutableStateOf(false) }

    fun applyFormattingWithHaptic(prefix: String, suffix: String = "") {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        applyFormatting(prefix, suffix)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Mode Toggle Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${wordCount} words • ${charCount} chars • ${readingTimeMinutes}m read",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showTocSheet = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Table of Contents",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Mode Toggle Switcher Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(2.dp),
            ) {
                listOf(
                    EditorMode.EDIT to "Edit",
                    EditorMode.SPLIT to "Split",
                    EditorMode.PREVIEW to "Preview",
                ).forEach { (targetMode, label) ->
                    val isSelected = mode == targetMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mode = targetMode
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

        // Formatting Toolbar
        if ((mode == EditorMode.EDIT) || (mode == EditorMode.SPLIT)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = { applyFormattingWithHaptic("# ") }, modifier = Modifier.size(40.dp)) {
                    Text("H1", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("## ") }, modifier = Modifier.size(40.dp)) {
                    Text("H2", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("**", "**") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("*", "*") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("- ") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("- [ ] ") }, modifier = Modifier.size(40.dp)) {
                    Text("[ ]", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("$$", "$$") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Functions, contentDescription = "Math LaTeX", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { applyFormattingWithHaptic("> ") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Quote Block", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("```kotlin\n", "\n```") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Code, contentDescription = "Code block", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("```mermaid\ngraph TD\n    A[Start] --> B[Process]\n```") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Hub, contentDescription = "Mermaid diagram", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { applyFormattingWithHaptic("[[", "]]") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Link, contentDescription = "Internal link", tint = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
        }

        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Untitled Note", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
            singleLine = true,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

        // Body Content Rendering according to EditorMode
        when (mode) {
            EditorMode.EDIT -> {
                val cursor = textFieldValue.selection.start
                val textUpToCursor = if (cursor in 0..textFieldValue.text.length) textFieldValue.text.substring(0, cursor) else ""
                val lastOpenBracket = textUpToCursor.lastIndexOf("[[")
                val showAutocomplete = lastOpenBracket != -1 && !textUpToCursor.substring(lastOpenBracket).contains("]]")
                val autocompleteQuery = if (showAutocomplete) textUpToCursor.substring(lastOpenBracket + 2) else ""

                val matchingNotes = if (showAutocomplete && allNotes.isNotEmpty()) {
                    allNotes.filter { note ->
                        note.title.contains(autocompleteQuery, ignoreCase = true) ||
                            note.aliases.any { it.contains(autocompleteQuery, ignoreCase = true) }
                    }.take(6)
                } else emptyList()

                // Tag auto-completion popup (#)
                val lastHash = textUpToCursor.lastIndexOf('#')
                val isTypingTag = lastHash != -1 && (lastHash == 0 || textUpToCursor[lastHash - 1].isWhitespace()) && !textUpToCursor.substring(lastHash).contains(' ')
                val tagQuery = if (isTypingTag) textUpToCursor.substring(lastHash + 1) else ""
                val allVaultTags = remember(allNotes) { allNotes.flatMap { it.tags }.distinct() }
                val matchingTags = if (isTypingTag && allVaultTags.isNotEmpty()) {
                    allVaultTags.filter { it.contains(tagQuery, ignoreCase = true) }.take(6)
                } else emptyList()

                val backtickCount = textUpToCursor.split("```").size - 1
                val inCodeFence = backtickCount % 2 != 0

                Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                    if (matchingNotes.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Link object (${matchingNotes.size} matches)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                                matchingNotes.forEach { targetNote ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val before = textUpToCursor.substring(0, lastOpenBracket)
                                                val after = if (cursor < textFieldValue.text.length) textFieldValue.text.substring(cursor) else ""
                                                val inserted = "[[" + targetNote.title + "]]"
                                                val newFullText = before + inserted + after
                                                val newCursor = before.length + inserted.length
                                                val updatedTfv = TextFieldValue(text = newFullText, selection = TextRange(newCursor))
                                                textFieldValue = updatedTfv
                                                onBodyChange(newFullText)
                                            }
                                            .padding(vertical = 6.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = getIconForObjectType(targetNote.typeId),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = targetNote.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "[${targetNote.typeId}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (matchingTags.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                matchingTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFD97706), RoundedCornerShape(8.dp))
                                            .clickable {
                                                val before = textUpToCursor.substring(0, lastHash)
                                                val after = if (cursor < textFieldValue.text.length) textFieldValue.text.substring(cursor) else ""
                                                val inserted = "#$tag "
                                                val newFullText = before + inserted + after
                                                val newCursor = before.length + inserted.length
                                                textFieldValue = TextFieldValue(text = newFullText, selection = TextRange(newCursor))
                                                onBodyChange(newFullText)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFD97706),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                    ) {
                        // Line-Number Gutter
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(top = 16.dp, end = 6.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            (1..lineCount.coerceAtMost(100)).forEach { lineNum ->
                                Text(
                                    text = lineNum.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontFamily = VercelMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                var nextVal = newValue
                                val oldText = textFieldValue.text
                                val newText = newValue.text
                                val oldCursor = textFieldValue.selection.start
                                val newCursor = newValue.selection.start

                                // Auto-pairing closing brackets
                                if (newText.length == oldText.length + 1 && newCursor > 0 && oldCursor == newCursor - 1) {
                                    val insertedChar = newText[newCursor - 1]
                                    val closingPair = when (insertedChar) {
                                        '[' -> ']'
                                        '(' -> ')'
                                        '{' -> '}'
                                        '"' -> '"'
                                        '\'' -> '\''
                                        '*' -> '*'
                                        else -> null
                                    }
                                    if (closingPair != null) {
                                        val autoPaired = newText.substring(0, newCursor) + closingPair + newText.substring(newCursor)
                                        nextVal = TextFieldValue(autoPaired, selection = TextRange(newCursor))
                                    }
                                }

                                // Smart bullet list continuation on Enter
                                if (newText.length == oldText.length + 1 && newCursor > 0 && newText[newCursor - 1] == '\n') {
                                    val lines = oldText.substring(0, oldCursor).lines()
                                    val prevLine = lines.lastOrNull() ?: ""
                                    val bulletPrefix = when {
                                        prevLine.trimStart().startsWith("- [ ] ") -> "- [ ] "
                                        prevLine.trimStart().startsWith("- [x] ") -> "- [ ] "
                                        prevLine.trimStart().startsWith("- ") -> "- "
                                        prevLine.trimStart().startsWith("* ") -> "* "
                                        Regex("^(\\d+)\\.\\s").containsMatchIn(prevLine.trimStart()) -> {
                                            val num = Regex("^(\\d+)\\.").find(prevLine.trimStart())?.groupValues?.get(1)?.toIntOrNull() ?: 1
                                            "${num + 1}. "
                                        }
                                        else -> null
                                    }
                                    if (bulletPrefix != null) {
                                        if (prevLine.trim() in listOf("- [ ]", "- [x]", "-", "*")) {
                                            val stripped = oldText.substring(0, (oldCursor - prevLine.length).coerceAtLeast(0)) + "\n" + oldText.substring(oldCursor)
                                            nextVal = TextFieldValue(stripped, selection = TextRange((oldCursor - prevLine.length + 1).coerceAtLeast(0)))
                                        } else {
                                            val continued = newText.substring(0, newCursor) + bulletPrefix + newText.substring(newCursor)
                                            nextVal = TextFieldValue(continued, selection = TextRange(newCursor + bulletPrefix.length))
                                        }
                                    }
                                }

                                textFieldValue = nextVal
                                if (nextVal.text != body) {
                                    onBodyChange(nextVal.text)
                                }
                            },
                            placeholder = { Text("Write notes in Markdown, code, or Mermaid diagrams...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            visualTransformation = visualTransformation,
                            keyboardOptions = KeyboardOptions(
                                capitalization = if (inCodeFence) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = !inCodeFence,
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
            EditorMode.SPLIT -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
                        textStyle = MaterialTheme.typography.bodyMedium,
                        visualTransformation = visualTransformation,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    )

                    // Right Preview Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    RenderMarkdownPreview(body, onLinkClick, onBodyChange)
                }
            }
        }

        // Table of Contents Bottom Sheet
        if (showTocSheet) {
            val headings = remember(body) {
                body.lines().mapIndexedNotNull { idx, l ->
                    val trimmed = l.trimStart()
                    when {
                        trimmed.startsWith("### ") -> Triple(3, trimmed.removePrefix("### ").trim(), idx)
                        trimmed.startsWith("## ") -> Triple(2, trimmed.removePrefix("## ").trim(), idx)
                        trimmed.startsWith("# ") -> Triple(1, trimmed.removePrefix("# ").trim(), idx)
                        else -> null
                    }
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showTocSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    Text(
                        text = "Table of Contents (${headings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (headings.isEmpty()) {
                        Text(
                            text = "No headers (# H1, ## H2, ### H3) found in note.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        headings.forEach { (level, title, _) ->
                            val indentDp = ((level - 1) * 16).dp
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showTocSheet = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp + indentDp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Title,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = title,
                                    style = if (level == 1) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownPreview(
    body: String,
    onLinkClick: (String) -> Unit,
    onBodyChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (body.isBlank()) {
        Text(
            text = "No content to display",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = modifier.padding(vertical = 12.dp),
        )
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val lines = body.lines()
    var i = 0
    val totalLines = lines.size

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        while (i < totalLines) {
            val line = lines[i]
            val trimmed = line.trim()

            // Code Fence block parsing
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim()
                val codeBuilder = StringBuilder()
                i++
                while (i < totalLines && !lines[i].trim().startsWith("```")) {
                    codeBuilder.append(lines[i]).append("\n")
                    i++
                }
                val codeContent = codeBuilder.toString().trimEnd()

                if (lang.equals("mermaid", ignoreCase = true)) {
                    MermaidDiagramView(code = codeContent, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        CodeBlockView(code = codeContent, language = lang.ifBlank { "text" })
                    }
                }
                i++
                continue
            }

            // Callout note parsing
            if (trimmed.startsWith("> [!")) {
                val type = trimmed.substringAfter("> [!").substringBefore("]").uppercase()
                val calloutTitle = trimmed.substringAfter("]", "").trim()
                val calloutBody = StringBuilder()
                if (calloutTitle.isNotBlank()) calloutBody.append(calloutTitle).append("\n")
                i++
                while (i < totalLines && lines[i].trim().startsWith(">")) {
                    calloutBody.append(lines[i].trim().removePrefix(">").trim()).append("\n")
                    i++
                }
                val (calloutColor, iconVector) = when (type) {
                    "WARNING" -> Color(0xFFEF4444) to Icons.Default.Warning
                    "IMPORTANT" -> Color(0xFF8B5CF6) to Icons.Default.Info
                    "TIP" -> Color(0xFF10B981) to Icons.Default.AutoAwesome
                    else -> primaryColor to Icons.Default.Info
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = calloutColor.copy(alpha = 0.1f)),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(imageVector = iconVector, contentDescription = null, tint = calloutColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = type, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = calloutColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = calloutBody.toString().trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                continue
            }

            // Headings
            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                trimmed == "---" || trimmed == "***" -> {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                }
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") -> {
                    val isChecked = trimmed.startsWith("- [x] ")
                    val taskText = trimmed.removePrefix(if (isChecked) "- [x] " else "- [ ] ").trim()
                    val lineIdx = i
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (onBodyChange != null) {
                                    val newLines = lines.toMutableList()
                                    val prefix = if (checked) "- [x] " else "- [ ] "
                                    newLines[lineIdx] = prefix + taskText
                                    onBodyChange(newLines.joinToString("\n"))
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = taskText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletText = trimmed.removePrefix("- ").removePrefix("* ").trim()
                    Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp), verticalAlignment = Alignment.Top) {
                        Text(text = "•", style = MaterialTheme.typography.bodyLarge, color = primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        val annotatedText = MarkdownParser.parseInlineMarkdown(bulletText, primaryColor, onSurfaceColor, codeBgColor)
                        ClickableText(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyLarge.copy(color = onSurfaceColor),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "NOTE_LINK", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation -> onLinkClick(annotation.item) }
                            },
                        )
                    }
                }
                trimmed.isNotBlank() -> {
                    val annotatedText = MarkdownParser.parseInlineMarkdown(trimmed, primaryColor, onSurfaceColor, codeBgColor)
                    ClickableText(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge.copy(color = onSurfaceColor),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(tag = "NOTE_LINK", start = offset, end = offset)
                                .firstOrNull()?.let { annotation -> onLinkClick(annotation.item) }
                        },
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            i++
        }
    }
}
