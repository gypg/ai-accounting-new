package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler

internal sealed class AIAssistantMessageExecutionResult {
    abstract val stage: AIAssistantInteractionStage

    data class Reply(
        val message: String,
        override val stage: AIAssistantInteractionStage = AIAssistantInteractionStage.Reply
    ) : AIAssistantMessageExecutionResult()

    data class ConfirmationRequired(
        val message: String,
        val continuationPayload: AIAssistantContinuationPayload? = null,
        override val stage: AIAssistantInteractionStage = AIAssistantInteractionStage.Confirmation
    ) : AIAssistantMessageExecutionResult()

    data class ClarificationRequired(
        val originalMessage: String,
        val question: String,
        val continuationPayload: AIAssistantContinuationPayload? = null,
        override val stage: AIAssistantInteractionStage = AIAssistantInteractionStage.Clarification
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
                    result = handleModificationConfirmation(message, currentButler.id),
                    stage = AIAssistantInteractionStage.Confirmation,
                    continuationPayload = null
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
            is ClarificationFlowResult.ContinueWithPayload -> {
                clearPendingClarificationAfterSuccessfulContinuation()
                try {
                    executeContinuationMessage(
                        continuationRequest = clarificationResult.payload,
                        currentButler = currentButler,
                        conversationHistory = conversationHistory,
                        isNetworkAvailable = isNetworkAvailable,
                        currentUseBuiltinConfig = currentUseBuiltinConfig,
                        currentAIConfig = currentAIConfig,
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

    private suspend fun executeContinuationMessage(
        continuationRequest: ClarificationContinuationRequest,
        currentButler: Butler,
        conversationHistory: List<String>,
        isNetworkAvailable: Boolean,
        currentUseBuiltinConfig: Boolean,
        currentAIConfig: AIConfig,
        handleModificationConfirmation: suspend (String, String) -> ModificationFlowResult,
        handleTransactionModification: suspend (String, String) -> ModificationFlowResult,
        processWithRemoteAI: suspend (String) -> String
    ): AIAssistantMessageExecutionResult {
        val reasoningResult = aiReasoningEngine.reason(
            AIReasoningEngine.ReasoningContext(
                userMessage = continuationRequest.resumedMessage,
                conversationHistory = conversationHistory
            ),
            currentButler.id
        )
        val route = messageOrchestrator.route(
            reasoningResult = reasoningResult,
            userMessage = continuationRequest.resumedMessage,
            butlerId = currentButler.id,
            isNetworkAvailable = isNetworkAvailable,
            isBuiltinConfigEnabled = currentUseBuiltinConfig,
            isAIEnabled = currentAIConfig.isEnabled,
            hasApiKey = currentAIConfig.apiKey.isNotBlank(),
            pendingInteractionState = null
        )
        val continuationPayload = AIAssistantContinuationPayload(
            originalMessage = continuationRequest.originalMessage,
            resumedMessage = continuationRequest.resumedMessage,
            trigger = continuationRequest.trigger,
            nextStep = when (route) {
                is AIAssistantMessageRoute.RemoteOrLocalFallback -> AIAssistantContinuationStep.RequestSecondRemote
                is AIAssistantMessageRoute.ModificationFlow -> AIAssistantContinuationStep.ExecuteModification
                is AIAssistantMessageRoute.LocalActions -> AIAssistantContinuationStep.ExecuteLocally
            }
        )

        return when (val decision = messageOrchestrator.decideContinuation(route, continuationPayload)) {
            is AIAssistantContinuationDecision.RequestSecondRemote -> {
                AIAssistantMessageExecutionResult.Reply(
                    message = processWithRemoteAI(decision.request.userMessage),
                    stage = decision.request.stage
                )
            }
            is AIAssistantContinuationDecision.ExecuteLocally -> {
                executeResolvedRoute(
                    route = decision.route,
                    message = continuationRequest.resumedMessage,
                    handleModificationConfirmation = handleModificationConfirmation,
                    handleTransactionModification = handleTransactionModification,
                    processWithRemoteAI = processWithRemoteAI,
                    continuationPayload = continuationPayload
                )
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

        return executeResolvedRoute(
            route = route,
            message = message,
            handleModificationConfirmation = handleModificationConfirmation,
            handleTransactionModification = handleTransactionModification,
            processWithRemoteAI = processWithRemoteAI,
            continuationPayload = null
        )
    }

    private suspend fun executeResolvedRoute(
        route: AIAssistantMessageRoute,
        message: String,
        handleModificationConfirmation: suspend (String, String) -> ModificationFlowResult,
        handleTransactionModification: suspend (String, String) -> ModificationFlowResult,
        processWithRemoteAI: suspend (String) -> String,
        continuationPayload: AIAssistantContinuationPayload?
    ): AIAssistantMessageExecutionResult {
        return when (route) {
            is AIAssistantMessageRoute.LocalActions -> {
                val reply = aiReasoningEngine.executeActions(route.actions)
                if (route.actions.any { it is AIReasoningEngine.AIAction.RequestClarification }) {
                    AIAssistantMessageExecutionResult.ClarificationRequired(
                        originalMessage = continuationPayload?.originalMessage ?: message,
                        question = reply,
                        continuationPayload = continuationPayload,
                        stage = route.stage
                    )
                } else {
                    AIAssistantMessageExecutionResult.Reply(
                        message = reply,
                        stage = route.stage
                    )
                }
            }
            is AIAssistantMessageRoute.RemoteOrLocalFallback -> {
                AIAssistantMessageExecutionResult.Reply(
                    message = processWithRemoteAI(route.request.userMessage),
                    stage = route.request.stage
                )
            }
            is AIAssistantMessageRoute.ModificationFlow -> {
                handleModificationFlowResult(
                    result = if (route.request.pendingState != null) {
                        handleModificationConfirmation(route.request.message, route.request.butlerId)
                    } else {
                        handleTransactionModification(route.request.message, route.request.butlerId)
                    },
                    stage = route.request.stage,
                    continuationPayload = continuationPayload
                )
            }
        }
    }

    private fun handleModificationFlowResult(
        result: ModificationFlowResult,
        stage: AIAssistantInteractionStage,
        continuationPayload: AIAssistantContinuationPayload?
    ): AIAssistantMessageExecutionResult {
        return when (result) {
            is ModificationFlowResult.Finish -> {
                AIAssistantMessageExecutionResult.Reply(
                    message = result.reply,
                    stage = AIAssistantInteractionStage.Reply
                )
            }
            is ModificationFlowResult.StartConfirmation -> {
                AIAssistantMessageExecutionResult.ConfirmationRequired(
                    message = result.reply,
                    continuationPayload = continuationPayload,
                    stage = stage
                )
            }
        }
    }
}
