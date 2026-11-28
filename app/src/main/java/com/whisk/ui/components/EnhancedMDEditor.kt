package com.whisk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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

enum class EnhancedEditorMode {
    EDIT, PREVIEW, SPLIT, READING
}

enum class EnhancedEditorTheme {
    LIGHT, DARK, SEPPIA, HIGH_CONTRAST
}

class EnhancedMarkdownVisualTransformation(
    private val primaryColor: Color,
    private val codeColor: Color,
    private val keywordColor: Color,
    private val headerColor: Color,
    private val linkColor: Color,
    private val listColor: Color,
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
                SpanStyle(color = linkColor, fontWeight = FontWeight.Bold),
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
fun EnhancedMarkdownEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    allNotes: List<Note> = emptyList(),
    theme: EnhancedEditorTheme = EnhancedEditorTheme.LIGHT,
    onModeChange: (EnhancedEditorMode) -> Unit = {},
) {
    var mode by remember { mutableStateOf(EnhancedEditorMode.EDIT) }
    var showOutlineSheet by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = body, selection = TextRange(body.length)))
    }

    LaunchedEffect(body) {
        if (body != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = body)
        }
    }

    val haptic = LocalHapticFeedback.current

    val wordCount = remember(body) {
        if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(body) { body.length }
    val lineCount = remember(body) { body.lines().size }
    val readingTimeMinutes = remember(wordCount) { maxOf(1, (wordCount + 100) / 200) }

    // Wikilink Autocomplete state
    val selectionStart = textFieldValue.selection.start
    val textBeforeCursor = textFieldValue.text.take(selectionStart)
    val wikilinkMatch = remember(textBeforeCursor) {
        val lastIndex = textBeforeCursor.lastIndexOf("[[")
        if (lastIndex != -1 && !textBeforeCursor.substring(lastIndex).contains("]]")) {
            textBeforeCursor.substring(lastIndex + 2)
        } else null
    }

    val filteredSuggestions = remember(wikilinkMatch, allNotes) {
        if (wikilinkMatch == null) emptyList()
        else {
            val query = wikilinkMatch.trim()
            allNotes.filter { note ->
                note.title.contains(query, ignoreCase = true)
            }.take(6)
        }
    }

    fun insertFormatting(prefix: String, suffix: String = prefix, defaultPlaceholder: String = "") {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val selectedText = if (selection.collapsed) defaultPlaceholder else text.substring(selection.start, selection.end)
        val newText = text.replaceRange(selection.start, selection.end, "$prefix$selectedText$suffix")
        val newCursor = selection.start + prefix.length + selectedText.length
        textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        onBodyChange(newText)
    }

    fun insertWikilink(targetTitle: String) {
        val text = textFieldValue.text
        val lastIndex = textBeforeCursor.lastIndexOf("[[")
        if (lastIndex != -1) {
            val newText = text.replaceRange(lastIndex, selectionStart, "[[$targetTitle]]")
            val newCursor = lastIndex + targetTitle.length + 4
            textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
            onBodyChange(newText)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Mode & Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mode Selectors
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EnhancedEditorMode.entries.forEach { editorMode ->
                    val isSelected = mode == editorMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mode = editorMode
                                onModeChange(editorMode)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = editorMode.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Outline & Info button
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showOutlineSheet = true
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = "Heading Outline",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        // Document Title Field
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            placeholder = {
                Text(
                    "Untitled Note",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Formatting Quick Bar (Visible in Edit and Split modes)
        if (mode == EnhancedEditorMode.EDIT || mode == EnhancedEditorMode.SPLIT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { insertFormatting("**", "**", "bold") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("*", "*", "italic") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("# ", "", "Heading") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Title, contentDescription = "Heading", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("- ", "", "List item") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "List", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("- [ ] ", "", "Task item") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CheckBox, contentDescription = "Task", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("> ", "", "Quote") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Quote", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("`", "`", "code") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Code, contentDescription = "Inline Code", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("```kotlin\n", "\n```", "fun main() {\n\n}") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Code, contentDescription = "Code Block", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("$$\n", "\n$$", "E = mc^2") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Functions, contentDescription = "Math Block", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("[[", "]]", "Note Title") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Link, contentDescription = "Wikilink", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("```mermaid\nflowchart TD\n  A[Start] --> B[Process]\n", "\n```", "") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Hub, contentDescription = "Mermaid Diagram", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { insertFormatting("\n| Header 1 | Header 2 |\n| --- | --- |\n| Cell 1 | Cell 2 |\n", "", "") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.TableChart, contentDescription = "Table", modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }

        // Wikilink Autocomplete Dropdown Box
        if (filteredSuggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "LINK TO VAULT NOTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    filteredSuggestions.forEach { suggestNote ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    insertWikilink(suggestNote.title)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = suggestNote.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // Main Workspace Content Area (Edit / Preview / Split)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (mode) {
                EnhancedEditorMode.EDIT -> {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            onBodyChange(it.text)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        visualTransformation = EnhancedMarkdownVisualTransformation(
                            primaryColor = MaterialTheme.colorScheme.primary,
                            codeColor = MaterialTheme.colorScheme.secondary,
                            keywordColor = MaterialTheme.colorScheme.tertiary,
                            headerColor = MaterialTheme.colorScheme.primary,
                            linkColor = MaterialTheme.colorScheme.primary,
                            listColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        placeholder = { Text("Start typing markdown or insert wikilinks with [[...", style = MaterialTheme.typography.bodyLarge) },
                    )
                }

                EnhancedEditorMode.PREVIEW, EnhancedEditorMode.READING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        RenderMarkdownPreview(
                            body = body,
                            onLinkClick = onLinkClick,
                        )
                    }
                }

                EnhancedEditorMode.SPLIT -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            OutlinedTextField(
                                value = textFieldValue,
                                onValueChange = {
                                    textFieldValue = it
                                    onBodyChange(it.text)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        ) {
                            RenderMarkdownPreview(
                                body = body,
                                onLinkClick = onLinkClick,
                            )
                        }
                    }
                }
            }
        }

        // Bottom Editor Metrics Status Bar
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$wordCount words • $charCount chars • $lineCount lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$readingTimeMinutes min read",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    // Heading Outline Bottom Sheet
    if (showOutlineSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOutlineSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = "Document Heading Outline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))

                val headings = remember(body) {
                    body.lines().filter { it.trimStart().startsWith("#") }
                }

                if (headings.isEmpty()) {
                    Text(
                        text = "No headings found. Use #, ##, or ### to construct an outline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(headings) { heading ->
                            val level = heading.takeWhile { it == '#' }.length
                            val titleText = heading.trimStart('#').trim()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        showOutlineSheet = false
                                        val idx = body.indexOf(heading)
                                        if (idx != -1) {
                                            textFieldValue = textFieldValue.copy(selection = TextRange(idx))
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(modifier = Modifier.width(((level - 1) * 16).dp))
                                Text(
                                    text = "H$level",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
