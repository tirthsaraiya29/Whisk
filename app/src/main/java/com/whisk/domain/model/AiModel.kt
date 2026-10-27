package com.whisk.domain.model

enum class AiProviderType {
    LOCAL_GEMMA, OPENAI, GEMINI, ANTHROPIC, OPENROUTER, CUSTOM_HTTPS
}

enum class AiRole {
    USER, ASSISTANT, SYSTEM
}

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
    val modelSizeBytes: Long = 1_800_000_000L,
    val isLoaded: Boolean = false,
    val loadError: String? = null
)

data class AiConfig(
    val providerType: AiProviderType = AiProviderType.LOCAL_GEMMA,
    val apiKey: String = "",
    val customEndpoint: String = "",
    val selectedModel: String = "gemma-4-e2b-it",
    val maxTokens: Int = 2048,
    val temperature: Float = 0.7f
)
