package com.whisk

import com.whisk.data.local.NotePropertyValueEntity
import com.whisk.data.local.PropertyDefinitionEntity
import com.whisk.data.repository.BackupData
import com.whisk.data.repository.NotePropertyValueDto
import com.whisk.data.repository.PropertyDefinitionDto
import com.whisk.domain.model.NotePropertyValue
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyFilter
import com.whisk.domain.model.PropertyOperator
import com.whisk.domain.model.PropertyType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PropertySystemTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testPropertyDefinitionEntityToDomainRoundtrip() {
        val domain = PropertyDefinition(
            id = "prop_1",
            name = "Priority",
            type = PropertyType.SELECT,
            description = "Note priority level",
            options = listOf("Low", "Medium", "High", "Urgent"),
            position = 1,
            isArchived = false,
        )

        val entity = PropertyDefinitionEntity.fromDomain(domain)
        assertEquals("prop_1", entity.id)
        assertEquals("Priority", entity.name)
        assertEquals("SELECT", entity.type)
        assertTrue(entity.optionsJson!!.contains("High"))

        val restored = entity.toDomain()
        assertEquals(domain.id, restored.id)
        assertEquals(domain.name, restored.name)
        assertEquals(domain.type, restored.type)
        assertEquals(4, restored.options.size)
        assertEquals("Urgent", restored.options[3])
    }

    @Test
    fun testNotePropertyValueEntityToDomainRoundtripForTypedValues() {
        val numberVal = NotePropertyValue(
            id = "val_1",
            noteId = "note_123",
            propertyDefinitionId = "prop_num",
            numberValue = 42.5,
        )

        val entityNum = NotePropertyValueEntity.fromDomain(numberVal)
        assertEquals(42.5, entityNum.numberValue!!, 0.001)

        val restoredNum = entityNum.toDomain()
        assertEquals(42.5, restoredNum.numberValue!!, 0.001)

        val multiSelectVal = NotePropertyValue(
            id = "val_2",
            noteId = "note_123",
            propertyDefinitionId = "prop_multi",
            listValue = listOf("Kotlin", "Android", "Compose"),
        )

        val entityMulti = NotePropertyValueEntity.fromDomain(multiSelectVal)
        assertNotNull(entityMulti.listValueJson)
        assertTrue(entityMulti.listValueJson!!.contains("Compose"))

        val restoredMulti = entityMulti.toDomain()
        assertEquals(3, restoredMulti.listValue.size)
        assertEquals("Compose", restoredMulti.listValue[2])
    }

    @Test
    fun testRelationPropertyValueReference() {
        val relationVal = NotePropertyValue(
            id = "val_rel_1",
            noteId = "source_note_1",
            propertyDefinitionId = "prop_rel",
            textValue = "target_note_456",
        )

        val entity = NotePropertyValueEntity.fromDomain(relationVal)
        assertEquals("target_note_456", entity.textValue)

        val restored = entity.toDomain()
        assertEquals("target_note_456", restored.textValue)
    }

    @Test
    fun testPropertyFilterDataModel() {
        val filter = PropertyFilter(
            propertyDefinitionId = "prop_status",
            operator = PropertyOperator.EQUALS,
            value = "In Progress",
        )

        assertEquals("prop_status", filter.propertyDefinitionId)
        assertEquals(PropertyOperator.EQUALS, filter.operator)
        assertEquals("In Progress", filter.value)
    }

    @Test
    fun testBackupDataSerializationWithProperties() {
        val propDto = PropertyDefinitionDto(
            id = "prop_1",
            name = "Status",
            type = "SELECT",
            description = "Status field",
            optionsJson = json.encodeToString(listOf("Draft", "Done")),
            position = 0,
            isArchived = false,
            createdAt = 1000L,
            updatedAt = 2000L,
        )

        val valDto = NotePropertyValueDto(
            id = "val_1",
            noteId = "note_1",
            propertyDefinitionId = "prop_1",
            textValue = "Draft",
            numberValue = null,
            booleanValue = null,
            listValueJson = null,
            createdAt = 1000L,
            updatedAt = 2000L,
        )

        val backupData = BackupData(
            propertyDefinitions = listOf(propDto),
            notePropertyValues = listOf(valDto),
        )

        val serialized = json.encodeToString(backupData)
        assertTrue(serialized.contains("propertyDefinitions"))
        assertTrue(serialized.contains("notePropertyValues"))

        val restored = json.decodeFromString<BackupData>(serialized)
        assertEquals(1, restored.propertyDefinitions.size)
        assertEquals("Status", restored.propertyDefinitions[0].name)
        assertEquals(1, restored.notePropertyValues.size)
        assertEquals("Draft", restored.notePropertyValues[0].textValue)
    }

    @Test
    fun testOlderBackupFormatCompatibilityWithoutPropertyFields() {
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
        assertEquals("Legacy Note", restored.notes[0].title)
        assertTrue(restored.propertyDefinitions.isEmpty())
        assertTrue(restored.notePropertyValues.isEmpty())
    }

    @Test
    fun testValidNumericValidation() {
        val validInt = "123".toDoubleOrNull()
        val validDecimal = "45.67".toDoubleOrNull()
        val invalidText = "abc12".toDoubleOrNull()

        assertNotNull(validInt)
        assertEquals(123.0, validInt!!, 0.001)

        assertNotNull(validDecimal)
        assertEquals(45.67, validDecimal!!, 0.001)

        assertNull(invalidText)
    }

    @Test
    fun testMultiSelectUniquenessAndFiltering() {
        val originalList = listOf("TagA", "TagB", "TagA", "TagC", "  ", "")
        val sanitized = originalList.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        assertEquals(3, sanitized.size)
        assertEquals(listOf("TagA", "TagB", "TagC"), sanitized)
    }
}
