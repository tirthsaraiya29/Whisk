package com.whisk.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * High-performance inline Markdown & Wiki-Link parser for Compose.
 * Strips raw markdown syntax characters (*, **, `, ~~, [[ ]]) and applies proper
 * typography styles (bold, italic, code chips, strikethrough, links).
 */
object MarkdownParser {

    private val inlineRegex = Regex(
        "(\\[\\[(?<wikilink>.*?)]])|" +
        "(\\[(?<linktext>.*?)]\\((?<linkurl>.*?)\\))|" +
        "(\\*\\*(?<bold1>.*?)\\*\\*)|" +
        "(__(?<bold2>.*?)__)|" +
        "(\\*(?<italic1>.*?)\\*)|" +
        "(_(?<italic2>.*?)_)|" +
        "(`(?<codeText>.*?)`)|" +
        "(~~(?<strike>.*?)~~)",
    )

    fun parseInlineMarkdown(
        text: String,
        primaryColor: Color,
        onBackgroundColor: Color,
        codeBgColor: Color,
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val matches = inlineRegex.findAll(text).toList()
        if (matches.isEmpty()) {
            return AnnotatedString(text)
        }

        return buildAnnotatedString {
            var lastIndex = 0

            matches.forEach { match ->
                val start = match.range.first
                val end = match.range.last

                if (start > lastIndex) {
                    append(text.substring(lastIndex, start))
                }

                val wikiLink = match.groups["wikilink"]?.value
                val linkText = match.groups["linktext"]?.value
                val linkUrl = match.groups["linkurl"]?.value
                val boldText = match.groups["bold1"]?.value ?: match.groups["bold2"]?.value
                val italicText = match.groups["italic1"]?.value ?: match.groups["italic2"]?.value
                val codeText = match.groups["codeText"]?.value
                val strikeText = match.groups["strike"]?.value

                when {
                    wikiLink != null -> {
                        val cleanTitle = wikiLink.trim()
                        val linkStart = length
                        append(" [[ $cleanTitle ]] ")
                        val linkEnd = length

                        addStyle(
                            style = SpanStyle(
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold,
                                background = primaryColor.copy(alpha = 0.15f),
                            ),
                            start = linkStart,
                            end = linkEnd,
                        )
                        addStringAnnotation(
                            tag = "NOTE_LINK",
                            annotation = cleanTitle,
                            start = linkStart,
                            end = linkEnd,
                        )
                    }
                    (linkText != null) && (linkUrl != null) -> {
                        val linkStart = length
                        append(linkText)
                        val linkEnd = length

                        addStyle(
                            style = SpanStyle(
                                color = primaryColor,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                            ),
                            start = linkStart,
                            end = linkEnd,
                        )
                        addStringAnnotation(
                            tag = "URL_LINK",
                            annotation = linkUrl,
                            start = linkStart,
                            end = linkEnd,
                        )
                    }
                    boldText != null -> {
                        val startPos = length
                        append(boldText)
                        val endPos = length
                        addStyle(
                            style = SpanStyle(fontWeight = FontWeight.Bold, color = onBackgroundColor),
                            start = startPos,
                            end = endPos,
                        )
                    }
                    italicText != null -> {
                        val startPos = length
                        append(italicText)
                        val endPos = length
                        addStyle(
                            style = SpanStyle(fontStyle = FontStyle.Italic),
                            start = startPos,
                            end = endPos,
                        )
                    }
                    codeText != null -> {
                        val startPos = length
                        append(" $codeText ")
                        val endPos = length
                        addStyle(
                            style = SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                background = codeBgColor,
                                color = primaryColor,
                            ),
                            start = startPos,
                            end = endPos,
                        )
                    }
                    strikeText != null -> {
                        val startPos = length
                        append(strikeText)
                        val endPos = length
                        addStyle(
                            style = SpanStyle(textDecoration = TextDecoration.LineThrough),
                            start = startPos,
                            end = endPos,
                        )
                    }
                    else -> {
                        append(match.value)
                    }
                }

                lastIndex = end + 1
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
