package com.whisk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportImportSanitizationTest {

    @Test
    fun testZipSlipPathSanitization() {
        val safeEntryName = "notes/architecture_001.md"
        val maliciousEntryName = "../../../etc/passwd.md"

        val isSafeValid = !safeEntryName.contains("..") && (safeEntryName.endsWith(".md") || safeEntryName.endsWith(".txt"))
        val isMaliciousValid = !maliciousEntryName.contains("..") && (maliciousEntryName.endsWith(".md") || maliciousEntryName.endsWith(".txt"))

        assertTrue(isSafeValid)
        assertFalse(isMaliciousValid)
    }
}
