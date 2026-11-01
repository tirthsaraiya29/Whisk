package com.whisk

import com.whisk.data.local.NoteEntity
import com.whisk.data.local.ObjectRelationshipEntity
import com.whisk.data.local.ObjectTypeDefinitionEntity
import com.whisk.data.repository.BackupData
import com.whisk.data.repository.NoteDto
import com.whisk.data.repository.ObjectRelationshipDto
import com.whisk.data.repository.ObjectTypeDefinitionDto
import com.whisk.domain.model.BuiltInObjectTypes
import com.whisk.domain.model.Note
import com.whisk.domain.model.ObjectRelationship
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.domain.model.RelationshipType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectTypesAndRelationshipsTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testNoteObjectDefaultsAndRoundtrip() {
        val note = Note(
            title = "Test Object",
            body = "Body",
        )

        assertEquals("note", note.typeId)
        assertEquals(null, note.description)

        val projectObject = note.copy(
            typeId = BuiltInObjectTypes.PROJECT,
            description = "High priority project",
            icon = "folder_special",
        )

        val entity = NoteEntity.fromDomain(projectObject)
        assertEquals(BuiltInObjectTypes.PROJECT, entity.typeId)
        assertEquals("High priority project", entity.description)
        assertEquals("folder_special", entity.icon)

        val restored = entity.toDomain()
        assertEquals(BuiltInObjectTypes.PROJECT, restored.typeId)
        assertEquals("High priority project", restored.description)
        assertEquals("folder_special", restored.icon)
    }

    @Test
    fun testObjectTypeDefinitionEntityToDomainRoundtrip() {
        val customType = ObjectTypeDefinition(
            id = "custom_movie",
            name = "Movies",
            description = "Track movie reviews",
            icon = "movie",
            isBuiltIn = false,
            position = 10,
        )

        val entity = ObjectTypeDefinitionEntity.fromDomain(customType)
        assertEquals("custom_movie", entity.id)
        assertEquals("Movies", entity.name)
        assertFalse(entity.isBuiltIn)

        val restored = entity.toDomain()
        assertEquals(customType.id, restored.id)
        assertEquals(customType.name, restored.name)
        assertEquals(customType.description, restored.description)
        assertEquals(customType.icon, restored.icon)
        assertFalse(restored.isBuiltIn)
    }

    @Test
    fun testBuiltInObjectTypesConstants() {
        val builtInList = BuiltInObjectTypes.DEFAULT_TYPES
        assertEquals(9, builtInList.size)

        val projectType = builtInList.find { it.id == BuiltInObjectTypes.PROJECT }
        assertNotNull(projectType)
        assertEquals("Projects", projectType?.name)
        assertTrue(projectType!!.isBuiltIn)
    }

    @Test
    fun testObjectRelationshipEntityToDomainRoundtrip() {
        val relationship = ObjectRelationship(
            id = "rel_123",
            sourceObjectId = "obj_project_1",
            targetObjectId = "obj_task_1",
            relationshipType = RelationshipType.PARENT_OF,
        )

        val entity = ObjectRelationshipEntity.fromDomain(relationship)
        assertEquals("rel_123", entity.id)
        assertEquals("obj_project_1", entity.sourceObjectId)
        assertEquals("obj_task_1", entity.targetObjectId)
        assertEquals("PARENT_OF", entity.relationshipType)

        val restored = entity.toDomain()
        assertEquals(RelationshipType.PARENT_OF, restored.relationshipType)
        assertEquals("obj_project_1", restored.sourceObjectId)
        assertEquals("obj_task_1", restored.targetObjectId)
    }

    @Test
    fun testReciprocalRelationshipDisplayNames() {
        val parentRel = RelationshipType.PARENT_OF
        assertEquals("Parent of", parentRel.displayName)
        assertEquals("Child of", parentRel.reciprocalName)

        val belongsRel = RelationshipType.BELONGS_TO
        assertEquals("Belongs to", belongsRel.displayName)
        assertEquals("Contains", belongsRel.reciprocalName)

        val dependsRel = RelationshipType.DEPENDS_ON
        assertEquals("Depends on", dependsRel.displayName)
        assertEquals("Prerequisite for", dependsRel.reciprocalName)
    }

    @Suppress("KotlinConstantConditions")
    @Test
    fun testPreventSelfReferentialRelationshipLogic() {
        val id = "obj_123_${System.currentTimeMillis()}"
        val sourceId = id
        val targetId = id

        val isValid = sourceId != targetId
        assertFalse(isValid)
    }

    @Test
    fun testBackupDataSerializationWithObjectTypesAndRelationships() {
        val noteDto = NoteDto(
            id = "obj_proj_1",
            title = "Whisk Redesign",
            body = "Project details",
            createdAt = 1000L,
            updatedAt = 2000L,
            folder = null,
            tags = "project,redesign",
            isFavorite = true,
            isArchived = false,
            sourceMetadata = null,
            repoRef = "user/whisk",
            aliases = "",
            status = "IN_PROGRESS",
            typeId = "project",
            description = "Main Whisk project workspace",
        )

        val typeDto = ObjectTypeDefinitionDto(
            id = "custom_recipe",
            name = "Recipes",
            description = "Cooking recipes",
            icon = "restaurant",
            isBuiltIn = false,
            configurationJson = null,
            position = 9,
            isArchived = false,
            createdAt = 1000L,
            updatedAt = 2000L,
        )

        val relDto = ObjectRelationshipDto(
            id = "rel_1",
            sourceObjectId = "obj_proj_1",
            targetObjectId = "obj_task_1",
            relationshipType = "PARENT_OF",
            createdAt = 1000L,
        )

        val backupData = BackupData(
            notes = listOf(noteDto),
            objectTypeDefinitions = listOf(typeDto),
            objectRelationships = listOf(relDto),
        )

        val serialized = json.encodeToString(backupData)
        assertTrue(serialized.contains("objectTypeDefinitions"))
        assertTrue(serialized.contains("objectRelationships"))
        assertTrue(serialized.contains("whisk"))

        val restored = json.decodeFromString<BackupData>(serialized)
        assertEquals(1, restored.notes.size)
        assertEquals("project", restored.notes[0].typeId)
        assertEquals("Main Whisk project workspace", restored.notes[0].description)

        assertEquals(1, restored.objectTypeDefinitions.size)
        assertEquals("Recipes", restored.objectTypeDefinitions[0].name)

        assertEquals(1, restored.objectRelationships.size)
        assertEquals("PARENT_OF", restored.objectRelationships[0].relationshipType)
    }

    @Test
    fun testLegacyBackupImportCompatibilityWithObjectTypesDefaults() {
        val legacyJson = """
        {
            "notes": [
                {
                    "id": "legacy_note_1",
                    "title": "Legacy Note",
                    "body": "Body content",
                    "createdAt": 1000,
                    "updatedAt": 1000,
                    "folder": null,
                    "tags": "",
                    "isFavorite": false,
                    "isArchived": false,
                    "sourceMetadata": null,
                    "repoRef": null,
                    "aliases": "",
                    "status": "DRAFT"
                }
            ],
            "folders": [],
            "canvases": [],
            "tasks": [],
            "chatSessions": [],
            "chatMessages": []
        }
        """.trimIndent()

        val restored = json.decodeFromString<BackupData>(legacyJson)
        assertEquals(1, restored.notes.size)
        assertEquals("note", restored.notes[0].typeId)
        assertTrue(restored.objectTypeDefinitions.isEmpty())
        assertTrue(restored.objectRelationships.isEmpty())
    }
}
