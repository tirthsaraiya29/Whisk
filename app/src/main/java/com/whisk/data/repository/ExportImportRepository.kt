package com.whisk.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.whisk.data.local.ActivityEventEntity
import com.whisk.data.local.CanvasEntity
import com.whisk.data.local.ChatMessageEntity
import com.whisk.data.local.ChatSessionEntity
import com.whisk.data.local.DailyNoteMetadataEntity
import com.whisk.data.local.DatabaseViewEntity
import com.whisk.data.local.FolderEntity
import com.whisk.data.local.NoteEntity
import com.whisk.data.local.NotePropertyValueEntity
import com.whisk.data.local.ObjectRelationshipEntity
import com.whisk.data.local.ObjectTypeDefinitionEntity
import com.whisk.data.local.PropertyDefinitionEntity
import com.whisk.data.local.TaskEntity
import com.whisk.data.local.WhiskDatabase
import com.whisk.data.local.rag.DocumentChunkEntity
import com.whisk.data.local.rag.EmbeddingVectorEntity
import com.whisk.data.local.rag.IndexingStateEntity
import com.whisk.data.local.rag.IndexingStatus
import com.whisk.data.rag.embedding.EmbeddingModelManager
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class RestoreMode {
    MERGE,
    REPLACE,
}

@Serializable
data class BackupManifest(
    val version: Int = 2,
    val formatVersion: Int = 2,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val dataChecksum: String,
    val embeddingModelId: String? = null,
    val embeddingDimensions: Int? = null,
)

@Serializable
data class DocumentChunkDto(
    val id: String,
    val sourceObjectId: String,
    val sourceObjectType: String,
    val sourceTitle: String,
    val headingPath: String = "",
    val content: String,
    val contentHash: String,
    val sequence: Int,
    val startOffset: Int,
    val endOffset: Int,
    val updatedAt: Long,
)

@Serializable
data class EmbeddingVectorDto(
    val chunkId: String,
    val sourceObjectId: String,
    val modelId: String,
    val dimensions: Int,
    val vectorBlobBase64: String,
    val contentHash: String,
    val updatedAt: Long,
)

@Serializable
data class IndexingStateDto(
    val objectId: String,
    val objectType: String,
    val contentHash: String,
    val status: String,
    val chunkCount: Int,
    val lastIndexedAt: Long,
    val failureReason: String? = null,
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
    val databaseViews: List<DatabaseViewDto> = emptyList(),
    val dailyNotes: List<DailyNoteMetadataDto> = emptyList(),
    val activityEvents: List<ActivityEventDto> = emptyList(),
    val documentChunks: List<DocumentChunkDto> = emptyList(),
    val embeddingVectors: List<EmbeddingVectorDto> = emptyList(),
    val indexingStates: List<IndexingStateDto> = emptyList(),
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

@Serializable
data class DatabaseViewDto(
    val id: String,
    val targetTypeId: String?,
    val name: String,
    val viewType: String,
    val visiblePropertiesJson: String?,
    val filterConfigJson: String?,
    val sortConfigJson: String?,
    val groupPropertyId: String?,
    val datePropertyId: String?,
    val cardSize: String,
    val position: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class DailyNoteMetadataDto(
    val noteId: String,
    val dateKey: String,
    val timezone: String,
    val createdAt: Long,
)

@Serializable
data class ActivityEventDto(
    val id: String,
    val eventType: String,
    val timestamp: Long,
    val dateKey: String,
    val relatedObjectId: String?,
    val relatedObjectTitle: String?,
    val metadata: String?,
)

class ExportImportRepository(
    private val context: Context,
    private val database: WhiskDatabase,
    private val notesRepository: NotesRepository,
    private val embeddingModelManager: EmbeddingModelManager? = null,
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    companion object {
        const val MAX_ZIP_ENTRY_COUNT = 5000
        const val MAX_SINGLE_ENTRY_BYTES = 50 * 1024 * 1024L // 50 MB
        const val MAX_TOTAL_DECOMPRESSED_BYTES = 100 * 1024 * 1024L // 100 MB
        const val MAX_MANIFEST_BYTES = 1 * 1024 * 1024L // 1 MB
        const val MAX_JSON_BYTES = 50 * 1024 * 1024L // 50 MB

        fun sanitizeZipEntryName(entryName: String): String {
            val clean = entryName.trim()
            if (clean.contains("..") || clean.startsWith("/") || clean.contains("\\..\\")) {
                throw SecurityException("Security violation: Zip path traversal detected in entry '$entryName'")
            }
            if (clean.contains(":") || clean.contains("*") || clean.contains("?") || clean.contains("<") || clean.contains(">") || clean.contains("|")) {
                throw SecurityException("Security violation: Illegal characters in entry name '$entryName'")
            }
            return clean
        }
    }

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
        val activeProvider = embeddingModelManager?.getActiveProvider()
        val currentModelId = activeProvider?.modelId ?: "whisk-minilm-l6-v2-384d-v1"
        val currentDimensions = activeProvider?.dimensions ?: 384

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
        val databaseViews = database.databaseViewDao().getAllViewsDirect().map {
            DatabaseViewDto(it.id, it.targetTypeId, it.name, it.viewType, it.visiblePropertiesJson, it.filterConfigJson, it.sortConfigJson, it.groupPropertyId, it.datePropertyId, it.cardSize, it.position, it.createdAt, it.updatedAt)
        }
        val dailyNotes = database.dailyNoteDao().getAllDailyNotesDirect().map {
            DailyNoteMetadataDto(it.noteId, it.dateKey, it.timezone, it.createdAt)
        }
        val activityEvents = database.activityEventDao().getAllEventsDirect().map {
            ActivityEventDto(it.id, it.eventType, it.timestamp, it.dateKey, it.relatedObjectId, it.relatedObjectTitle, it.metadata)
        }

        val chunks = database.documentChunkDao().getAllChunksDirect().map {
            DocumentChunkDto(it.id, it.sourceObjectId, it.sourceObjectType, it.sourceTitle, it.headingPath, it.content, it.contentHash, it.sequence, it.startOffset, it.endOffset, it.updatedAt)
        }
        val vectors = database.embeddingVectorDao().getAllVectorsForModel(currentModelId).map {
            EmbeddingVectorDto(it.chunkId, it.sourceObjectId, it.modelId, it.dimensions, Base64.getEncoder().encodeToString(it.vectorBlob), it.contentHash, it.updatedAt)
        }
        val indexingStates = database.indexingStateDao().getAllIndexingStatesDirect().map {
            IndexingStateDto(it.objectId, it.objectType, it.contentHash, it.status.name, it.chunkCount, it.lastIndexedAt, it.failureReason)
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
            databaseViews = databaseViews,
            dailyNotes = dailyNotes,
            activityEvents = activityEvents,
            documentChunks = chunks,
            embeddingVectors = vectors,
            indexingStates = indexingStates,
        )
        val dataJsonString = json.encodeToString(backupData)
        val dataBytes = dataJsonString.toByteArray(Charsets.UTF_8)

        val checksum = MessageDigest.getInstance("SHA-256").digest(dataBytes)
            .joinToString("") { "%02x".format(it) }

        val manifest = BackupManifest(
            version = 2,
            formatVersion = 2,
            timestamp = System.currentTimeMillis(),
            appVersion = "1.0",
            dataChecksum = checksum,
            embeddingModelId = currentModelId,
            embeddingDimensions = currentDimensions,
        )
        val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

        ZipOutputStream(outputStream).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write(manifestBytes)
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("data.json"))
            zipOut.write(dataBytes)
            zipOut.closeEntry()

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

    suspend fun importVaultFromZip(uri: Uri, mode: RestoreMode = RestoreMode.MERGE): Int = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for Uri: $uri")
        importVaultFromInputStream(inputStream, mode)
    }

    suspend fun importVaultFromInputStream(inputStream: InputStream, mode: RestoreMode = RestoreMode.MERGE): Int = withContext(Dispatchers.IO) {
        var manifestBytes: ByteArray? = null
        var dataJsonBytes: ByteArray? = null
        var legacyImportCount = 0
        val legacyNotes = mutableListOf<Note>()

        var totalEntryCount = 0
        var totalDecompressedBytes = 0L

        ZipInputStream(inputStream).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                totalEntryCount++
                if (totalEntryCount > MAX_ZIP_ENTRY_COUNT) {
                    throw SecurityException("Security violation: ZIP contains too many entries (exceeds limit $MAX_ZIP_ENTRY_COUNT)")
                }

                val rawName = entry.name
                if (!entry.isDirectory) {
                    val entryName = sanitizeZipEntryName(rawName)
                    var bytesRead = 0L
                    val buffer = ByteArray(8192)
                    val outStream = ByteArrayOutputStream()
                    var count: Int
                    while (zipIn.read(buffer).also { count = it } != -1) {
                        outStream.write(buffer, 0, count)
                        bytesRead += count
                        totalDecompressedBytes += count

                        if (bytesRead > MAX_SINGLE_ENTRY_BYTES) {
                            throw SecurityException("Security violation: ZIP entry '$entryName' exceeds single entry limit of $MAX_SINGLE_ENTRY_BYTES bytes")
                        }
                        if (totalDecompressedBytes > MAX_TOTAL_DECOMPRESSED_BYTES) {
                            throw SecurityException("Security violation: Total decompressed bytes exceed limit of $MAX_TOTAL_DECOMPRESSED_BYTES bytes")
                        }
                    }

                    val bytes = outStream.toByteArray()

                    if (entryName == "manifest.json") {
                        if (bytes.size > MAX_MANIFEST_BYTES) {
                            throw SecurityException("Security violation: manifest.json exceeds maximum allowed size")
                        }
                        manifestBytes = bytes
                    } else if (entryName == "data.json") {
                        if (bytes.size > MAX_JSON_BYTES) {
                            throw SecurityException("Security violation: data.json exceeds maximum allowed size")
                        }
                        dataJsonBytes = bytes
                    } else if (entryName.endsWith(".md") || entryName.endsWith(".txt")) {
                        if (!entryName.startsWith("notes/")) {
                            val text = String(bytes, Charsets.UTF_8)
                            val lines = text.lines()
                            val title = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim()
                                ?: entryName.removeSuffix(".md").removeSuffix(".txt")
                            val body = if (lines.firstOrNull()?.startsWith("# ") == true) {
                                lines.drop(1).joinToString("\n").trim()
                            } else {
                                text
                            }
                            legacyNotes.add(Note(title = title, body = body, tags = listOf("imported")))
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        if (dataJsonBytes != null) {
            if (manifestBytes == null) {
                throw IllegalArgumentException("Backup archive is invalid: missing manifest.json")
            }

            val manifest = try {
                json.decodeFromString<BackupManifest>(String(manifestBytes, Charsets.UTF_8))
            } catch (e: Exception) {
                throw IllegalArgumentException("Backup archive is invalid: malformed manifest.json - ${e.message}")
            }

            val computedSha256 = MessageDigest.getInstance("SHA-256").digest(dataJsonBytes)
                .joinToString("") { "%02x".format(it) }

            if (!computedSha256.equals(manifest.dataChecksum, ignoreCase = true)) {
                throw SecurityException("Backup integrity check failed: SHA-256 checksum mismatch. Expected '${manifest.dataChecksum}', computed '$computedSha256'. Archive may be corrupted or tampered.")
            }

            val backupData = try {
                json.decodeFromString<BackupData>(String(dataJsonBytes, Charsets.UTF_8))
            } catch (e: Exception) {
                throw IllegalArgumentException("Backup archive is invalid: malformed data.json - ${e.message}")
            }

            validateBackupData(backupData)

            val activeProvider = embeddingModelManager?.getActiveProvider()
            val currentModelId = activeProvider?.modelId ?: "whisk-minilm-l6-v2-384d-v1"
            val currentDimensions = activeProvider?.dimensions ?: 384

            val isModelCompatible = (manifest.embeddingModelId == currentModelId && manifest.embeddingDimensions == currentDimensions)

            var importedCount = 0

            database.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    database.clearAllTables()
                }

                val folderDao = database.folderDao()
                val noteDao = database.noteDao()
                val canvasDao = database.canvasDao()
                val taskDao = database.taskDao()
                val chatDao = database.chatDao()
                val propertyDao = database.propertyDao()
                val objectTypeDao = database.objectTypeDao()
                val relationshipDao = database.relationshipDao()
                val databaseViewDao = database.databaseViewDao()
                val dailyNoteDao = database.dailyNoteDao()
                val activityEventDao = database.activityEventDao()
                val chunkDao = database.documentChunkDao()
                val vectorDao = database.embeddingVectorDao()
                val indexingStateDao = database.indexingStateDao()

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
                backupData.databaseViews.forEach { v ->
                    databaseViewDao.insertView(DatabaseViewEntity(v.id, v.targetTypeId, v.name, v.viewType, v.visiblePropertiesJson, v.filterConfigJson, v.sortConfigJson, v.groupPropertyId, v.datePropertyId, v.cardSize, v.position, v.createdAt, v.updatedAt))
                }
                backupData.dailyNotes.forEach { dn ->
                    dailyNoteDao.insertDailyNoteMetadata(DailyNoteMetadataEntity(dn.noteId, dn.dateKey, dn.timezone, dn.createdAt))
                }
                backupData.activityEvents.forEach { ev ->
                    activityEventDao.insertEvent(ActivityEventEntity(ev.id, ev.eventType, ev.timestamp, ev.dateKey, ev.relatedObjectId, ev.relatedObjectTitle, ev.metadata))
                }

                if (backupData.documentChunks.isNotEmpty()) {
                    val chunkEntities = backupData.documentChunks.map { c ->
                        DocumentChunkEntity(c.id, c.sourceObjectId, c.sourceObjectType, c.sourceTitle, c.headingPath, c.content, c.contentHash, c.sequence, c.startOffset, c.endOffset, c.updatedAt)
                    }
                    chunkDao.insertChunks(chunkEntities)
                }

                if (isModelCompatible && backupData.embeddingVectors.isNotEmpty()) {
                    val vectorEntities = backupData.embeddingVectors.map { v ->
                        EmbeddingVectorEntity(v.chunkId, v.sourceObjectId, v.modelId, v.dimensions, Base64.getDecoder().decode(v.vectorBlobBase64), v.contentHash, v.updatedAt)
                    }
                    vectorDao.insertVectors(vectorEntities)

                    backupData.indexingStates.forEach { st ->
                        val statusEnum = try { IndexingStatus.valueOf(st.status) } catch (_: Exception) { IndexingStatus.INDEXED }
                        indexingStateDao.insertIndexingState(IndexingStateEntity(st.objectId, st.objectType, st.contentHash, statusEnum, st.chunkCount, st.lastIndexedAt, st.failureReason))
                    }
                } else {
                    backupData.notes.forEach { n ->
                        indexingStateDao.insertIndexingState(
                            IndexingStateEntity(
                                objectId = n.id,
                                objectType = n.typeId,
                                contentHash = "",
                                status = IndexingStatus.PENDING,
                                chunkCount = 0,
                                lastIndexedAt = System.currentTimeMillis(),
                                failureReason = "Model or vector dimensions changed on restore",
                            ),
                        )
                    }
                }
            }
            importedCount
        } else {
            legacyNotes.forEach { note ->
                notesRepository.saveNote(note)
                legacyImportCount++
            }
            legacyImportCount
        }
    }

    private fun validateBackupData(data: BackupData) {
        data.notes.forEach { n ->
            if (n.id.isBlank() || n.title.isBlank()) {
                throw IllegalArgumentException("Invalid note record in backup: ID and title must not be blank")
            }
        }
        data.folders.forEach { f ->
            if (f.id.isBlank() || f.name.isBlank()) {
                throw IllegalArgumentException("Invalid folder record in backup: ID and name must not be blank")
            }
        }
    }
}
