package com.whisk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whisk.domain.model.PropertyDefinition
import com.whisk.domain.model.PropertyType
import kotlinx.serialization.json.Json

@Entity(tableName = "property_definitions")
data class PropertyDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val description: String?,
    val optionsJson: String?,
    val position: Int,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain(): PropertyDefinition {
        val parsedOptions = if (!optionsJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<String>>(optionsJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val propType = try {
            PropertyType.valueOf(type)
        } catch (_: Exception) {
            PropertyType.TEXT
        }

        return PropertyDefinition(
            id = id,
            name = name,
            type = propType,
            description = description,
            options = parsedOptions,
            position = position,
            isArchived = isArchived,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun fromDomain(domain: PropertyDefinition): PropertyDefinitionEntity {
            val jsonStr = if (domain.options.isNotEmpty()) {
                Json.encodeToString(domain.options)
            } else {
                null
            }

            return PropertyDefinitionEntity(
                id = domain.id,
                name = domain.name,
                type = domain.type.name,
                description = domain.description,
                optionsJson = jsonStr,
                position = domain.position,
                isArchived = domain.isArchived,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
        }
    }
}
