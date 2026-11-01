package com.whisk.domain.model

data class ObjectTypeDefinition(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String = "description",
    val isBuiltIn: Boolean = false,
    val configurationJson: String? = null,
    val position: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

object BuiltInObjectTypes {
    const val NOTE = "note"
    const val PROJECT = "project"
    const val PERSON = "person"
    const val BOOK = "book"
    const val COURSE = "course"
    const val REPOSITORY = "repository"
    const val CONCEPT = "concept"
    const val TASK = "task"
    const val MEETING = "meeting"

    val DEFAULT_TYPES = listOf(
        ObjectTypeDefinition(
            id = NOTE,
            name = "Notes",
            description = "General-purpose knowledge and writing",
            icon = "description",
            isBuiltIn = true,
            position = 0,
        ),
        ObjectTypeDefinition(
            id = PROJECT,
            name = "Projects",
            description = "Organise work, goals, tasks and resources",
            icon = "folder_special",
            isBuiltIn = true,
            position = 1,
        ),
        ObjectTypeDefinition(
            id = PERSON,
            name = "People",
            description = "Store information about individuals",
            icon = "person",
            isBuiltIn = true,
            position = 2,
        ),
        ObjectTypeDefinition(
            id = BOOK,
            name = "Books",
            description = "Track books, authors, reading progress and notes",
            icon = "menu_book",
            isBuiltIn = true,
            position = 3,
        ),
        ObjectTypeDefinition(
            id = COURSE,
            name = "Courses",
            description = "Organise educational material and learning progress",
            icon = "school",
            isBuiltIn = true,
            position = 4,
        ),
        ObjectTypeDefinition(
            id = REPOSITORY,
            name = "Repositories",
            description = "Track software repositories and development information",
            icon = "code",
            isBuiltIn = true,
            position = 5,
        ),
        ObjectTypeDefinition(
            id = CONCEPT,
            name = "Concepts",
            description = "Store interconnected ideas and explanations",
            icon = "lightbulb",
            isBuiltIn = true,
            position = 6,
        ),
        ObjectTypeDefinition(
            id = TASK,
            name = "Tasks",
            description = "Track actionable work",
            icon = "check_circle",
            isBuiltIn = true,
            position = 7,
        ),
        ObjectTypeDefinition(
            id = MEETING,
            name = "Meetings",
            description = "Store meeting notes, participants, decisions and action items",
            icon = "event",
            isBuiltIn = true,
            position = 8,
        ),
    )
}
