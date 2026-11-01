package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {
    @Query("SELECT * FROM object_relationships WHERE sourceObjectId = :objectId OR targetObjectId = :objectId ORDER BY createdAt DESC")
    fun getRelationshipsForObject(objectId: String): Flow<List<ObjectRelationshipEntity>>

    @Query("SELECT * FROM object_relationships WHERE sourceObjectId = :objectId OR targetObjectId = :objectId ORDER BY createdAt DESC")
    suspend fun getRelationshipsForObjectDirect(objectId: String): List<ObjectRelationshipEntity>

    @Query("SELECT * FROM object_relationships ORDER BY createdAt DESC")
    fun getAllRelationships(): Flow<List<ObjectRelationshipEntity>>

    @Query("SELECT * FROM object_relationships")
    suspend fun getAllRelationshipsDirect(): List<ObjectRelationshipEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelationship(entity: ObjectRelationshipEntity)

    @Query("DELETE FROM object_relationships WHERE id = :id")
    suspend fun deleteRelationshipById(id: String): Int

    @Query("DELETE FROM object_relationships WHERE (sourceObjectId = :objA AND targetObjectId = :objB) OR (sourceObjectId = :objB AND targetObjectId = :objA)")
    suspend fun deleteRelationshipsBetweenObjects(objA: String, objB: String): Int

    @Query("DELETE FROM object_relationships WHERE sourceObjectId = :objectId OR targetObjectId = :objectId")
    suspend fun deleteRelationshipsForObject(objectId: String): Int
}
