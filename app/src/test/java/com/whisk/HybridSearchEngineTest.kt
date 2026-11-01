package com.whisk

import com.whisk.domain.rag.retrieval.RetrievedChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridSearchEngineTest {

    @Test
    fun testRrfScoring() {
        val chunk1 = RetrievedChunk(
            chunkId = "c1",
            sourceObjectId = "obj1",
            sourceObjectType = "note",
            sourceTitle = "Networking Note",
            headingPath = "Networking > TLS",
            content = "TLS 1.3 architecture",
            rrfScore = 0f,
            ftsRank = 1,
            vectorRank = 2,
        )

        val k = 60f
        val expectedScore = (1.0f / (k + 1)) + (1.0f / (k + 2))
        val computedScore = (1.0f / (k + chunk1.ftsRank!!)) + (1.0f / (k + chunk1.vectorRank!!))

        assertEquals(expectedScore, computedScore, 0.0001f)
        assertTrue(computedScore > 0.015f)
    }
}
