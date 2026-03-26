package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.ChatMessage
import kotlinx.coroutines.CancellationException

internal class AIAssistantRemoteExecutionHandler(
    private val streamCollector: AIAssistantRemoteStreamCollector,
    private val integrityChecker: AIAssistantRemoteResponseIntegrityChecker,
    private val interpreter: AIAssistantRemoteResponseInterpreter,
    private val recordUsageSuccess: suspend () -> Unit,
    private val recordUsageFailure: suspend () -> Unit
) {

    suspend fun execute(
        userMessage: String,
        messages: List<ChatMessage>,
        config: AIConfig
    ): AIAssistantRemoteExecutionResult {
        return try {
            when (val collected = streamCollector.collect(messages, config)) {
                is RemoteStreamCollectionResult.Timeout -> AIAssistantRemoteExecutionResult.Timeout
                is RemoteStreamCollectionResult.Failure -> AIAssistantRemoteExecutionResult.TransportFailure(collected.message)
                is RemoteStreamCollectionResult.Success -> {
                    if (!integrityChecker.isComplete(collected.response)) {
                        recordUsageFailure()
                        return AIAssistantRemoteExecutionResult.IncompleteResponse
                    }

                    recordUsageSuccess()

                    when (val decision = interpreter.interpret(userMessage = userMessage, remoteResponse = collected.response)) {
                        is RemoteResponseDecision.ExecuteActions -> {
                            AIAssistantRemoteExecutionResult.ActionExecutionRequested(decision.envelope)
                        }
                        is RemoteResponseDecision.FallbackToLocalTransaction -> {
                            AIAssistantRemoteExecutionResult.LocalFallbackRequested(decision.remoteReply)
                        }
                        is RemoteResponseDecision.ReturnRemoteReply -> {
                            AIAssistantRemoteExecutionResult.RemoteReply(decision.reply)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        }
    }
}
