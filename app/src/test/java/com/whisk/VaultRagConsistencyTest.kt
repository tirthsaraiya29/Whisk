package com.whisk

import com.whisk.data.local.ObjectRelationshipEntity
import com.whisk.data.rag.indexer.DocumentIndexer
import com.whisk.domain.model.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VaultRagConsistencyTest {

    @Test
    fun testFingerprintIdenticalInputsProduceIdenticalHash() {
        val rels = listOf(
            ObjectRelationshipEntity(
                id = "r1",
                sourceObjectId = "n1",
                targetObjectId = "n2",
                relationshipType = RelationshipType.DEPENDS_ON.name,
                createdAt = 1000L,
            ),
        )

        val hash1 = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body",
            tags = "tag1",
            typeId = "note",
            relationships = rels,
            modelId = "model1",
        )

        val hash2 = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body",
            tags = "tag1",
            typeId = "note",
            relationships = rels,
            modelId = "model1",
        )

        assertEquals(hash1, hash2)
    }

    @Test
    fun testFingerprintContentChangeProducesDifferentHash() {
        val rels = emptyList<ObjectRelationshipEntity>()

        val baseHash = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body V1",
            tags = "tag1",
            typeId = "note",
            relationships = rels,
            modelId = "model1",
        )

        val editedBodyHash = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body V2",
            tags = "tag1",
            typeId = "note",
            relationships = rels,
            modelId = "model1",
        )

        assertNotEquals(baseHash, editedBodyHash)
    }

    @Test
    fun testFingerprintRelationshipChangeProducesDifferentHash() {
        val rels1 = emptyList<ObjectRelationshipEntity>()
        val rels2 = listOf(
            ObjectRelationshipEntity(
                id = "r1",
                sourceObjectId = "n1",
                targetObjectId = "n2",
                relationshipType = RelationshipType.DEPENDS_ON.name,
                createdAt = 1000L,
            ),
        )

        val hash1 = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body",
            tags = "tag1",
            typeId = "note",
            relationships = rels1,
            modelId = "model1",
        )

        val hash2 = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body",
            tags = "tag1",
            typeId = "note",
            relationships = rels2,
            modelId = "model1",
        )

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testFingerprintObjectTypeChangeProducesDifferentHash() {
        val rels = emptyList<ObjectRelationshipEntity>()

        val hash1 = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body",
            tags = "tag1",
            typeId = "note",
            relationships = rels,
            modelId = "model1",
        )

        val hash2 = DocumentIndexer.computeFingerprint(
            noteId = "n1",
            title = "Title",
            description = "Desc",
            body = "Body",
            tags = "tag1",
            typeId = "project",
            relationships = rels,
            modelId = "model1",
        )

        assertNotEquals(hash1, hash2)
    }
}
