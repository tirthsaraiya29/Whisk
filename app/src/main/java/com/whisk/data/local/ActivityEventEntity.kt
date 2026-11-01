package com.whisk.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.whisk.domain.model.ActivityEvent
import com.whisk.domain.model.ActivityType

@Entity(
    tableName = "activity_events",
    indices = [
        Index(value = ["dateKey"]),
        Index(value = ["timestamp"]),
        Index(value = ["relatedObjectId"]),
    ],
)
data class ActivityEventEntity(
    @PrimaryKey val id: String,
    val eventType: String,
    val timestamp: Long,
    val dateKey: String,
    val relatedObjectId: String?,
    val relatedObjectTitle: String?,
    val metadata: String?,
) {
    fun toDomain(): ActivityEvent {
        return ActivityEvent(
            id = id,
            eventType = ActivityType.fromString(eventType),
            timestamp = timestamp,
            dateKey = dateKey,
            relatedObjectId = relatedObjectId,
            relatedObjectTitle = relatedObjectTitle,
            metadata = metadata,
        )
    }

    companion object {
        fun fromDomain(domain: ActivityEvent): ActivityEventEntity {
            return ActivityEventEntity(
                id = domain.id,
                eventType = domain.eventType.name,
                timestamp = domain.timestamp,
                dateKey = domain.dateKey,
                relatedObjectId = domain.relatedObjectId,
                relatedObjectTitle = domain.relatedObjectTitle,
                metadata = domain.metadata,
            )
        }
    }
}
