package com.whisk.domain.model

data class GitHubRepo(
    val fullName: String,
    val name: String,
    val owner: String,
    val description: String?,
    val defaultBranch: String = "main",
    val isPrivate: Boolean = false,
    val stars: Int = 0
)

data class GitHubIssue(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val author: String,
    val url: String
)

data class GitHubPullRequest(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val author: String,
    val url: String,
    val diffUrl: String? = null
)

data class GitHubCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String
)

data class McpTool(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val requiresApproval: Boolean = false
)

data class McpToolLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: String,
    val status: String,
    val result: String?,
    val timestamp: Long = System.currentTimeMillis()
)
