package com.whisk

import com.whisk.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesRepositoryTest {

    @Test
    fun testInternalLinkRegexExtraction() {
        val body = "Refer to [[Architecture Overview]] and [[Database Schema]] for details."
        val linkRegex = Regex("\\[\\[(.*?)\\]\\]")
        val matches = linkRegex.findAll(body).map { it.groupValues[1].trim() }.toList()

        assertEquals(2, matches.size)
        assertEquals("Architecture Overview", matches[0])
        assertEquals("Database Schema", matches[1])
    }

    @Test
    fun testTaskRegexExtraction() {
        val noteBody = """
# Sprint Tasks
- [ ] Implement Room DAO
- [x] Create Jetpack Compose UI
- [X] Setup Local Gemma AI
        """.trimIndent()

        val taskRegex = Regex("^\\s*-\\s*\\[([ xX])\\]\\s*(.*)")
        val matches = noteBody.lines().mapNotNull { taskRegex.find(it) }

        assertEquals(3, matches.size)
        assertFalse(matches[0].groupValues[1].equals("x", ignoreCase = true))
        assertTrue(matches[1].groupValues[1].equals("x", ignoreCase = true))
        assertTrue(matches[2].groupValues[1].equals("x", ignoreCase = true))
        assertEquals("Implement Room DAO", matches[0].groupValues[2])
    }

    @Test
    fun testNoteDomainTagSplitting() {
        val note = Note(
            title = "Test",
            body = "Body",
            tags = listOf("android", "kotlin", "architecture")
        )
        assertEquals(3, note.tags.size)
        assertEquals("android", note.tags[0])
    }
}
