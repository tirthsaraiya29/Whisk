package com.whisk.domain.model

import kotlinx.serialization.Serializable

enum class AiProviderType {
    LOCAL_GEMMA, OPENAI, GEMINI, ANTHROPIC, OPENROUTER, CUSTOM_HTTPS
}

@Serializable
enum class AiRole {
    USER, ASSISTANT, SYSTEM
}

@Serializable
data class AiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: AiRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val contextPreview: String? = null
)

data class LocalModelStatus(
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val statusText: String = "Not downloaded",
    val modelSizeBytes: Long = 2_010_000_000L,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedKbps: Float = 0f,
    val isLoaded: Boolean = false,
    val loadError: String? = null
)

data class AiConfig(
    val providerType: AiProviderType = AiProviderType.LOCAL_GEMMA,
    val apiKey: String = "",
    val customEndpoint: String = "",
    val selectedModel: String = "gemma-4-E2B-it-gpu.litertlm",
    val maxTokens: Int = 2048,
    val temperature: Float = 0.7f,
    val hfModelUrl: String = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-gpu.litertlm",
    val hfApiToken: String = "",
    val useGpuAccelerator: Boolean = true
)
