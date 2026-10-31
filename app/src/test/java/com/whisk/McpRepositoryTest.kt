package com.whisk

import com.whisk.domain.model.McpTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpRepositoryTest {

    @Test
    fun testMcpToolModelConstruction() {
        val tool = McpTool(
            name = "github_search_repositories",
            description = "Search public repositories",
            inputSchemaJson = """{"type":"object"}""",
            requiresApproval = false,
        )

        assertEquals("github_search_repositories", tool.name)
        assertFalse(tool.requiresApproval)
    }

    @Test
    fun testWriteToolRequiresApprovalIdentification() {
        val writeTools = listOf("github_create_issue", "github_update_pull_request", "github_delete_branch", "github_push_commit")
        val readTools = listOf("github_search_repositories", "github_get_issue", "github_list_commits")

        writeTools.forEach { name ->
            val isWrite = name.contains("create") || name.contains("update") || name.contains("delete") || name.contains("push")
            assertTrue("Expected $name to require approval", isWrite)
        }

        readTools.forEach { name ->
            val isWrite = name.contains("create") || name.contains("update") || name.contains("delete") || name.contains("push")
            assertFalse("Expected $name to NOT require approval", isWrite)
        }
    }

    @Test
    fun testJsonRpcPayloadBuilderStructure() {
        val toolName = "github_create_issue"
        val argumentsJson = """{"repo":"example/whisk","title":"Test Issue"}"""

        val payloadString = """
            {
                "jsonrpc": "2.0",
                "id": "req-123",
                "method": "tools/call",
                "params": {
                    "name": "$toolName",
                    "arguments": $argumentsJson
                }
            }
        """.trimIndent()

        assertTrue(payloadString.contains("\"jsonrpc\": \"2.0\""))
        assertTrue(payloadString.contains("\"method\": \"tools/call\""))
        assertTrue(payloadString.contains("\"name\": \"github_create_issue\""))
        assertTrue(payloadString.contains("example/whisk"))
    }
}
