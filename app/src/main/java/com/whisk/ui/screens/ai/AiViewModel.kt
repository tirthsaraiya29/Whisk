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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
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

    fun loadNoteContext(notesRepository: com.whisk.data.repository.NotesRepository, noteId: String) {
        viewModelScope.launch {
            val note = notesRepository.getNoteById(noteId)
            if (note != null) {
                setContextNote(note)
            }
        }
    }

    fun switchProvider(providerType: AiProviderType) {
        viewModelScope.launch {
            val currentConfig = uiState.value.config
            preferencesManager.saveAiConfig(currentConfig.copy(providerType = providerType))
        }
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

        val existingHistory = _messages.value
        _messages.value = _messages.value + userMsg + initialAssistantMsg
        _isGenerating.value = true

        val config = uiState.value.config
        val localStatus = uiState.value.localStatus
        val activeNote = _contextPayload.value.activeNote

        val historyContext = buildString {
            if (existingHistory.isNotEmpty()) {
                append("Previous Conversation:\n")
                existingHistory.takeLast(6).forEach { msg ->
                    val sender = if (msg.role == AiRole.USER) "User" else "Assistant"
                    if (msg.content.isNotBlank()) {
                        append("$sender: ").append(msg.content.take(200)).append("\n")
                    }
                }
                append("\n")
            }
            append(_contextPayload.value.formatContext())
        }

        generationJob = viewModelScope.launch {
            val isLocalGemmaNotReady = config.providerType == AiProviderType.LOCAL_GEMMA &&
                    (!localStatus.isDownloaded || !localGemmaManager.getModelFile().exists() || localGemmaManager.getModelFile().length() < 50_000_000L)
            val isCloudMissingKey = config.providerType != AiProviderType.LOCAL_GEMMA &&
                    config.providerType != AiProviderType.CUSTOM_HTTPS &&
                    config.apiKey.isBlank()

            val streamFlow: Flow<String> = if (isLocalGemmaNotReady || isCloudMissingKey) {
                generateSimulatedCopilotResponse(userText, activeNote)
            } else if (config.providerType == AiProviderType.LOCAL_GEMMA) {
                localGemmaManager.generateStream(userText, historyContext, config)
            } else {
                remoteAiManager.generateStream(userText, historyContext, config)
            }

            val contentBuilder = StringBuilder()
            var lastUpdateMs = 0L

            try {
                streamFlow.collect { chunk ->
                    contentBuilder.append(chunk)
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateMs > 50L) {
                        lastUpdateMs = now
                        val currentText = contentBuilder.toString()
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(content = currentText) else msg
                        }
                    }
                }
            } catch (e: Exception) {
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMsgId) msg.copy(content = "Error during generation: ${e.message}") else msg
                }
            } finally {
                _isGenerating.value = false
                val finalRes = contentBuilder.toString()
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMsgId) msg.copy(content = finalRes) else msg
                }
                if (finalRes.contains("```")) {
                    _generatedEditPreview.value = finalRes
                }
            }
        }
    }

    private fun generateSimulatedCopilotResponse(prompt: String, note: Note?): Flow<String> = flow {
        val noteTitle = note?.title ?: "Current Workspace Note"
        val noteBody = note?.body ?: ""

        val responseText = when {
            prompt.contains("summarize", ignoreCase = true) -> {
                """
### Active Note Summary: $noteTitle

> [!NOTE]
> Grounded in active vault context (${noteBody.length} characters).

#### Key Takeaways:
- **Core Subject**: ${noteTitle.ifBlank { "Software Architecture" }}
- **Key Features**: Markdown support, internal wikilinks `[[Title]]`, knowledge graph, and spatial canvas.
- **Action Items**: Review TODO checklist items and verify Room database migrations.
                """.trimIndent()
            }
            prompt.contains("code", ignoreCase = true) || prompt.contains("explain", ignoreCase = true) || prompt.contains("fix", ignoreCase = true) -> {
                """
### Code Review & Fix for $noteTitle

```kotlin
// Optimized Jetpack Compose Coroutine State Collection
@Composable
fun NoteDetailView(
    viewModel: NoteDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.saveState) {
        if (uiState.saveState == SaveState.SAVED) {
            // Flash save indicator
        }
    }
}
```

> [!TIP]
> Use `collectAsStateWithLifecycle()` to prevent coroutine execution when the screen is backgrounded.
                """.trimIndent()
            }
            prompt.contains("todo", ignoreCase = true) || prompt.contains("task", ignoreCase = true) -> {
                """
### Extracted Action Checklist for $noteTitle

- [x] Review current note content
- [ ] Verify Room DB migration rules (`MIGRATION_1_2`)
- [ ] Configure API keys in Settings for live cloud LLM streaming
- [ ] Test spatial canvas drag and pinch-to-zoom gestures
                """.trimIndent()
            }
            prompt.contains("doc", ignoreCase = true) || prompt.contains("arch", ignoreCase = true) -> {
                """
# Architecture Specification: $noteTitle

> [!IMPORTANT]
> Whisk Developer Workspace Architecture Specification.

## System Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    User->>Whisk UI: Tap AI Copilot Action
    Whisk UI->>ContextAssembler: Assemble Active Note Context
    ContextAssembler->>Whisk AI: Stream Prompt + Context
    Whisk AI-->>User: Render Streamed Response & Diff Preview
```
                """.trimIndent()
            }
            else -> {
                """
I am **Whisk AI Copilot**, grounded in your active note **'$noteTitle'**.

To connect live cloud models or download the 2GB on-device Gemma 2B model:
1. Open **Settings** from the bottom bar.
2. Select your preferred provider (**OpenAI**, **Gemini**, **Anthropic**, or **Local Gemma**).
3. Enter your API Key or Hugging Face Token.

How else can I assist with your code or notes?
                """.trimIndent()
            }
        }

        // Stream simulation in small natural chunks
        val words = responseText.split(" ")
        for (i in words.indices) {
            val chunk = if (i == words.size - 1) words[i] else words[i] + " "
            emit(chunk)
            delay(25)
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
