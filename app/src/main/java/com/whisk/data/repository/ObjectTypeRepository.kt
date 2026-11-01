package com.whisk.data.repository

import com.whisk.data.local.NoteDao
import com.whisk.data.local.ObjectTypeDao
import com.whisk.data.local.ObjectTypeDefinitionEntity
import com.whisk.data.local.PropertyDao
import com.whisk.data.local.PropertyDefinitionEntity
import com.whisk.domain.model.BuiltInObjectTypes
import com.whisk.domain.model.ObjectTypeDefinition
import com.whisk.domain.model.PropertyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

enum class CustomTypeDeleteAction {
    CONVERT_TO_NOTES,
    REASSIGN_TYPE,
    ARCHIVE_OBJECTS,
    DELETE_OBJECTS
}

class ObjectTypeRepository(
    private val objectTypeDao: ObjectTypeDao,
    private val noteDao: NoteDao,
    private val propertyDao: PropertyDao,
) {

    val allObjectTypeDefinitions: Flow<List<ObjectTypeDefinition>> =
        objectTypeDao.getAllObjectTypeDefinitions().map { entities ->
            entities.map { it.toDomain() }
        }

    val nonArchivedObjectTypeDefinitions: Flow<List<ObjectTypeDefinition>> =
        objectTypeDao.getNonArchivedObjectTypeDefinitions().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getObjectTypeDefinitionById(id: String): ObjectTypeDefinition? {
        return objectTypeDao.getObjectTypeDefinitionById(id)?.toDomain()
    }

    suspend fun saveObjectTypeDefinition(typeDef: ObjectTypeDefinition) {
        val entity = ObjectTypeDefinitionEntity.fromDomain(typeDef.copy(updatedAt = System.currentTimeMillis()))
        objectTypeDao.insertObjectTypeDefinition(entity)
    }

    suspend fun deleteObjectTypeDefinition(
        id: String,
        action: CustomTypeDeleteAction,
        reassignTypeId: String? = null,
    ) {
        val targetDef = objectTypeDao.getObjectTypeDefinitionById(id)
        if (targetDef == null || targetDef.isBuiltIn) return // Protect built-in types

        when (action) {
            CustomTypeDeleteAction.CONVERT_TO_NOTES -> {
                objectTypeDao.reassignObjectsType(id, BuiltInObjectTypes.NOTE)
            }
            CustomTypeDeleteAction.REASSIGN_TYPE -> {
                val newType = reassignTypeId ?: BuiltInObjectTypes.NOTE
                objectTypeDao.reassignObjectsType(id, newType)
            }
            CustomTypeDeleteAction.ARCHIVE_OBJECTS -> {
                val objects = noteDao.getAllNotesDirect().filter { it.typeId == id }
                objects.forEach { obj ->
                    noteDao.updateNote(obj.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                }
            }
            CustomTypeDeleteAction.DELETE_OBJECTS -> {
                val objects = noteDao.getAllNotesDirect().filter { it.typeId == id }
                objects.forEach { obj ->
                    noteDao.deleteNoteById(obj.id)
                }
            }
        }

        objectTypeDao.deleteObjectTypeDefinitionById(id)
    }

    suspend fun seedBuiltInObjectTypes() {
        val existing = objectTypeDao.getAllObjectTypeDefinitionsDirect()
        if (existing.isEmpty()) {
            val entities = BuiltInObjectTypes.DEFAULT_TYPES.map { ObjectTypeDefinitionEntity.fromDomain(it) }
            objectTypeDao.insertObjectTypeDefinitions(entities)
            seedDefaultPropertiesForBuiltInTypes()
        }
    }

    private suspend fun seedDefaultPropertiesForBuiltInTypes() {
        val defaultProperties = listOf(
            // Projects
            PropertyDefinitionEntity(
                id = "prop_proj_status",
                name = "Status",
                type = PropertyType.SELECT.name,
                description = "Project state",
                optionsJson = "[\"PLANNING\",\"IN_PROGRESS\",\"PAUSED\",\"COMPLETED\",\"CANCELLED\"]",
                position = 0,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            PropertyDefinitionEntity(
                id = "prop_proj_priority",
                name = "Priority",
                type = PropertyType.SELECT.name,
                description = "Urgency level",
                optionsJson = "[\"LOW\",\"MEDIUM\",\"HIGH\",\"URGENT\"]",
                position = 1,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            PropertyDefinitionEntity(
                id = "prop_proj_due_date",
                name = "Due Date",
                type = PropertyType.DATE.name,
                description = "Target completion date",
                optionsJson = null,
                position = 2,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            // People
            PropertyDefinitionEntity(
                id = "prop_person_email",
                name = "Email",
                type = PropertyType.TEXT.name,
                description = "Contact email address",
                optionsJson = null,
                position = 3,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            PropertyDefinitionEntity(
                id = "prop_person_org",
                name = "Organisation",
                type = PropertyType.TEXT.name,
                description = "Company or organisation",
                optionsJson = null,
                position = 4,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            PropertyDefinitionEntity(
                id = "prop_person_role",
                name = "Role",
                type = PropertyType.TEXT.name,
                description = "Job title or role",
                optionsJson = null,
                position = 5,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            // Books
            PropertyDefinitionEntity(
                id = "prop_book_author",
                name = "Author",
                type = PropertyType.TEXT.name,
                description = "Book author",
                optionsJson = null,
                position = 6,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            PropertyDefinitionEntity(
                id = "prop_book_rating",
                name = "Rating",
                type = PropertyType.NUMBER.name,
                description = "Rating out of 5",
                optionsJson = null,
                position = 7,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            // Repositories
            PropertyDefinitionEntity(
                id = "prop_repo_url",
                name = "GitHub URL",
                type = PropertyType.TEXT.name,
                description = "Repository Web URL",
                optionsJson = null,
                position = 8,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            PropertyDefinitionEntity(
                id = "prop_repo_lang",
                name = "Primary Language",
                type = PropertyType.TEXT.name,
                description = "Programming language",
                optionsJson = null,
                position = 9,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            // Meetings
            PropertyDefinitionEntity(
                id = "prop_meeting_date",
                name = "Meeting Date",
                type = PropertyType.DATE.name,
                description = "Date and time of meeting",
                optionsJson = null,
                position = 10,
                isArchived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )

        defaultProperties.forEach { prop ->
            if (propertyDao.getPropertyDefinitionById(prop.id) == null) {
                propertyDao.insertPropertyDefinition(prop)
            }
        }
    }
}
