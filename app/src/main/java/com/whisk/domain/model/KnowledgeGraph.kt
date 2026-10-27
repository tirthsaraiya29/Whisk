package com.whisk.domain.model

enum class NodeType {
    NOTE, TAG, FOLDER, GITHUB_REPO, GITHUB_ISSUE, GITHUB_PR, SOURCE_FILE
}

enum class EdgeType {
    EXPLICIT_LINK, BACKLINK, TAG_MEMBERSHIP, FOLDER_PARENT, CODE_DEPENDENCY, AI_SUGGESTED
}

data class GraphNode(
    val id: String,
    val label: String,
    val type: NodeType,
    val subtitle: String? = null,
    val isOrphan: Boolean = false,
    val isArchived: Boolean = false,
    val noteId: String? = null
)

data class GraphEdge(
    val sourceId: String,
    val targetId: String,
    val type: EdgeType,
    val label: String? = null
)

data class KnowledgeGraph(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)
