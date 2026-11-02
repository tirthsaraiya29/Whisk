package com.whisk

import com.whisk.data.repository.GitHubRepository
import com.whisk.domain.model.GitHubError
import com.whisk.domain.model.GitHubRepo
import com.whisk.domain.model.GitHubResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GitHubRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createFakeHttpClient(
        responseCode: Int,
        responseBody: String,
        headers: Map<String, String> = emptyMap(),
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .dns { listOf(java.net.InetAddress.getLoopbackAddress()) }
            .addInterceptor { chain ->
                val request = chain.request()
                val builder = Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(responseCode)
                    .message("OK")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))

                headers.forEach { (k, v) -> builder.addHeader(k, v) }
                builder.build()
            }
            .build()
    }

    @Test
    fun testResultSuccess() {
        val repo = GitHubRepo(
            fullName = "whisk/android",
            name = "android",
            owner = "whisk",
            description = "Whisk App",
            stars = 42,
        )
        val result: GitHubResult<GitHubRepo> = GitHubResult.Success(repo)

        assertEquals("whisk/android", result.getOrNull()?.fullName)
    }

    @Test
    fun testAuthenticationRequired401() = runTest {
        val repo = GitHubRepository(client = createFakeHttpClient(401, "{}"))
        val result = repo.searchRepositories("android", enableRetry = false)

        assertTrue(result is GitHubResult.Error)
        val error = (result as GitHubResult.Error).error
        assertTrue(error is GitHubError.AuthenticationRequired)
    }

    @Test
    fun testRateLimit403() = runTest {
        val repo = GitHubRepository(
            client = createFakeHttpClient(
                responseCode = 403,
                responseBody = "Rate Limit",
                headers = mapOf("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "1700000000"),
            ),
        )
        val result = repo.searchRepositories("android", enableRetry = false)

        assertTrue(result is GitHubResult.Error)
        val error = (result as GitHubResult.Error).error
        assertTrue(error is GitHubError.RateLimited)
        assertEquals(1700000000L, (error as GitHubError.RateLimited).resetTimeEpochSeconds)
    }

    @Test
    fun testNotFound404() = runTest {
        val repo = GitHubRepository(client = createFakeHttpClient(404, "{}"))
        val result = repo.getRepository("whisk", "nonexistent", enableRetry = false)

        assertTrue(result is GitHubResult.Error)
        val error = (result as GitHubResult.Error).error
        assertTrue(error is GitHubError.NotFound)
    }

    @Test
    fun testServerError500() = runTest {
        val repo = GitHubRepository(client = createFakeHttpClient(500, "Internal Error"))
        val result = repo.getIssues("whisk/android", enableRetry = false)

        assertTrue(result is GitHubResult.Error)
        val error = (result as GitHubResult.Error).error
        assertTrue(error is GitHubError.ServerError)
        assertEquals(500, (error as GitHubError.ServerError).code)
    }

    @Test
    fun testCancellationExceptionIsRethrown() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor {
                throw CancellationException("Operation cancelled")
            }
            .build()

        val repo = GitHubRepository(client = client)

        try {
            repo.searchRepositories("android", enableRetry = false)
            assertTrue("Should have thrown CancellationException", false)
        } catch (e: Exception) {
            assertTrue(e is CancellationException)
        }
    }
}
