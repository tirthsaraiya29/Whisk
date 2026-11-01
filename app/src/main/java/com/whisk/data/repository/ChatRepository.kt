package com.whisk.data.repository

import com.whisk.data.local.ChatDao
import com.whisk.data.local.ChatMessageEntity
import com.whisk.data.local.ChatSessionEntity
import com.whisk.domain.model.AiMessage
import com.whisk.ui.screens.ai.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<AiMessage>> {
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getMessagesForSessionPaginated(
        sessionId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): List<AiMessage> {
        return chatDao.getMessagesForSessionPaginated(sessionId, limit, offset).map { it.toDomain() }
    }

    suspend fun saveSession(
        session: ChatSession,
        provider: String? = null,
        model: String? = null,
        contextRef: String? = null,
    ) {
        val sessionEntity = ChatSessionEntity(
            id = session.id,
            title = session.title,
            timestamp = session.timestamp,
            provider = provider,
            model = model,
            contextRef = contextRef,
        )
        val messageEntities = session.messages.map { ChatMessageEntity.fromDomain(session.id, it) }
        chatDao.saveSessionWithMessages(sessionEntity, messageEntities)
    }

    suspend fun saveMessage(sessionId: String, message: AiMessage) {
        chatDao.insertMessage(ChatMessageEntity.fromDomain(sessionId, message))
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSessionAndMessages(sessionId)
    }

    suspend fun deleteAllSessions() {
        chatDao.deleteAllSessions()
    }
}
