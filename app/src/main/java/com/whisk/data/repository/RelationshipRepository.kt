package com.whisk.data.repository

import com.whisk.data.local.NoteDao
import com.whisk.data.local.ObjectRelationshipEntity
import com.whisk.data.local.ObjectTypeDao
import com.whisk.data.local.RelationshipDao
import com.whisk.data.rag.repository.IndexingRepository
import com.whisk.domain.model.ObjectRelationship
import com.whisk.domain.model.RelationshipDetail
import com.whisk.domain.model.RelationshipType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RelationshipRepository(
    private val relationshipDao: RelationshipDao,
    private val noteDao: NoteDao,
    private val objectTypeDao: ObjectTypeDao,
    private val indexingRepository: IndexingRepository? = null,
) {

    fun getRelationshipsForObject(objectId: String): Flow<List<RelationshipDetail>> {
        return relationshipDao.getRelationshipsForObject(objectId).map { entities ->
            val typeDefs = objectTypeDao.getAllObjectTypeDefinitionsDirect().associateBy { it.id }
            val notesMap = noteDao.getAllNotesDirect().associateBy { it.id }

            entities.mapNotNull { entity ->
                val isSource = (entity.sourceObjectId == objectId)
                val otherId = if (isSource) entity.targetObjectId else entity.sourceObjectId
                val otherNote = notesMap[otherId] ?: return@mapNotNull null

                val relType = RelationshipType.fromString(entity.relationshipType)
                val displayLabel = if (isSource) relType.displayName else relType.reciprocalName
                val typeDef = typeDefs[otherNote.typeId]

                RelationshipDetail(
                    relationshipId = entity.id,
                    isSource = isSource,
                    otherObjectId = otherId,
                    otherObjectTitle = otherNote.title,
                    otherObjectType = otherNote.typeId,
                    otherObjectIcon = typeDef?.icon ?: "description",
                    relationshipType = relType,
                    displayLabel = displayLabel,
                    createdAt = entity.createdAt,
                )
            }
        }
    }

    suspend fun addRelationship(
        sourceObjectId: String,
        targetObjectId: String,
        relationshipType: RelationshipType,
    ): Boolean {
        if (sourceObjectId == targetObjectId) return false // Prevent self-references

        val entity = ObjectRelationshipEntity(
            id = UUID.randomUUID().toString(),
            sourceObjectId = sourceObjectId,
            targetObjectId = targetObjectId,
            relationshipType = relationshipType.name,
            createdAt = System.currentTimeMillis(),
        )
        relationshipDao.insertRelationship(entity)
        indexingRepository?.deleteIndexingState(sourceObjectId)
        indexingRepository?.deleteIndexingState(targetObjectId)
        return true
    }

    suspend fun removeRelationship(id: String) {
        val rel = relationshipDao.getAllRelationshipsDirect().find { it.id == id }
        relationshipDao.deleteRelationshipById(id)
        if (rel != null) {
            indexingRepository?.deleteIndexingState(rel.sourceObjectId)
            indexingRepository?.deleteIndexingState(rel.targetObjectId)
        }
    }

    suspend fun removeRelationshipsBetween(objA: String, objB: String) {
        relationshipDao.deleteRelationshipsBetweenObjects(objA, objB)
        indexingRepository?.deleteIndexingState(objA)
        indexingRepository?.deleteIndexingState(objB)
    }

    fun getAllRelationships(): Flow<List<ObjectRelationship>> {
        return relationshipDao.getAllRelationships().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
