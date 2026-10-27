package com.whisk.ai

import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.Note

data class ContextPayload(
    val activeNote: Note? = null,
    val selectedNotes: List<Note> = emptyList(),
    val selectedIssue: GitHubIssue? = null,
    val selectedPullRequest: GitHubPullRequest? = null,
    val additionalSnippet: String? = null
) {
    fun formatContext(): String {
        val builder = StringBuilder()
        if (activeNote != null) {
            builder.append("### Active Note: ").append(activeNote.title).append("\n")
            builder.append(activeNote.body).append("\n\n")
        }
        if (selectedNotes.isNotEmpty()) {
            builder.append("### Linked / Selected Vault Notes:\n")
            selectedNotes.forEach { n ->
                builder.append("#### Note: ").append(n.title).append("\n")
                builder.append(n.body).append("\n\n")
            }
        }
        if (selectedIssue != null) {
            builder.append("### GitHub Issue #").append(selectedIssue.number).append(": ").append(selectedIssue.title).append("\n")
            builder.append(selectedIssue.body ?: "").append("\n\n")
        }
        if (selectedPullRequest != null) {
            builder.append("### GitHub Pull Request #").append(selectedPullRequest.number).append(": ").append(selectedPullRequest.title).append("\n")
            builder.append(selectedPullRequest.body ?: "").append("\n\n")
        }
        if (!additionalSnippet.isNullOrBlank()) {
            builder.append("### Code / Excerpt Snippet:\n")
            builder.append(additionalSnippet).append("\n\n")
        }
        return builder.toString().trim()
    }

    fun estimateTokenCount(): Int {
        val text = formatContext()
        return text.length / 4
    }
}
