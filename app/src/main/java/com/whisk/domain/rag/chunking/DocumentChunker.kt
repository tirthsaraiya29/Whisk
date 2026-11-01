package com.whisk.domain.rag.chunking

import java.security.MessageDigest

object DocumentChunker {

    fun computeHash(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun chunkDocument(
        objectId: String,
        objectType: String,
        title: String,
        body: String,
        description: String? = null,
        updatedAt: Long = System.currentTimeMillis(),
        config: ChunkingConfig = ChunkingConfig(),
    ): List<DocumentChunk> {
        val fullText = buildFullDocumentText(description, body)
        if (fullText.isBlank()) return emptyList()

        val rawSections = parseMarkdownSections(title, fullText)
        val chunks = mutableListOf<DocumentChunk>()
        var globalSeq = 0

        for (section in rawSections) {
            val sectionChunks = splitSectionIntoChunks(
                objectId = objectId,
                objectType = objectType,
                title = title,
                headingPath = section.headingPath,
                text = section.text,
                startOffset = section.startOffset,
                updatedAt = updatedAt,
                config = config,
            )

            for (sc in sectionChunks) {
                val finalChunk = sc.copy(
                    id = "${objectId}_chunk_$globalSeq",
                    sequence = globalSeq,
                )
                chunks.add(finalChunk)
                globalSeq++
            }
        }

        return chunks
    }

    private fun buildFullDocumentText(description: String?, body: String): String {
        val sb = StringBuilder()
        if (description != null && description.isNotBlank()) {
            sb.append("Summary: ").append(description.trim()).append("\n\n")
        }
        sb.append(body.trim())
        return sb.toString()
    }

    private data class MarkdownSection(
        val headingPath: String,
        val text: String,
        val startOffset: Int,
    )

    private fun parseMarkdownSections(title: String, content: String): List<MarkdownSection> {
        val sections = mutableListOf<MarkdownSection>()
        val lines = content.lines()
        val headingStack = mutableListOf<Pair<Int, String>>()

        var currentSectionText = StringBuilder()
        var currentStartOffset = 0
        var currentPos = 0

        fun commitCurrentSection() {
            val text = currentSectionText.toString().trim()
            if (text.isNotEmpty()) {
                val path = if (headingStack.isEmpty()) {
                    title
                } else {
                    "$title > " + headingStack.joinToString(" > ") { it.second }
                }
                sections.add(MarkdownSection(path, text, currentStartOffset))
            }
            currentSectionText = StringBuilder()
        }

        var inCodeBlock = false

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
            }

            if (!inCodeBlock && trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                val headingText = trimmed.drop(level).trim()

                if (level in 1..6 && headingText.isNotEmpty()) {
                    commitCurrentSection()
                    currentStartOffset = currentPos

                    while (headingStack.isNotEmpty() && headingStack.last().first >= level) {
                        headingStack.removeAt(headingStack.size - 1)
                    }
                    headingStack.add(Pair(level, headingText))
                }
            }

            currentSectionText.append(line).append("\n")
            currentPos += line.length + 1
        }

        commitCurrentSection()
        return sections
    }

    private fun splitSectionIntoChunks(
        objectId: String,
        objectType: String,
        title: String,
        headingPath: String,
        text: String,
        startOffset: Int,
        updatedAt: Long,
        config: ChunkingConfig,
    ): List<DocumentChunk> {
        if (text.length <= config.maxChunkSizeChars) {
            val hash = computeHash(text)
            return listOf(
                DocumentChunk(
                    id = "",
                    sourceObjectId = objectId,
                    sourceObjectType = objectType,
                    sourceTitle = title,
                    headingPath = headingPath,
                    content = text,
                    contentHash = hash,
                    sequence = 0,
                    startOffset = startOffset,
                    endOffset = startOffset + text.length,
                    updatedAt = updatedAt,
                ),
            )
        }

        // Section is longer than maxChunkSizeChars — split gracefully on paragraph/newline or sentence boundaries with overlap
        val result = mutableListOf<DocumentChunk>()
        var start = 0

        while (start < text.length) {
            var end = (start + config.maxChunkSizeChars).coerceAtMost(text.length)

            if (end < text.length) {
                // Try to find natural breaking point: double newline, newline, period, or space
                val preferredBreak = findBestBreakPoint(text, start, end)
                if (preferredBreak > start + config.minChunkSizeChars) {
                    end = preferredBreak
                }
            }

            val chunkText = text.substring(start, end).trim()
            if (chunkText.length >= config.minChunkSizeChars || result.isEmpty()) {
                val hash = computeHash(chunkText)
                result.add(
                    DocumentChunk(
                        id = "",
                        sourceObjectId = objectId,
                        sourceObjectType = objectType,
                        sourceTitle = title,
                        headingPath = headingPath,
                        content = chunkText,
                        contentHash = hash,
                        sequence = 0,
                        startOffset = startOffset + start,
                        endOffset = startOffset + end,
                        updatedAt = updatedAt,
                    ),
                )
            }

            if (end >= text.length) break
            val nextStart = (end - config.chunkOverlapChars).coerceAtLeast(start + 1)
            start = nextStart
        }

        return result
    }

    private fun findBestBreakPoint(text: String, start: Int, targetEnd: Int): Int {
        val windowStart = (targetEnd - 150).coerceAtLeast(start)
        val window = text.substring(windowStart, targetEnd)

        val doubleNewlineIdx = window.lastIndexOf("\n\n")
        if (doubleNewlineIdx != -1) return windowStart + doubleNewlineIdx + 2

        val newlineIdx = window.lastIndexOf("\n")
        if (newlineIdx != -1) return windowStart + newlineIdx + 1

        val periodIdx = window.lastIndexOf(". ")
        if (periodIdx != -1) return windowStart + periodIdx + 2

        val spaceIdx = window.lastIndexOf(" ")
        if (spaceIdx != -1) return windowStart + spaceIdx + 1

        return targetEnd
    }
}
