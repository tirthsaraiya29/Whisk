package com.whisk

import com.whisk.ai.ContextPayload
import com.whisk.data.local.NoteEntity
import com.whisk.data.rag.embedding.LocalEmbeddingProvider
import com.whisk.domain.rag.chunking.DocumentChunker
import com.whisk.domain.rag.retrieval.RetrievedChunk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultRagIntegrationTest {

    @Test
    fun testEndToEndChunkingEmbeddingRetrievalAndContextAssembly() = runBlocking {
        // 1. Create mock notes
        val note1 = NoteEntity(
            id = "note_net_01",
            title = "Tether Networking Architecture",
            body = """
# Tether Networking

Tether uses TCP with TLS 1.3 encryption for local node discovery over mDNS.

## Port Configuration
Default control port is 9443.
            """.trimIndent(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            folder = null,
            tags = "networking,security",
            isFavorite = false,
            isArchived = false,
            sourceMetadata = null,
            repoRef = "tether-net",
        )

        // 2. Chunk note
        val chunks = DocumentChunker.chunkDocument(
            objectId = note1.id,
            objectType = note1.typeId,
            title = note1.title,
            body = note1.body,
        )

        assertTrue(chunks.size >= 2)

        // 3. Generate embeddings
        val provider = LocalEmbeddingProvider(dimensions = 384)
        val vectors = provider.generateEmbeddings(chunks.map { it.content })

        assertEquals(chunks.size, vectors.size)

        // 4. Query vector search
        val queryVector = provider.generateEmbedding("What port and TLS encryption does Tether use?")
        val topChunkScore = LocalEmbeddingProvider.cosineSimilarity(queryVector, vectors[0])

        assertTrue(topChunkScore > 0.1f)

        // 5. Construct RetrievedChunk
        val retrievedChunk = RetrievedChunk(
            chunkId = chunks[0].id,
            sourceObjectId = chunks[0].sourceObjectId,
            sourceObjectType = chunks[0].sourceObjectType,
            sourceTitle = chunks[0].sourceTitle,
            headingPath = chunks[0].headingPath,
            content = chunks[0].content,
            rrfScore = 0.03f,
            vectorScore = topChunkScore,
        )

        // 6. Context Assembly with token budgeting and citations
        val payload = ContextPayload(
            retrievedChunks = listOf(retrievedChunk),
            maxTokenBudget = 1000,
        )

        val (formattedContext, citations) = payload.formatContextWithCitations()

        assertTrue(formattedContext.contains("Tether Networking Architecture"))
        assertTrue(formattedContext.contains("TLS 1.3"))
        assertEquals(1, citations.size)
        assertEquals("Tether Networking Architecture", citations.first().sourceTitle)
    }
}
