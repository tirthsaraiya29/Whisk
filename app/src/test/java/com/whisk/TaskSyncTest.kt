package com.whisk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSyncTest {

    @Test
    fun testTaskStatusRegexReplacementInNoteBody() {
        val originalBody = """
# Sprint Checklist
- [ ] Task 1: Setup Room DB
- [ ] Task 2: Build Compose UI
- [x] Task 3: Seed welcome note
        """.trimIndent()

        val lines = originalBody.lines().toMutableList()
        val targetIndex = 1 // Task 1 line
        val line = lines[targetIndex]
        val newCheck = "- [x]"
        val updatedLine = line.replace(Regex("^\\s*-\\s*\\[([ xX])]"), newCheck)
        lines[targetIndex] = updatedLine

        val resultBody = lines.joinToString("\n")
        assertTrue(resultBody.contains("- [x] Task 1: Setup Room DB"))
        assertTrue(resultBody.contains("- [ ] Task 2: Build Compose UI"))
    }

    @Test
    fun testUncheckingTaskInNoteBody() {
        val originalBody = "- [x] Task completed"
        val line = originalBody
        val newCheck = "- [ ]"
        val updatedLine = line.replace(Regex("^\\s*-\\s*\\[([ xX])\\]"), newCheck)

        assertEquals("- [ ] Task completed", updatedLine)
    }
}
