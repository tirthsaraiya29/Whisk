package com.whisk.data.repository

import com.whisk.domain.model.McpTool
import com.whisk.domain.model.McpToolLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class McpConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

data class McpServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val endpointUrl: String,
    val isEnabled: Boolean = true,
    val connectionState: McpConnectionState = McpConnectionState.DISCONNECTED,
    val lastErrorText: String? = null,
)

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED,
    CANCELLED,
}

data class McpApprovalRequest(
    val id: String = UUID.randomUUID().toString(),
    val serverId: String = "",
    val endpointUrl: String,
    val toolName: String,
    val argumentsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 60_000L,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
) {
    val logId: String get() = id
}

sealed class EndpointValidationResult {
    data class Valid(val uri: URI) : EndpointValidationResult()
    data class Invalid(val reason: String) : EndpointValidationResult()
}

class McpRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _serverConfigs = MutableStateFlow<List<McpServerConfig>>(
        listOf(
            McpServerConfig(
                id = "default-local-mcp",
                displayName = "Local MCP Server",
                endpointUrl = "http://localhost:8080/mcp",
            ),
        ),
    )
    val serverConfigs: StateFlow<List<McpServerConfig>> = _serverConfigs.asStateFlow()

    private val _activeServer = MutableStateFlow<McpServerConfig?>(_serverConfigs.value.firstOrNull())
    val activeServer: StateFlow<McpServerConfig?> = _activeServer.asStateFlow()

    private val _isConnected = MutableStateFlow(value = false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _discoveredTools = MutableStateFlow<List<McpTool>>(emptyList())
    val discoveredTools: StateFlow<List<McpTool>> = _discoveredTools.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<McpToolLog>>(emptyList())
    val executionLogs: StateFlow<List<McpToolLog>> = _executionLogs.asStateFlow()

    private val _pendingApprovalRequests = MutableStateFlow<List<McpApprovalRequest>>(emptyList())
    val pendingApprovalRequests: StateFlow<List<McpApprovalRequest>> = _pendingApprovalRequests.asStateFlow()

    private val pendingDeferreds = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val pendingTimeoutJobs = ConcurrentHashMap<String, Job>()

    private val trustedServers = ConcurrentHashMap<String, Boolean>()
    private val explicitlyTrustedTools = ConcurrentHashMap<String, MutableSet<String>>()

    fun validateEndpoint(endpointUrl: String): EndpointValidationResult {
        val cleanUrl = endpointUrl.trim()
        if (cleanUrl.isBlank()) {
            return EndpointValidationResult.Invalid("Endpoint URL cannot be empty")
        }

        val uri = try {
            URI(cleanUrl)
        } catch (e: Exception) {
            return EndpointValidationResult.Invalid("Malformed URI syntax: ${e.message}")
        }

        val scheme = uri.scheme?.lowercase()
            ?: return EndpointValidationResult.Invalid("Missing URI scheme (must be https or http for local development)")

        if (scheme != "http" && scheme != "https") {
            return EndpointValidationResult.Invalid("Unsupported scheme '$scheme'. Only http and https are supported.")
        }

        val host = uri.host?.lowercase()
            ?: return EndpointValidationResult.Invalid("Missing host in URI")

        if (scheme == "http") {
            val isAllowedLocalHost = host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"
            if (!isAllowedLocalHost) {
                return EndpointValidationResult.Invalid(
                    "Insecure HTTP transport is disabled for remote host '$host'. Use HTTPS for remote connections.",
                )
            }
        }

        return EndpointValidationResult.Valid(uri)
    }

    fun addServerConfig(displayName: String, endpointUrl: String): McpServerConfig? {
        val validation = validateEndpoint(endpointUrl)
        if (validation is EndpointValidationResult.Invalid) return null

        val newConfig = McpServerConfig(
            displayName = displayName,
            endpointUrl = endpointUrl,
        )
        _serverConfigs.value = _serverConfigs.value + newConfig
        return newConfig
    }

    fun setActiveServer(serverId: String) {
        val config = _serverConfigs.value.firstOrNull { it.id == serverId }
        _activeServer.value = config
    }

    fun setServerTrust(endpointUrl: String, trusted: Boolean) {
        trustedServers[endpointUrl] = trusted
    }

    fun trustTool(endpointUrl: String, toolName: String) {
        explicitlyTrustedTools.getOrPut(endpointUrl) { ConcurrentHashMap.newKeySet() }.add(toolName)
    }

    fun revokeToolTrust(endpointUrl: String, toolName: String) {
        explicitlyTrustedTools[endpointUrl]?.remove(toolName)
    }

    fun requiresApproval(endpointUrl: String, toolName: String): Boolean {
        if (trustedServers[endpointUrl] == true) return false
        if (explicitlyTrustedTools[endpointUrl]?.contains(toolName) == true) return false
        // Default-deny: only confidently read-only verbs are approval-free.
        return !isReadOnlyToolName(toolName)
    }

    private fun isReadOnlyToolName(toolName: String): Boolean {
        // Normalize case and separators (._- / spaces) so "Get_File", "read-file", "LIST__items" can't bypass.
        val tokens = toolName.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        val writeVerbs = setOf(
            "create", "add", "insert", "post", "put", "update", "edit", "modify", "rename", "move",
            "copy", "duplicate", "delete", "remove", "destroy", "drop", "clear", "purge", "truncate",
            "push", "write", "save", "store", "set", "upload", "import", "export", "transfer", "merge",
            "rebase", "revert", "restore", "replace", "apply", "patch", "exec", "execute", "run", "invoke",
            "call", "send", "submit", "commit", "approve", "close", "reopen", "assign", "archive",
            "unarchive", "toggle", "enable", "disable", "grant", "revoke", "reset", "regenerate", "cancel",
            "alter", "change", "swap", "shift", "convert", "transform", "wipe", "flush", "kill", "terminate",
            "stop", "start", "restart", "boot", "reboot", "shutdown", "upgrade", "downgrade", "install",
            "uninstall", "overwrite", "prune", "detach", "unbind", "unlink", "lock", "unlock", "sync",
            "format", "clean", "dispatch", "publish", "deploy", "erase",
        )
        val readOnlyVerbs = setOf(
            "get", "read", "list", "search", "fetch", "find", "query", "view", "show",
            "describe", "check", "lookup", "browse", "select", "count", "ping", "status", "health",
        )
        val safeTokens = setOf(
            "github", "mcp", "system", "local", "remote", "api", "file", "files", "note", "notes",
            "vault", "user", "users", "data", "issue", "issues", "repo", "repository", "repositories",
            "commit", "commits", "branch", "branches", "pr", "pull", "request", "requests", "info",
            "item", "items", "all", "by", "id", "name", "tag", "tags", "and", "or", "with", "for",
            "in", "on", "at", "to", "from", "detail", "details", "metadata", "history", "log", "logs",
            "metric", "metrics", "summary",
        )

        // Any write verb means approval, even mixed with read verbs ("create_and_get").
        if (tokens.any { it in writeVerbs }) return false

        // Strict validation: every token in a read-only tool must be a known read verb or safe token,
        // and at least one read verb must be present. Unrecognized tokens default to requiring approval.
        val hasReadOnlyVerb = tokens.any { it in readOnlyVerbs }
        val allTokensSafe = tokens.all { it in readOnlyVerbs || it in safeTokens }

        return hasReadOnlyVerb && allTokensSafe
    }

    fun resolveApproval(requestId: String, approved: Boolean) {
        pendingTimeoutJobs.remove(requestId)?.cancel()

        _pendingApprovalRequests.value = _pendingApprovalRequests.value.map { req ->
            if (req.id == requestId) {
                req.copy(status = if (approved) ApprovalStatus.APPROVED else ApprovalStatus.REJECTED)
            } else req
        }.filter { it.status == ApprovalStatus.PENDING }

        pendingDeferreds.remove(requestId)?.complete(approved)
    }

    suspend fun connectAndDiscoverTools(endpointUrl: String): Boolean = withContext(Dispatchers.IO) {
        val validation = validateEndpoint(endpointUrl)
        if (validation is EndpointValidationResult.Invalid) {
            _isConnected.value = false
            return@withContext false
        }

        val serverConfig = _serverConfigs.value.firstOrNull { it.endpointUrl == endpointUrl }
            ?: McpServerConfig(displayName = "MCP Server", endpointUrl = endpointUrl)

        try {
            val initRequestId = UUID.randomUUID().toString()
            val initPayload = """
                {"jsonrpc":"2.0","id":"$initRequestId","method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"roots":{"listChanged":true}},"clientInfo":{"name":"Whisk","version":"1.0.0"}}}
            """.trimIndent()

            val initBody = initPayload.toRequestBody("application/json".toMediaType())
            val initRequest = Request.Builder().url(endpointUrl).post(initBody).build()
            val initCall = client.newCall(initRequest)
            val initResponse = try {
                initCall.execute()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isConnected.value = false
                updateServerState(serverConfig.id, McpConnectionState.ERROR, e.message)
                return@withContext false
            }

            if (!initResponse.isSuccessful) {
                _isConnected.value = false
                updateServerState(serverConfig.id, McpConnectionState.ERROR, "HTTP ${initResponse.code}: Initialise failed")
                return@withContext false
            }

            val initializedPayload = """{"jsonrpc":"2.0","method":"notifications/initialized"}"""
            val initializedBody = initializedPayload.toRequestBody("application/json".toMediaType())
            val initializedRequest = Request.Builder().url(endpointUrl).post(initializedBody).build()
            client.newCall(initializedRequest).execute()

            val listRequestId = UUID.randomUUID().toString()
            val listPayload = """{"jsonrpc":"2.0","id":"$listRequestId","method":"tools/list","params":{}}"""

            val listBody = listPayload.toRequestBody("application/json".toMediaType())
            val listRequest = Request.Builder().url(endpointUrl).post(listBody).build()
            val listCall = client.newCall(listRequest)
            val listResponse = try {
                listCall.execute()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isConnected.value = false
                updateServerState(serverConfig.id, McpConnectionState.ERROR, e.message)
                return@withContext false
            }

            if (listResponse.isSuccessful) {
                val respStr = listResponse.body.string()
                val parsedJson = jsonParser.parseToJsonElement(respStr).jsonObject
                val resultObj = parsedJson["result"]?.jsonObject
                val toolsArray = resultObj?.get("tools")?.jsonArray ?: JsonArray(emptyList())
                val toolsList = mutableListOf<McpTool>()

                for (item in toolsArray) {
                    val toolObj = item.jsonObject
                    val name = toolObj["name"]?.jsonPrimitive?.content ?: ""
                    val desc = toolObj["description"]?.jsonPrimitive?.content ?: ""
                    val schema = toolObj["inputSchema"]?.toString() ?: "{}"

                    toolsList.add(
                        McpTool(
                            name = name,
                            description = desc,
                            inputSchemaJson = schema,
                            requiresApproval = requiresApproval(endpointUrl, name),
                            serverId = serverConfig.id,
                            serverEndpoint = endpointUrl,
                        ),
                    )
                }

                _discoveredTools.value = toolsList
                _isConnected.value = true
                updateServerState(serverConfig.id, McpConnectionState.CONNECTED, null)
                true
            } else {
                _discoveredTools.value = emptyList()
                _isConnected.value = false
                updateServerState(serverConfig.id, McpConnectionState.ERROR, "Tools list HTTP ${listResponse.code}")
                false
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _discoveredTools.value = emptyList()
            _isConnected.value = false
            updateServerState(serverConfig.id, McpConnectionState.ERROR, e.message)
            false
        }
    }

    suspend fun invokeTool(
        endpointUrl: String,
        toolName: String,
        argumentsJson: String,
        timeoutMs: Long = 60_000L,
    ): String = withContext(Dispatchers.IO) {
        val validation = validateEndpoint(endpointUrl)
        if (validation is EndpointValidationResult.Invalid) {
            val errorMsg = "Invalid MCP endpoint: ${validation.reason}"
            logExecution(toolName, argumentsJson, "FAILED", errorMsg, endpointUrl)
            return@withContext errorMsg
        }

        val requestId = UUID.randomUUID().toString()
        val serverConfig = _serverConfigs.value.firstOrNull { it.endpointUrl == endpointUrl }
        val serverId = serverConfig?.id ?: ""

        val initialLog = McpToolLog(
            id = requestId,
            toolName = toolName,
            arguments = sanitizeLogArguments(argumentsJson),
            status = "PENDING",
            result = null,
            serverId = serverId,
            serverEndpoint = endpointUrl,
        )
        _executionLogs.value = listOf(initialLog) + _executionLogs.value

        try {
            if (requiresApproval(endpointUrl, toolName)) {
                val deferred = CompletableDeferred<Boolean>()
                pendingDeferreds[requestId] = deferred

                val approvalReq = McpApprovalRequest(
                    id = requestId,
                    serverId = serverId,
                    endpointUrl = endpointUrl,
                    toolName = toolName,
                    argumentsJson = argumentsJson,
                    expiresAt = System.currentTimeMillis() + timeoutMs,
                )

                _pendingApprovalRequests.value = _pendingApprovalRequests.value + approvalReq

                val timeoutJob = scope.launch {
                    delay(timeoutMs)
                    if (pendingDeferreds.containsKey(requestId)) {
                        _pendingApprovalRequests.value = _pendingApprovalRequests.value.map {
                            if (it.id == requestId) it.copy(status = ApprovalStatus.EXPIRED) else it
                        }.filter { it.status == ApprovalStatus.PENDING }
                        pendingDeferreds.remove(requestId)?.complete(false)
                    }
                }
                pendingTimeoutJobs[requestId] = timeoutJob

                val isApproved = try {
                    deferred.await()
                } finally {
                    pendingTimeoutJobs.remove(requestId)?.cancel()
                    pendingDeferreds.remove(requestId)
                    _pendingApprovalRequests.value = _pendingApprovalRequests.value.filter { it.id != requestId }
                }

                if (!isApproved) {
                    val updatedLog = initialLog.copy(
                        status = "FAILED",
                        result = "Execution denied or timed out after ${timeoutMs / 1000}s.",
                    )
                    _executionLogs.value = _executionLogs.value.map { if (it.id == requestId) updatedLog else it }
                    return@withContext "Execution denied or timed out after ${timeoutMs / 1000}s."
                }
            }

            val jsonRpcId = UUID.randomUUID().toString()
            val cleanArgs = argumentsJson.ifBlank { "{}" }
            val payload = """{"jsonrpc":"2.0","id":"$jsonRpcId","method":"tools/call","params":{"name":"$toolName","arguments":$cleanArgs}}"""

            val body = payload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(endpointUrl).post(body).build()
            val call = client.newCall(request)

            val response = try {
                call.execute()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val errorResult = "Execution error: ${e.message}"
                val updatedLog = initialLog.copy(
                    status = "FAILED",
                    result = errorResult,
                )
                _executionLogs.value = _executionLogs.value.map { if (it.id == requestId) updatedLog else it }
                return@withContext errorResult
            }

            val respStr = response.body.string()

            if (response.isSuccessful) {
                val resultText = respStr.ifBlank { "Tool executed successfully." }
                val updatedLog = initialLog.copy(
                    status = "SUCCESS",
                    result = resultText,
                )
                _executionLogs.value = _executionLogs.value.map { if (it.id == requestId) updatedLog else it }
                resultText
            } else {
                val errorResult = "Tool call failed [HTTP ${response.code}]: $respStr"
                val updatedLog = initialLog.copy(
                    status = "FAILED",
                    result = errorResult,
                )
                _executionLogs.value = _executionLogs.value.map { if (it.id == requestId) updatedLog else it }
                errorResult
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val errorResult = "Execution error: ${e.message}"
            val updatedLog = initialLog.copy(
                status = "FAILED",
                result = errorResult,
            )
            _executionLogs.value = _executionLogs.value.map { if (it.id == requestId) updatedLog else it }
            errorResult
        }
    }

    private fun logExecution(
        toolName: String,
        arguments: String,
        status: String,
        result: String,
        endpointUrl: String,
    ) {
        val log = McpToolLog(
            toolName = toolName,
            arguments = sanitizeLogArguments(arguments),
            status = status,
            result = result,
            serverEndpoint = endpointUrl,
        )
        _executionLogs.value = listOf(log) + _executionLogs.value
    }

    private fun sanitizeLogArguments(argumentsJson: String): String {
        return try {
            val parsed = jsonParser.parseToJsonElement(argumentsJson)
            sanitizeJsonElement(parsed).toString()
        } catch (_: Exception) {
            argumentsJson
        }
    }

    private fun sanitizeJsonElement(element: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonElement {
        val secretKeys = listOf("token", "secret", "password", "key", "authorization", "auth", "credential", "private_key", "bearer")
        return when (element) {
            is JsonObject -> {
                val sanitizedMap = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
                for ((key, value) in element) {
                    if (secretKeys.any { key.lowercase().contains(it) }) {
                        sanitizedMap[key] = kotlinx.serialization.json.JsonPrimitive("******")
                    } else {
                        sanitizedMap[key] = sanitizeJsonElement(value)
                    }
                }
                JsonObject(sanitizedMap)
            }
            is JsonArray -> {
                JsonArray(element.map { sanitizeJsonElement(it) })
            }
            else -> element
        }
    }

    private fun updateServerState(serverId: String, state: McpConnectionState, errorText: String?) {
        _serverConfigs.value = _serverConfigs.value.map { config ->
            if (config.id == serverId) {
                config.copy(connectionState = state, lastErrorText = errorText)
            } else config
        }
        if (_activeServer.value?.id == serverId) {
            _activeServer.value = _activeServer.value?.copy(connectionState = state, lastErrorText = errorText)
        }
    }
}
