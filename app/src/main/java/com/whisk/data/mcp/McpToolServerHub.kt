package com.whisk.data.mcp

import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
)

data class McpToolExecutionResult(
    val isError: Boolean = false,
    val content: String,
)

class McpToolServerHub(
    private val notesRepository: NotesRepository,
) {

    fun getDeclaredTools(): List<McpToolDefinition> {
        return listOf(
            McpToolDefinition(
                name = "search_vault_notes",
                description = "Perform full-text keyword search across all notes in the vault.",
                inputSchemaJson = """{"type":"object","properties":{"query":{"type":"string","description":"Search keywords"}},"required":["query"]}""",
            ),
            McpToolDefinition(
                name = "get_note_content",
                description = "Retrieve the complete Markdown content, metadata, and tags of a note by title or ID.",
                inputSchemaJson = """{"type":"object","properties":{"identifier":{"type":"string","description":"Note title or UUID"}},"required":["identifier"]}""",
            ),
            McpToolDefinition(
                name = "create_vault_note",
                description = "Create a new structured Markdown note in the user's personal knowledge vault.",
                inputSchemaJson = """{"type":"object","properties":{"title":{"type":"string"},"body":{"type":"string"},"tags":{"type":"string","description":"Comma-separated tags"}},"required":["title","body"]}""",
            ),
            McpToolDefinition(
                name = "create_vault_task",
                description = "Add a new checklist task linked to a note in the vault.",
                inputSchemaJson = """{"type":"object","properties":{"title":{"type":"string"},"noteTitle":{"type":"string"}},"required":["title"]}""",
            ),
            McpToolDefinition(
                name = "get_vault_summary",
                description = "Get summary statistics of notes, tasks, and tags in the vault.",
                inputSchemaJson = """{"type":"object","properties":{}}""",
            ),
        )
    }

    suspend fun executeTool(name: String, argumentsJson: String): McpToolExecutionResult = withContext(Dispatchers.IO) {
        val args = try {
            JSONObject(argumentsJson.ifBlank { "{}" })
        } catch (_: Exception) {
            JSONObject()
        }

        try {
            when (name) {
                "search_vault_notes" -> {
                    val query = args.optString("query", "")
                    val results = notesRepository.searchNotes(query)
                    // Take first batch
                    var notesList: List<Note> = emptyList()
                    val job = kotlinx.coroutines.flow.firstOrNull {
                        notesList = it
                        true
                    }
                    val jsonArray = JSONArray()
                    notesList.take(10).forEach { note ->
                        jsonArray.put(
                            JSONObject().apply {
                                put("id", note.id)
                                put("title", note.title)
                                put("tags", JSONArray(note.tags))
                                put("snippet", note.body.take(120))
                            },
                        )
                    }
                    McpToolExecutionResult(content = jsonArray.toString())
                }

                "get_note_content" -> {
                    val identifier = args.optString("identifier", "")
                    val note = notesRepository.getNoteByTitle(identifier) ?: notesRepository.getNoteById(identifier)
                    if (note != null) {
                        val obj = JSONObject().apply {
                            put("id", note.id)
                            put("title", note.title)
                            put("body", note.body)
                            put("tags", JSONArray(note.tags))
                            put("folder", note.folder ?: "VAULT")
                            put("isFavorite", note.isFavorite)
                            put("updatedAt", note.updatedAt)
                        }
                        McpToolExecutionResult(content = obj.toString())
                    } else {
                        McpToolExecutionResult(isError = true, content = "Note '$identifier' not found in vault.")
                    }
                }

                "create_vault_note" -> {
                    val title = args.optString("title", "Untitled Note")
                    val body = args.optString("body", "")
                    val tagsRaw = args.optString("tags", "")
                    val tagsList = tagsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    val newNote = Note(
                        title = title,
                        body = body,
                        tags = tagsList,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                    notesRepository.saveNote(newNote)
                    McpToolExecutionResult(content = "Note '${newNote.title}' created with ID: ${newNote.id}")
                }

                "create_vault_task" -> {
                    val taskTitle = args.optString("title", "")
                    val noteTitle = args.optString("noteTitle", "Daily Notes")
                    var note = notesRepository.getNoteByTitle(noteTitle)
                    if (note == null) {
                        note = Note(title = noteTitle, body = "- [ ] $taskTitle\n")
                    } else {
                        note = note.copy(body = note.body + "\n- [ ] $taskTitle\n", updatedAt = System.currentTimeMillis())
                    }
                    notesRepository.saveNote(note)
                    McpToolExecutionResult(content = "Task created in '$noteTitle'")
                }

                "get_vault_summary" -> {
                    val allNotes = notesRepository.getObjectsForPicker("", null, limit = 1000)
                    val result = JSONObject().apply {
                        put("totalNotes", allNotes.size)
                        put("totalWords", allNotes.sumOf { it.body.split(Regex("\\s+")).size })
                        put("favoriteCount", allNotes.count { it.isFavorite })
                    }
                    McpToolExecutionResult(content = result.toString())
                }

                else -> McpToolExecutionResult(isError = true, content = "Unknown MCP tool: '$name'")
            }
        } catch (e: Exception) {
            McpToolExecutionResult(isError = true, content = "Tool execution error: ${e.message}")
        }
    }
}
