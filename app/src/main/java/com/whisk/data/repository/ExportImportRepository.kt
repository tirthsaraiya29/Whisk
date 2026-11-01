package com.whisk.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.whisk.data.local.CanvasEntity
import com.whisk.data.local.ChatMessageEntity
import com.whisk.data.local.ChatSessionEntity
import com.whisk.data.local.FolderEntity
import com.whisk.data.local.NoteEntity
import com.whisk.data.local.TaskEntity
import com.whisk.data.local.WhiskDatabase
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupManifest(
    val version: Int,
    val timestamp: Long,
    val dataChecksum: String,
)

@Serializable
data class BackupData(
    val notes: List<NoteDto> = emptyList(),
    val folders: List<FolderDto> = emptyList(),
    val canvases: List<CanvasDto> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    val chatSessions: List<ChatSessionDto> = emptyList(),
    val chatMessages: List<ChatMessageDto> = emptyList(),
)

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val folder: String?,
    val tags: String,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val sourceMetadata: String?,
    val repoRef: String?,
    val aliases: String,
    val status: String?,
)

@Serializable
data class FolderDto(
    val id: String,
    val name: String,
    val parentId: String?,
)

@Serializable
data class CanvasDto(
    val id: String,
    val title: String,
    val cardsJson: String,
    val connectionsJson: String,
    val updatedAt: Long,
)

@Serializable
data class TaskDto(
    val id: String,
    val noteId: String,
    val noteTitle: String,
    val title: String,
    val isCompleted: Boolean,
    val lineNumber: Int,
    val tag: String?,
)

@Serializable
data class ChatSessionDto(
    val id: String,
    val title: String,
    val timestamp: Long,
    val provider: String?,
    val model: String?,
    val contextRef: String?,
)

@Serializable
data class ChatMessageDto(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val contextPreview: String?,
)

class ExportImportRepository(
    private val context: Context,
    private val database: WhiskDatabase,
    private val notesRepository: NotesRepository,
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportNoteToMarkdown(note: Note, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val markdownContent = buildString {
            append("# ").append(note.title).append("\n\n")
            if (note.tags.isNotEmpty()) {
                append("Tags: ").append(note.tags.joinToString { "#$it" }).append("\n\n")
            }
            append(note.body)
        }
        outputStream.write(markdownContent.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    suspend fun exportVaultToZip(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val notes = database.noteDao().getAllNotesDirect().map {
            NoteDto(it.id, it.title, it.body, it.createdAt, it.updatedAt, it.folder, it.tags, it.isFavorite, it.isArchived, it.sourceMetadata, it.repoRef, it.aliases, it.status)
        }
        val folders = database.folderDao().getAllFolders().first().map {
            FolderDto(it.id, it.name, it.parentId)
        }
        val canvases = database.canvasDao().getAllCanvases().first().map {
            CanvasDto(it.id, it.title, it.cardsJson, it.connectionsJson, it.updatedAt)
        }
        val tasks = database.taskDao().getAllTasks().first().map {
            TaskDto(it.id, it.noteId, it.noteTitle, it.title, it.isCompleted, it.lineNumber, it.tag)
        }
        val chatSessions = database.chatDao().getAllSessions().first().map {
            ChatSessionDto(it.id, it.title, it.timestamp, it.provider, it.model, it.contextRef)
        }
        val chatMessages = database.chatDao().getAllMessagesDirect().map {
            ChatMessageDto(it.id, it.sessionId, it.role, it.content, it.timestamp, it.contextPreview)
        }

        val backupData = BackupData(notes, folders, canvases, tasks, chatSessions, chatMessages)
        val dataJsonString = json.encodeToString(backupData)
        val dataBytes = dataJsonString.toByteArray(Charsets.UTF_8)

        val checksum = MessageDigest.getInstance("SHA-256").digest(dataBytes)
            .joinToString("") { "%02x".format(it) }

        val manifest = BackupManifest(
            version = 1,
            timestamp = System.currentTimeMillis(),
            dataChecksum = checksum,
        )
        val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

        ZipOutputStream(outputStream).use { zipOut ->
            // Manifest
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write(manifestBytes)
            zipOut.closeEntry()

            // Data
            zipOut.putNextEntry(ZipEntry("data.json"))
            zipOut.write(dataBytes)
            zipOut.closeEntry()

            // Markdown notes
            notes.forEach { note ->
                val safeTitle = note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val fileName = "notes/${safeTitle}_${note.id.take(6)}.md"
                val entry = ZipEntry(fileName)
                zipOut.putNextEntry(entry)
                val content = buildString {
                    append("# ").append(note.title).append("\n\n")
                    if (note.tags.isNotBlank()) {
                        append("Tags: ").append(note.tags.split(",").joinToString { "#${it.trim()}" }).append("\n\n")
                    }
                    append(note.body)
                }
                zipOut.write(content.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
    }

    suspend fun importVaultFromZip(uri: Uri): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        var dataJsonStr: String? = null
        val MAX_SIZE = 100 * 1024 * 1024 // 100 MB limit
        var legacyImportCount = 0

        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext 0
        ZipInputStream(inputStream).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (!entry.isDirectory && !entryName.contains("..")) {
                    var bytesRead = 0
                    val buffer = ByteArray(8192)
                    val outputStream = java.io.ByteArrayOutputStream()
                    var count: Int
                    while (zipIn.read(buffer).also { count = it } != -1) {
                        outputStream.write(buffer, 0, count)
                        bytesRead += count
                        if (bytesRead > MAX_SIZE) {
                            throw SecurityException("Zip entry too large")
                        }
                    }

                    val bytes = outputStream.toByteArray()

                    if (entryName == "data.json") {
                        dataJsonStr = String(bytes, Charsets.UTF_8)
                    } else if (entryName.endsWith(".md") || entryName.endsWith(".txt")) {
                        // Fallback import logic for older markdown exports or bare markdown files
                        if (dataJsonStr == null && !entryName.startsWith("notes/")) {
                            val text = String(bytes, Charsets.UTF_8)
                            val lines = text.lines()
                            val title = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()
                                ?: entryName.removeSuffix(".md").removeSuffix(".txt")
                            val body = if (lines.firstOrNull()?.startsWith("# ") == true) {
                                lines.drop(1).asSequence().joinToString("\n").trim()
                            } else {
                                text
                            }
                            val note = Note(
                                title = title,
                                body = body,
                                tags = listOf("imported"),
                            )
                            notesRepository.saveNote(note)
                            legacyImportCount++
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        if (dataJsonStr != null) {
            val backupData = json.decodeFromString<BackupData>(dataJsonStr)
            database.withTransaction {
                val folderDao = database.folderDao()
                val noteDao = database.noteDao()
                val canvasDao = database.canvasDao()
                val taskDao = database.taskDao()
                val chatDao = database.chatDao()

                backupData.folders.forEach { f ->
                    folderDao.insertFolder(FolderEntity(f.id, f.name, f.parentId))
                }
                backupData.notes.forEach { n ->
                    noteDao.insertNote(NoteEntity(n.id, n.title, n.body, n.createdAt, n.updatedAt, n.folder, n.tags, n.isFavorite, n.isArchived, n.sourceMetadata, n.repoRef, n.aliases, n.status))
                    importedCount++
                }
                backupData.canvases.forEach { c ->
                    canvasDao.insertCanvas(CanvasEntity(c.id, c.title, c.cardsJson, c.connectionsJson, c.updatedAt))
                }
                if (backupData.tasks.isNotEmpty()) {
                    val taskEntities = backupData.tasks.map { t -> TaskEntity(t.id, t.noteId, t.noteTitle, t.title, t.isCompleted, t.lineNumber, t.tag) }
                    taskDao.insertTasks(taskEntities)
                }
                backupData.chatSessions.forEach { cs ->
                    chatDao.insertSession(ChatSessionEntity(cs.id, cs.title, cs.timestamp, cs.provider, cs.model, cs.contextRef))
                }
                if (backupData.chatMessages.isNotEmpty()) {
                    val msgEntities = backupData.chatMessages.map { cm -> ChatMessageEntity(cm.id, cm.sessionId, cm.role, cm.content, cm.timestamp, cm.contextPreview) }
                    chatDao.insertMessages(msgEntities)
                }
            }
            importedCount
        } else {
            legacyImportCount
        }
    }
}
