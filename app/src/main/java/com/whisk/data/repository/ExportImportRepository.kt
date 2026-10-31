package com.whisk.data.repository

import android.content.Context
import android.net.Uri
import com.whisk.data.local.NoteDao
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExportImportRepository(
    private val context: Context,
    private val noteDao: NoteDao,
    private val notesRepository: NotesRepository,
) {

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
        val notes = noteDao.getAllNotesDirect()
        ZipOutputStream(outputStream).use { zipOut ->
            notes.forEach { (id, title, body, _, _, _, tags) ->
                val safeTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val fileName = "${safeTitle}_${id.take(6)}.md"
                val entry = ZipEntry(fileName)
                zipOut.putNextEntry(entry)
                val content = buildString {
                    append("# ").append(title).append("\n\n")
                    if (tags.isNotBlank()) {
                        append("Tags: ").append(tags).append("\n\n")
                    }
                    append(body)
                }
                zipOut.write(content.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
    }

    suspend fun importVaultFromZip(uri: Uri): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext 0
        ZipInputStream(inputStream).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                val entryName = entry.name
                // Security check: prevent directory traversal attacks (Zip Slip)
                if (!entry.isDirectory && !entryName.contains("..") && (entryName.endsWith(".md") || entryName.endsWith(".txt"))) {
                    val bytes = zipIn.readBytes()
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
                    importedCount++
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        importedCount
    }
}
