package com.whisk.data.repository

import com.whisk.domain.model.GitHubCommit
import com.whisk.domain.model.GitHubIssue
import com.whisk.domain.model.GitHubPullRequest
import com.whisk.domain.model.GitHubRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GitHubRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchRepositories(query: String, token: String? = null): List<GitHubRepo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://api.github.com/search/repositories?q=${query.trim()}&per_page=10"
        val requestBuilder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github.v3+json")
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val bodyStr = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(bodyStr)
            val items = json.optJSONArray("items") ?: JSONArray()
            val repos = mutableListOf<GitHubRepo>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                repos.add(
                    GitHubRepo(
                        fullName = item.getString("full_name"),
                        name = item.getString("name"),
                        owner = item.getJSONObject("owner").getString("login"),
                        description = item.optString("description", ""),
                        defaultBranch = item.optString("default_branch", "main"),
                        isPrivate = item.getBoolean("private"),
                        stars = item.getInt("stargazers_count")
                    )
                )
            }
            repos
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getIssues(repoFullName: String, token: String? = null): List<GitHubIssue> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$repoFullName/issues?per_page=15&state=all"
        val requestBuilder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github.v3+json")
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val bodyStr = response.body?.string() ?: return@withContext emptyList()
            val array = JSONArray(bodyStr)
            val issues = mutableListOf<GitHubIssue>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.has("pull_request")) continue // skip PRs in issues list
                issues.add(
                    GitHubIssue(
                        number = obj.getInt("number"),
                        title = obj.getString("title"),
                        body = obj.optString("body", ""),
                        state = obj.getString("state"),
                        author = obj.getJSONObject("user").getString("login"),
                        url = obj.getString("html_url")
                    )
                )
            }
            issues
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPullRequests(repoFullName: String, token: String? = null): List<GitHubPullRequest> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$repoFullName/pulls?per_page=15&state=all"
        val requestBuilder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github.v3+json")
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val bodyStr = response.body?.string() ?: return@withContext emptyList()
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
                        diffUrl = obj.optString("diff_url", "")
                    )
                )
            }
            prs
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCommits(repoFullName: String, token: String? = null): List<GitHubCommit> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$repoFullName/commits?per_page=15"
        val requestBuilder = Request.Builder().url(url)
            .header("Accept", "application/vnd.github.v3+json")
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val bodyStr = response.body?.string() ?: return@withContext emptyList()
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
                        date = commitObj.getJSONObject("author").optString("date", "")
                    )
                )
            }
            commits
        } catch (e: Exception) {
            emptyList()
        }
    }
}
