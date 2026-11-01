package com.whisk

import android.content.Context
import android.content.ContextWrapper
import com.whisk.ai.LocalGemmaManager
import com.whisk.ai.RemoteAiManager
import com.whisk.data.local.PreferencesManager
import com.whisk.domain.model.AiConfig
import com.whisk.domain.model.AiProviderType
import com.whisk.ui.screens.ai.AiViewModel
import com.whisk.ui.screens.ai.ChatSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TestContext : ContextWrapper(null)

class FakeLocalGemmaManager(context: Context = TestContext()) : LocalGemmaManager(context) {

    var resetCount = 0
    var lastResetSessionId: String? = null
    val generationCalls = mutableListOf<GenerationCall>()
    var streamDelayMs: Long = 0L

    data class GenerationCall(
        val prompt: String,
        val contextText: String,
        val sessionId: String?,
    )

    override fun checkModelFile() {}

    override suspend fun initializeEngine(config: AiConfig): Boolean = true

    override suspend fun resetConversation(sessionId: String?) {
        resetCount++
        lastResetSessionId = sessionId
    }

    override fun generateStream(
        prompt: String,
        contextText: String,
        config: AiConfig,
        sessionId: String?,
    ): Flow<String> = flow {
        generationCalls.add(GenerationCall(prompt, contextText, sessionId))
        if (streamDelayMs > 0) {
            delay(streamDelayMs.milliseconds)
        }
        emit("Response for: $prompt")
    }
}

class FakePreferencesManager(context: Context = TestContext()) : PreferencesManager(context) {
    val configFlow = MutableStateFlow(AiConfig(providerType = AiProviderType.LOCAL_GEMMA))
    val sessionsFlow = MutableStateFlow("[]")

    override val aiConfig: Flow<AiConfig> = configFlow
    override val savedAiSessionsJson: Flow<String> = sessionsFlow

    override suspend fun saveAiConfig(config: AiConfig) {
        configFlow.value = config
    }

    override suspend fun saveAiSessionsJson(jsonStr: String) {
        sessionsFlow.value = jsonStr
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AiIsolationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testNewChatIsIsolatedFromPreviousChatHistory() = runTest {
        val fakeLocalGemmaManager = FakeLocalGemmaManager()
        val fakeRemoteAiManager = RemoteAiManager()
        val fakePreferencesManager = FakePreferencesManager()

        val viewModel = AiViewModel(fakeLocalGemmaManager, fakeRemoteAiManager, fakePreferencesManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // 1. Send prompt in Session 1 containing sensitive context
        viewModel.sendMessage("Top secret API key is SECRET_98765")
        advanceUntilIdle()

        val session1Messages = viewModel.uiState.value.messages
        assertTrue(session1Messages.any { it.content.contains("SECRET_98765") })
        val session1Id = viewModel.uiState.value.historySessions.firstOrNull()?.id

        // Reset tracking counters
        fakeLocalGemmaManager.generationCalls.clear()
        val resetCountBefore = fakeLocalGemmaManager.resetCount

        // 2. Start a new session
        viewModel.startNewSession()
        advanceUntilIdle()

        // Verify state reset and conversation reset call
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertTrue(fakeLocalGemmaManager.resetCount > resetCountBefore)

        // 3. Send prompt in Session 2
        viewModel.sendMessage("What is the top secret API key?")
        advanceUntilIdle()

        val session2Id = viewModel.uiState.value.historySessions.firstOrNull()?.id
        assertNotEquals(session1Id, session2Id)

        // 4. Verify Session 2 prompt & context sent to model does NOT leak Session 1 secret
        val lastCall = fakeLocalGemmaManager.generationCalls.lastOrNull()
        assertTrue(lastCall != null)
        assertFalse(lastCall!!.contextText.contains("SECRET_98765"))
        assertFalse(lastCall.prompt.contains("SECRET_98765"))

        // Verify Session 2 messages contain NO leakage from Session 1
        val session2Messages = viewModel.uiState.value.messages
        assertTrue(session2Messages.none { it.content.contains("SECRET_98765") })
    }

    @Test
    fun testSessionSwitchingCancelsActiveGenerationAndResetsModelConversation() = runTest {
        val fakeLocalGemmaManager = FakeLocalGemmaManager()
        val fakeRemoteAiManager = RemoteAiManager()
        val fakePreferencesManager = FakePreferencesManager()

        val viewModel = AiViewModel(fakeLocalGemmaManager, fakeRemoteAiManager, fakePreferencesManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Create session A and B
        val sessionA = ChatSession(id = "session_A", title = "Session A", messages = emptyList())
        val sessionB = ChatSession(id = "session_B", title = "Session B", messages = emptyList())

        viewModel.loadSession(sessionA)
        advanceUntilIdle()

        val resetCountBefore = fakeLocalGemmaManager.resetCount

        // Switch to session B
        viewModel.loadSession(sessionB)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isGenerating)
        assertTrue(fakeLocalGemmaManager.resetCount > resetCountBefore)
        assertEquals("session_B", fakeLocalGemmaManager.lastResetSessionId)
    }

    @Test
    fun testConcurrentGenerationsAreBlocked() = runTest {
        val fakeLocalGemmaManager = FakeLocalGemmaManager()
        fakeLocalGemmaManager.streamDelayMs = 500L // Delay stream to keep generation active

        val fakeRemoteAiManager = RemoteAiManager()
        val fakePreferencesManager = FakePreferencesManager()

        val viewModel = AiViewModel(fakeLocalGemmaManager, fakeRemoteAiManager, fakePreferencesManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Send first prompt (starts generation and delays 500ms)
        viewModel.sendMessage("First generation prompt")
        assertTrue(viewModel.uiState.value.isGenerating)

        // Attempt a second concurrent generation immediately while generation is active
        viewModel.sendMessage("Second concurrent prompt")

        // Finish stream delay
        advanceUntilIdle()

        // The second prompt should have been blocked and not added to messages
        val userMessages = viewModel.uiState.value.messages.filter { it.role == com.whisk.domain.model.AiRole.USER }
        assertEquals(1, userMessages.size)
        assertEquals("First generation prompt", userMessages[0].content)
    }
}
