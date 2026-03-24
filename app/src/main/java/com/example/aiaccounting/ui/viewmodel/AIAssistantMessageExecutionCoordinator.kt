package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler

internal sealed class AIAssistantMessageExecutionResult {
    data class Reply(val message: String) : AIAssistantMessageExecutionResult()
    data class ConfirmationRequired(val message: String) : AIAssistantMessageExecutionResult()
    data class ClarificationRequired(
        val originalMessage: String,
        val question: String
    ) : AIAssistantMessageExecutionResult()
}

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
        pendingInteractionState: PendingInteractionState?,
        continuePendingClarification: (String, String) -> ClarificationFlowResult,
        clearPendingClarificationAfterSuccessfulContinuation: () -> Unit,
        restorePendingClarification: (PendingClarificationState) -> Unit,
        handleModificationConfirmation: suspend (String, String) -> ModificationFlowResult,
        handleTransactionModification: suspend (String, String) -> ModificationFlowResult,
        processWithRemoteAI: suspend (String) -> String
    ): AIAssistantMessageExecutionResult {
        return when (pendingInteractionState) {
            is PendingInteractionState.Clarification -> {
                handleClarificationContinuation(
                    message = message,
                    currentButler = currentButler,
                    conversationHistory = conversationHistory,
                    isNetworkAvailable = isNetworkAvailable,
                    currentUseBuiltinConfig = currentUseBuiltinConfig,
                    currentAIConfig = currentAIConfig,
                    pendingState = pendingInteractionState.state,
                    continuePendingClarification = continuePendingClarification,
                    clearPendingClarificationAfterSuccessfulContinuation = clearPendingClarificationAfterSuccessfulContinuation,
                    restorePendingClarification = restorePendingClarification,
                    handleModificationConfirmation = handleModificationConfirmation,
                    handleTransactionModification = handleTransactionModification,
                    processWithRemoteAI = processWithRemoteAI
                )
            }
            is PendingInteractionState.Modification -> {
                handleModificationFlowResult(
                    handleModificationConfirmation(message, currentButler.id)
                )
            }
            null -> executeNewMessage(
                message = message,
                currentButler = currentButler,
                conversationHistory = conversationHistory,
                isNetworkAvailable = isNetworkAvailable,
                currentUseBuiltinConfig = currentUseBuiltinConfig,
                currentAIConfig = currentAIConfig,
                pendingInteractionState = null,
                handleModificationConfirmation = handleModificationConfirmation,
                handleTransactionModification = handleTransactionModification,
                processWithRemoteAI = processWithRemoteAI
            )
        }
    }

    private suspend fun handleClarificationContinuation(
        message: String,
        currentButler: Butler,
        conversationHistory: List<String>,
        isNetworkAvailable: Boolean,
        currentUseBuiltinConfig: Boolean,
        currentAIConfig: AIConfig,
        pendingState: PendingClarificationState,
        continuePendingClarification: (String, String) -> ClarificationFlowResult,
        clearPendingClarificationAfterSuccessfulContinuation: () -> Unit,
        restorePendingClarification: (PendingClarificationState) -> Unit,
        handleModificationConfirmation: suspend (String, String) -> ModificationFlowResult,
        handleTransactionModification: suspend (String, String) -> ModificationFlowResult,
        processWithRemoteAI: suspend (String) -> String
    ): AIAssistantMessageExecutionResult {
        return when (val clarificationResult = continuePendingClarification(message, currentButler.id)) {
            is ClarificationFlowResult.RequestClarification -> {
                AIAssistantMessageExecutionResult.ClarificationRequired(
                    originalMessage = clarificationResult.pendingState.originalMessage,
                    question = clarificationResult.reply
                )
            }
            is ClarificationFlowResult.Finish -> AIAssistantMessageExecutionResult.Reply(clarificationResult.reply)
            is ClarificationFlowResult.ContinueWithMessage -> {
                clearPendingClarificationAfterSuccessfulContinuation()
                try {
                    executeNewMessage(
                        message = clarificationResult.message,
                        currentButler = currentButler,
                        conversationHistory = conversationHistory,
                        isNetworkAvailable = isNetworkAvailable,
                        currentUseBuiltinConfig = currentUseBuiltinConfig,
                        currentAIConfig = currentAIConfig,
                        pendingInteractionState = null,
                        handleModificationConfirmation = handleModificationConfirmation,
                        handleTransactionModification = handleTransactionModification,
                        processWithRemoteAI = processWithRemoteAI
                    )
                } catch (e: Exception) {
                    restorePendingClarification(pendingState)
                    throw e
                }
            }
        }
    }

    private suspend fun executeNewMessage(
        message: String,
        currentButler: Butler,
        conversationHistory: List<String>,
        isNetworkAvailable: Boolean,
        currentUseBuiltinConfig: Boolean,
        currentAIConfig: AIConfig,
        pendingInteractionState: PendingInteractionState?,
        handleModificationConfirmation: suspend (String, String) -> ModificationFlowResult,
        handleTransactionModification: suspend (String, String) -> ModificationFlowResult,
        processWithRemoteAI: suspend (String) -> String
    ): AIAssistantMessageExecutionResult {
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
                pendingInteractionState = pendingInteractionState
            )
        ) {
            is AIAssistantMessageRoute.LocalActions -> {
                val reply = aiReasoningEngine.executeActions(route.actions)
                if (route.actions.any { it is AIReasoningEngine.AIAction.RequestClarification }) {
                    AIAssistantMessageExecutionResult.ClarificationRequired(
                        originalMessage = message,
                        question = reply
                    )
                } else {
                    AIAssistantMessageExecutionResult.Reply(reply)
                }
            }
            is AIAssistantMessageRoute.RemoteOrLocalFallback -> {
                AIAssistantMessageExecutionResult.Reply(
                    processWithRemoteAI(route.userMessage)
                )
            }
            is AIAssistantMessageRoute.ModificationFlow -> {
                handleModificationFlowResult(
                    if (route.pendingState != null) {
                        handleModificationConfirmation(route.message, route.butlerId)
                    } else {
                        handleTransactionModification(route.message, route.butlerId)
                    }
                )
            }
        }
    }

    private fun handleModificationFlowResult(
        result: ModificationFlowResult
    ): AIAssistantMessageExecutionResult {
        return when (result) {
            is ModificationFlowResult.Finish -> AIAssistantMessageExecutionResult.Reply(result.reply)
            is ModificationFlowResult.StartConfirmation -> {
                AIAssistantMessageExecutionResult.ConfirmationRequired(result.reply)
            }
        }
    }
}
