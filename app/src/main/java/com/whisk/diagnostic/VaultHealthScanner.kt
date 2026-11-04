package com.whisk.diagnostic

import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

data class VaultHealthIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val affectedNoteIds: List<String> = emptyList(),
    val targetTitle: String? = null
)

enum class IssueType {
    BROKEN_LINK,
    ORPHAN_NOTE,
    DUPLICATE_TITLE,
    CIRCULAR_REFERENCE,
    EMPTY_STUB
}

enum class IssueSeverity {
    CRITICAL,
    WARNING,
    INFO
}

data class VaultHealthReport(
    val healthScore: Int, // 0 - 100
    val totalNotes: Int,
    val totalLinks: Int,
    val brokenLinks: List<VaultHealthIssue>,
    val orphanNotes: List<VaultHealthIssue>,
    val duplicateTitles: List<VaultHealthIssue>,
    val circularReferences: List<VaultHealthIssue>,
    val emptyStubs: List<VaultHealthIssue>,
    val scannedAt: Long = System.currentTimeMillis()
) {
    val totalIssues: Int
        get() = brokenLinks.size + orphanNotes.size + duplicateTitles.size + circularReferences.size + emptyStubs.size

    val isClean: Boolean
        get() = totalIssues == 0
}

class VaultHealthScanner(
    private val notesRepository: NotesRepository
) {
    private val wikilinkRegex = Pattern.compile("\\[\\[([^\\]|#]+)(?:[|#][^\\]]*)?\\]\\]")

    suspend fun scan(): VaultHealthReport = withContext(Dispatchers.Default) {
        val notes = notesRepository.getAllNotesDirect()
        val totalNotes = notes.size

        if (totalNotes == 0) {
            return@withContext VaultHealthReport(
                healthScore = 100,
                totalNotes = 0,
                totalLinks = 0,
                brokenLinks = emptyList(),
                orphanNotes = emptyList(),
                duplicateTitles = emptyList(),
                circularReferences = emptyList(),
                emptyStubs = emptyList()
            )
        }

        val titleToNoteMap = mutableMapOf<String, MutableList<Note>>()
        val outgoingLinksMap = mutableMapOf<String, MutableSet<String>>() // noteId -> Set<normalized target title>
        val incomingLinksMap = mutableMapOf<String, MutableSet<String>>() // normalized note title -> Set<source noteId>
        var totalLinks = 0

        val emptyStubs = mutableListOf<VaultHealthIssue>()
        val duplicateTitles = mutableListOf<VaultHealthIssue>()

        // 1. Index titles and detect empty stubs
        notes.forEach { note ->
            val normTitle = note.title.trim().lowercase()
            titleToNoteMap.getOrPut(normTitle) { mutableListOf() }.add(note)

            val bodyTrimmed = note.body.trim()
            if (bodyTrimmed.isEmpty() || bodyTrimmed == "# ${note.title}") {
                emptyStubs.add(
                    VaultHealthIssue(
                        type = IssueType.EMPTY_STUB,
                        severity = IssueSeverity.INFO,
                        title = "Empty Note Stub",
                        description = "Note \"${note.title}\" has no content.",
                        affectedNoteIds = listOf(note.id)
                    )
                )
            }

            // Extract wikilinks from body
            val matcher = wikilinkRegex.matcher(note.body)
            val outgoing = outgoingLinksMap.getOrPut(note.id) { mutableSetOf() }
            while (matcher.find()) {
                val targetTitle = matcher.group(1)?.trim()
                if (!targetTitle.isNullOrEmpty()) {
                    outgoing.add(targetTitle.lowercase())
                    incomingLinksMap.getOrPut(targetTitle.lowercase()) { mutableSetOf() }.add(note.id)
                    totalLinks++
                }
            }
        }

        // 2. Detect duplicate titles
        titleToNoteMap.forEach { (normTitle, noteGroup) ->
            if (noteGroup.size > 1) {
                duplicateTitles.add(
                    VaultHealthIssue(
                        type = IssueType.DUPLICATE_TITLE,
                        severity = IssueSeverity.WARNING,
                        title = "Duplicate Note Title",
                        description = "${noteGroup.size} notes share the title \"${noteGroup.first().title}\".",
                        affectedNoteIds = noteGroup.map { it.id },
                        targetTitle = noteGroup.first().title
                    )
                )
            }
        }

        // 3. Detect broken wikilinks
        val brokenLinks = mutableListOf<VaultHealthIssue>()
        outgoingLinksMap.forEach { (sourceId, targets) ->
            val sourceNote = notes.find { it.id == sourceId } ?: return@forEach
            targets.forEach { targetNormTitle ->
                if (!titleToNoteMap.containsKey(targetNormTitle)) {
                    brokenLinks.add(
                        VaultHealthIssue(
                            type = IssueType.BROKEN_LINK,
                            severity = IssueSeverity.CRITICAL,
                            title = "Unresolved Wikilink",
                            description = "Link to \"$targetNormTitle\" in \"${sourceNote.title}\" targets a missing note.",
                            affectedNoteIds = listOf(sourceId),
                            targetTitle = targetNormTitle
                        )
                    )
                }
            }
        }

        // 4. Detect orphan notes (0 incoming, 0 outgoing)
        val orphanNotes = mutableListOf<VaultHealthIssue>()
        notes.forEach { note ->
            val normTitle = note.title.trim().lowercase()
            val hasOutgoing = (outgoingLinksMap[note.id]?.size ?: 0) > 0
            val hasIncoming = (incomingLinksMap[normTitle]?.size ?: 0) > 0

            if (!hasOutgoing && !hasIncoming && !note.isArchived) {
                orphanNotes.add(
                    VaultHealthIssue(
                        type = IssueType.ORPHAN_NOTE,
                        severity = IssueSeverity.INFO,
                        title = "Orphan Note",
                        description = "Note \"${note.title}\" has no incoming or outgoing connections.",
                        affectedNoteIds = listOf(note.id)
                    )
                )
            }
        }

        // 5. Detect circular references (A links to B, and B links to A)
        val circularReferences = mutableListOf<VaultHealthIssue>()
        val processedPairs = mutableSetOf<String>()
        notes.forEach { noteA ->
            val targetsA = outgoingLinksMap[noteA.id] ?: emptySet()
            targetsA.forEach { targetBTitle ->
                val noteBGroup = titleToNoteMap[targetBTitle] ?: emptyList()
                noteBGroup.forEach { noteB ->
                    if (noteA.id != noteB.id) {
                        val targetsB = outgoingLinksMap[noteB.id] ?: emptySet()
                        if (targetsB.contains(noteA.title.trim().lowercase())) {
                            val pairKey = listOf(noteA.id, noteB.id).sorted().joinToString("<->")
                            if (processedPairs.add(pairKey)) {
                                circularReferences.add(
                                    VaultHealthIssue(
                                        type = IssueType.CIRCULAR_REFERENCE,
                                        severity = IssueSeverity.INFO,
                                        title = "Mutual Reference",
                                        description = "\"${noteA.title}\" and \"${noteB.title}\" reference each other directly.",
                                        affectedNoteIds = listOf(noteA.id, noteB.id)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compute score
        val penalty = (brokenLinks.size * 8) + (duplicateTitles.size * 5) + (orphanNotes.size * 2) + (emptyStubs.size * 1)
        val healthScore = maxOf(0, 100 - penalty)

        VaultHealthReport(
            healthScore = healthScore,
            totalNotes = totalNotes,
            totalLinks = totalLinks,
            brokenLinks = brokenLinks,
            orphanNotes = orphanNotes,
            duplicateTitles = duplicateTitles,
            circularReferences = circularReferences,
            emptyStubs = emptyStubs
        )
    }

    suspend fun repairBrokenLinks(missingTitles: List<String>): Int = withContext(Dispatchers.IO) {
        var created = 0
        missingTitles.distinct().forEach { title ->
            if (notesRepository.getNoteByTitle(title) == null) {
                notesRepository.createNoteFromBrokenLink(title)
                created++
            }
        }
        created
    }

    suspend fun deduplicateTitles(conflictingNoteIds: List<String>): Int = withContext(Dispatchers.IO) {
        var renamed = 0
        val notesToRename = conflictingNoteIds.drop(1) // Keep first one intact
        notesToRename.forEachIndexed { index, noteId ->
            val note = notesRepository.getNoteById(noteId)
            if (note != null) {
                val newTitle = "${note.title} (${index + 1})"
                notesRepository.saveNote(note.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
                renamed++
            }
        }
        renamed
    }

    suspend fun archiveOrphans(orphanNoteIds: List<String>): Int = withContext(Dispatchers.IO) {
        var archived = 0
        orphanNoteIds.forEach { noteId ->
            val note = notesRepository.getNoteById(noteId)
            if (note != null && !note.isArchived) {
                notesRepository.saveNote(note.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
                archived++
            }
        }
        archived
    }

    suspend fun purgeEmptyStubs(stubNoteIds: List<String>): Int = withContext(Dispatchers.IO) {
        var purged = 0
        stubNoteIds.forEach { noteId ->
            notesRepository.softDeleteNote(noteId)
            purged++
        }
        purged
    }
}
