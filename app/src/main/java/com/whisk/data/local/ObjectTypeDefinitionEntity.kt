package com.whisk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whisk.domain.model.ObjectTypeDefinition

@Entity(tableName = "object_type_definitions")
data class ObjectTypeDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val isBuiltIn: Boolean,
    val configurationJson: String?,
    val position: Int,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain(): ObjectTypeDefinition {
        return ObjectTypeDefinition(
            id = id,
            name = name,
            description = description,
            icon = icon ?: "description",
            isBuiltIn = isBuiltIn,
            configurationJson = configurationJson,
            position = position,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun fromDomain(domain: ObjectTypeDefinition): ObjectTypeDefinitionEntity {
            return ObjectTypeDefinitionEntity(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                icon = domain.icon,
                isBuiltIn = domain.isBuiltIn,
                configurationJson = domain.configurationJson,
                position = domain.position,
                isArchived = domain.isArchived,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
        }
    }
}
