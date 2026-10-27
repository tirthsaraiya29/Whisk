package com.whisk

import com.whisk.domain.model.DeveloperTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperTemplatesTest {

    @Test
    fun testDailyNoteTemplateGeneration() {
        val dailyNote = DeveloperTemplates.createDailyNote()

        assertTrue(dailyNote.title.startsWith("Daily Note:"))
        assertTrue(dailyNote.body.contains("## Top Focus Areas"))
        assertTrue(dailyNote.body.contains("## Today's Notes & Learnings"))
        assertTrue(dailyNote.tags.contains("daily-note"))
    }

    @Test
    fun testADRTemplateGeneration() {
        val adrNote = DeveloperTemplates.createADR("Use Room FTS5 for Offline Search")

        assertEquals("Use Room FTS5 for Offline Search", adrNote.title)
        assertTrue(adrNote.body.contains("## Status"))
        assertTrue(adrNote.body.contains("## Context & Problem Statement"))
        assertTrue(adrNote.body.contains("## Decision Outcome"))
        assertTrue(adrNote.tags.contains("adr"))
    }

    @Test
    fun testDebuggingJournalTemplateGeneration() {
        val journalNote = DeveloperTemplates.createDebuggingJournal("Compose Recomposition Loop")

        assertEquals("Compose Recomposition Loop", journalNote.title)
        assertTrue(journalNote.body.contains("## Symptoms & Error Message"))
        assertTrue(journalNote.body.contains("## Root Cause Analysis"))
        assertTrue(journalNote.tags.contains("debugging"))
    }

    @Test
    fun testCodeSnippetTemplateGeneration() {
        val snippetNote = DeveloperTemplates.createCodeSnippetNote("OkHttp Interceptor")

        assertEquals("Snippet: OkHttp Interceptor", snippetNote.title)
        assertTrue(snippetNote.body.contains("```kotlin"))
        assertTrue(snippetNote.tags.contains("snippet"))
    }
}
