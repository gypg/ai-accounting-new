package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.ai.TransactionModificationHandler

internal enum class AIAssistantInteractionStage {
    Analysis,
    Clarification,
    Confirmation,
    Execution,
    Reply
}

internal sealed class AIAssistantContinuationStep {
    data object ExecuteLocally : AIAssistantContinuationStep()
    data object ExecuteModification : AIAssistantContinuationStep()
    data object RequestSecondRemote : AIAssistantContinuationStep()
}

internal data class AIAssistantContinuationPayload(
    val originalMessage: String,
    val resumedMessage: String,
    val trigger: ClarificationTrigger,
    val nextStep: AIAssistantContinuationStep
)

internal data class RemoteExecutionRequest(
    val userMessage: String,
    val continuationPayload: AIAssistantContinuationPayload? = null,
    val stage: AIAssistantInteractionStage = AIAssistantInteractionStage.Execution
)

internal data class ModificationExecutionRequest(
    val message: String,
    val butlerId: String,
    val pendingState: PendingModificationState?,
    val stage: AIAssistantInteractionStage
)

internal sealed class AIAssistantMessageRoute {
    data class LocalActions(
        val actions: List<AIReasoningEngine.AIAction>,
        val stage: AIAssistantInteractionStage = AIAssistantInteractionStage.Execution
    ) : AIAssistantMessageRoute()

    data class RemoteOrLocalFallback(val request: RemoteExecutionRequest) : AIAssistantMessageRoute()

    data class ModificationFlow(val request: ModificationExecutionRequest) : AIAssistantMessageRoute()
}

internal sealed class AIAssistantContinuationDecision {
    data class ExecuteLocally(val route: AIAssistantMessageRoute) : AIAssistantContinuationDecision()
    data class RequestSecondRemote(val request: RemoteExecutionRequest) : AIAssistantContinuationDecision()
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

    fun decideContinuation(
        route: AIAssistantMessageRoute,
        continuationPayload: AIAssistantContinuationPayload
    ): AIAssistantContinuationDecision {
        return when (route) {
            is AIAssistantMessageRoute.RemoteOrLocalFallback -> {
                AIAssistantContinuationDecision.RequestSecondRemote(
                    route.request.copy(continuationPayload = continuationPayload)
                )
            }
            is AIAssistantMessageRoute.ModificationFlow -> {
                AIAssistantContinuationDecision.ExecuteLocally(
                    AIAssistantMessageRoute.ModificationFlow(
                        route.request.copy(stage = AIAssistantInteractionStage.Confirmation)
                    )
                )
            }
            is AIAssistantMessageRoute.LocalActions -> {
                AIAssistantContinuationDecision.ExecuteLocally(
                    route.copy(stage = AIAssistantInteractionStage.Execution)
                )
            }
        }
    }

    fun route(
        reasoningResult: AIReasoningEngine.ReasoningResult,
        userMessage: String,
        butlerId: String,
        isNetworkAvailable: Boolean,
        isBuiltinConfigEnabled: Boolean,
        isAIEnabled: Boolean,
        hasApiKey: Boolean,
        pendingInteractionState: PendingInteractionState?
    ): AIAssistantMessageRoute {
        if (pendingInteractionState is PendingInteractionState.Modification) {
            return AIAssistantMessageRoute.ModificationFlow(
                request = ModificationExecutionRequest(
                    message = userMessage,
                    butlerId = butlerId,
                    pendingState = pendingInteractionState.state,
                    stage = AIAssistantInteractionStage.Confirmation
                )
            )
        }

        if (reasoningResult.actions.any { it is AIReasoningEngine.AIAction.RequestClarification }) {
            return AIAssistantMessageRoute.LocalActions(
                actions = reasoningResult.actions,
                stage = AIAssistantInteractionStage.Clarification
            )
        }

        return when (reasoningResult.intent) {
            AIReasoningEngine.UserIntent.IDENTITY_CONFIRMATION,
            AIReasoningEngine.UserIntent.QUERY_INFORMATION,
            AIReasoningEngine.UserIntent.ANALYZE_DATA -> {
                AIAssistantMessageRoute.LocalActions(
                    actions = reasoningResult.actions,
                    stage = AIAssistantInteractionStage.Execution
                )
            }

            AIReasoningEngine.UserIntent.MODIFY_TRANSACTION,
            AIReasoningEngine.UserIntent.DELETE_TRANSACTION -> {
                AIAssistantMessageRoute.ModificationFlow(
                    request = ModificationExecutionRequest(
                        message = userMessage,
                        butlerId = butlerId,
                        pendingState = null,
                        stage = AIAssistantInteractionStage.Confirmation
                    )
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
                    AIAssistantMessageRoute.RemoteOrLocalFallback(
                        RemoteExecutionRequest(userMessage = userMessage)
                    )
                } else {
                    AIAssistantMessageRoute.LocalActions(
                        actions = reasoningResult.actions,
                        stage = AIAssistantInteractionStage.Execution
                    )
                }
            }
        }
    }
}
