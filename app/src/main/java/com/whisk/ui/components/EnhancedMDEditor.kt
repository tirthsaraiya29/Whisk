package com.whisk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
        var inMath = false
        var inTable = false

        val keywords = setOf(
            "fun", "val", "var", "class", "import", "return", "if", "else",
            "function", "let", "const", "def", "select", "from", "where", "async", "await",
        )
        val keywordRegex = Regex("\\\\b(" + keywords.joinToString("|") + ")\\\\b", RegexOption.IGNORE_CASE)
        val linkRegex = Regex("\\\\[\\\\[(.*?)]]")
        val headerRegex = Regex("^#{1,3}\\\\s.*")

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
    var showTableSheet by remember { mutableStateOf(false) }
    var showImageSheet by remember { mutableStateOf(false) }
    var showLinkSheet by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }

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
        if (body.isBlank()) 0 else body.trim().split(Regex("\\\\s+")).size
    }
    val charCount = remember(body) { body.length }
    val lineCount = remember(body) { body.lines().size }
    val readingTimeMinutes = remember(wordCount) { maxOf(1, (wordCount + 100) / 200) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                placeholder = { Text("Note Title", style = MaterialTheme.typography.titleLarge) }
            )
        }
        
        HorizontalDivider()

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { 
                textFieldValue = it
                onBodyChange(it.text)
            },
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
            placeholder = { Text("Start typing...") }
        )
    }
}
