package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.dao.AIConversationDao
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.ChatMessageEntity
import com.example.aiaccounting.data.local.entity.ChatSession
import com.example.aiaccounting.data.local.entity.ConversationRole
import com.example.aiaccounting.data.local.entity.MessageRole
import com.example.aiaccounting.data.repository.AIConversationRepository
import com.example.aiaccounting.data.repository.ChatSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AIAssistantSessionRestoreTest {

    @Test
    fun `getOrCreateCurrentSession prefers active session when current is null`() = runBlocking {
        val chatSessionRepository = mockk<ChatSessionRepository>()
        val activeSession = ChatSession(id = "active-session", title = "Active", isActive = true)
        val coordinator = AIAssistantSessionCoordinator(chatSessionRepository)

        coEvery { chatSessionRepository.getActiveSession() } returns activeSession
        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = "other-session")))

        val result = coordinator.getOrCreateCurrentSession(null)

        assertEquals("active-session", result)
    }

    @Test
    fun `restoreMessage preserves timestamp and image uri`() = runBlocking {
        val conversationDao = mockk<AIConversationDao>(relaxed = true)
        val repository = AIConversationRepository(conversationDao)
        val restored = mutableListOf<AIConversation>()

        coEvery { conversationDao.insertConversation(any()) } answers {
            restored.add(firstArg<AIConversation>())
            1L
        }

        repository.restoreMessage(
            role = ConversationRole.USER,
            content = "发送了1张图片",
            timestamp = 1_775_520_000_000,
            imageUri = "content://image/1"
        )

        assertEquals(1, restored.size)
        assertEquals(ConversationRole.USER, restored.first().role)
        assertEquals("发送了1张图片", restored.first().content)
        assertEquals(1_775_520_000_000, restored.first().timestamp)
        assertEquals("content://image/1", restored.first().imageUri)
    }

    @Test
    fun `switchSession restores original message metadata`() = runBlocking {
        val chatSessionRepository = mockk<ChatSessionRepository>()
        val coordinator = AIAssistantSessionCoordinator(chatSessionRepository)
        val timestamp = 1_775_520_000_000L
        val messages = listOf(
            ChatMessageEntity(
                sessionId = "session-1",
                role = MessageRole.USER,
                content = "发送了1张图片",
                timestamp = timestamp,
                imageUris = "content://image/1,content://image/2"
            )
        )

        coEvery { chatSessionRepository.switchSession("session-1") } returns Unit
        every { chatSessionRepository.getMessages("session-1") } returns flowOf(messages)

        val result = coordinator.switchSession("session-1")

        assertEquals(1, result.size)
        assertEquals(timestamp, result.first().timestamp)
        assertEquals("content://image/1,content://image/2", result.first().imageUris)
        coVerify(exactly = 1) { chatSessionRepository.switchSession("session-1") }
    }
}
