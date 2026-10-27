package com.whisk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whisk.domain.model.TaskItem

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val noteTitle: String,
    val title: String,
    val isCompleted: Boolean,
    val lineNumber: Int,
    val tag: String?
) {
    fun toDomain() = TaskItem(
        id = id,
        noteId = noteId,
        noteTitle = noteTitle,
        title = title,
        isCompleted = isCompleted,
        lineNumber = lineNumber,
        tag = tag
    )

    companion object {
        fun fromDomain(item: TaskItem) = TaskEntity(
            id = item.id,
            noteId = item.noteId,
            noteTitle = item.noteTitle,
            title = item.title,
            isCompleted = item.isCompleted,
            lineNumber = item.lineNumber,
            tag = item.tag
        )
    }
}
