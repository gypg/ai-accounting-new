package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIReasoningEngine

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

internal enum class AIAssistantRemoteResponseRequirement {
    ReplyAllowed,
    ActionEnvelopeRequired
}

internal data class RemoteExecutionRequest(
    val userMessage: String,
    val continuationPayload: AIAssistantContinuationPayload? = null,
    val stage: AIAssistantInteractionStage = AIAssistantInteractionStage.Execution,
    val responseRequirement: AIAssistantRemoteResponseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
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

internal enum class AIAssistantEngineMode {
    Remote,
    Local
}

internal enum class AIAssistantTopLevelIntent {
    DAILY_CHAT,
    BOOKKEEPING,
    OCR_IMAGE
}

internal data class AIAssistantMessageAnalysis(
    val reasoningResult: AIReasoningEngine.ReasoningResult,
    val topLevelIntent: AIAssistantTopLevelIntent,
    val userMessage: String,
    val butlerId: String,
    val pendingInteractionState: PendingInteractionState?,
    val engineMode: AIAssistantEngineMode,
    val hasClarificationAction: Boolean
)

internal class AIAssistantMessageOrchestrator {
    fun resolveEngineMode(
        isNetworkAvailable: Boolean,
        isAIEnabled: Boolean,
        hasApiKey: Boolean
    ): AIAssistantEngineMode {
        return if (isNetworkAvailable && isAIEnabled && hasApiKey) {
            AIAssistantEngineMode.Remote
        } else {
            AIAssistantEngineMode.Local
        }
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

    fun analyze(
        reasoningResult: AIReasoningEngine.ReasoningResult,
        userMessage: String,
        butlerId: String,
        isNetworkAvailable: Boolean,
        isAIEnabled: Boolean,
        hasApiKey: Boolean,
        pendingInteractionState: PendingInteractionState?
    ): AIAssistantMessageAnalysis {
        return AIAssistantMessageAnalysis(
            reasoningResult = reasoningResult,
            topLevelIntent = resolveTopLevelIntent(reasoningResult.intent, userMessage),
            userMessage = userMessage,
            butlerId = butlerId,
            pendingInteractionState = pendingInteractionState,
            engineMode = resolveEngineMode(
                isNetworkAvailable = isNetworkAvailable,
                isAIEnabled = isAIEnabled,
                hasApiKey = hasApiKey
            ),
            hasClarificationAction = reasoningResult.actions.any {
                it is AIReasoningEngine.AIAction.RequestClarification
            }
        )
    }

    fun route(analysis: AIAssistantMessageAnalysis): AIAssistantMessageRoute {
        val pendingInteractionState = analysis.pendingInteractionState
        if (pendingInteractionState is PendingInteractionState.Modification) {
            return AIAssistantMessageRoute.ModificationFlow(
                request = ModificationExecutionRequest(
                    message = analysis.userMessage,
                    butlerId = analysis.butlerId,
                    pendingState = pendingInteractionState.state,
                    stage = AIAssistantInteractionStage.Confirmation
                )
            )
        }

        if (analysis.hasClarificationAction) {
            return AIAssistantMessageRoute.LocalActions(
                actions = analysis.reasoningResult.actions,
                stage = AIAssistantInteractionStage.Clarification
            )
        }

        return when (analysis.reasoningResult.intent) {
            AIReasoningEngine.UserIntent.IDENTITY_CONFIRMATION,
            AIReasoningEngine.UserIntent.QUERY_INFORMATION,
            AIReasoningEngine.UserIntent.ANALYZE_DATA -> {
                AIAssistantMessageRoute.LocalActions(
                    actions = analysis.reasoningResult.actions,
                    stage = AIAssistantInteractionStage.Execution
                )
            }

            AIReasoningEngine.UserIntent.MODIFY_TRANSACTION,
            AIReasoningEngine.UserIntent.DELETE_TRANSACTION -> {
                AIAssistantMessageRoute.ModificationFlow(
                    request = ModificationExecutionRequest(
                        message = analysis.userMessage,
                        butlerId = analysis.butlerId,
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
                when (analysis.topLevelIntent) {
                    AIAssistantTopLevelIntent.DAILY_CHAT -> {
                        if (analysis.engineMode == AIAssistantEngineMode.Remote) {
                            AIAssistantMessageRoute.RemoteOrLocalFallback(
                                RemoteExecutionRequest(
                                    userMessage = analysis.userMessage,
                                    responseRequirement = AIAssistantRemoteResponseRequirement.ReplyAllowed
                                )
                            )
                        } else {
                            AIAssistantMessageRoute.LocalActions(
                                actions = analysis.reasoningResult.actions,
                                stage = AIAssistantInteractionStage.Execution
                            )
                        }
                    }
                    AIAssistantTopLevelIntent.OCR_IMAGE -> {
                        AIAssistantMessageRoute.LocalActions(
                            actions = analysis.reasoningResult.actions,
                            stage = AIAssistantInteractionStage.Execution
                        )
                    }
                    AIAssistantTopLevelIntent.BOOKKEEPING -> {
                        if (analysis.engineMode == AIAssistantEngineMode.Remote) {
                            AIAssistantMessageRoute.RemoteOrLocalFallback(
                                RemoteExecutionRequest(
                                    userMessage = analysis.userMessage,
                                    responseRequirement = AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired
                                )
                            )
                        } else {
                            AIAssistantMessageRoute.LocalActions(
                                actions = analysis.reasoningResult.actions,
                                stage = AIAssistantInteractionStage.Execution
                            )
                        }
                    }
                }
            }
        }
    }

    fun route(
        reasoningResult: AIReasoningEngine.ReasoningResult,
        userMessage: String,
        butlerId: String,
        isNetworkAvailable: Boolean,
        isAIEnabled: Boolean,
        hasApiKey: Boolean,
        pendingInteractionState: PendingInteractionState?
    ): AIAssistantMessageRoute {
        val analysis = analyze(
            reasoningResult = reasoningResult,
            userMessage = userMessage,
            butlerId = butlerId,
            isNetworkAvailable = isNetworkAvailable,
            isAIEnabled = isAIEnabled,
            hasApiKey = hasApiKey,
            pendingInteractionState = pendingInteractionState
        )
        return route(analysis)
    }

    fun buildBookkeepingEnvelopeCorrectionMessage(): String {
        return "请仅返回可执行动作 envelope。不要输出解释性文本，不要 markdown，不要代码块。仅输出 JSON，对象格式为 {\"actions\":[...],\"reply\":\"...\" }。"
    }

    private fun resolveTopLevelIntent(
        reasoningIntent: AIReasoningEngine.UserIntent,
        userMessage: String
    ): AIAssistantTopLevelIntent {
        if (looksLikeOcrImageMessage(userMessage)) {
            return AIAssistantTopLevelIntent.OCR_IMAGE
        }

        return when (reasoningIntent) {
            AIReasoningEngine.UserIntent.GENERAL_CONVERSATION,
            AIReasoningEngine.UserIntent.IDENTITY_CONFIRMATION,
            AIReasoningEngine.UserIntent.UNKNOWN -> AIAssistantTopLevelIntent.DAILY_CHAT
            AIReasoningEngine.UserIntent.RECORD_TRANSACTION,
            AIReasoningEngine.UserIntent.MODIFY_TRANSACTION,
            AIReasoningEngine.UserIntent.DELETE_TRANSACTION,
            AIReasoningEngine.UserIntent.QUERY_INFORMATION,
            AIReasoningEngine.UserIntent.ANALYZE_DATA,
            AIReasoningEngine.UserIntent.MANAGE_ACCOUNT,
            AIReasoningEngine.UserIntent.MANAGE_CATEGORY -> AIAssistantTopLevelIntent.BOOKKEEPING
        }
    }

    private fun looksLikeOcrImageMessage(message: String): Boolean {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return false
        }

        val imageFileRegex = Regex("""(?:https?|file)://\S+\.(png|jpg|jpeg|webp|gif)\b""", RegexOption.IGNORE_CASE)
        return trimmed.startsWith("data:image/") || imageFileRegex.containsMatchIn(trimmed)
    }
}
