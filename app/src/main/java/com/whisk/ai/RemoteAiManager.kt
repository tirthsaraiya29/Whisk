package com.whisk.ai

import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.AiProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RemoteAiManager {

    private val certificatePinner = CertificatePinner.Builder()
        .add("api.openai.com", "sha256/Cua12II7MZbEHsOwMvD0/MXnQ+Y8Et2v/oxvYK50vFk=")
        .add("openrouter.ai", "sha256/ELd2CUy1xHluSJlrnpGlGuYoGXcCzktGV+lXdRX4Ovk=")
        .add("generativelanguage.googleapis.com", "sha256/4Ate169FdgaRIIYrIEqsJtDCzMmgxovjQrGl8jRHzd0=")
        .add("api.anthropic.com", "sha256/QpqHLpYF7aGBvIWS0uXh4JZJImXrKQsT0I+2Iz9Li/c=")
        .build()

    private val client = OkHttpClient.Builder()
        .certificatePinner(certificatePinner)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(config: AiConfig): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (config.providerType == AiProviderType.LOCAL_GEMMA) {
            return@withContext Pair(true, "Local Gemma engine ready.")
        }
        if (config.apiKey.isBlank() && config.providerType != AiProviderType.CUSTOM_HTTPS) {
            return@withContext Pair(false, "API key is required.")
        }

        try {
            when (config.providerType) {
                AiProviderType.OPENAI, AiProviderType.OPENROUTER, AiProviderType.CUSTOM_HTTPS -> {
                    val endpoint = if (config.providerType == AiProviderType.OPENROUTER) {
                        "https://openrouter.ai/api/v1/models"
                    } else if (config.customEndpoint.isNotBlank()) {
                        config.customEndpoint
                    } else {
                        "https://api.openai.com/v1/models"
                    }

                    val requestBuilder = Request.Builder().url(endpoint).get()
                    if (config.apiKey.isNotBlank()) {
                        requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
                    }
                    val response = client.newCall(requestBuilder.build()).execute()
                    if (response.isSuccessful) Pair(true, "Connection successful.") else Pair(false, "HTTP ${response.code}: ${response.message}")
                }
                AiProviderType.GEMINI -> {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${config.apiKey}"
                    val response = client.newCall(Request.Builder().url(url).get().build()).execute()
                    if (response.isSuccessful) Pair(true, "Gemini API connected.") else Pair(false, "Gemini HTTP ${response.code}")
                }
                AiProviderType.ANTHROPIC -> {
                    val url = "https://api.anthropic.com/v1/messages"
                    val requestBuilder = Request.Builder().url(url)
                        .header("x-api-key", config.apiKey)
                        .header("anthropic-version", "2023-06-01")
                    val body = """{"model":"claude-3-haiku-20240307","max_tokens":1,"messages":[{"role":"user","content":"hi"}]}""".toRequestBody("application/json".toMediaType())
                    val response = client.newCall(requestBuilder.post(body).build()).execute()
                    if (response.isSuccessful || response.code == 400) Pair(true, "Anthropic API connected.") else Pair(false, "Anthropic HTTP ${response.code}")
                }
                else -> Pair(false, "Unsupported provider type.")
            }
        } catch (e: Exception) {
            Pair(false, "Connection failed: ${e.message}")
        }
    }

    fun generateStream(prompt: String, contextText: String, config: AiConfig): Flow<String> = flow {
        if (config.apiKey.isBlank() && config.providerType != AiProviderType.CUSTOM_HTTPS && config.providerType != AiProviderType.LOCAL_GEMMA) {
            emit("Error: API Key missing for ${config.providerType.name}.")
            return@flow
        }

        val fullSystemPrompt = "You are Whisk AI, a senior Android & software engineering copilot. Context:\n$contextText"

        try {
            when (config.providerType) {
                AiProviderType.OPENAI, AiProviderType.OPENROUTER, AiProviderType.CUSTOM_HTTPS -> {
                    generateOpenAiStream(prompt, fullSystemPrompt, config).collect { emit(it) }
                }
                AiProviderType.GEMINI -> {
                    generateGeminiStream(prompt, fullSystemPrompt, config).collect { emit(it) }
                }
                AiProviderType.ANTHROPIC -> {
                    generateAnthropicStream(prompt, fullSystemPrompt, config).collect { emit(it) }
                }
                else -> {
                    emit("Error: ${config.providerType.name} is not configured for cloud streaming.")
                }
            }
        } catch (e: Exception) {
            emit("Error connecting to ${config.providerType.name}: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    private fun generateOpenAiStream(prompt: String, systemPrompt: String, config: AiConfig): Flow<String> = flow {
        val endpoint = if (config.providerType == AiProviderType.OPENROUTER) {
            "https://openrouter.ai/api/v1/chat/completions"
        } else if (config.customEndpoint.isNotBlank()) {
            config.customEndpoint
        } else {
            "https://api.openai.com/v1/chat/completions"
        }

        val payload = JSONObject().apply {
            put("model", config.selectedModel.ifBlank { "gpt-4o-mini" })
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: response.message
            emit("API Request Failed [${response.code}]: $errBody")
            return@flow
        }

        val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line?.trim() ?: continue
            if (l.startsWith("data: ")) {
                val data = l.substring(6).trim()
                if (data == "[DONE]") break
                try {
                    val json = JSONObject(data)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val delta = choices.getJSONObject(0).optJSONObject("delta")
                        val content = delta?.optString("content", "") ?: ""
                        if (content.isNotEmpty()) {
                            emit(content)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore line parse errors during SSE streaming
                }
            }
        }
    }

    private fun generateGeminiStream(prompt: String, systemPrompt: String, config: AiConfig): Flow<String> = flow {
        val model = config.selectedModel.ifBlank { "gemini-1.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=${config.apiKey}"

        val fullPromptText = "$systemPrompt\n\nUser Question:\n$prompt"

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", fullPromptText))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: response.message
            emit("Gemini Request Failed [${response.code}]: $errBody")
            return@flow
        }

        val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line?.trim() ?: continue
            if (l.startsWith("data: ")) {
                val data = l.substring(6).trim()
                try {
                    val json = JSONObject(data)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            if (text.isNotEmpty()) {
                                emit(text)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore line parse errors during SSE streaming
                }
            }
        }
    }

    private fun generateAnthropicStream(prompt: String, systemPrompt: String, config: AiConfig): Flow<String> = flow {
        val url = "https://api.anthropic.com/v1/messages"
        val model = config.selectedModel.ifBlank { "claude-3-haiku-20240307" }

        val payload = JSONObject().apply {
            put("model", model)
            put("max_tokens", config.maxTokens)
            put("stream", true)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: response.message
            emit("Anthropic Request Failed [${response.code}]: $errBody")
            return@flow
        }

        val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line?.trim() ?: continue
            if (l.startsWith("data: ")) {
                val data = l.substring(6).trim()
                try {
                    val json = JSONObject(data)
                    val type = json.optString("type")
                    if (type == "content_block_delta") {
                        val delta = json.optJSONObject("delta")
                        val text = delta?.optString("text", "") ?: ""
                        if (text.isNotEmpty()) {
                            emit(text)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore line parse errors during SSE streaming
                }
            }
        }
    }
}
