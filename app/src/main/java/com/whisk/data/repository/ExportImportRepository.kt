package com.whisk.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.whisk.data.local.CanvasEntity
import com.whisk.data.local.ChatMessageEntity
import com.whisk.data.local.ChatSessionEntity
import com.whisk.data.local.FolderEntity
import com.whisk.data.local.NoteEntity
import com.whisk.data.local.NotePropertyValueEntity
import com.whisk.data.local.ObjectRelationshipEntity
import com.whisk.data.local.ObjectTypeDefinitionEntity
import com.whisk.data.local.PropertyDefinitionEntity
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
    val propertyDefinitions: List<PropertyDefinitionDto> = emptyList(),
    val notePropertyValues: List<NotePropertyValueDto> = emptyList(),
    val objectTypeDefinitions: List<ObjectTypeDefinitionDto> = emptyList(),
    val objectRelationships: List<ObjectRelationshipDto> = emptyList(),
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
    val typeId: String = "note",
    val description: String? = null,
    val icon: String? = null,
    val coverImage: String? = null,
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

@Serializable
data class PropertyDefinitionDto(
    val id: String,
    val name: String,
    val type: String,
    val description: String?,
    val optionsJson: String?,
    val position: Int,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class NotePropertyValueDto(
    val id: String,
    val noteId: String,
    val propertyDefinitionId: String,
    val textValue: String?,
    val numberValue: Double?,
    val booleanValue: Boolean?,
    val listValueJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ObjectTypeDefinitionDto(
    val id: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val isBuiltIn: Boolean,
    val configurationJson: String?,
    val position: Int,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ObjectRelationshipDto(
    val id: String,
    val sourceObjectId: String,
    val targetObjectId: String,
    val relationshipType: String,
    val createdAt: Long,
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
            NoteDto(
                id = it.id,
                title = it.title,
                body = it.body,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                folder = it.folder,
                tags = it.tags,
                isFavorite = it.isFavorite,
                isArchived = it.isArchived,
                sourceMetadata = it.sourceMetadata,
                repoRef = it.repoRef,
                aliases = it.aliases,
                status = it.status,
                typeId = it.typeId,
                description = it.description,
                icon = it.icon,
                coverImage = it.coverImage,
            )
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
        val propertyDefinitions = database.propertyDao().getAllPropertyDefinitionsDirect().map {
            PropertyDefinitionDto(it.id, it.name, it.type, it.description, it.optionsJson, it.position, it.isArchived, it.createdAt, it.updatedAt)
        }
        val notePropertyValues = database.propertyDao().getAllNotePropertyValuesDirect().map {
            NotePropertyValueDto(it.id, it.noteId, it.propertyDefinitionId, it.textValue, it.numberValue, it.booleanValue, it.listValueJson, it.createdAt, it.updatedAt)
        }
        val objectTypeDefinitions = database.objectTypeDao().getAllObjectTypeDefinitionsDirect().map {
            ObjectTypeDefinitionDto(it.id, it.name, it.description, it.icon, it.isBuiltIn, it.configurationJson, it.position, it.isArchived, it.createdAt, it.updatedAt)
        }
        val objectRelationships = database.relationshipDao().getAllRelationshipsDirect().map {
            ObjectRelationshipDto(it.id, it.sourceObjectId, it.targetObjectId, it.relationshipType, it.createdAt)
        }

        val backupData = BackupData(
            notes = notes,
            folders = folders,
            canvases = canvases,
            tasks = tasks,
            chatSessions = chatSessions,
            chatMessages = chatMessages,
            propertyDefinitions = propertyDefinitions,
            notePropertyValues = notePropertyValues,
            objectTypeDefinitions = objectTypeDefinitions,
            objectRelationships = objectRelationships,
        )
        val dataJsonString = json.encodeToString(backupData)
        val dataBytes = dataJsonString.toByteArray(Charsets.UTF_8)

        val checksum = MessageDigest.getInstance("SHA-256").digest(dataBytes)
            .joinToString("") { "%02x".format(it) }

        val manifest = BackupManifest(
            version = 2,
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
                val propertyDao = database.propertyDao()
                val objectTypeDao = database.objectTypeDao()
                val relationshipDao = database.relationshipDao()

                backupData.objectTypeDefinitions.forEach { otd ->
                    objectTypeDao.insertObjectTypeDefinition(ObjectTypeDefinitionEntity(otd.id, otd.name, otd.description, otd.icon, otd.isBuiltIn, otd.configurationJson, otd.position, otd.isArchived, otd.createdAt, otd.updatedAt))
                }
                backupData.propertyDefinitions.forEach { pd ->
                    propertyDao.insertPropertyDefinition(PropertyDefinitionEntity(pd.id, pd.name, pd.type, pd.description, pd.optionsJson, pd.position, pd.isArchived, pd.createdAt, pd.updatedAt))
                }
                backupData.folders.forEach { f ->
                    folderDao.insertFolder(FolderEntity(f.id, f.name, f.parentId))
                }
                backupData.notes.forEach { n ->
                    noteDao.insertNote(
                        NoteEntity(
                            id = n.id,
                            title = n.title,
                            body = n.body,
                            createdAt = n.createdAt,
                            updatedAt = n.updatedAt,
                            folder = n.folder,
                            tags = n.tags,
                            isFavorite = n.isFavorite,
                            isArchived = n.isArchived,
                            sourceMetadata = n.sourceMetadata,
                            repoRef = n.repoRef,
                            aliases = n.aliases,
                            status = n.status,
                            typeId = n.typeId,
                            description = n.description,
                            icon = n.icon,
                            coverImage = n.coverImage,
                        ),
                    )
                    importedCount++
                }
                backupData.objectRelationships.forEach { rel ->
                    relationshipDao.insertRelationship(ObjectRelationshipEntity(rel.id, rel.sourceObjectId, rel.targetObjectId, rel.relationshipType, rel.createdAt))
                }
                backupData.notePropertyValues.forEach { pv ->
                    propertyDao.insertNotePropertyValue(NotePropertyValueEntity(pv.id, pv.noteId, pv.propertyDefinitionId, pv.textValue, pv.numberValue, pv.booleanValue, pv.listValueJson, pv.createdAt, pv.updatedAt))
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
