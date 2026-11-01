package com.whisk

import com.whisk.data.repository.ChatRepository
import com.whisk.domain.model.AiMessage
import com.whisk.domain.model.AiRole
import com.whisk.ui.screens.ai.ChatSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun testSessionAndMessagePersistenceWithMetadata() = runTest {
        val chatDao = FakeChatDao()
        val repository = ChatRepository(chatDao)

        val message1 = AiMessage(id = "m1", role = AiRole.USER, content = "Hello Room")
        val message2 = AiMessage(id = "m2", role = AiRole.ASSISTANT, content = "Hello Developer")
        val session = ChatSession(id = "session_101", title = "Room Architecture Chat", messages = listOf(message1, message2))

        repository.saveSession(
            session = session,
            provider = "LOCAL_GEMMA",
            model = "gemma-4-E2B-it-gpu.litertlm",
            contextRef = "note_architecture_01",
        )
        advanceUntilIdle()

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)
        val savedSession = sessions[0]
        assertEquals("session_101", savedSession.id)
        assertEquals("Room Architecture Chat", savedSession.title)
        assertEquals("LOCAL_GEMMA", savedSession.provider)
        assertEquals("gemma-4-E2B-it-gpu.litertlm", savedSession.model)
        assertEquals("note_architecture_01", savedSession.contextRef)

        val messages = repository.getMessagesForSession("session_101").first()
        assertEquals(2, messages.size)
        assertEquals("Hello Room", messages[0].content)
        assertEquals("Hello Developer", messages[1].content)
    }

    @Test
    fun testPaginatedMessageLoading() = runTest {
        val chatDao = FakeChatDao()
        val repository = ChatRepository(chatDao)

        val session = ChatSession(id = "session_paginated", title = "Long Chat")
        repository.saveSession(session)

        for (i in 1..10) {
            val msg = AiMessage(id = "m_$i", role = if (i % 2 == 1) AiRole.USER else AiRole.ASSISTANT, content = "Message $i", timestamp = 1000L + i)
            repository.saveMessage("session_paginated", msg)
        }
        advanceUntilIdle()

        val page1 = repository.getMessagesForSessionPaginated("session_paginated", limit = 5, offset = 0)
        assertEquals(5, page1.size)
        assertEquals("Message 1", page1[0].content)
        assertEquals("Message 5", page1[4].content)

        val page2 = repository.getMessagesForSessionPaginated("session_paginated", limit = 5, offset = 5)
        assertEquals(5, page2.size)
        assertEquals("Message 6", page2[0].content)
        assertEquals("Message 10", page2[4].content)
    }

    @Test
    fun testTransactionalSessionDeletionCascadesToMessages() = runTest {
        val chatDao = FakeChatDao()
        val repository = ChatRepository(chatDao)

        val session = ChatSession(id = "s_delete", title = "To Delete", messages = listOf(AiMessage(id = "m_del", role = AiRole.USER, content = "Transient")))
        repository.saveSession(session)
        advanceUntilIdle()

        assertEquals(1, repository.allSessions.first().size)
        assertEquals(1, repository.getMessagesForSession("s_delete").first().size)

        repository.deleteSession("s_delete")
        advanceUntilIdle()

        assertEquals(0, repository.allSessions.first().size)
        assertEquals(0, repository.getMessagesForSession("s_delete").first().size)
        assertTrue(chatDao.getAllMessagesDirect().isEmpty())
    }
}
