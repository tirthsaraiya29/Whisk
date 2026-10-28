package com.whisk.data.repository

import com.whisk.domain.model.McpTool
import com.whisk.domain.model.McpToolLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class McpRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _discoveredTools = MutableStateFlow<List<McpTool>>(emptyList())
    val discoveredTools: StateFlow<List<McpTool>> = _discoveredTools.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<McpToolLog>>(emptyList())
    val executionLogs: StateFlow<List<McpToolLog>> = _executionLogs.asStateFlow()

    suspend fun connectAndDiscoverTools(endpointUrl: String): Boolean = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()
        val initPayload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", requestId)
            put("method", "tools/list")
            put("params", JSONObject())
        }

        try {
            val body = initPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpointUrl).post(body).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val result = respJson.optJSONObject("result")
                val toolsArray = result?.optJSONArray("tools") ?: JSONArray()
                val toolsList = mutableListOf<McpTool>()

                for (i in 0 until toolsArray.length()) {
                    val toolObj = toolsArray.getJSONObject(i)
                    val name = toolObj.getString("name")
                    val desc = toolObj.optString("description", "")
                    val schema = toolObj.optJSONObject("inputSchema")?.toString() ?: "{}"
                    val isWrite = name.contains("create") || name.contains("update") || name.contains("delete") || name.contains("push")
                    toolsList.add(
                        McpTool(
                            name = name,
                            description = desc,
                            inputSchemaJson = schema,
                            requiresApproval = isWrite
                        )
                    )
                }

                _discoveredTools.value = toolsList
                _isConnected.value = true
                true
            } else {
                _discoveredTools.value = emptyList()
                _isConnected.value = false
                false
            }
        } catch (e: Exception) {
            _discoveredTools.value = emptyList()
            _isConnected.value = false
            false
        }
    }

    suspend fun invokeTool(
        endpointUrl: String,
        toolName: String,
        argumentsJson: String
    ): String = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()
        val logId = UUID.randomUUID().toString()

        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", requestId)
            put("method", "tools/call")
            put("params", JSONObject().apply {
                put("name", toolName)
                put("arguments", JSONObject(argumentsJson.ifBlank { "{}" }))
            })
        }

        val initialLog = McpToolLog(
            id = logId,
            toolName = toolName,
            arguments = argumentsJson,
            status = "PENDING",
            result = null
        )
        _executionLogs.value = listOf(initialLog) + _executionLogs.value

        try {
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpointUrl).post(body).build()
            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val updatedLog = McpToolLog(
                    id = logId,
                    toolName = toolName,
                    arguments = argumentsJson,
                    status = "SUCCESS",
                    result = respStr.ifBlank { "Tool executed successfully." }
                )
                _executionLogs.value = _executionLogs.value.map { if (it.id == logId) updatedLog else it }
                respStr.ifBlank { "Tool executed successfully." }
            } else {
                val errorResult = "Tool call failed [HTTP ${response.code}]: $respStr"
                val updatedLog = McpToolLog(
                    id = logId,
                    toolName = toolName,
                    arguments = argumentsJson,
                    status = "FAILED",
                    result = errorResult
                )
                _executionLogs.value = _executionLogs.value.map { if (it.id == logId) updatedLog else it }
                errorResult
            }
        } catch (e: Exception) {
            val errorResult = "Execution error: ${e.message}"
            val updatedLog = McpToolLog(
                id = logId,
                toolName = toolName,
                arguments = argumentsJson,
                status = "FAILED",
                result = errorResult
            )
            _executionLogs.value = _executionLogs.value.map { if (it.id == logId) updatedLog else it }
            errorResult
        }
    }
}
