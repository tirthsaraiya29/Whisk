package com.whisk

import com.whisk.data.repository.UnlinkedMentionDetector
import com.whisk.data.repository.WikilinkParser
import com.whisk.domain.model.Note
import com.whisk.domain.model.RelationshipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedBacklinksAndRelationsTest {

    @Test
    fun testWikilinkParsingWithAliases() {
        val markdown = """
Check out [[Project Alpha]] and also [[Project Alpha|Alpha Control]].
Link to non-existent [[Unknown Architecture]].
        """.trimIndent()

        val parsed = WikilinkParser.parseWikilinks(markdown)
        assertEquals(3, parsed.size)

        assertEquals("Project Alpha", parsed[0].targetTitle)
        assertEquals(null, parsed[0].alias)

        assertEquals("Project Alpha", parsed[1].targetTitle)
        assertEquals("Alpha Control", parsed[1].alias)

        assertEquals("Unknown Architecture", parsed[2].targetTitle)
    }

    @Test
    fun testWikilinkResolutionByTitleAndAlias() {
        val notes = listOf(
            Note(id = "n1", title = "Tether Architecture", body = "", aliases = listOf("Tether", "Tether Control")),
            Note(id = "n2", title = "Database Schema", body = ""),
        )

        // Resolve by exact title
        val r1 = WikilinkParser.resolveWikilink("Tether Architecture", notes)
        assertNotNull(r1)
        assertEquals("n1", r1?.id)

        // Resolve by alias
        val r2 = WikilinkParser.resolveWikilink("Tether Control", notes)
        assertNotNull(r2)
        assertEquals("n1", r2?.id)

        // Case-insensitive alias match
        val r3 = WikilinkParser.resolveWikilink("tether", notes)
        assertNotNull(r3)
        assertEquals("n1", r3?.id)
    }

    @Test
    fun testUnlinkedMentionDetectionExcludesCodeBlocksAndExistingLinks() {
        val targetNote = Note(id = "target_1", title = "Project Whisk", body = "", aliases = listOf("Whisk Engine"))
        val sourceNote = Note(
            id = "source_1",
            title = "Weekly Update",
            body = """
Mentioning Project Whisk in plain text.
Existing link: [[Project Whisk]].
Inside code block:
```kotlin
val name = "Project Whisk"
```
Also mentioning Whisk Engine here.
            """.trimIndent(),
        )

        val mentions = UnlinkedMentionDetector.findUnlinkedMentions(sourceNote, listOf(targetNote, sourceNote))
        assertTrue(mentions.isNotEmpty())

        val matchedTexts = mentions.map { it.matchedText.lowercase() }
        assertTrue(matchedTexts.contains("project whisk") || matchedTexts.contains("whisk engine"))
    }

    @Test
    fun testConvertUnlinkedMentionToWikilink() {
        val body = "Working on Project Whisk today."
        val converted = UnlinkedMentionDetector.convertMentionToWikilink(
            body = body,
            matchedText = "Project Whisk",
            targetTitle = "Project Whisk",
        )

        assertEquals("Working on [[Project Whisk]] today.", converted)

        val bodyAlias = "Working on Whisk Engine today."
        val convertedAlias = UnlinkedMentionDetector.convertMentionToWikilink(
            body = bodyAlias,
            matchedText = "Whisk Engine",
            targetTitle = "Project Whisk",
        )

        assertEquals("Working on [[Project Whisk|Whisk Engine]] today.", convertedAlias)
    }

    @Test
    fun testBidirectionalRelationshipReciprocity() {
        val relType = RelationshipType.PARENT_OF
        assertEquals("Parent of", relType.displayName)
        assertEquals("Child of", relType.reciprocalName)

        val relDepends = RelationshipType.DEPENDS_ON
        assertEquals("Depends on", relDepends.displayName)
        assertEquals("Prerequisite for", relDepends.reciprocalName)
    }
}
