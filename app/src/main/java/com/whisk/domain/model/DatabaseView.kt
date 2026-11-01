package com.whisk.domain.model

import kotlinx.serialization.Serializable

enum class ViewType(val displayName: String, val iconName: String) {
    TABLE("Table", "table_chart"),
    LIST("List", "format_list_bulleted"),
    GALLERY("Gallery", "grid_view"),
    BOARD("Board", "view_kanban"),
    CALENDAR("Calendar", "calendar_month");

    companion object {
        fun fromString(value: String): ViewType {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                TABLE
            }
        }
    }
}

@Serializable
data class FilterRule(
    val propertyId: String, // Property ID or special keys: "title", "status", "typeId", "createdAt", "updatedAt"
    val operator: PropertyOperator,
    val value: String? = null,
)

enum class SortDirection {
    ASCENDING, DESCENDING
}

@Serializable
data class SortRule(
    val propertyId: String,
    val direction: SortDirection = SortDirection.ASCENDING,
)

data class DatabaseView(
    val id: String,
    val targetTypeId: String? = null, // null means all objects / vault
    val name: String,
    val viewType: ViewType,
    val visiblePropertyIds: List<String> = emptyList(),
    val filters: List<FilterRule> = emptyList(),
    val sorts: List<SortRule> = emptyList(),
    val groupPropertyId: String? = null, // Property ID or "status", "typeId"
    val datePropertyId: String? = null, // Property ID or "createdAt", "updatedAt" for Calendar view
    val cardSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE for Gallery
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

