package com.whisk.domain.model

enum class ActivityType(val displayName: String) {
    NOTE_CREATED("Note Created"),
    NOTE_UPDATED("Note Updated"),
    TASK_CREATED("Task Created"),
    TASK_COMPLETED("Task Completed"),
    PROJECT_ACTIVITY("Project Activity"),
    MEETING_CREATED("Meeting Created"),
    AI_CONVERSATION_CREATED("AI Conversation"),
    LINK_CAPTURED("Link Captured");

    companion object {
        fun fromString(value: String): ActivityType {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                NOTE_UPDATED
            }
        }
    }
}

data class ActivityEvent(
    val id: String,
    val eventType: ActivityType,
    val timestamp: Long = System.currentTimeMillis(),
    val dateKey: String, // "YYYY-MM-DD"
    val relatedObjectId: String? = null,
    val relatedObjectTitle: String? = null,
    val metadata: String? = null,
)
