package com.whisk

import com.whisk.data.local.DatabaseViewEntity
import com.whisk.data.repository.BackupData
import com.whisk.data.repository.DatabaseViewDto
import com.whisk.data.repository.ViewEngine
import com.whisk.domain.model.DatabaseView
import com.whisk.domain.model.FilterRule
import com.whisk.domain.model.Note
import com.whisk.domain.model.NotePropertyValue
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyOperator
import com.whisk.domain.model.PropertyType
import com.whisk.domain.model.SortDirection
import com.whisk.domain.model.SortRule
import com.whisk.domain.model.ViewType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseViewsTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testDatabaseViewEntityToDomainRoundtrip() {
        val domain = DatabaseView(
            id = "view_123",
            targetTypeId = "project",
            name = "Active Projects Board",
            viewType = ViewType.BOARD,
            visiblePropertyIds = listOf("prop_status", "prop_priority"),
            filters = listOf(
                FilterRule("status", PropertyOperator.NOT_EQUALS, "Completed"),
            ),
            sorts = listOf(
                SortRule("updatedAt", SortDirection.DESCENDING),
            ),
            groupPropertyId = "status",
            position = 1,
        )

        val entity = DatabaseViewEntity.fromDomain(domain)
        assertEquals("view_123", entity.id)
        assertEquals("BOARD", entity.viewType)
        assertTrue(entity.filterConfigJson!!.contains("NOT_EQUALS"))

        val restored = entity.toDomain()
        assertEquals(domain.id, restored.id)
        assertEquals(domain.name, restored.name)
        assertEquals(ViewType.BOARD, restored.viewType)
        assertEquals(1, restored.filters.size)
        assertEquals("status", restored.filters[0].propertyId)
    }

    @Test
    fun testViewEngineFilteringAndSorting() {
        val notes = listOf(
            Note(id = "n1", title = "Alpha Project", body = "", status = "In Progress", typeId = "project", updatedAt = 2000L),
            Note(id = "n2", title = "Beta Task", body = "", status = "Draft", typeId = "task", updatedAt = 1000L),
            Note(id = "n3", title = "Gamma Project", body = "", status = "Done", typeId = "project", updatedAt = 3000L),
        )

        val view = DatabaseView(
            id = "v1",
            name = "Active Projects",
            viewType = ViewType.TABLE,
            targetTypeId = "project",
            filters = listOf(
                FilterRule("status", PropertyOperator.NOT_EQUALS, "Done"),
            ),
        )

        val filtered = ViewEngine.applyViewQuery(
            notes = notes,
            propertyValues = emptyMap(),
            propertyDefinitions = emptyMap(),
            view = view,
        )

        assertEquals(1, filtered.size)
        assertEquals("Alpha Project", filtered[0].title)
    }

    @Test
    fun testViewEngineCustomPropertyValueFilter() {
        val propDef = PropertyDefinition(
            id = "prop_priority",
            name = "Priority",
            type = PropertyType.NUMBER,
            position = 0,
        )

        val notes = listOf(
            Note(id = "n1", title = "High Priority Note", body = ""),
            Note(id = "n2", title = "Low Priority Note", body = ""),
        )

        val values = mapOf(
            "n1" to listOf(NotePropertyValue(id = "v1", noteId = "n1", propertyDefinitionId = "prop_priority", numberValue = 10.0)),
            "n2" to listOf(NotePropertyValue(id = "v2", noteId = "n2", propertyDefinitionId = "prop_priority", numberValue = 2.0)),
        )

        val view = DatabaseView(
            id = "v_prio",
            name = "High Priority",
            viewType = ViewType.LIST,
            filters = listOf(
                FilterRule("prop_priority", PropertyOperator.GREATER_THAN, "5"),
            ),
        )

        val filtered = ViewEngine.applyViewQuery(
            notes = notes,
            propertyValues = values,
            propertyDefinitions = mapOf("prop_priority" to propDef),
            view = view,
        )

        assertEquals(1, filtered.size)
        assertEquals("High Priority Note", filtered[0].title)
    }

    @Test
    fun testBackupDataSerializationWithDatabaseViews() {
        val viewDto = DatabaseViewDto(
            id = "view_1",
            targetTypeId = "project",
            name = "Kanban",
            viewType = "BOARD",
            visiblePropertiesJson = null,
            filterConfigJson = null,
            sortConfigJson = null,
            groupPropertyId = "status",
            datePropertyId = null,
            cardSize = "MEDIUM",
            position = 0,
            createdAt = 1000L,
            updatedAt = 2000L,
        )

        val backup = BackupData(databaseViews = listOf(viewDto))
        val serialized = json.encodeToString(backup)
        assertTrue(serialized.contains("databaseViews"))

        val restored = json.decodeFromString<BackupData>(serialized)
        assertEquals(1, restored.databaseViews.size)
        assertEquals("Kanban", restored.databaseViews[0].name)
    }
}
