package com.whisk.ai

import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.Note
import com.whisk.domain.model.RelationshipDetail

data class ContextPayload(
    val activeNote: Note? = null,
    val selectedNotes: List<Note> = emptyList(),
    val relatedObjects: List<Note> = emptyList(),
    val relationships: List<RelationshipDetail> = emptyList(),
    val selectedIssue: GitHubIssue? = null,
    val selectedPullRequest: GitHubPullRequest? = null,
    val additionalSnippet: String? = null,
) {
    fun formatContext(): String {
        val builder = StringBuilder()

        if (activeNote != null) {
            builder.append("### Active Note: ").append(activeNote.title).append("\n")
            builder.append("Type: ").append(activeNote.typeId.uppercase()).append("\n")
            if (!activeNote.description.isNullOrBlank()) {
                builder.append("Description: ").append(activeNote.description).append("\n")
            }
            if (!activeNote.status.isNullOrBlank()) {
                builder.append("Status: ").append(activeNote.status).append("\n")
            }
            if (!activeNote.repoRef.isNullOrBlank()) {
                builder.append("Linked Repo: ").append(activeNote.repoRef).append("\n")
            }
            builder.append("Content:\n").append(activeNote.body).append("\n\n")
        }

        if (relationships.isNotEmpty()) {
            builder.append("### Object Relationships:\n")
            relationships.forEach { rel ->
                builder.append("- ").append(rel.displayLabel).append(" -> ").append(rel.otherObjectTitle)
                    .append(" [Type: ").append(rel.otherObjectType.uppercase()).append(", Ref: ").append(rel.otherObjectId).append("]\n")
            }
            builder.append("\n")
        }

        if (relatedObjects.isNotEmpty()) {
            builder.append("### Linked / Related Vault Objects:\n")
            relatedObjects.forEach { obj ->
                builder.append("#### Object: ").append(obj.title).append(" [Type: ").append(obj.typeId.uppercase()).append("]\n")
                if (!obj.description.isNullOrBlank()) {
                    builder.append("Summary: ").append(obj.description).append("\n")
                }
                builder.append(obj.body.take(500)).append("\n\n")
            }
        }

        if (selectedIssue != null) {
            builder.append("### GitHub Issue #").append(selectedIssue.number).append(": ").append(selectedIssue.title).append("\n")
            builder.append(selectedIssue.body.orEmpty()).append("\n\n")
        }

        if (selectedPullRequest != null) {
            builder.append("### GitHub Pull Request #").append(selectedPullRequest.number).append(": ").append(selectedPullRequest.title).append("\n")
            builder.append(selectedPullRequest.body.orEmpty()).append("\n\n")
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
