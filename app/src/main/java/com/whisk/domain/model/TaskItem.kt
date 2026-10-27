package com.whisk.domain.model

import java.util.UUID

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val noteTitle: String,
    val title: String,
    val isCompleted: Boolean = false,
    val lineNumber: Int = 0,
    val tag: String? = null
)

data class Template(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val content: String
)
