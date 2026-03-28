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
        messages: List<ChatMessage>,
        config: AIConfig,
        responseRequirement: AIAssistantRemoteResponseRequirement
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

                    when (val decision = interpreter.interpret(remoteResponse = collected.response)) {
                        is RemoteResponseDecision.QueryBeforeExecute -> {
                            AIAssistantRemoteExecutionResult.QueryBeforeExecutionRequested(decision.envelope)
                        }
                        is RemoteResponseDecision.ExecuteActions -> {
                            AIAssistantRemoteExecutionResult.ActionExecutionRequested(decision.envelope)
                        }
                        is RemoteResponseDecision.ReturnRemoteReply -> {
                            if (responseRequirement == AIAssistantRemoteResponseRequirement.ActionEnvelopeRequired) {
                                AIAssistantRemoteExecutionResult.TransactionActionMissing
                            } else {
                                AIAssistantRemoteExecutionResult.RemoteReply(decision.reply)
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        }
    }
}
