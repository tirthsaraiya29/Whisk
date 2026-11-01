package com.whisk

import com.whisk.domain.rag.chunking.ChunkingConfig
import com.whisk.domain.rag.chunking.DocumentChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentChunkerTest {

    @Test
    fun testChunkingBasicMarkdown() {
        val markdown = """
# Getting Started

Whisk is a knowledge app.

## Architecture

It uses Room database and local AI models.
        """.trimIndent()

        val chunks = DocumentChunker.chunkDocument(
            objectId = "note1",
            objectType = "note",
            title = "Test Note",
            body = markdown,
        )

        assertTrue(chunks.isNotEmpty())
        assertEquals("note1", chunks.first().sourceObjectId)
        assertTrue(chunks.any { it.headingPath.contains("Getting Started") })
        assertTrue(chunks.any { it.headingPath.contains("Architecture") })
    }

    @Test
    fun testChunkingLargeDocumentSplitsWithOverlap() {
        val longBody = "Paragraph line. ".repeat(100)
        val chunks = DocumentChunker.chunkDocument(
            objectId = "note2",
            objectType = "note",
            title = "Long Note",
            body = longBody,
            config = ChunkingConfig(maxChunkSizeChars = 200, minChunkSizeChars = 30, chunkOverlapChars = 20),
        )

        assertTrue(chunks.size > 1)
        for (chunk in chunks) {
            assertTrue(chunk.content.length <= 250)
        }
    }
}
