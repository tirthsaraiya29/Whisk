package com.whisk.git

import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NoteRevision(
    val commitId: String,
    val noteId: String,
    val timestamp: Long,
    val message: String,
    val title: String,
    val contentSnippet: String,
)

data class DiffLine(
    val type: DiffType,
    val text: String,
)

enum class DiffType {
    ADDED, REMOVED, UNCHANGED
}

class GitVaultManager(
    private val storageDir: File,
    private val notesRepository: NotesRepository,
) {

    private val revisionsDir = File(storageDir, "git_revisions").apply { mkdirs() }

    suspend fun commitNoteChange(note: Note, message: String = "Update note"): NoteRevision = withContext(Dispatchers.IO) {
        val commitId = UUID.randomUUID().toString().take(8)
        val noteDir = File(revisionsDir, note.id).apply { mkdirs() }
        val commitFile = File(noteDir, "$commitId.md")

        val payload = buildString {
            append("---").append("\n")
            append("commitId: ").append(commitId).append("\n")
            append("noteId: ").append(note.id).append("\n")
            append("title: ").append(note.title).append("\n")
            append("timestamp: ").append(System.currentTimeMillis()).append("\n")
            append("message: ").append(message).append("\n")
            append("---").append("\n")
            append(note.body)
        }
        commitFile.writeText(payload, Charsets.UTF_8)

        NoteRevision(
            commitId = commitId,
            noteId = note.id,
            timestamp = System.currentTimeMillis(),
            message = message,
            title = note.title,
            contentSnippet = note.body.take(80),
        )
    }

    suspend fun getRevisionHistory(noteId: String): List<NoteRevision> = withContext(Dispatchers.IO) {
        val noteDir = File(revisionsDir, noteId)
        if (!noteDir.exists()) return@withContext emptyList()

        noteDir.listFiles { f -> f.extension == "md" }?.mapNotNull { file ->
            try {
                val lines = file.readLines()
                val commitId = file.nameWithoutExtension
                val title = lines.find { it.startsWith("title: ") }?.removePrefix("title: ") ?: "Untitled"
                val ts = lines.find { it.startsWith("timestamp: ") }?.removePrefix("timestamp: ")?.toLongOrNull() ?: file.lastModified()
                val msg = lines.find { it.startsWith("message: ") }?.removePrefix("message: ") ?: "Commit"
                val body = lines.dropWhile { it != "---" }.drop(1).dropWhile { it != "---" }.drop(1).joinToString("\n")

                NoteRevision(
                    commitId = commitId,
                    noteId = noteId,
                    timestamp = ts,
                    message = msg,
                    title = title,
                    contentSnippet = body.take(80),
                )
            } catch (_: Exception) {
                null
            }
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }

    suspend fun calculateDiff(oldText: String, newText: String): List<DiffLine> = withContext(Dispatchers.Default) {
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        val diff = mutableListOf<DiffLine>()

        val oldSet = oldLines.toSet()
        val newSet = newLines.toSet()

        oldLines.forEach { line ->
            if (line !in newSet) {
                diff.add(DiffLine(DiffType.REMOVED, line))
            } else {
                diff.add(DiffLine(DiffType.UNCHANGED, line))
            }
        }

        newLines.forEach { line ->
            if (line !in oldSet) {
                diff.add(DiffLine(DiffType.ADDED, line))
            }
        }

        diff
    }

    suspend fun revertToCommit(noteId: String, commitId: String): Note? = withContext(Dispatchers.IO) {
        val commitFile = File(revisionsDir, "$noteId/$commitId.md")
        if (!commitFile.exists()) return@withContext null

        val lines = commitFile.readLines()
        val title = lines.find { it.startsWith("title: ") }?.removePrefix("title: ") ?: "Untitled"
        val body = lines.dropWhile { it != "---" }.drop(1).dropWhile { it != "---" }.drop(1).joinToString("\n")

        val current = notesRepository.getNoteById(noteId)
        val reverted = current?.copy(
            title = title,
            body = body,
            updatedAt = System.currentTimeMillis(),
        ) ?: Note(
            id = noteId,
            title = title,
            body = body,
            updatedAt = System.currentTimeMillis(),
        )

        notesRepository.saveNote(reverted)
        commitNoteChange(reverted, "Revert to commit $commitId")
        reverted
    }
}
