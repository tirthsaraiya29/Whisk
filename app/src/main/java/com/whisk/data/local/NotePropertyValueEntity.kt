package com.whisk.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.whisk.domain.model.NotePropertyValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "note_property_values",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PropertyDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyDefinitionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["propertyDefinitionId"]),
        Index(value = ["noteId", "propertyDefinitionId"], unique = true),
    ],
)
data class NotePropertyValueEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val propertyDefinitionId: String,
    val textValue: String?,
    val numberValue: Double?,
    val booleanValue: Boolean?,
    val listValueJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain(): NotePropertyValue {
        val parsedList = if (!listValueJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<String>>(listValueJson)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return NotePropertyValue(
            id = id,
            noteId = noteId,
            propertyDefinitionId = propertyDefinitionId,
            textValue = textValue,
            numberValue = numberValue,
            booleanValue = booleanValue,
            listValue = parsedList,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun fromDomain(domain: NotePropertyValue): NotePropertyValueEntity {
            val jsonList = if (domain.listValue.isNotEmpty()) {
                Json.encodeToString(domain.listValue)
            } else {
                null
            }

            return NotePropertyValueEntity(
                id = domain.id,
                noteId = domain.noteId,
                propertyDefinitionId = domain.propertyDefinitionId,
                textValue = domain.textValue,
                numberValue = domain.numberValue,
                booleanValue = domain.booleanValue,
                listValueJson = jsonList,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
            )
        }
    }
}
