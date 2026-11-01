package com.whisk.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.whisk.domain.model.AiMessage
import com.whisk.domain.model.AiRole

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val contextPreview: String? = null,
) {
    fun toDomain(): AiMessage {
        val aiRole = try {
            AiRole.valueOf(role)
        } catch (_: Exception) {
            AiRole.ASSISTANT
        }
        return AiMessage(
            id = id,
            role = aiRole,
            content = content,
            timestamp = timestamp,
            contextPreview = contextPreview,
        )
    }

    companion object {
        fun fromDomain(sessionId: String, message: AiMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                sessionId = sessionId,
                role = message.role.name,
                content = message.content,
                timestamp = message.timestamp,
                contextPreview = message.contextPreview,
            )
        }
    }
}
