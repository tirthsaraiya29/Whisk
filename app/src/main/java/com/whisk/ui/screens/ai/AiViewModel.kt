package com.whisk.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whisk.ai.CitationReference
import com.whisk.ai.ContextPayload
import com.whisk.ai.LocalGemmaManager
import com.whisk.ai.RemoteAiManager
import com.whisk.data.local.PreferencesManager
import com.whisk.data.rag.indexer.DocumentIndexer
import com.whisk.data.rag.indexer.IndexingProgress
import com.whisk.data.rag.repository.IndexingRepository
import com.whisk.data.repository.ChatRepository
import com.whisk.data.repository.NotesRepository
import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.AiMessage
import com.whisk.domain.model.AiProviderType
import com.whisk.domain.model.AiRole
import com.whisk.domain.model.LocalModelStatus
import com.whisk.domain.model.MessageStatus
import com.whisk.domain.model.Note
import com.whisk.domain.rag.retrieval.HybridSearchEngine
import com.whisk.domain.rag.retrieval.RetrievalFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<AiMessage> = emptyList(),
)

data class AiUiState(
    val config: AiConfig = AiConfig(),
    val localStatus: LocalModelStatus = LocalModelStatus(),
    val messages: List<AiMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val contextPayload: ContextPayload = ContextPayload(),
    val generatedEditPreview: String? = null,
    val historySessions: List<ChatSession> = emptyList(),
    val activeSessionTitle: String = "NEW CHAT",
    val isVaultRagEnabled: Boolean = true,
    val selectedSearchScopeTypeId: String? = null,
    val isSearchingVault: Boolean = false,
    val retrievedSources: List<CitationReference> = emptyList(),
    val indexingProgress: IndexingProgress = IndexingProgress(),
    val estimatedTokensUsed: Int = 0,
    val maxContextTokens: Int = 4096,
    val isLocalModelMissing: Boolean = false,
)

class AiViewModel(
    private val localGemmaManager: LocalGemmaManager,
    private val remoteAiManager: RemoteAiManager,
    private val preferencesManager: PreferencesManager,
    private val chatRepository: ChatRepository,
    private val hybridSearchEngine: HybridSearchEngine? = null,
    private val documentIndexer: DocumentIndexer? = null,
    private val indexingRepository: IndexingRepository? = null,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    private val _isGenerating = MutableStateFlow(value = false)
    private val _contextPayload = MutableStateFlow(ContextPayload())
    private val _generatedEditPreview = MutableStateFlow<String?>(null)
    private val _historySessions = MutableStateFlow<List<ChatSession>>(emptyList())
    private val _activeSessionId = MutableStateFlow<String?>(null)
    private val _activeSessionTitle = MutableStateFlow("NEW CHAT")

    private val _isVaultRagEnabled = MutableStateFlow(true)
    private val _selectedSearchScopeTypeId = MutableStateFlow<String?>(null)
    private val _isSearchingVault = MutableStateFlow(false)
    private val _retrievedSources = MutableStateFlow<List<CitationReference>>(emptyList())

    private var generationJob: Job? = null
    private var activeGenerationId: String? = null
    private var messageObserveJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    init {
        // Sanitize any left-over generating messages from process termination
        viewModelScope.launch {
            try {
                chatRepository.sanitizeInterruptedMessages()
            } catch (_: Exception) {}
        }

        // Observe sessions from Room
        viewModelScope.launch {
            chatRepository.allSessions.collect { sessionEntities ->
                val sessions = sessionEntities.map { (id, title, timestamp) ->
                    ChatSession(
                        id = id,
                        title = title,
                        timestamp = timestamp,
                        messages = emptyList(),
                    )
                }
                _historySessions.value = sessions

                if ((_activeSessionId.value == null) && sessions.isNotEmpty()) {
                    val first = sessions.first()
                    _activeSessionId.value = first.id
                    _activeSessionTitle.value = first.title
                    loadMessagesForActiveSession(first.id)
                    localGemmaManager.resetConversation(first.id)
                }
            }
        }

        // Migrate legacy sessions from DataStore JSON into Room
        viewModelScope.launch {
            preferencesManager.savedAiSessionsJson.collect { jsonStr ->
                if (jsonStr.isNotBlank() && jsonStr != "[]") {
                    try {
                        val legacySessions = json.decodeFromString<List<ChatSession>>(jsonStr)
                        for (session in legacySessions) {
                            chatRepository.saveSession(session)
                        }
                        preferencesManager.saveAiSessionsJson("")
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun loadMessagesForActiveSession(sessionId: String) {
        messageObserveJob?.cancel()
        messageObserveJob = viewModelScope.launch {
            chatRepository.getMessagesForSession(sessionId).collect { msgs ->
                // Ensure interrupted status is preserved
                _messages.value = msgs
            }
        }
    }

    private val dummyIndexerProgress = MutableStateFlow(IndexingProgress())

    val uiState: StateFlow<AiUiState> = combine(
        combine(
            preferencesManager.aiConfig,
            localGemmaManager.modelStatus,
            _messages,
            _isGenerating,
        ) { config, localStatus, messages, isGenerating ->
            Tuple4(config, localStatus, messages, isGenerating)
        },
        combine(
            _contextPayload,
            _generatedEditPreview,
            _historySessions,
            _activeSessionTitle,
        ) { contextPayload, editPreview, historySessions, title ->
            Tuple4(contextPayload, editPreview, historySessions, title)
        },
        combine(
            _isVaultRagEnabled,
            _selectedSearchScopeTypeId,
            _isSearchingVault,
            _retrievedSources,
            documentIndexer?.progress ?: dummyIndexerProgress,
        ) { ragEnabled, scopeType, isSearching, sources, indexProgress ->
            Tuple5(ragEnabled, scopeType, isSearching, sources, indexProgress)
        },
    ) { (config, localStatus, messages, isGenerating), (contextPayload, editPreview, historySessions, title), (ragEnabled, scopeType, isSearching, sources, indexProgress) ->
        val estimatedTokens = messages.sumOf { it.content.length } / 4
        val isModelMissing = config.providerType == AiProviderType.LOCAL_GEMMA && !localStatus.isDownloaded

        AiUiState(
            config = config,
            localStatus = localStatus,
            messages = messages,
            isGenerating = isGenerating,
            contextPayload = contextPayload,
            generatedEditPreview = editPreview,
            historySessions = historySessions,
            activeSessionTitle = title,
            isVaultRagEnabled = ragEnabled,
            selectedSearchScopeTypeId = scopeType,
            isSearchingVault = isSearching,
            retrievedSources = sources,
            indexingProgress = indexProgress,
            estimatedTokensUsed = estimatedTokens,
            maxContextTokens = config.maxTokens,
            isLocalModelMissing = isModelMissing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiUiState())

    fun exportChatToNote(notesRepository: NotesRepository, onExported: (String) -> Unit) {
        val msgs = _messages.value
        if (msgs.isEmpty()) return
        viewModelScope.launch {
            val title = "AI Conversation - ${_activeSessionTitle.value}"
            val body = buildString {
                append("# ").append(title).append("\n\n")
                append("Exported: ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())).append("\n\n")
                msgs.forEach { m ->
                    val roleLabel = if (m.role == AiRole.USER) "**User:**" else "**Whisk AI:**"
                    append(roleLabel).append("\n\n").append(m.content).append("\n\n---\n\n")
                }
            }
            val note = Note(
                title = title,
                body = body,
                tags = listOf("ai-chat", "conversation"),
            )
            notesRepository.saveNote(note)
            onExported(note.id)
        }
    }

    fun toggleVaultRag(enabled: Boolean) {
        _isVaultRagEnabled.value = enabled
    }

    fun setSearchScope(typeId: String?) {
        _selectedSearchScopeTypeId.value = typeId
    }

    fun triggerWorkspaceIndexing(forceReindex: Boolean = false) {
        viewModelScope.launch {
            documentIndexer?.indexWorkspace(forceReindex = forceReindex)
        }
    }

    fun clearIndex() {
        viewModelScope.launch {
            documentIndexer?.clearIndex()
        }
    }

    fun setContextNote(note: Note?) {
        _contextPayload.value = _contextPayload.value.copy(activeNote = note)
    }

    fun loadNoteContext(notesRepository: com.whisk.data.repository.NotesRepository, noteId: String) {
        viewModelScope.launch {
            val note = notesRepository.getNoteById(noteId)
            note?.let { setContextNote(it) }
        }
    }

    fun switchProvider(providerType: AiProviderType) {
        stopGeneration()
        viewModelScope.launch {
            val currentConfig = uiState.value.config
            preferencesManager.saveAiConfig(currentConfig.copy(providerType = providerType))
            if (providerType == AiProviderType.LOCAL_GEMMA) {
                localGemmaManager.resetConversation(_activeSessionId.value)
            }
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
        return cleanTitle.ifBlank { "AI CHAT" }
    }

    private fun persistCurrentSession() {
        val currentMsgs = _messages.value
        val sessionId = _activeSessionId.value ?: return
        val sessionTitle = _activeSessionTitle.value
        val config = uiState.value.config

        val currentSession = ChatSession(
            id = sessionId,
            title = sessionTitle,
            timestamp = System.currentTimeMillis(),
            messages = currentMsgs,
        )

        viewModelScope.launch {
            try {
                chatRepository.saveSession(
                    session = currentSession,
                    provider = config.providerType.name,
                    model = config.selectedModel,
                    contextRef = _contextPayload.value.activeNote?.id,
                )
            } catch (_: Exception) {}
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        if (_isGenerating.value) return

        if (_messages.value.isEmpty() || _activeSessionId.value == null) {
            val autoTitle = generateAutomaticTitle(userText)
            _activeSessionTitle.value = autoTitle
            if (_activeSessionId.value == null) {
                val newId = UUID.randomUUID().toString()
                _activeSessionId.value = newId
                loadMessagesForActiveSession(newId)
            }
        }

        val currentSessionId = _activeSessionId.value ?: return
        val generationId = UUID.randomUUID().toString()
        activeGenerationId = generationId

        val userMsg = AiMessage(role = AiRole.USER, content = userText, status = MessageStatus.COMPLETED)
        val assistantMsgId = UUID.randomUUID().toString()
        val initialAssistantMsg = AiMessage(
            id = assistantMsgId,
            role = AiRole.ASSISTANT,
            content = "",
            status = MessageStatus.GENERATING,
        )

        val existingHistory = _messages.value
        _messages.value = _messages.value + userMsg + initialAssistantMsg
        _isGenerating.value = true

        val config = uiState.value.config

        generationJob = viewModelScope.launch {
            // Await initial message persistence before starting stream pipeline
            try {
                chatRepository.saveMessage(currentSessionId, userMsg)
                chatRepository.saveMessage(currentSessionId, initialAssistantMsg)
                persistCurrentSession()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _isGenerating.value = false
                val errorMsg = AiMessage(
                    id = assistantMsgId,
                    role = AiRole.ASSISTANT,
                    content = "Database persistence error: ${e.message}",
                    status = MessageStatus.FAILED,
                )
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == assistantMsgId) errorMsg else msg
                }
                return@launch
            }

            // Run RAG hybrid retrieval if enabled
            val retrievedChunks = if (_isVaultRagEnabled.value && hybridSearchEngine != null) {
                _isSearchingVault.value = true
                try {
                    when (val searchRes = hybridSearchEngine.search(
                        query = userText,
                        topK = 8,
                        filter = RetrievalFilter(typeId = _selectedSearchScopeTypeId.value),
                    )) {
                        is com.whisk.domain.rag.retrieval.SearchResult.Success -> searchRes.chunks
                        is com.whisk.domain.rag.retrieval.SearchResult.Failure -> emptyList()
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    emptyList()
                } finally {
                    _isSearchingVault.value = false
                }
            } else {
                emptyList()
            }

            val currentPayload = _contextPayload.value.copy(retrievedChunks = retrievedChunks)
            val (ragContextText, citations) = currentPayload.formatContextWithCitations()
            _retrievedSources.value = citations

            val historyContext = buildString {
                if (existingHistory.isNotEmpty()) {
                    append("Previous Conversation:\n")
                    existingHistory.takeLast(6).forEach { (_, role, content) ->
                        val sender = if (role == AiRole.USER) "User" else "Assistant"
                        if (content.isNotBlank()) {
                            append("$sender: ").append(content.take(200)).append("\n")
                        }
                    }
                    append("\n")
                }
                append(ragContextText)
            }

            val streamFlow: Flow<String> = if (config.providerType == AiProviderType.LOCAL_GEMMA) {
                val isLoaded = localGemmaManager.initializeEngine(config)
                if (isLoaded) {
                    localGemmaManager.generateStream(userText, historyContext, config, currentSessionId)
                } else {
                    kotlinx.coroutines.flow.flowOf("Error initializing LiteRT-LM engine. Verify model file in Settings or select a Cloud Provider.")
                }
            } else {
                remoteAiManager.generateStream(userText, historyContext, config)
            }

            val contentBuilder = StringBuilder()
            var lastUpdateMs = 0L
            var finalStatus = MessageStatus.COMPLETED

            try {
                streamFlow.collect { chunk ->
                    if (activeGenerationId != generationId) {
                        return@collect
                    }
                    contentBuilder.append(chunk)
                    val now = System.currentTimeMillis()
                    if ((now - lastUpdateMs) > 250L) {
                        lastUpdateMs = now
                        val currentText = contentBuilder.toString()
                        if (_activeSessionId.value == currentSessionId) {
                            _messages.value = _messages.value.map { msg ->
                                if (msg.id == assistantMsgId) msg.copy(content = currentText, status = MessageStatus.GENERATING) else msg
                            }
                        }
                        // Incremental durable persistence
                        chatRepository.updateMessageContentAndStatus(assistantMsgId, currentText, MessageStatus.GENERATING.name)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    finalStatus = MessageStatus.CANCELLED
                } else {
                    finalStatus = MessageStatus.FAILED
                    val errText = "Error during generation: ${e.message}"
                    contentBuilder.clear().append(errText)
                }
            } finally {
                _isGenerating.value = false
                val finalRes = contentBuilder.toString()
                val finalAssistantMsg = AiMessage(
                    id = assistantMsgId,
                    role = AiRole.ASSISTANT,
                    content = finalRes,
                    status = finalStatus,
                )
                if (_activeSessionId.value == currentSessionId) {
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == assistantMsgId) finalAssistantMsg else msg
                    }
                    if (finalRes.contains("```") && finalStatus == MessageStatus.COMPLETED) {
                        _generatedEditPreview.value = finalRes
                    }
                }

                try {
                    chatRepository.saveMessage(currentSessionId, finalAssistantMsg)
                    persistCurrentSession()
                } catch (_: Exception) {}

                if (activeGenerationId == generationId) {
                    activeGenerationId = null
                }
            }
        }
    }

    fun stopGeneration() {
        val jobToCancel = generationJob
        generationJob = null
        jobToCancel?.cancel()
        _isGenerating.value = false
        persistCurrentSession()
    }

    fun startNewSession() {
        stopGeneration()
        val newSessionId = UUID.randomUUID().toString()
        _activeSessionId.value = newSessionId
        _activeSessionTitle.value = "NEW CHAT"
        _messages.value = emptyList()
        _generatedEditPreview.value = null
        _retrievedSources.value = emptyList()
        loadMessagesForActiveSession(newSessionId)
        viewModelScope.launch {
            localGemmaManager.resetConversation(newSessionId)
        }
    }

    fun loadSession(session: ChatSession) {
        stopGeneration()
        _activeSessionId.value = session.id
        _activeSessionTitle.value = session.title
        _generatedEditPreview.value = null
        _retrievedSources.value = emptyList()
        loadMessagesForActiveSession(session.id)
        viewModelScope.launch {
            localGemmaManager.resetConversation(session.id)
        }
    }

    fun deleteSession(sessionId: String) {
        val isActive = (_activeSessionId.value == sessionId)
        if (isActive) {
            stopGeneration()
        }
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (isActive) {
                val newSessionId = UUID.randomUUID().toString()
                _activeSessionId.value = newSessionId
                _activeSessionTitle.value = "NEW CHAT"
                _messages.value = emptyList()
                _generatedEditPreview.value = null
                _retrievedSources.value = emptyList()
                loadMessagesForActiveSession(newSessionId)
                localGemmaManager.resetConversation(newSessionId)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.deleteAllSessions()
            startNewSession()
        }
    }

    fun dismissEditPreview() {
        _generatedEditPreview.value = null
    }

    override fun onCleared() {
        localGemmaManager.close()
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)
