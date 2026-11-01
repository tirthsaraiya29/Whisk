package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectTypeDao {
    @Query("SELECT * FROM object_type_definitions ORDER BY position ASC, name ASC")
    fun getAllObjectTypeDefinitions(): Flow<List<ObjectTypeDefinitionEntity>>

    @Query("SELECT * FROM object_type_definitions WHERE isArchived = 0 ORDER BY position ASC, name ASC")
    fun getNonArchivedObjectTypeDefinitions(): Flow<List<ObjectTypeDefinitionEntity>>

    @Query("SELECT * FROM object_type_definitions WHERE id = :id LIMIT 1")
    suspend fun getObjectTypeDefinitionById(id: String): ObjectTypeDefinitionEntity?

    @Query("SELECT * FROM object_type_definitions")
    suspend fun getAllObjectTypeDefinitionsDirect(): List<ObjectTypeDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjectTypeDefinition(entity: ObjectTypeDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertObjectTypeDefinitions(entities: List<ObjectTypeDefinitionEntity>)

    @Update
    suspend fun updateObjectTypeDefinition(entity: ObjectTypeDefinitionEntity)

    @Query("DELETE FROM object_type_definitions WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteObjectTypeDefinitionById(id: String): Int

    @Query("UPDATE notes SET typeId = :newTypeId WHERE typeId = :oldTypeId")
    suspend fun reassignObjectsType(oldTypeId: String, newTypeId: String): Int
}
