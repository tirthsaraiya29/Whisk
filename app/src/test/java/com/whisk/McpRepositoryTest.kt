package com.whisk

import com.whisk.data.repository.EndpointValidationResult
import com.whisk.data.repository.McpRepository
import com.whisk.domain.model.McpTool
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpRepositoryTest {

    @Test
    fun testEndpointValidationSecurityRules() {
        val repository = McpRepository()

        // Valid HTTPS endpoint
        val validHttps = repository.validateEndpoint("https://api.github.com/mcp")
        assertTrue("HTTPS endpoint should be valid", validHttps is EndpointValidationResult.Valid)

        // Valid local HTTP endpoint
        val validLocalHttp = repository.validateEndpoint("http://localhost:8080/mcp")
        assertTrue("Localhost HTTP endpoint should be valid", validLocalHttp is EndpointValidationResult.Valid)

        val validIpHttp = repository.validateEndpoint("http://127.0.0.1:8080/mcp")
        assertTrue("127.0.0.1 HTTP endpoint should be valid", validIpHttp is EndpointValidationResult.Valid)

        val validEmulatorHttp = repository.validateEndpoint("http://10.0.2.2:8080/mcp")
        assertTrue("10.0.2.2 HTTP endpoint should be valid", validEmulatorHttp is EndpointValidationResult.Valid)

        // Insecure remote HTTP endpoint (MUST BE REJECTED)
        val invalidRemoteHttp = repository.validateEndpoint("http://malicious-remote-server.com/mcp")
        assertTrue("Remote HTTP endpoint should be rejected", invalidRemoteHttp is EndpointValidationResult.Invalid)

        val emptyEndpoint = repository.validateEndpoint("   ")
        assertTrue("Empty endpoint should be rejected", emptyEndpoint is EndpointValidationResult.Invalid)
    }

    @Test
    fun testPendingApprovalObservableStateBeforeUiSubscription() = runBlocking {
        val repository = McpRepository()
        val endpoint = "http://localhost:8080/mcp"
        val toolName = "github_create_issue"
        val argsJson = """{"title":"Security Fix"}"""

        // Invoke tool asynchronously (will pause waiting for user approval)
        val invokeDeferred = async {
            repository.invokeTool(endpoint, toolName, argsJson, timeoutMs = 5000L)
        }

        // Simulate delayed UI subscription: state flow MUST contain the pending request!
        kotlinx.coroutines.delay(100)
        val pendingRequests = repository.pendingApprovalRequests.value
        assertEquals(1, pendingRequests.size)
        assertEquals(toolName, pendingRequests.first().toolName)

        // Resolve approval
        repository.resolveApproval(pendingRequests.first().id, approved = true)

        val result = invokeDeferred.await()
        // Result was executed (or attempted HTTP call)
        assertTrue(result.contains("Tool call failed") || result.contains("Execution error") || result.contains("successfully"))
    }

    @Test
    fun testRejectionAndTimeoutHandling() = runBlocking {
        val repository = McpRepository()
        val endpoint = "http://localhost:8080/mcp"
        val toolName = "github_delete_branch"
        val argsJson = """{"branch":"main"}"""

        val invokeDeferred = async {
            repository.invokeTool(endpoint, toolName, argsJson, timeoutMs = 1000L)
        }

        kotlinx.coroutines.delay(100)
        val pendingRequests = repository.pendingApprovalRequests.value
        assertEquals(1, pendingRequests.size)

        // User rejects execution
        repository.resolveApproval(pendingRequests.first().id, approved = false)

        val result = invokeDeferred.await()
        assertTrue("Expected rejection error message", result.contains("denied"))
    }

    @Test
    fun testMcpToolModelConstruction() {
        val tool = McpTool(
            name = "github_search_repositories",
            description = "Search public repositories",
            inputSchemaJson = """{"type":"object"}""",
            requiresApproval = false,
            serverId = "server-1",
            serverEndpoint = "https://mcp.example.com",
        )

        assertEquals("github_search_repositories", tool.name)
        assertFalse(tool.requiresApproval)
        assertEquals("server-1", tool.serverId)
        assertEquals("https://mcp.example.com", tool.serverEndpoint)
    }

    @Test
    fun testWriteToolRequiresApprovalIdentification() {
        val repository = McpRepository()
        val endpoint = "http://localhost:8080/mcp"

        val writeTools = listOf(
            "github_create_issue",
            "github_update_pull_request",
            "github_delete_branch",
            "github_push_commit",
            "github_remove_collaborator",
            "write_file",
            "transfer_ownership",
            "modify_permissions",
            "erase_database",
            "wipe_cache",
            "get_and_transfer_data",
        )
        val readTools = listOf("github_search_repositories", "github_get_issue", "github_list_commits")

        writeTools.forEach { name ->
            assertTrue("Expected $name to require approval", repository.requiresApproval(endpoint, name))
        }

        readTools.forEach { name ->
            assertFalse("Expected $name to NOT require approval", repository.requiresApproval(endpoint, name))
        }
    }

    @Test
    fun testNestedCredentialSanitizationInLogs() = runBlocking {
        val repository = McpRepository()
        val endpoint = "http://localhost:8080/mcp"

        val nestedArgs = """
            {
              "user": {
                "name": "admin",
                "secret_password": "supersecretpassword123"
              },
              "config": [
                {"auth_token": "bearer-xyz-123"}
              ]
            }
        """.trimIndent()

        // Invoke tool (rejected so it completes quickly)
        val invokeDeferred = async {
            repository.invokeTool(endpoint, "github_delete_branch", nestedArgs, timeoutMs = 1000L)
        }
        kotlinx.coroutines.delay(50)
        val pendingRequests = repository.pendingApprovalRequests.value
        if (pendingRequests.isNotEmpty()) {
            repository.resolveApproval(pendingRequests.first().id, approved = false)
        }
        invokeDeferred.await()

        val logs = repository.executionLogs.value
        assertTrue("Execution log should exist", logs.isNotEmpty())
        val logArguments = logs.first().arguments
        assertFalse("Logged arguments must not contain secret password", logArguments.contains("supersecretpassword123"))
        assertFalse("Logged arguments must not contain bearer token", logArguments.contains("bearer-xyz-123"))
        assertTrue("Logged arguments must contain masked value", logArguments.contains("******"))
    }
}
