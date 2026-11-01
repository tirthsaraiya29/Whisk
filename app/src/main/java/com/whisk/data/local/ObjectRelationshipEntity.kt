package com.whisk.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.whisk.domain.model.ObjectRelationship
import com.whisk.domain.model.RelationshipType

@Entity(
    tableName = "object_relationships",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceObjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetObjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sourceObjectId"]),
        Index(value = ["targetObjectId"]),
        Index(value = ["sourceObjectId", "targetObjectId", "relationshipType"], unique = true),
    ],
)
data class ObjectRelationshipEntity(
    @PrimaryKey val id: String,
    val sourceObjectId: String,
    val targetObjectId: String,
    val relationshipType: String,
    val createdAt: Long,
) {
    fun toDomain(): ObjectRelationship {
        return ObjectRelationship(
            id = id,
            sourceObjectId = sourceObjectId,
            targetObjectId = targetObjectId,
            relationshipType = RelationshipType.fromString(relationshipType),
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomain(domain: ObjectRelationship): ObjectRelationshipEntity {
            return ObjectRelationshipEntity(
                id = domain.id,
                sourceObjectId = domain.sourceObjectId,
                targetObjectId = domain.targetObjectId,
                relationshipType = domain.relationshipType.name,
                createdAt = domain.createdAt,
            )
        }
    }
}
