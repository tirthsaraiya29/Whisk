package com.whisk.data.repository

import com.whisk.domain.model.GitHubCommit
import com.whisk.domain.model.GitHubError
import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.GitHubRepo
import com.whisk.domain.model.GitHubResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {

    private suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 200L,
        block: suspend () -> GitHubResult<T>,
    ): GitHubResult<T> {
        var currentDelay = initialDelayMs
        var attempts = 0
        while (true) {
            attempts++
            val result = block()
            if (result is GitHubResult.Success || attempts >= maxRetries) {
                return result
            }
            val error = (result as GitHubResult.Error).error
            val isTransient = error is GitHubError.NetworkError || error is GitHubError.ServerError
            if (!isTransient) {
                return result
            }
            val jitter = (Math.random() * 100).toLong()
            delay(currentDelay + jitter)
            currentDelay *= 2
        }
    }

    private fun parseHttpError(response: Response): GitHubError {
        val code = response.code
        val resetHeader = response.header("X-RateLimit-Reset")?.toLongOrNull()
        val remainingHeader = response.header("X-RateLimit-Remaining")?.toIntOrNull()

        return when {
            code == 401 -> GitHubError.AuthenticationRequired()
            code == 403 && (remainingHeader == 0 || resetHeader != null) ->
                GitHubError.RateLimited(resetHeader)
            code == 403 -> GitHubError.PermissionDenied()
            code == 404 -> GitHubError.NotFound()
            code in 500..599 -> GitHubError.ServerError(code)
            else -> GitHubError.UnknownError(code)
        }
    }

    suspend fun searchRepositories(
        query: String,
        token: String? = null,
        enableRetry: Boolean = true,
    ): GitHubResult<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext GitHubResult.Success(emptyList())

        val action = suspend {
            val url = "https://api.github.com/search/repositories?q=${query.trim()}&per_page=10"
            val requestBuilder = Request.Builder().url(url)
                .header("Accept", "application/vnd.github.v3+json")
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            try {
                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    GitHubResult.Error(parseHttpError(response))
                } else {
                    val bodyStr = response.body.string()
                    val json = JSONObject(bodyStr)
                    val items = json.optJSONArray("items") ?: JSONArray()
                    val repos = mutableListOf<GitHubRepo>()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        repos.add(
                            GitHubRepo(
                                fullName = item.optString("full_name", ""),
                                name = item.optString("name", ""),
                                owner = item.optJSONObject("owner")?.optString("login", "Unknown") ?: "Unknown",
                                description = item.optString("description", ""),
                                defaultBranch = item.optString("default_branch", "main"),
                                isPrivate = item.optBoolean("private", false),
                                stars = item.optInt("stargazers_count", 0),
                            ),
                        )
                    }
                    GitHubResult.Success(repos)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when (e) {
                    is IOException -> GitHubResult.Error(GitHubError.NetworkError(e))
                    is JSONException -> GitHubResult.Error(GitHubError.ParsingError(e))
                    else -> GitHubResult.Error(GitHubError.UnknownError(null, e.message ?: "Unknown error"))
                }
            }
        }

        if (enableRetry) executeWithRetry(block = action) else action()
    }

    suspend fun getRepository(
        owner: String,
        name: String,
        token: String? = null,
        enableRetry: Boolean = true,
    ): GitHubResult<GitHubRepo> = withContext(Dispatchers.IO) {
        val action = suspend {
            val url = "https://api.github.com/repos/$owner/$name"
            val requestBuilder = Request.Builder().url(url)
                .header("Accept", "application/vnd.github.v3+json")
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            try {
                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    GitHubResult.Error(parseHttpError(response))
                } else {
                    val bodyStr = response.body.string()
                    val item = JSONObject(bodyStr)
                    val repo = GitHubRepo(
                        fullName = item.optString("full_name", ""),
                        name = item.optString("name", ""),
                        owner = item.optJSONObject("owner")?.optString("login", "Unknown") ?: "Unknown",
                        description = item.optString("description", ""),
                        defaultBranch = item.optString("default_branch", "main"),
                        isPrivate = item.optBoolean("private", false),
                        stars = item.optInt("stargazers_count", 0),
                    )
                    GitHubResult.Success(repo)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when (e) {
                    is IOException -> GitHubResult.Error(GitHubError.NetworkError(e))
                    is JSONException -> GitHubResult.Error(GitHubError.ParsingError(e))
                    else -> GitHubResult.Error(GitHubError.UnknownError(null, e.message ?: "Unknown error"))
                }
            }
        }

        if (enableRetry) executeWithRetry(block = action) else action()
    }

    suspend fun getIssues(
        repoFullName: String,
        token: String? = null,
        enableRetry: Boolean = true,
    ): GitHubResult<List<GitHubIssue>> = withContext(Dispatchers.IO) {
        val action = suspend {
            val url = "https://api.github.com/repos/$repoFullName/issues?per_page=15&state=all"
            val requestBuilder = Request.Builder().url(url)
                .header("Accept", "application/vnd.github.v3+json")
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            try {
                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    GitHubResult.Error(parseHttpError(response))
                } else {
                    val bodyStr = response.body.string()
                    val array = JSONArray(bodyStr)
                    val issues = mutableListOf<GitHubIssue>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        if (obj.has("pull_request")) continue // skip PRs in issues list
                        issues.add(
                            GitHubIssue(
                                number = obj.optInt("number", 0),
                                title = obj.optString("title", ""),
                                body = obj.optString("body", ""),
                                state = obj.optString("state", "open"),
                                author = obj.optJSONObject("user")?.optString("login", "Unknown") ?: "Unknown",
                                url = obj.optString("html_url", ""),
                            ),
                        )
                    }
                    GitHubResult.Success(issues)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when (e) {
                    is IOException -> GitHubResult.Error(GitHubError.NetworkError(e))
                    is JSONException -> GitHubResult.Error(GitHubError.ParsingError(e))
                    else -> GitHubResult.Error(GitHubError.UnknownError(null, e.message ?: "Unknown error"))
                }
            }
        }

        if (enableRetry) executeWithRetry(block = action) else action()
    }

    suspend fun getPullRequests(
        repoFullName: String,
        token: String? = null,
        enableRetry: Boolean = true,
    ): GitHubResult<List<GitHubPullRequest>> = withContext(Dispatchers.IO) {
        val action = suspend {
            val url = "https://api.github.com/repos/$repoFullName/pulls?per_page=15&state=all"
            val requestBuilder = Request.Builder().url(url)
                .header("Accept", "application/vnd.github.v3+json")
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            try {
                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    GitHubResult.Error(parseHttpError(response))
                } else {
                    val bodyStr = response.body.string()
                    val array = JSONArray(bodyStr)
                    val prs = mutableListOf<GitHubPullRequest>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        prs.add(
                            GitHubPullRequest(
                                number = obj.getInt("number"),
                                title = obj.getString("title"),
                                body = obj.optString("body", ""),
                                state = obj.getString("state"),
                                author = obj.getJSONObject("user").getString("login"),
                                url = obj.getString("html_url"),
                                diffUrl = obj.optString("diff_url", ""),
                            ),
                        )
                    }
                    GitHubResult.Success(prs)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when (e) {
                    is IOException -> GitHubResult.Error(GitHubError.NetworkError(e))
                    is JSONException -> GitHubResult.Error(GitHubError.ParsingError(e))
                    else -> GitHubResult.Error(GitHubError.UnknownError(null, e.message ?: "Unknown error"))
                }
            }
        }

        if (enableRetry) executeWithRetry(block = action) else action()
    }

    suspend fun getCommits(
        repoFullName: String,
        token: String? = null,
        enableRetry: Boolean = true,
    ): GitHubResult<List<GitHubCommit>> = withContext(Dispatchers.IO) {
        val action = suspend {
            val url = "https://api.github.com/repos/$repoFullName/commits?per_page=15"
            val requestBuilder = Request.Builder().url(url)
                .header("Accept", "application/vnd.github.v3+json")
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            try {
                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    GitHubResult.Error(parseHttpError(response))
                } else {
                    val bodyStr = response.body?.string() ?: ""
                    val array = JSONArray(bodyStr)
                    val commits = mutableListOf<GitHubCommit>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val commitObj = obj.getJSONObject("commit")
                        commits.add(
                            GitHubCommit(
                                sha = obj.getString("sha").take(7),
                                message = commitObj.getString("message").lines().firstOrNull() ?: "",
                                author = commitObj.getJSONObject("author").optString("name", "Unknown"),
                                date = commitObj.getJSONObject("author").optString("date", ""),
                            ),
                        )
                    }
                    GitHubResult.Success(commits)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                when (e) {
                    is IOException -> GitHubResult.Error(GitHubError.NetworkError(e))
                    is JSONException -> GitHubResult.Error(GitHubError.ParsingError(e))
                    else -> GitHubResult.Error(GitHubError.UnknownError(null, e.message ?: "Unknown error"))
                }
            }
        }

        if (enableRetry) executeWithRetry(block = action) else action()
    }
}
