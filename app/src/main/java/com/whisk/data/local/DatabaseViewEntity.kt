package com.whisk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whisk.domain.model.DatabaseView
import com.whisk.domain.model.FilterRule
import com.whisk.domain.model.SortRule
import com.whisk.domain.model.ViewType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "database_views")
data class DatabaseViewEntity(
    @PrimaryKey val id: String,
    val targetTypeId: String?,
    val name: String,
    val viewType: String,
    val visiblePropertiesJson: String?,
    val filterConfigJson: String?,
    val sortConfigJson: String?,
    val groupPropertyId: String?,
    val datePropertyId: String?,
    val cardSize: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain(): DatabaseView {
        val visibleProps = if (!visiblePropertiesJson.isNullOrBlank()) {
            try { Json.decodeFromString<List<String>>(visiblePropertiesJson) } catch (_: Exception) { emptyList() }
        } else emptyList()

        val parsedFilters = if (!filterConfigJson.isNullOrBlank()) {
            try { Json.decodeFromString<List<FilterRule>>(filterConfigJson) } catch (_: Exception) { emptyList() }
        } else emptyList()

        val parsedSorts = if (!sortConfigJson.isNullOrBlank()) {
            try { Json.decodeFromString<List<SortRule>>(sortConfigJson) } catch (_: Exception) { emptyList() }
        } else emptyList()

        return DatabaseView(
            id = id,
            targetTypeId = targetTypeId,
            name = name,
            viewType = ViewType.fromString(viewType),
            visiblePropertyIds = visibleProps,
            filters = parsedFilters,
            sorts = parsedSorts,
            groupPropertyId = groupPropertyId,
            datePropertyId = datePropertyId,
            cardSize = cardSize,
            position = position,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun fromDomain(domain: DatabaseView): DatabaseViewEntity {
            return DatabaseViewEntity(
                id = domain.id,
                targetTypeId = domain.targetTypeId,
                name = domain.name,
                viewType = domain.viewType.name,
                visiblePropertiesJson = if (domain.visiblePropertyIds.isNotEmpty()) Json.encodeToString(domain.visiblePropertyIds) else null,
                filterConfigJson = if (domain.filters.isNotEmpty()) Json.encodeToString(domain.filters) else null,
                sortConfigJson = if (domain.sorts.isNotEmpty()) Json.encodeToString(domain.sorts) else null,
                groupPropertyId = domain.groupPropertyId,
                datePropertyId = domain.datePropertyId,
                cardSize = domain.cardSize,
                position = domain.position,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
        }
    }
}
