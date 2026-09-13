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
    private val appLogLogger: com.example.aiaccounting.logging.AppLogLogger? = null,
    private val buildBookkeepingEnvelopeCorrectionMessage: () -> String = {
        "请仅返回可执行动作 envelope。不要输出解释性文本，不要 markdown，不要代码块。仅输出 JSON，对象格式为 {\"actions\":[...],\"reply\":\"...\" }。"
    }
) {

    suspend fun execute(
        messages: List<ChatMessage>,
        config: AIConfig,
        responseRequirement: AIAssistantRemoteResponseRequirement,
        traceId: String? = null
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
                hasRetriedIncompleteResponse = false,
                traceId = traceId
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
        hasRetriedIncompleteResponse: Boolean,
        traceId: String?
    ): AIAssistantRemoteExecutionResult {
        return when (val collected = streamCollector.collect(messages, config, traceId)) {
            is RemoteStreamCollectionResult.Timeout -> AIAssistantRemoteExecutionResult.Timeout
            is RemoteStreamCollectionResult.Failure -> AIAssistantRemoteExecutionResult.TransportFailure(collected.message)
            is RemoteStreamCollectionResult.Success -> {
                if (!integrityChecker.isComplete(collected.response)) {
                    appLogLogger?.warning(
                        source = "AI",
                        category = "remote_response_incomplete",
                        message = "远端响应不完整",
                        details = "responseLength=${collected.response.length},allowRetry=$allowIncompleteRetry,hasRetried=$hasRetriedIncompleteResponse,responseRequirement=${responseRequirement.name}",
                        traceId = traceId
                    )
                    recordUsageFailure()
                    if (allowIncompleteRetry) {
                        appLogLogger?.info(
                            source = "AI",
                            category = "remote_response_retry",
                            message = "远端响应重试",
                            details = "reason=incomplete_response,responseRequirement=${responseRequirement.name}",
                            traceId = traceId
                        )
                        return executeInternal(
                            messages = messages,
                            config = config,
                            responseRequirement = responseRequirement,
                            allowEnvelopeCorrectionRetry = allowEnvelopeCorrectionRetry,
                            allowEmptyReplyRetry = allowEmptyReplyRetry,
                            allowIncompleteRetry = false,
                            hasRetriedEnvelopeCorrection = hasRetriedEnvelopeCorrection,
                            hasRetriedEmptyReply = hasRetriedEmptyReply,
                            hasRetriedIncompleteResponse = true,
                            traceId = traceId
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
                        appLogLogger?.info(
                            source = "AI",
                            category = "remote_response_interpreted",
                            message = "远端响应解析完成",
                            details = "decision=query_before_execute,actions=${decision.envelope.actions.size}",
                            traceId = traceId
                        )
                        recordUsageSuccess()
                        AIAssistantRemoteExecutionResult.QueryBeforeExecutionRequested(decision.envelope)
                    }
                    is RemoteResponseDecision.ExecuteActions -> {
                        appLogLogger?.info(
                            source = "AI",
                            category = "remote_response_interpreted",
                            message = "远端响应解析完成",
                            details = "decision=execute_actions,actions=${decision.envelope.actions.size}",
                            traceId = traceId
                        )
                        recordUsageSuccess()
                        AIAssistantRemoteExecutionResult.ActionExecutionRequested(decision.envelope)
                    }
                    is RemoteResponseDecision.ReturnRemoteReply -> {
                        if (responseRequirement == AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired) {
                            if (allowEnvelopeCorrectionRetry) {
                                appLogLogger?.warning(
                                    source = "AI",
                                    category = "remote_response_retry",
                                    message = "远端响应缺少动作 envelope，准备重试",
                                    details = "reason=missing_action_envelope,responseLength=${decision.reply.length},hasRetried=$hasRetriedEnvelopeCorrection",
                                    traceId = traceId
                                )
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
                                    hasRetriedIncompleteResponse = hasRetriedIncompleteResponse,
                                    traceId = traceId
                                )
                            }
                            appLogLogger?.warning(
                                source = "AI",
                                category = "remote_response_empty",
                                message = "远端响应未返回可执行动作",
                                details = "reason=transaction_action_missing,retried=$hasRetriedEnvelopeCorrection",
                                traceId = traceId
                            )
                            recordUsageFailure()
                            AIAssistantRemoteExecutionResult.TransactionActionMissing(
                                retriedWithEnvelopeCorrection = hasRetriedEnvelopeCorrection
                            )
                        } else if (decision.reply.isBlank()) {
                            if (allowEmptyReplyRetry) {
                                appLogLogger?.warning(
                                    source = "AI",
                                    category = "remote_response_retry",
                                    message = "远端响应为空，准备重试",
                                    details = "reason=blank_reply,hasRetried=$hasRetriedEmptyReply,responseRequirement=${responseRequirement.name}",
                                    traceId = traceId
                                )
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
                                    hasRetriedIncompleteResponse = hasRetriedIncompleteResponse,
                                    traceId = traceId
                                )
                            }
                            appLogLogger?.warning(
                                source = "AI",
                                category = "remote_response_empty",
                                message = "远端响应为空",
                                details = "repliedBlank=true,retried=$hasRetriedEmptyReply,responseRequirement=${responseRequirement.name}",
                                traceId = traceId
                            )
                            recordUsageFailure()
                            AIAssistantRemoteExecutionResult.EmptyRemoteReply(retried = hasRetriedEmptyReply)
                        } else {
                            appLogLogger?.info(
                                source = "AI",
                                category = "remote_response_interpreted",
                                message = "远端响应解析完成",
                                details = "decision=return_reply,replyLength=${decision.reply.length}",
                                traceId = traceId
                            )
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
