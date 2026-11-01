package com.whisk.ai

import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.Note
import com.whisk.domain.model.RelationshipDetail
import com.whisk.domain.rag.retrieval.RetrievedChunk

data class CitationReference(
    val index: Int,
    val sourceObjectId: String,
    val sourceObjectType: String,
    val sourceTitle: String,
    val headingPath: String,
    val excerpt: String,
)

data class ContextPayload(
    val activeNote: Note? = null,
    val selectedNotes: List<Note> = emptyList(),
    val relatedObjects: List<Note> = emptyList(),
    val relationships: List<RelationshipDetail> = emptyList(),
    val selectedIssue: GitHubIssue? = null,
    val selectedPullRequest: GitHubPullRequest? = null,
    val additionalSnippet: String? = null,
    val retrievedChunks: List<RetrievedChunk> = emptyList(),
    val maxTokenBudget: Int = 2500,
) {
    fun formatContextWithCitations(): Pair<String, List<CitationReference>> {
        val builder = StringBuilder()
        val citationRefs = mutableListOf<CitationReference>()

        // 1. Vault RAG Retrieved Chunks
        if (retrievedChunks.isNotEmpty()) {
            builder.append("### Vault Knowledge Retrieval (RAG Evidence):\n")
            builder.append("Instruction: Answer using this evidence. Use citation markers e.g. [1], [2] corresponding to the sources below.\n\n")

            var currentLength = 0
            val maxCharBudget = maxTokenBudget * 4

            retrievedChunks.forEachIndexed { index, chunk ->
                val citeIndex = index + 1
                val chunkHeader = "Source [$citeIndex]: ${chunk.sourceTitle} [Type: ${chunk.sourceObjectType.uppercase()}] > ${chunk.headingPath} (ID: ${chunk.sourceObjectId})\n"
                val chunkContent = "${chunk.content.trim()}\n\n"
                val entryLength = chunkHeader.length + chunkContent.length

                if (currentLength + entryLength <= maxCharBudget) {
                    builder.append(chunkHeader)
                    builder.append(chunkContent)
                    citationRefs.add(
                        CitationReference(
                            index = citeIndex,
                            sourceObjectId = chunk.sourceObjectId,
                            sourceObjectType = chunk.sourceObjectType,
                            sourceTitle = chunk.sourceTitle,
                            headingPath = chunk.headingPath,
                            excerpt = chunk.content.take(150),
                        ),
                    )
                    currentLength += entryLength
                }
            }
        }

        // 2. Active note if present
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

        // 3. Relationships
        if (relationships.isNotEmpty()) {
            builder.append("### Object Relationships:\n")
            relationships.forEach { rel ->
                builder.append("- ").append(rel.displayLabel).append(" -> ").append(rel.otherObjectTitle)
                    .append(" [Type: ").append(rel.otherObjectType.uppercase()).append(", Ref: ").append(rel.otherObjectId).append("]\n")
            }
            builder.append("\n")
        }

        // 4. Related objects
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

        return Pair(builder.toString().trim(), citationRefs)
    }

    fun formatContext(): String {
        return formatContextWithCitations().first
    }

    fun estimateTokenCount(): Int {
        val text = formatContext()
        return text.length / 4
    }
}
