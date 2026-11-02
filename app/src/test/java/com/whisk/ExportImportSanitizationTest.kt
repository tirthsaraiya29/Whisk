package com.whisk

import com.whisk.data.repository.ExportImportRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportImportSanitizationTest {

    @Test
    fun testValidZipEntryName() {
        val safeEntryName = "notes/architecture_001.md"
        val result = ExportImportRepository.sanitizeZipEntryName(safeEntryName)
        assertEquals("notes/architecture_001.md", result)
    }

    @Test(expected = SecurityException::class)
    fun testZipPathTraversalRejected() {
        ExportImportRepository.sanitizeZipEntryName("../../../etc/passwd.md")
    }

    @Test(expected = SecurityException::class)
    fun testZipBackslashTraversalRejected() {
        ExportImportRepository.sanitizeZipEntryName("notes\\..\\secret.txt")
    }

    @Test(expected = SecurityException::class)
    fun testZipAbsoluteWindowsPathRejected() {
        ExportImportRepository.sanitizeZipEntryName("C:\\Windows\\System32\\cmd.exe")
    }

    @Test(expected = SecurityException::class)
    fun testZipLeadingSlashPathRejected() {
        ExportImportRepository.sanitizeZipEntryName("/etc/passwd")
    }
}
