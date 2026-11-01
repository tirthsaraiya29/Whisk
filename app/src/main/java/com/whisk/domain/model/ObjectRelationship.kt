package com.whisk.domain.model

import java.util.UUID

enum class RelationshipType(val displayName: String, val reciprocalName: String) {
    RELATED_TO("Related to", "Related to"),
    PART_OF("Part of", "Has part"),
    PARENT_OF("Parent of", "Child of"),
    CHILD_OF("Child of", "Parent of"),
    CREATED_BY("Created by", "Created"),
    AUTHORED_BY("Authored by", "Authored"),
    ASSIGNED_TO("Assigned to", "Assignee for"),
    BELONGS_TO("Belongs to", "Contains"),
    REFERENCES("References", "Referenced by"),
    DEPENDS_ON("Depends on", "Prerequisite for"),
    BLOCKS("Blocks", "Blocked by"),
    DISCUSSED_IN("Discussed in", "Discusses");

    companion object {
        fun fromString(typeStr: String): RelationshipType {
            return try {
                valueOf(typeStr)
            } catch (_: Exception) {
                RELATED_TO
            }
        }
    }
}

data class ObjectRelationship(
    val id: String = UUID.randomUUID().toString(),
    val sourceObjectId: String,
    val targetObjectId: String,
    val relationshipType: RelationshipType,
    val createdAt: Long = System.currentTimeMillis(),
)

data class RelationshipDetail(
    val relationshipId: String,
    val isSource: Boolean,
    val otherObjectId: String,
    val otherObjectTitle: String,
    val otherObjectType: String,
    val otherObjectIcon: String?,
    val relationshipType: RelationshipType,
    val displayLabel: String,
    val createdAt: Long,
)
