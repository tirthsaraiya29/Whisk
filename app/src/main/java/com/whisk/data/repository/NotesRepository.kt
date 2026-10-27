package com.whisk.data.repository

import com.whisk.data.local.FolderDao
import com.whisk.data.local.FolderEntity
import com.whisk.data.local.NoteDao
import com.whisk.data.local.NoteEntity
import com.whisk.data.local.TaskDao
import com.whisk.data.local.TaskEntity
import com.whisk.domain.model.Backlink
import com.whisk.domain.model.Folder
import com.whisk.domain.model.Note
import com.whisk.domain.model.NoteLink
import com.whisk.domain.model.Tag
import com.whisk.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val taskDao: TaskDao
) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    val favoriteNotes: Flow<List<Note>> = noteDao.getFavoriteNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    val folders: Flow<List<Folder>> = folderDao.getAllFolders().map { entities ->
        entities.map { Folder(id = it.id, name = it.name, parentId = it.parentId) }
    }

    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }

    suspend fun getNoteByTitle(title: String): Note? {
        return noteDao.getNoteByTitle(title)?.toDomain()
    }

    suspend fun saveNote(note: Note) {
        val entity = NoteEntity.fromDomain(note.copy(updatedAt = System.currentTimeMillis()))
        noteDao.insertNote(entity)
        syncTasksForNote(note)
    }

    suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
        taskDao.deleteTasksForNote(id)
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun extractLinks(note: Note): List<NoteLink> {
        val pattern = Regex("\\[\\[(.*?)\\]\\]")
        val matches = pattern.findAll(note.body).toList()
        val links = mutableListOf<NoteLink>()
        for (match in matches) {
            val title = match.groupValues[1].trim()
            val target = noteDao.getNoteByTitle(title)
            links.add(
                NoteLink(
                    sourceNoteId = note.id,
                    targetTitle = title,
                    targetNoteId = target?.id,
                    isBroken = target == null
                )
            )
        }
        return links
    }

    suspend fun getBacklinksForNote(noteTitle: String, noteId: String): List<Backlink> {
        val all = noteDao.getAllNotesDirect()
        val backlinks = mutableListOf<Backlink>()
        val linkRegex = Regex("\\[\\[${Regex.escape(noteTitle)}\\]\\]", RegexOption.IGNORE_CASE)
        for (n in all) {
            if (n.id == noteId) continue
            if (linkRegex.containsMatchIn(n.body) || n.body.contains(noteTitle, ignoreCase = true)) {
                val line = n.body.lines().find { it.contains(noteTitle, ignoreCase = true) } ?: ""
                backlinks.add(
                    Backlink(
                        sourceNoteId = n.id,
                        sourceNoteTitle = n.title,
                        snippet = if (line.length > 80) line.take(80) + "..." else line,
                        updatedAt = n.updatedAt
                    )
                )
            }
        }
        return backlinks
    }

    suspend fun getTagsWithCounts(): List<Tag> {
        val notes = noteDao.getAllNotesDirect()
        val tagCounts = mutableMapOf<String, Int>()
        notes.forEach { n ->
            val tags = n.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            tags.forEach { t -> tagCounts[t] = (tagCounts[t] ?: 0) + 1 }
        }
        return tagCounts.map { Tag(name = it.key, noteCount = it.value) }.sortedByDescending { it.noteCount }
    }

    suspend fun addFolder(name: String) {
        folderDao.insertFolder(FolderEntity(id = java.util.UUID.randomUUID().toString(), name = name, parentId = null))
    }

    suspend fun toggleTaskStatus(taskId: String, isCompleted: Boolean) {
        taskDao.updateTaskStatus(taskId, isCompleted)
    }

    private suspend fun syncTasksForNote(note: Note) {
        taskDao.deleteTasksForNote(note.id)
        val lines = note.body.lines()
        val taskEntities = mutableListOf<TaskEntity>()
        val taskRegex = Regex("^\\s*-\\s*\\[([ xX])\\]\\s*(.*)")
        lines.forEachIndexed { index, line ->
            val match = taskRegex.find(line)
            if (match != null) {
                val completed = match.groupValues[1].equals("x", ignoreCase = true)
                val taskText = match.groupValues[2].trim()
                if (taskText.isNotEmpty()) {
                    taskEntities.add(
                        TaskEntity(
                            id = "${note.id}_task_$index",
                            noteId = note.id,
                            noteTitle = note.title,
                            title = taskText,
                            isCompleted = completed,
                            lineNumber = index + 1,
                            tag = note.tags.firstOrNull()
                        )
                    )
                }
            }
        }
        if (taskEntities.isNotEmpty()) {
            taskDao.insertTasks(taskEntities)
        }
    }

    suspend fun seedWelcomeNoteIfEmpty() {
        if (noteDao.getNoteCount() == 0) {
            val welcomeNote = Note(
                id = "welcome_note_001",
                title = "Welcome to Whisk",
                body = """
# Welcome to Whisk 🚀

Whisk is your modern, minimalist developer knowledge workspace.

## Key Features
- **Markdown & Code**: Write fast with full syntax highlighting.
- **Internal Links**: Connect ideas using `[[Architecture Overview]]`.
- **Interactive Graph**: Visualize note relationships in 2D space.
- **Spatial Canvas**: Drag, arrange, and link cards freely.
- **Local AI & Remote Providers**: Leverage Google Gemma on-device or connect remote LLMs.
- **GitHub Integration**: Reference issues, PRs, commits, and MCP tool execution.

```kotlin
fun main() {
    println("Hello Whisk!")
}
```

### Quick Checklist
- [x] Launch Whisk
- [ ] Create your first note
- [ ] Link notes with `[[Title]]`
- [ ] Explore the Knowledge Graph
                """.trimIndent(),
                tags = listOf("welcome", "getting-started"),
                isFavorite = true
            )
            saveNote(welcomeNote)
        }
    }
}
