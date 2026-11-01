package com.whisk.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String): Int

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions(): Int

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesForSessionPaginated(
        sessionId: String,
        limit: Int,
        offset: Int,
    ): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages")
    suspend fun getAllMessagesDirect(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String): Int

    @Transaction
    suspend fun deleteSessionAndMessages(sessionId: String) {
        deleteMessagesForSession(sessionId)
        deleteSessionById(sessionId)
    }

    @Transaction
    suspend fun saveSessionWithMessages(
        session: ChatSessionEntity,
        messages: List<ChatMessageEntity>,
    ) {
        insertSession(session)
        insertMessages(messages)
    }
}
