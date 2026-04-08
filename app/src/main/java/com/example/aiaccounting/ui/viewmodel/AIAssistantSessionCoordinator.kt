package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.local.entity.ChatMessageEntity
import com.example.aiaccounting.data.local.entity.ChatSession
import com.example.aiaccounting.data.repository.ChatSessionRepository
import kotlinx.coroutines.flow.first

internal sealed class DeleteSessionResult {
    data object NoChange : DeleteSessionResult()
    data object ResetCurrentSingleSession : DeleteSessionResult()
    data object DeletedNonCurrentSession : DeleteSessionResult()
    data object ClearedCurrentSession : DeleteSessionResult()
    data class SwitchedToSession(
        val sessionId: String,
        val messages: List<ChatMessageEntity>
    ) : DeleteSessionResult()
}

internal class AIAssistantSessionCoordinator(
    private val chatSessionRepository: ChatSessionRepository
) {
    suspend fun getOrCreateCurrentSession(currentSessionId: String?): String {
        currentSessionId?.let { return it }

        val activeSession = chatSessionRepository.getActiveSession()
        if (activeSession != null) {
            return activeSession.id
        }

        val existingSession = chatSessionRepository.getAllSessions().first().firstOrNull()
        return existingSession?.id ?: chatSessionRepository.createSession("新对话").id
    }

    suspend fun createNewSession(title: String): ChatSession {
        return chatSessionRepository.createSession(title)
    }

    suspend fun switchSession(sessionId: String): List<ChatMessageEntity> {
        chatSessionRepository.switchSession(sessionId)
        return chatSessionRepository.getMessages(sessionId).first()
    }

    suspend fun deleteSession(
        sessionId: String,
        currentSessionId: String?
    ): DeleteSessionResult {
        val allSessions = chatSessionRepository.getAllSessions().first()

        if (allSessions.size <= 1) {
            return if (currentSessionId == sessionId) {
                chatSessionRepository.updateSessionTitle(sessionId, "新对话")
                DeleteSessionResult.ResetCurrentSingleSession
            } else {
                DeleteSessionResult.NoChange
            }
        }

        chatSessionRepository.deleteSession(sessionId)

        if (currentSessionId != sessionId) {
            return DeleteSessionResult.DeletedNonCurrentSession
        }

        val replacementSession = chatSessionRepository.getAllSessions().first().firstOrNull()
            ?: return DeleteSessionResult.ClearedCurrentSession
        val replacementMessages = chatSessionRepository.getMessages(replacementSession.id).first()

        return DeleteSessionResult.SwitchedToSession(
            sessionId = replacementSession.id,
            messages = replacementMessages
        )
    }

    suspend fun renameSession(sessionId: String, newTitle: String) {
        chatSessionRepository.updateSessionTitle(sessionId, newTitle)
    }
}
