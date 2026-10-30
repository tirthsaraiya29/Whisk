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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<AiMessage>
)

data class AiUiState(
    val config: AiConfig = AiConfig(),
    val localStatus: LocalModelStatus = LocalModelStatus(),
    val messages: List<AiMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val contextPayload: ContextPayload = ContextPayload(),
    val generatedEditPreview: String? = null,
    val historySessions: List<ChatSession> = emptyList(),
    val activeSessionTitle: String = "NEW CHAT"
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
    private val _historySessions = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _activeSessionId = MutableStateFlow<String?>(null)
    private val _activeSessionTitle = MutableStateFlow("NEW CHAT")

    private var generationJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    init {
        // Load persisted chat sessions from DataStore
        viewModelScope.launch {
            preferencesManager.savedAiSessionsJson.collect { jsonStr ->
                if (jsonStr.isNotBlank() && jsonStr != "[]") {
                    try {
                        val loadedSessions = json.decodeFromString<List<ChatSession>>(jsonStr)
                        _historySessions.value = loadedSessions
                        if (_messages.value.isEmpty() && loadedSessions.isNotEmpty()) {
                            val mostRecent = loadedSessions.first()
                            _activeSessionId.value = mostRecent.id
                            _activeSessionTitle.value = mostRecent.title
                            _messages.value = mostRecent.messages
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val uiState: StateFlow<AiUiState> = combine(
        combine(preferencesManager.aiConfig, localGemmaManager.modelStatus) { config, localStatus -> Pair(config, localStatus) },
        combine(_messages, _isGenerating) { messages, isGenerating -> Pair(messages, isGenerating) },
        combine(_contextPayload, _generatedEditPreview, _historySessions, _activeSessionTitle) { contextPayload, editPreview, historySessions, title ->
            Quadruple(contextPayload, editPreview, historySessions, title)
        }
    ) { p1, p2, p3 ->
        AiUiState(
            config = p1.first,
            localStatus = p1.second,
            messages = p2.first,
            isGenerating = p2.second,
            contextPayload = p3.first,
            generatedEditPreview = p3.second,
            historySessions = p3.third,
            activeSessionTitle = p3.fourth
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

    private fun generateAutomaticTitle(userText: String): String {
        val snippet = userText.trim().lines().firstOrNull()?.take(28)?.trim() ?: "AI CHAT"
        val cleanTitle = snippet.replace(Regex("[^a-zA-Z0-9\\s_\\-]"), "").trim().uppercase()
        return if (cleanTitle.isNotBlank()) cleanTitle else "AI CHAT"
    }

    private fun persistCurrentSession() {
        val currentMsgs = _messages.value
        if (currentMsgs.isEmpty()) return

        var sessionId = _activeSessionId.value
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString()
            _activeSessionId.value = sessionId
        }

        val sessionTitle = _activeSessionTitle.value

        val updatedSession = ChatSession(
            id = sessionId,
            title = sessionTitle,
            timestamp = System.currentTimeMillis(),
            messages = currentMsgs
        )

        val updatedHistory = listOf(updatedSession) + _historySessions.value.filterNot { it.id == sessionId }
        _historySessions.value = updatedHistory

        viewModelScope.launch {
            try {
                val jsonStr = json.encodeToString(updatedHistory)
                preferencesManager.saveAiSessionsJson(jsonStr)
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // Automatic Title Generation on first message of a session
        if (_messages.value.isEmpty() || _activeSessionId.value == null) {
            val autoTitle = generateAutomaticTitle(userText)
            _activeSessionTitle.value = autoTitle
            _activeSessionId.value = UUID.randomUUID().toString()
        }

        val userMsg = AiMessage(role = AiRole.USER, content = userText)
        val assistantMsgId = UUID.randomUUID().toString()
        val initialAssistantMsg = AiMessage(id = assistantMsgId, role = AiRole.ASSISTANT, content = "")

        val existingHistory = _messages.value
        _messages.value = _messages.value + userMsg + initialAssistantMsg
        _isGenerating.value = true

        persistCurrentSession()

        val config = uiState.value.config

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
            val streamFlow: Flow<String> = if (config.providerType == AiProviderType.LOCAL_GEMMA) {
                val isLoaded = localGemmaManager.initializeEngine(config)
                if (isLoaded) {
                    localGemmaManager.generateStream(userText, historyContext, config)
                } else {
                    kotlinx.coroutines.flow.flowOf("Error initializing LiteRT-LM engine. Verify model file in Settings or select a Cloud Provider.")
                }
            } else {
                remoteAiManager.generateStream(userText, historyContext, config)
            }

            val contentBuilder = StringBuilder()
            var lastUpdateMs = 0L

            try {
                streamFlow.collect { chunk ->
                    contentBuilder.append(chunk)
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateMs > 30L) {
                        lastUpdateMs = now
                        val currentText = contentBuilder.toString()
                        _messages.value = _messages.value.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(content = currentText) else msg
                        }
                        persistCurrentSession() // Token-by-token live saving
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
                persistCurrentSession() // Final token save
                if (finalRes.contains("```")) {
                    _generatedEditPreview.value = finalRes
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        persistCurrentSession()
    }

    fun startNewSession() {
        persistCurrentSession()
        _activeSessionId.value = UUID.randomUUID().toString()
        _activeSessionTitle.value = "NEW CHAT"
        _messages.value = emptyList()
        _generatedEditPreview.value = null
    }

    fun loadSession(session: ChatSession) {
        persistCurrentSession()
        _activeSessionId.value = session.id
        _activeSessionTitle.value = session.title
        _messages.value = session.messages
        _generatedEditPreview.value = null
    }

    fun deleteSession(sessionId: String) {
        _historySessions.value = _historySessions.value.filterNot { it.id == sessionId }
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = null
            _activeSessionTitle.value = "NEW CHAT"
            _messages.value = emptyList()
        }
        viewModelScope.launch {
            try {
                val jsonStr = json.encodeToString(_historySessions.value)
                preferencesManager.saveAiSessionsJson(jsonStr)
            } catch (_: Exception) {}
        }
    }

    fun clearHistory() {
        startNewSession()
    }

    fun dismissEditPreview() {
        _generatedEditPreview.value = null
    }

    override fun onCleared() {
        super.onCleared()
        localGemmaManager.close()
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
