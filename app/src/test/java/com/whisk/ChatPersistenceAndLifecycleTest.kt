package com.whisk

import com.whisk.data.local.ChatMessageEntity
import com.whisk.data.repository.ChatRepository
import com.whisk.domain.model.AiMessage
import com.whisk.domain.model.AiRole
import com.whisk.domain.model.MessageStatus
import com.whisk.ui.screens.ai.ChatSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatPersistenceAndLifecycleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testIncrementalStreamingPersistence() = runTest {
        val chatDao = FakeChatDao()
        val repository = ChatRepository(chatDao)

        val session = ChatSession(id = "s_stream", title = "Streaming Chat")
        repository.saveSession(session)

        val userMsg = AiMessage(id = "user_1", role = AiRole.USER, content = "Stream test")
        val initialAssistantMsg = AiMessage(
            id = "asst_1",
            role = AiRole.ASSISTANT,
            content = "Partial...",
            status = MessageStatus.GENERATING,
        )

        repository.saveMessage("s_stream", userMsg)
        repository.saveMessage("s_stream", initialAssistantMsg)
        advanceUntilIdle()

        var msgs = repository.getMessagesForSession("s_stream").first()
        assertEquals(2, msgs.size)
        assertEquals(MessageStatus.GENERATING, msgs[1].status)
        assertEquals("Partial...", msgs[1].content)

        // Update during streaming
        repository.updateMessageContentAndStatus("asst_1", "Partial... Complete!", MessageStatus.GENERATING.name)
        advanceUntilIdle()

        msgs = repository.getMessagesForSession("s_stream").first()
        assertEquals("Partial... Complete!", msgs[1].content)
        assertEquals(MessageStatus.GENERATING, msgs[1].status)

        // Final completion
        val finalAssistantMsg = initialAssistantMsg.copy(
            content = "Partial... Complete!",
            status = MessageStatus.COMPLETED,
        )
        repository.saveMessage("s_stream", finalAssistantMsg)
        advanceUntilIdle()

        msgs = repository.getMessagesForSession("s_stream").first()
        assertEquals(MessageStatus.COMPLETED, msgs[1].status)
    }

    @Test
    fun testProcessDeathInterruptedRecovery() = runTest {
        val chatDao = FakeChatDao()
        val repository = ChatRepository(chatDao)

        val session = ChatSession(id = "s_interrupted", title = "Process Death Chat")
        repository.saveSession(session)

        // Save a message that was left in GENERATING state when process died
        val leftOverMsg = ChatMessageEntity(
            id = "msg_interrupted_1",
            sessionId = "s_interrupted",
            role = "ASSISTANT",
            content = "Partial token content before kill",
            timestamp = 1000L,
            status = "GENERATING",
        )
        chatDao.insertMessage(leftOverMsg)
        advanceUntilIdle()

        assertEquals("GENERATING", chatDao.getAllMessagesDirect().first().status)

        // On app startup, sanitizeInterruptedMessages is called
        val sanitizedCount = repository.sanitizeInterruptedMessages()
        advanceUntilIdle()

        assertEquals(1, sanitizedCount)
        val msgs = repository.getMessagesForSession("s_interrupted").first()
        assertEquals(1, msgs.size)
        assertEquals(MessageStatus.INTERRUPTED, msgs[0].status)
        assertEquals("Partial token content before kill", msgs[0].content)
    }

    @Test
    fun testCancelledAndFailedMessageStatusPersistence() = runTest {
        val chatDao = FakeChatDao()
        val repository = ChatRepository(chatDao)

        val session = ChatSession(id = "s_status", title = "Status Chat")
        repository.saveSession(session)

        val cancelledMsg = AiMessage(id = "m_cancel", role = AiRole.ASSISTANT, content = "Cancelled output", status = MessageStatus.CANCELLED)
        val failedMsg = AiMessage(id = "m_failed", role = AiRole.ASSISTANT, content = "Error text", status = MessageStatus.FAILED)

        repository.saveMessage("s_status", cancelledMsg)
        repository.saveMessage("s_status", failedMsg)
        advanceUntilIdle()

        val msgs = repository.getMessagesForSession("s_status").first()
        assertEquals(2, msgs.size)
        assertEquals(MessageStatus.CANCELLED, msgs[0].status)
        assertEquals(MessageStatus.FAILED, msgs[1].status)
    }

    @Test
    fun testNoCrossSessionContamination() = runTest {
        val chatDao = FakeChatDao()
        val repository = ChatRepository(chatDao)

        val session1 = ChatSession(id = "s_1", title = "Session 1")
        val session2 = ChatSession(id = "s_2", title = "Session 2")
        repository.saveSession(session1)
        repository.saveSession(session2)

        repository.saveMessage("s_1", AiMessage(id = "m_s1", role = AiRole.USER, content = "For Session 1"))
        repository.saveMessage("s_2", AiMessage(id = "m_s2", role = AiRole.USER, content = "For Session 2"))
        advanceUntilIdle()

        val msgs1 = repository.getMessagesForSession("s_1").first()
        val msgs2 = repository.getMessagesForSession("s_2").first()

        assertEquals(1, msgs1.size)
        assertEquals("For Session 1", msgs1[0].content)

        assertEquals(1, msgs2.size)
        assertEquals("For Session 2", msgs2[0].content)
    }
}
