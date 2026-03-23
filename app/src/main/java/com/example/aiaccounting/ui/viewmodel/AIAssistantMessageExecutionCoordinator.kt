package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler

internal class AIAssistantMessageExecutionCoordinator(
    private val aiReasoningEngine: AIReasoningEngine,
    private val messageOrchestrator: AIAssistantMessageOrchestrator
) {
    suspend fun execute(
        message: String,
        currentButler: Butler,
        conversationHistory: List<String>,
        isNetworkAvailable: Boolean,
        currentUseBuiltinConfig: Boolean,
        currentAIConfig: AIConfig,
        pendingState: PendingModificationState?,
        handleModificationConfirmation: suspend (String, String) -> String,
        handleTransactionModification: suspend (String, String) -> String,
        processWithRemoteAI: suspend (String) -> String
    ): String {
        pendingState?.let {
            return handleModificationConfirmation(message, currentButler.id)
        }

        val context = AIReasoningEngine.ReasoningContext(
            userMessage = message,
            conversationHistory = conversationHistory
        )

        val reasoningResult = aiReasoningEngine.reason(context, currentButler.id)
        return when (
            val route = messageOrchestrator.route(
                reasoningResult = reasoningResult,
                userMessage = message,
                butlerId = currentButler.id,
                isNetworkAvailable = isNetworkAvailable,
                isBuiltinConfigEnabled = currentUseBuiltinConfig,
                isAIEnabled = currentAIConfig.isEnabled,
                hasApiKey = currentAIConfig.apiKey.isNotBlank(),
                pendingState = pendingState
            )
        ) {
            is AIAssistantMessageRoute.LocalActions -> aiReasoningEngine.executeActions(route.actions)
            is AIAssistantMessageRoute.RemoteOrLocalFallback -> processWithRemoteAI(route.userMessage)
            is AIAssistantMessageRoute.ModificationFlow -> {
                if (route.pendingState != null) {
                    handleModificationConfirmation(route.message, route.butlerId)
                } else {
                    handleTransactionModification(route.message, route.butlerId)
                }
            }
        }
    }
}
