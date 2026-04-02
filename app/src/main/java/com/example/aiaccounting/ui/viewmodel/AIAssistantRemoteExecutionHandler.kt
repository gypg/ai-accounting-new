package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.ChatMessage
import kotlinx.coroutines.CancellationException

internal class AIAssistantRemoteExecutionHandler(
    private val streamCollector: AIAssistantRemoteStreamCollector,
    private val integrityChecker: AIAssistantRemoteResponseIntegrityChecker,
    private val interpreter: AIAssistantRemoteResponseInterpreter,
    private val recordUsageSuccess: suspend () -> Unit,
    private val recordUsageFailure: suspend () -> Unit,
    private val buildBookkeepingEnvelopeCorrectionMessage: () -> String = {
        "请仅返回可执行动作 envelope。不要输出解释性文本，不要 markdown，不要代码块。仅输出 JSON，对象格式为 {\"actions\":[...],\"reply\":\"...\" }。"
    }
) {

    suspend fun execute(
        messages: List<ChatMessage>,
        config: AIConfig,
        responseRequirement: AIAssistantRemoteResponseRequirement
    ): AIAssistantRemoteExecutionResult {
        return try {
            executeInternal(
                messages = messages,
                config = config,
                responseRequirement = responseRequirement,
                allowEnvelopeCorrectionRetry = true,
                allowEmptyReplyRetry = true,
                allowIncompleteRetry = true,
                hasRetriedEnvelopeCorrection = false,
                hasRetriedEmptyReply = false,
                hasRetriedIncompleteResponse = false
            )
        } catch (e: CancellationException) {
            throw e
        }
    }

    private suspend fun executeInternal(
        messages: List<ChatMessage>,
        config: AIConfig,
        responseRequirement: AIAssistantRemoteResponseRequirement,
        allowEnvelopeCorrectionRetry: Boolean,
        allowEmptyReplyRetry: Boolean,
        allowIncompleteRetry: Boolean,
        hasRetriedEnvelopeCorrection: Boolean,
        hasRetriedEmptyReply: Boolean,
        hasRetriedIncompleteResponse: Boolean
    ): AIAssistantRemoteExecutionResult {
        return when (val collected = streamCollector.collect(messages, config)) {
            is RemoteStreamCollectionResult.Timeout -> AIAssistantRemoteExecutionResult.Timeout
            is RemoteStreamCollectionResult.Failure -> AIAssistantRemoteExecutionResult.TransportFailure(collected.message)
            is RemoteStreamCollectionResult.Success -> {
                if (!integrityChecker.isComplete(collected.response)) {
                    recordUsageFailure()
                    if (allowIncompleteRetry) {
                        return executeInternal(
                            messages = messages,
                            config = config,
                            responseRequirement = responseRequirement,
                            allowEnvelopeCorrectionRetry = allowEnvelopeCorrectionRetry,
                            allowEmptyReplyRetry = allowEmptyReplyRetry,
                            allowIncompleteRetry = false,
                            hasRetriedEnvelopeCorrection = hasRetriedEnvelopeCorrection,
                            hasRetriedEmptyReply = hasRetriedEmptyReply,
                            hasRetriedIncompleteResponse = true
                        )
                    }
                    return if (hasRetriedIncompleteResponse) {
                        AIAssistantRemoteExecutionResult.IncompleteResponseAfterRetry
                    } else {
                        AIAssistantRemoteExecutionResult.IncompleteResponse
                    }
                }

                when (val decision = interpreter.interpret(remoteResponse = collected.response)) {
                    is RemoteResponseDecision.QueryBeforeExecute -> {
                        recordUsageSuccess()
                        AIAssistantRemoteExecutionResult.QueryBeforeExecutionRequested(decision.envelope)
                    }
                    is RemoteResponseDecision.ExecuteActions -> {
                        recordUsageSuccess()
                        AIAssistantRemoteExecutionResult.ActionExecutionRequested(decision.envelope)
                    }
                    is RemoteResponseDecision.ReturnRemoteReply -> {
                        if (responseRequirement == AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired) {
                            if (allowEnvelopeCorrectionRetry) {
                                recordUsageFailure()
                                return executeInternal(
                                    messages = appendEnvelopeCorrectionMessage(messages),
                                    config = config,
                                    responseRequirement = responseRequirement,
                                    allowEnvelopeCorrectionRetry = false,
                                    allowEmptyReplyRetry = allowEmptyReplyRetry,
                                    allowIncompleteRetry = allowIncompleteRetry,
                                    hasRetriedEnvelopeCorrection = true,
                                    hasRetriedEmptyReply = hasRetriedEmptyReply,
                                    hasRetriedIncompleteResponse = hasRetriedIncompleteResponse
                                )
                            }
                            recordUsageFailure()
                            AIAssistantRemoteExecutionResult.TransactionActionMissing(
                                retriedWithEnvelopeCorrection = hasRetriedEnvelopeCorrection
                            )
                        } else if (decision.reply.isBlank()) {
                            if (allowEmptyReplyRetry) {
                                recordUsageFailure()
                                return executeInternal(
                                    messages = messages,
                                    config = config,
                                    responseRequirement = responseRequirement,
                                    allowEnvelopeCorrectionRetry = allowEnvelopeCorrectionRetry,
                                    allowEmptyReplyRetry = false,
                                    allowIncompleteRetry = allowIncompleteRetry,
                                    hasRetriedEnvelopeCorrection = hasRetriedEnvelopeCorrection,
                                    hasRetriedEmptyReply = true,
                                    hasRetriedIncompleteResponse = hasRetriedIncompleteResponse
                                )
                            }
                            recordUsageFailure()
                            AIAssistantRemoteExecutionResult.EmptyRemoteReply(retried = hasRetriedEmptyReply)
                        } else {
                            recordUsageSuccess()
                            AIAssistantRemoteExecutionResult.RemoteReply(decision.reply)
                        }
                    }
                }
            }
        }
    }

    private fun appendEnvelopeCorrectionMessage(messages: List<ChatMessage>): List<ChatMessage> {
        return messages + ChatMessage(
            role = com.example.aiaccounting.data.model.MessageRole.USER,
            content = buildBookkeepingEnvelopeCorrectionMessage()
        )
    }
}
