package com.whisk.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.ai.ContextPayload
import com.whisk.ai.LocalGemmaManager
import com.whisk.ai.RemoteAiManager
import com.whisk.data.local.PreferencesManager
import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.AiMessage
import com.whisk.domain.model.AiProviderType
import com.whisk.domain.model.AiRole
import com.whisk.domain.model.LocalModelStatus
import com.whisk.domain.model.Note
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AiUiState(
    val config: AiConfig = AiConfig(),
    val localStatus: LocalModelStatus = LocalModelStatus(),
    val messages: List<AiMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val contextPayload: ContextPayload = ContextPayload(),
    val generatedEditPreview: String? = null
)

class AiViewModel(
    private val localGemmaManager: LocalGemmaManager,
    private val remoteAiManager: RemoteAiManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    private val _isGenerating = MutableStateFlow(false)
    private val _contextPayload = MutableStateFlow(ContextPayload())
    private val _generatedEditPreview = MutableStateFlow<String?>(null)

    private var generationJob: Job? = null

    val uiState: StateFlow<AiUiState> = combine(
        combine(preferencesManager.aiConfig, localGemmaManager.modelStatus) { config, localStatus -> Pair(config, localStatus) },
        combine(_messages, _isGenerating) { messages, isGenerating -> Pair(messages, isGenerating) },
        combine(_contextPayload, _generatedEditPreview) { contextPayload, editPreview -> Pair(contextPayload, editPreview) }
    ) { p1, p2, p3 ->
        AiUiState(
            config = p1.first,
            localStatus = p1.second,
            messages = p2.first,
            isGenerating = p2.second,
            contextPayload = p3.first,
            generatedEditPreview = p3.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiUiState())

    fun setContextNote(note: Note?) {
        _contextPayload.value = _contextPayload.value.copy(activeNote = note)
    }

    fun downloadLocalModel() {
        viewModelScope.launch {
            val config = uiState.value.config
            localGemmaManager.downloadModel(config) { _ -> }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = AiMessage(role = AiRole.USER, content = userText)
        val assistantMsgId = java.util.UUID.randomUUID().toString()
        val initialAssistantMsg = AiMessage(id = assistantMsgId, role = AiRole.ASSISTANT, content = "")

        _messages.value = _messages.value + userMsg + initialAssistantMsg
        _isGenerating.value = true

        val config = uiState.value.config
        val contextText = _contextPayload.value.formatContext()

        generationJob = viewModelScope.launch {
            val streamFlow = if (config.providerType == AiProviderType.LOCAL_GEMMA) {
                localGemmaManager.generateStream(userText, contextText, config)
            } else {
                remoteAiManager.generateStream(userText, contextText, config)
            }

            val contentBuilder = StringBuilder()
            try {
                streamFlow.collect { chunk ->
                    contentBuilder.append(chunk)
                    val currentContent = contentBuilder.toString()
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(content = currentContent) else msg
                    }
                }
            } catch (e: Exception) {
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMsgId) msg.copy(content = "Error during generation: ${e.message}") else msg
                }
            } finally {
                _isGenerating.value = false
                val finalRes = contentBuilder.toString()
                if (finalRes.contains("```")) {
                    _generatedEditPreview.value = finalRes
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
    }

    fun clearHistory() {
        _messages.value = emptyList()
        _generatedEditPreview.value = null
    }

    fun dismissEditPreview() {
        _generatedEditPreview.value = null
    }

    override fun onCleared() {
        super.onCleared()
        localGemmaManager.close()
    }
}
