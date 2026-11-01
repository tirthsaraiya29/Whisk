package com.whisk.domain.model

enum class PropertyType {
    TEXT,
    NUMBER,
    DATE,
    CHECKBOX,
    SELECT,
    MULTI_SELECT,
    RELATION
}

data class PropertyDefinition(
    val id: String,
    val name: String,
    val type: PropertyType,
    val description: String? = null,
    val options: List<String> = emptyList(),
    val position: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class NotePropertyValue(
    val id: String,
    val noteId: String,
    val propertyDefinitionId: String,
    val textValue: String? = null,
    val numberValue: Double? = null,
    val booleanValue: Boolean? = null,
    val listValue: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class NotePropertyItem(
    val definition: PropertyDefinition,
    val value: NotePropertyValue?,
    val relatedNoteTitle: String? = null,
)

enum class PropertyOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    GREATER_THAN,
    LESS_THAN,
    BEFORE,
    AFTER,
    IS_EMPTY,
    IS_NOT_EMPTY
}

data class PropertyFilter(
    val propertyDefinitionId: String,
    val operator: PropertyOperator,
    val value: String? = null,
)
