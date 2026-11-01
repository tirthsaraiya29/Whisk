package com.whisk.data.repository

import com.whisk.data.local.NoteDao
import com.whisk.data.local.NotePropertyValueEntity
import com.whisk.data.local.PropertyDao
import com.whisk.data.local.PropertyDefinitionEntity
import com.whisk.domain.model.NotePropertyItem
import com.whisk.domain.model.NotePropertyValue
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyFilter
import com.whisk.domain.model.PropertyOperator
import com.whisk.domain.model.PropertyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PropertyRepository(
    private val propertyDao: PropertyDao,
    private val noteDao: NoteDao,
) {

    val allPropertyDefinitions: Flow<List<PropertyDefinition>> =
        propertyDao.getAllPropertyDefinitions().map { entities ->
            entities.map { it.toDomain() }
        }

    val nonArchivedPropertyDefinitions: Flow<List<PropertyDefinition>> =
        propertyDao.getNonArchivedPropertyDefinitions().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getPropertyDefinitionById(id: String): PropertyDefinition? {
        return propertyDao.getPropertyDefinitionById(id)?.toDomain()
    }

    suspend fun savePropertyDefinition(definition: PropertyDefinition) {
        val existing = propertyDao.getPropertyDefinitionById(definition.id)
        val entity = PropertyDefinitionEntity.fromDomain(
            definition.copy(updatedAt = System.currentTimeMillis())
        )
        propertyDao.insertPropertyDefinition(entity)

        // If type changed or options removed, clean up non-matching values
        if (existing != null) {
            val oldType = PropertyType.valueOf(existing.type)
            if (oldType != definition.type || (definition.type == PropertyType.SELECT || definition.type == PropertyType.MULTI_SELECT)) {
                sanitizePropertyValuesForDefinition(definition)
            }
        }
    }

    suspend fun deletePropertyDefinition(id: String) {
        propertyDao.deletePropertyValuesForProperty(id)
        propertyDao.deletePropertyDefinitionById(id)
    }

    suspend fun seedDefaultPropertiesIfEmpty() {
        val existingDirect = propertyDao.getAllPropertyDefinitionsDirect()
        if (existingDirect.isEmpty()) {
            val defaults = listOf(
                PropertyDefinition(
                    id = "default_prop_status",
                    name = "Status",
                    type = PropertyType.SELECT,
                    description = "Current lifecycle status of the note",
                    options = listOf("Draft", "In Progress", "In Review", "Done"),
                    position = 0,
                ),
                PropertyDefinition(
                    id = "default_prop_priority",
                    name = "Priority",
                    type = PropertyType.SELECT,
                    description = "Importance level",
                    options = listOf("Low", "Medium", "High", "Urgent"),
                    position = 1,
                ),
                PropertyDefinition(
                    id = "default_prop_due_date",
                    name = "Due Date",
                    type = PropertyType.DATE,
                    description = "Target completion date",
                    position = 2,
                ),
                PropertyDefinition(
                    id = "default_prop_author",
                    name = "Author",
                    type = PropertyType.TEXT,
                    description = "Primary owner or author",
                    position = 3,
                ),
            )
            defaults.forEach { savePropertyDefinition(it) }
        }
    }

    fun getPropertyItemsForNote(noteId: String): Flow<List<NotePropertyItem>> {
        return combine(
            nonArchivedPropertyDefinitions,
            propertyDao.getPropertyValuesForNote(noteId),
        ) { definitions, valueEntities ->
            val valueMap = valueEntities.associateBy { it.propertyDefinitionId }
            definitions.map { def ->
                val valEntity = valueMap[def.id]
                val domainValue = valEntity?.toDomain()

                var relatedTitle: String? = null
                if (def.type == PropertyType.RELATION && domainValue?.textValue != null) {
                    val targetNote = noteDao.getNoteById(domainValue.textValue)
                    relatedTitle = targetNote?.title ?: "Note not found"
                }

                NotePropertyItem(
                    definition = def,
                    value = domainValue,
                    relatedNoteTitle = relatedTitle,
                )
            }
        }
    }

    suspend fun saveNotePropertyValue(value: NotePropertyValue) {
        val definition = propertyDao.getPropertyDefinitionById(value.propertyDefinitionId)?.toDomain()
            ?: return

        val sanitizedValue = when (definition.type) {
            PropertyType.TEXT -> value.copy(
                numberValue = null,
                booleanValue = null,
                listValue = emptyList(),
            )
            PropertyType.NUMBER -> value.copy(
                textValue = value.numberValue?.toString(),
                booleanValue = null,
                listValue = emptyList(),
            )
            PropertyType.DATE -> value.copy(
                numberValue = null,
                booleanValue = null,
                listValue = emptyList(),
            )
            PropertyType.CHECKBOX -> value.copy(
                textValue = value.booleanValue?.toString(),
                numberValue = null,
                listValue = emptyList(),
            )
            PropertyType.SELECT -> {
                val selected = value.textValue
                val isValid = selected != null && definition.options.contains(selected)
                value.copy(
                    textValue = if (isValid) selected else null,
                    numberValue = null,
                    booleanValue = null,
                    listValue = emptyList(),
                )
            }
            PropertyType.MULTI_SELECT -> {
                val validList = value.listValue.distinct().filter { definition.options.contains(it) }
                value.copy(
                    textValue = null,
                    numberValue = null,
                    booleanValue = null,
                    listValue = validList,
                )
            }
            PropertyType.RELATION -> value.copy(
                numberValue = null,
                booleanValue = null,
                listValue = emptyList(),
            )
        }

        val entity = NotePropertyValueEntity.fromDomain(sanitizedValue.copy(updatedAt = System.currentTimeMillis()))
        propertyDao.insertNotePropertyValue(entity)
    }

    suspend fun deleteNotePropertyValue(noteId: String, propertyDefinitionId: String) {
        propertyDao.deleteNotePropertyValue(noteId, propertyDefinitionId)
    }

    private suspend fun sanitizePropertyValuesForDefinition(definition: PropertyDefinition) {
        val values = propertyDao.getAllNotePropertyValuesDirect()
            .filter { it.propertyDefinitionId == definition.id }

        for (vEntity in values) {
            val domain = vEntity.toDomain()
            when (definition.type) {
                PropertyType.SELECT -> {
                    if (domain.textValue != null && !definition.options.contains(domain.textValue)) {
                        propertyDao.deleteNotePropertyValue(domain.noteId, domain.propertyDefinitionId)
                    }
                }
                PropertyType.MULTI_SELECT -> {
                    val filtered = domain.listValue.filter { definition.options.contains(it) }
                    if (filtered.isEmpty()) {
                        propertyDao.deleteNotePropertyValue(domain.noteId, domain.propertyDefinitionId)
                    } else {
                        saveNotePropertyValue(domain.copy(listValue = filtered))
                    }
                }
                else -> {
                    // Re-save to enforce column cleanup
                    saveNotePropertyValue(domain)
                }
            }
        }
    }

    suspend fun filterNoteIdsByProperties(filters: List<PropertyFilter>): List<String> {
        if (filters.isEmpty()) return emptyList()

        var matchingIds: Set<String>? = null

        for (filter in filters) {
            val propId = filter.propertyDefinitionId
            val op = filter.operator
            val rawVal = filter.value ?: ""

            val idsForFilter: List<String> = when (op) {
                PropertyOperator.EQUALS -> propertyDao.getNoteIdsMatchingTextEquals(propId, rawVal)
                    .ifEmpty {
                        val numVal = rawVal.toDoubleOrNull()
                        if (numVal != null) {
                            propertyDao.getNoteIdsMatchingNumberEquals(propId, numVal)
                        } else if (rawVal.equals("true", ignoreCase = true) || rawVal.equals("false", ignoreCase = true)) {
                            propertyDao.getNoteIdsMatchingBooleanEquals(propId, rawVal.toBoolean())
                        } else {
                            emptyList()
                        }
                    }
                PropertyOperator.NOT_EQUALS -> propertyDao.getNoteIdsMatchingTextNotEquals(propId, rawVal)
                    .ifEmpty {
                        val numVal = rawVal.toDoubleOrNull()
                        if (numVal != null) {
                            propertyDao.getNoteIdsMatchingNumberNotEquals(propId, numVal)
                        } else {
                            emptyList()
                        }
                    }
                PropertyOperator.CONTAINS -> propertyDao.getNoteIdsMatchingTextContains(propId, rawVal)
                    .ifEmpty { propertyDao.getNoteIdsMatchingMultiSelectContains(propId, rawVal) }
                PropertyOperator.GREATER_THAN -> {
                    val numVal = rawVal.toDoubleOrNull()
                    if (numVal != null) propertyDao.getNoteIdsMatchingNumberGreaterThan(propId, numVal) else emptyList()
                }
                PropertyOperator.LESS_THAN -> {
                    val numVal = rawVal.toDoubleOrNull()
                    if (numVal != null) propertyDao.getNoteIdsMatchingNumberLessThan(propId, numVal) else emptyList()
                }
                PropertyOperator.BEFORE -> propertyDao.getNoteIdsMatchingDateBefore(propId, rawVal)
                PropertyOperator.AFTER -> propertyDao.getNoteIdsMatchingDateAfter(propId, rawVal)
                PropertyOperator.IS_EMPTY -> propertyDao.getNoteIdsMatchingIsEmpty(propId)
                PropertyOperator.IS_NOT_EMPTY -> propertyDao.getNoteIdsMatchingIsNotEmpty(propId)
            }

            matchingIds = if (matchingIds == null) {
                idsForFilter.toSet()
            } else {
                matchingIds.intersect(idsForFilter.toSet())
            }

            if (matchingIds.isEmpty()) break
        }

        return matchingIds?.toList() ?: emptyList()
    }
}
