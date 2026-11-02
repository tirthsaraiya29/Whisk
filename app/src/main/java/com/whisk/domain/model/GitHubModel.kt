package com.whisk.domain.model

data class GitHubRepo(
    val fullName: String,
    val name: String,
    val owner: String,
    val description: String?,
    val defaultBranch: String = "main",
    val isPrivate: Boolean = false,
    val stars: Int = 0,
)

data class GitHubIssue(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val author: String,
    val url: String,
)

data class GitHubPullRequest(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    val author: String,
    val url: String,
    val diffUrl: String? = null,
)

data class GitHubCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
)

sealed interface GitHubResult<out T> {
    data class Success<out T>(val data: T) : GitHubResult<T>
    data class Error(val error: GitHubError) : GitHubResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.data
}

sealed class GitHubError(open val message: String) {
    data class AuthenticationRequired(override val message: String = "Authentication required (401)") : GitHubError(message)
    data class PermissionDenied(override val message: String = "Permission denied (403)") : GitHubError(message)
    data class RateLimited(val resetTimeEpochSeconds: Long?, override val message: String = "GitHub API rate limit exceeded") : GitHubError(message)
    data class NotFound(override val message: String = "Resource not found (404)") : GitHubError(message)
    data class NetworkError(val cause: Throwable, override val message: String = "Network connection error") : GitHubError(message)
    data class ServerError(val code: Int, override val message: String = "GitHub server error ($code)") : GitHubError(message)
    data class ParsingError(val cause: Throwable, override val message: String = "Failed to parse GitHub response") : GitHubError(message)
    data class UnknownError(val code: Int?, override val message: String = "Unexpected GitHub API error") : GitHubError(message)
}

data class McpTool(
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val requiresApproval: Boolean = false,
    val serverId: String = "",
    val serverEndpoint: String = "",
)

data class McpToolLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: String,
    val status: String,
    val result: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val serverId: String = "",
    val serverEndpoint: String = "",
)
