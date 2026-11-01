package com.whisk.data.repository

import com.whisk.domain.model.McpTool
import com.whisk.domain.model.McpToolLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class McpApprovalRequest(
    val logId: String,
    val endpointUrl: String,
    val toolName: String,
    val argumentsJson: String,
)

class McpRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _isConnected = MutableStateFlow(value = false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _discoveredTools = MutableStateFlow<List<McpTool>>(emptyList())
    val discoveredTools: StateFlow<List<McpTool>> = _discoveredTools.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<McpToolLog>>(emptyList())
    val executionLogs: StateFlow<List<McpToolLog>> = _executionLogs.asStateFlow()

    private val _approvalRequests = MutableSharedFlow<McpApprovalRequest>()
    val approvalRequests: SharedFlow<McpApprovalRequest> = _approvalRequests.asSharedFlow()

    private val pendingApprovals = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    
    // Server Trust Settings
    private val trustedServers = ConcurrentHashMap<String, Boolean>()
    private val explicitlyTrustedTools = ConcurrentHashMap<String, MutableSet<String>>()

    fun setServerTrust(endpointUrl: String, trusted: Boolean) {
        trustedServers[endpointUrl] = trusted
    }
    
    fun trustTool(endpointUrl: String, toolName: String) {
        explicitlyTrustedTools.getOrPut(endpointUrl) { ConcurrentHashMap.newKeySet() }.add(toolName)
    }
    
    fun revokeToolTrust(endpointUrl: String, toolName: String) {
        explicitlyTrustedTools[endpointUrl]?.remove(toolName)
    }

    private fun requiresApproval(endpointUrl: String, toolName: String): Boolean {
        if (trustedServers[endpointUrl] == true) return false
        if (explicitlyTrustedTools[endpointUrl]?.contains(toolName) == true) return false
        return true
    }

    fun resolveApproval(logId: String, approved: Boolean) {
        pendingApprovals.remove(logId)?.complete(approved)
    }

    suspend fun connectAndDiscoverTools(endpointUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Send initialize
            val initRequestId = UUID.randomUUID().toString()
            val initPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", initRequestId)
                put("method", "initialize")
                put("params", JSONObject().apply {
                    put("protocolVersion", "2024-11-05")
                    put("capabilities", JSONObject().apply {
                        put("roots", JSONObject().apply {
                            put("listChanged", true)
                        })
                    })
                    put("clientInfo", JSONObject().apply {
                        put("name", "Whisk")
                        put("version", "1.0.0")
                    })
                })
            }

            val initBody = initPayload.toString().toRequestBody("application/json".toMediaType())
            val initRequest = Request.Builder().url(endpointUrl).post(initBody).build()
            val initResponse = client.newCall(initRequest).execute()
            
            if (!initResponse.isSuccessful) {
                _isConnected.value = false
                return@withContext false
            }

            // 2. Send notifications/initialized
            val initializedPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "notifications/initialized")
            }
            val initializedBody = initializedPayload.toString().toRequestBody("application/json".toMediaType())
            val initializedRequest = Request.Builder().url(endpointUrl).post(initializedBody).build()
            client.newCall(initializedRequest).execute()

            // 3. Request tools/list
            val listRequestId = UUID.randomUUID().toString()
            val listPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", listRequestId)
                put("method", "tools/list")
                put("params", JSONObject())
            }
            
            val listBody = listPayload.toString().toRequestBody("application/json".toMediaType())
            val listRequest = Request.Builder().url(endpointUrl).post(listBody).build()
            val listResponse = client.newCall(listRequest).execute()

            if (listResponse.isSuccessful) {
                val respStr = listResponse.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val result = respJson.optJSONObject("result")
                val toolsArray = result?.optJSONArray("tools") ?: JSONArray()
                val toolsList = mutableListOf<McpTool>()

                for (i in 0 until toolsArray.length()) {
                    val toolObj = toolsArray.getJSONObject(i)
                    val name = toolObj.getString("name")
                    val desc = toolObj.optString("description", "")
                    val schema = toolObj.optJSONObject("inputSchema")?.toString() ?: "{}"
                    
                    toolsList.add(
                        McpTool(
                            name = name,
                            description = desc,
                            inputSchemaJson = schema,
                            requiresApproval = requiresApproval(endpointUrl, name),
                        ),
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
        } catch (_: Exception) {
            _discoveredTools.value = emptyList()
            _isConnected.value = false
            false
        }
    }

    suspend fun invokeTool(
        endpointUrl: String,
        toolName: String,
        argumentsJson: String,
    ): String = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()
        val logId = UUID.randomUUID().toString()

        val initialLog = McpToolLog(
            id = logId,
            toolName = toolName,
            arguments = argumentsJson,
            status = "PENDING",
            result = null,
        )
        _executionLogs.value = listOf(initialLog) + _executionLogs.value

        if (requiresApproval(endpointUrl, toolName)) {
            val deferred = CompletableDeferred<Boolean>()
            pendingApprovals[logId] = deferred
            _approvalRequests.emit(McpApprovalRequest(logId, endpointUrl, toolName, argumentsJson))
            
            val isApproved = deferred.await()
            if (!isApproved) {
                val updatedLog = McpToolLog(
                    id = logId,
                    toolName = toolName,
                    arguments = argumentsJson,
                    status = "FAILED",
                    result = "Execution denied by user.",
                )
                _executionLogs.value = _executionLogs.value.map { if (it.id == logId) updatedLog else it }
                return@withContext "Execution denied by user."
            }
        }

        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", requestId)
            put("method", "tools/call")
            put(
                "params",
                JSONObject().apply {
                    put("name", toolName)
                    put("arguments", JSONObject(argumentsJson.ifBlank { "{}" }))
                },
            )
        }

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
                    result = respStr.ifBlank { "Tool executed successfully." },
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
                    result = errorResult,
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
                result = errorResult,
            )
            _executionLogs.value = _executionLogs.value.map { if (it.id == logId) updatedLog else it }
            errorResult
        }
    }
}
