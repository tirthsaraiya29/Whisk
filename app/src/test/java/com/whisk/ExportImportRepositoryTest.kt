package com.whisk

import android.content.ContextWrapper
import com.whisk.data.local.ActivityEventDao
import com.whisk.data.local.CanvasDao
import com.whisk.data.local.ChatDao
import com.whisk.data.local.DailyNoteDao
import com.whisk.data.local.DatabaseViewDao
import com.whisk.data.local.FolderDao
import com.whisk.data.local.NoteDao
import com.whisk.data.local.ObjectTypeDao
import com.whisk.data.local.PropertyDao
import com.whisk.data.local.RelationshipDao
import com.whisk.data.local.TaskDao
import com.whisk.data.local.WhiskDatabase
import com.whisk.data.local.rag.DocumentChunkDao
import com.whisk.data.local.rag.EmbeddingVectorDao
import com.whisk.data.local.rag.IndexingStateDao
import com.whisk.data.repository.ExportImportRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.data.repository.RestoreMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportImportRepositoryTest {

    @Test
    fun testZipSlipPathSanitization() {
        val safeEntryName = "notes/architecture_001.md"
        val maliciousEntryName = "../../../etc/passwd.md"

        val sanitizedSafe = ExportImportRepository.sanitizeZipEntryName(safeEntryName)
        assertEquals("notes/architecture_001.md", sanitizedSafe)

        try {
            ExportImportRepository.sanitizeZipEntryName(maliciousEntryName)
            fail("Expected SecurityException for path traversal attempt")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Zip path traversal") == true)
        }
    }

    @Test
    fun testModifiedDataJsonChecksumMismatchRejection() {
        val validDataJson = """{"notes":[{"id":"n1","title":"Note 1","body":"Body 1","createdAt":1000,"updatedAt":1000,"folder":null,"tags":"","isFavorite":false,"isArchived":false,"sourceMetadata":null,"repoRef":null,"aliases":"","status":"DRAFT","typeId":"note"}]}"""
        val validBytes = validDataJson.toByteArray(StandardCharsets.UTF_8)
        val validSha256 = MessageDigest.getInstance("SHA-256").digest(validBytes)
            .joinToString("") { "%02x".format(it) }

        val manifestJson = """{"version":2,"formatVersion":2,"timestamp":1000,"appVersion":"1.0","dataChecksum":"$validSha256"}"""

        val tamperedDataJson = validDataJson.replace("Note 1", "Tampered Note")

        val zipOutputStream = ByteArrayOutputStream()
        ZipOutputStream(zipOutputStream).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write(manifestJson.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("data.json"))
            zipOut.write(tamperedDataJson.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()
        }

        val zipBytes = zipOutputStream.toByteArray()

        val mockContext = ContextWrapper(null)
        val dummyDb = DummyWhiskDatabase()
        val dummyNotesRepo = NotesRepository(
            dummyDb.noteDao(),
            dummyDb.folderDao(),
            dummyDb.taskDao(),
        )

        val repo = ExportImportRepository(
            context = mockContext,
            database = dummyDb,
            notesRepository = dummyNotesRepo,
        )

        try {
            kotlinx.coroutines.runBlocking {
                repo.importVaultFromInputStream(ByteArrayInputStream(zipBytes), RestoreMode.MERGE)
            }
            fail("Expected SecurityException due to SHA-256 checksum mismatch")
        } catch (e: SecurityException) {
            assertTrue("Expected SHA-256 checksum mismatch error", e.message?.contains("SHA-256 checksum mismatch") == true)
        }
    }

    @Test
    fun testMissingManifestRejection() {
        val dataJson = """{"notes":[]}"""
        val zipOutputStream = ByteArrayOutputStream()
        ZipOutputStream(zipOutputStream).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("data.json"))
            zipOut.write(dataJson.toByteArray(StandardCharsets.UTF_8))
            zipOut.closeEntry()
        }

        val mockContext = ContextWrapper(null)
        val dummyDb = DummyWhiskDatabase()
        val dummyNotesRepo = NotesRepository(
            dummyDb.noteDao(),
            dummyDb.folderDao(),
            dummyDb.taskDao(),
        )

        val repo = ExportImportRepository(
            context = mockContext,
            database = dummyDb,
            notesRepository = dummyNotesRepo,
        )

        try {
            kotlinx.coroutines.runBlocking {
                repo.importVaultFromInputStream(ByteArrayInputStream(zipOutputStream.toByteArray()), RestoreMode.MERGE)
            }
            fail("Expected IllegalArgumentException due to missing manifest.json")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("missing manifest.json") == true)
        }
    }
}

class DummyWhiskDatabase : WhiskDatabase() {
    private val dummyProxy = java.lang.reflect.Proxy.newProxyInstance(
        NoteDao::class.java.classLoader,
        arrayOf(
            NoteDao::class.java,
            FolderDao::class.java,
            TaskDao::class.java,
            CanvasDao::class.java,
            ChatDao::class.java,
            PropertyDao::class.java,
            ObjectTypeDao::class.java,
            RelationshipDao::class.java,
            DatabaseViewDao::class.java,
            DailyNoteDao::class.java,
            ActivityEventDao::class.java,
            DocumentChunkDao::class.java,
            EmbeddingVectorDao::class.java,
            IndexingStateDao::class.java,
        ),
    ) { _, _, _ -> null }

    override fun noteDao(): NoteDao = dummyProxy as NoteDao
    override fun folderDao(): FolderDao = dummyProxy as FolderDao
    override fun canvasDao(): CanvasDao = dummyProxy as CanvasDao
    override fun taskDao(): TaskDao = dummyProxy as TaskDao
    override fun chatDao(): ChatDao = dummyProxy as ChatDao
    override fun propertyDao(): PropertyDao = dummyProxy as PropertyDao
    override fun objectTypeDao(): ObjectTypeDao = dummyProxy as ObjectTypeDao
    override fun relationshipDao(): RelationshipDao = dummyProxy as RelationshipDao
    override fun databaseViewDao(): DatabaseViewDao = dummyProxy as DatabaseViewDao
    override fun dailyNoteDao(): DailyNoteDao = dummyProxy as DailyNoteDao
    override fun activityEventDao(): ActivityEventDao = dummyProxy as ActivityEventDao
    override fun documentChunkDao(): DocumentChunkDao = dummyProxy as DocumentChunkDao
    override fun embeddingVectorDao(): EmbeddingVectorDao = dummyProxy as EmbeddingVectorDao
    override fun indexingStateDao(): IndexingStateDao = dummyProxy as IndexingStateDao
    override fun clearAllTables() {}
    override fun createInvalidationTracker(): androidx.room.InvalidationTracker {
        return java.lang.reflect.Proxy.newProxyInstance(
            androidx.room.InvalidationTracker::class.java.classLoader,
            arrayOf(androidx.room.InvalidationTracker::class.java),
        ) { _, _, _ -> null } as androidx.room.InvalidationTracker
    }
    @Deprecated("Deprecated in Java")
    override fun createOpenHelper(config: androidx.room.DatabaseConfiguration): androidx.sqlite.db.SupportSQLiteOpenHelper {
        return java.lang.reflect.Proxy.newProxyInstance(
            androidx.sqlite.db.SupportSQLiteOpenHelper::class.java.classLoader,
            arrayOf(androidx.sqlite.db.SupportSQLiteOpenHelper::class.java),
        ) { _, _, _ -> null } as androidx.sqlite.db.SupportSQLiteOpenHelper
    }
}
