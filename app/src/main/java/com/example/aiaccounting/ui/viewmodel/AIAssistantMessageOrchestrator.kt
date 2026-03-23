package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.ai.TransactionModificationHandler

internal sealed class AIAssistantMessageRoute {
    data class LocalActions(val actions: List<AIReasoningEngine.AIAction>) : AIAssistantMessageRoute()
    data class RemoteOrLocalFallback(val userMessage: String) : AIAssistantMessageRoute()
    data class ModificationFlow(
        val message: String,
        val butlerId: String,
        val pendingState: PendingModificationState?
    ) : AIAssistantMessageRoute()
}

internal class AIAssistantMessageOrchestrator {
    fun shouldUseRemoteAI(
        isNetworkAvailable: Boolean,
        isBuiltinConfigEnabled: Boolean,
        isAIEnabled: Boolean,
        hasApiKey: Boolean
    ): Boolean {
        if (isBuiltinConfigEnabled) return false
        return isNetworkAvailable && isAIEnabled && hasApiKey
    }

    fun route(
        reasoningResult: AIReasoningEngine.ReasoningResult,
        userMessage: String,
        butlerId: String,
        isNetworkAvailable: Boolean,
        isBuiltinConfigEnabled: Boolean,
        isAIEnabled: Boolean,
        hasApiKey: Boolean,
        pendingState: PendingModificationState?
    ): AIAssistantMessageRoute {
        if (pendingState != null) {
            return AIAssistantMessageRoute.ModificationFlow(
                message = userMessage,
                butlerId = butlerId,
                pendingState = pendingState
            )
        }

        if (reasoningResult.actions.any { it is AIReasoningEngine.AIAction.RequestClarification }) {
            return AIAssistantMessageRoute.LocalActions(reasoningResult.actions)
        }

        return when (reasoningResult.intent) {
            AIReasoningEngine.UserIntent.IDENTITY_CONFIRMATION,
            AIReasoningEngine.UserIntent.QUERY_INFORMATION,
            AIReasoningEngine.UserIntent.ANALYZE_DATA -> {
                AIAssistantMessageRoute.LocalActions(reasoningResult.actions)
            }

            AIReasoningEngine.UserIntent.MODIFY_TRANSACTION,
            AIReasoningEngine.UserIntent.DELETE_TRANSACTION -> {
                AIAssistantMessageRoute.ModificationFlow(
                    message = userMessage,
                    butlerId = butlerId,
                    pendingState = null
                )
            }

            AIReasoningEngine.UserIntent.RECORD_TRANSACTION,
            AIReasoningEngine.UserIntent.GENERAL_CONVERSATION,
            AIReasoningEngine.UserIntent.MANAGE_ACCOUNT,
            AIReasoningEngine.UserIntent.MANAGE_CATEGORY,
            AIReasoningEngine.UserIntent.UNKNOWN -> {
                if (
                    shouldUseRemoteAI(
                        isNetworkAvailable = isNetworkAvailable,
                        isBuiltinConfigEnabled = isBuiltinConfigEnabled,
                        isAIEnabled = isAIEnabled,
                        hasApiKey = hasApiKey
                    )
                ) {
                    AIAssistantMessageRoute.RemoteOrLocalFallback(userMessage)
                } else {
                    AIAssistantMessageRoute.LocalActions(reasoningResult.actions)
                }
            }
        }
    }
}
