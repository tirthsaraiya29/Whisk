package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM property_definitions ORDER BY position ASC, name ASC")
    fun getAllPropertyDefinitions(): Flow<List<PropertyDefinitionEntity>>

    @Query("SELECT * FROM property_definitions WHERE isArchived = 0 ORDER BY position ASC, name ASC")
    fun getNonArchivedPropertyDefinitions(): Flow<List<PropertyDefinitionEntity>>

    @Query("SELECT * FROM property_definitions WHERE id = :id LIMIT 1")
    suspend fun getPropertyDefinitionById(id: String): PropertyDefinitionEntity?

    @Query("SELECT * FROM property_definitions WHERE name = :name LIMIT 1")
    suspend fun getPropertyDefinitionByName(name: String): PropertyDefinitionEntity?

    @Query("SELECT * FROM property_definitions")
    suspend fun getAllPropertyDefinitionsDirect(): List<PropertyDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPropertyDefinition(entity: PropertyDefinitionEntity)

    @Update
    suspend fun updatePropertyDefinition(entity: PropertyDefinitionEntity)

    @Query("DELETE FROM property_definitions WHERE id = :id")
    suspend fun deletePropertyDefinitionById(id: String): Int

    @Query("SELECT * FROM note_property_values WHERE noteId = :noteId")
    fun getPropertyValuesForNote(noteId: String): Flow<List<NotePropertyValueEntity>>

    @Query("SELECT * FROM note_property_values WHERE noteId = :noteId")
    suspend fun getPropertyValuesForNoteDirect(noteId: String): List<NotePropertyValueEntity>

    @Query("SELECT * FROM note_property_values WHERE noteId = :noteId AND propertyDefinitionId = :propertyDefinitionId LIMIT 1")
    suspend fun getNotePropertyValue(noteId: String, propertyDefinitionId: String): NotePropertyValueEntity?

    @Query("SELECT * FROM note_property_values")
    suspend fun getAllNotePropertyValuesDirect(): List<NotePropertyValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotePropertyValue(entity: NotePropertyValueEntity)

    @Query("DELETE FROM note_property_values WHERE noteId = :noteId AND propertyDefinitionId = :propertyDefinitionId")
    suspend fun deleteNotePropertyValue(noteId: String, propertyDefinitionId: String): Int

    @Query("DELETE FROM note_property_values WHERE propertyDefinitionId = :propertyDefinitionId")
    suspend fun deletePropertyValuesForProperty(propertyDefinitionId: String): Int

    // Property Filter Queries
    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND textValue = :targetValue")
    suspend fun getNoteIdsMatchingTextEquals(propId: String, targetValue: String): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND textValue != :targetValue")
    suspend fun getNoteIdsMatchingTextNotEquals(propId: String, targetValue: String): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND textValue LIKE '%' || :targetValue || '%'")
    suspend fun getNoteIdsMatchingTextContains(propId: String, targetValue: String): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND textValue < :targetValue")
    suspend fun getNoteIdsMatchingDateBefore(propId: String, targetValue: String): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND textValue > :targetValue")
    suspend fun getNoteIdsMatchingDateAfter(propId: String, targetValue: String): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND numberValue = :targetValue")
    suspend fun getNoteIdsMatchingNumberEquals(propId: String, targetValue: Double): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND numberValue != :targetValue")
    suspend fun getNoteIdsMatchingNumberNotEquals(propId: String, targetValue: Double): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND numberValue > :targetValue")
    suspend fun getNoteIdsMatchingNumberGreaterThan(propId: String, targetValue: Double): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND numberValue < :targetValue")
    suspend fun getNoteIdsMatchingNumberLessThan(propId: String, targetValue: Double): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND booleanValue = :targetValue")
    suspend fun getNoteIdsMatchingBooleanEquals(propId: String, targetValue: Boolean): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND listValueJson LIKE '%' || :targetValue || '%'")
    suspend fun getNoteIdsMatchingMultiSelectContains(propId: String, targetValue: String): List<String>

    @Query("SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND (textValue IS NOT NULL OR numberValue IS NOT NULL OR booleanValue IS NOT NULL OR listValueJson IS NOT NULL)")
    suspend fun getNoteIdsMatchingIsNotEmpty(propId: String): List<String>

    @Query("SELECT id FROM notes WHERE id NOT IN (SELECT noteId FROM note_property_values WHERE propertyDefinitionId = :propId AND (textValue IS NOT NULL OR numberValue IS NOT NULL OR booleanValue IS NOT NULL OR listValueJson IS NOT NULL)) AND isArchived = 0")
    suspend fun getNoteIdsMatchingIsEmpty(propId: String): List<String>
}
