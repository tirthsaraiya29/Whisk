package com.whisk

import com.whisk.ai.ContextPayload
import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiContextAssemblerTest {

    @Test
    fun testContextFormattingWithActiveNote() {
        val note = Note(
            title = "Room Database Architecture",
            body = "Room uses SQLite under the hood with reactive StateFlow flows."
        )

        val payload = ContextPayload(activeNote = note)
        val formatted = payload.formatContext()

        assertTrue(formatted.contains("### Active Note: Room Database Architecture"))
        assertTrue(formatted.contains("Room uses SQLite under the hood"))
    }

    @Test
    fun testContextFormattingWithGitHubIssueAndPR() {
        val issue = GitHubIssue(
            number = 42,
            title = "Fix memory leak in LiteRT-LM model unloading",
            body = "Native handles were not freed on process pause.",
            state = "open",
            author = "android-dev",
            url = "https://github.com/example/repo/issues/42"
        )
        val pr = GitHubPullRequest(
            number = 43,
            title = "Fix LiteRT-LM memory leak",
            body = "Replaced static reference with weak reference.",
            state = "open",
            author = "android-dev",
            url = "https://github.com/example/repo/pull/43"
        )

        val payload = ContextPayload(selectedIssue = issue, selectedPullRequest = pr)
        val formatted = payload.formatContext()

        assertTrue(formatted.contains("GitHub Issue #42"))
        assertTrue(formatted.contains("GitHub Pull Request #43"))
    }

    @Test
    fun testTokenEstimation() {
        val note = Note(title = "Title", body = "A short test note body text string.")
        val payload = ContextPayload(activeNote = note)
        val estimatedTokens = payload.estimateTokenCount()

        assertTrue(estimatedTokens > 0)
        assertEquals(payload.formatContext().length / 4, estimatedTokens)
    }
}
