package com.example.aiaccounting.ui.viewmodel

internal sealed class AIAssistantRemoteExecutionResult {
    data object Timeout : AIAssistantRemoteExecutionResult()
    data class TransportFailure(val message: String) : AIAssistantRemoteExecutionResult()
    data object IncompleteResponse : AIAssistantRemoteExecutionResult()
    data class QueryBeforeExecutionRequested(val envelope: AIAssistantActionEnvelope) : AIAssistantRemoteExecutionResult()
    data class ActionExecutionRequested(val envelope: AIAssistantActionEnvelope) : AIAssistantRemoteExecutionResult()
    data class TransactionActionMissing(
        val retriedWithEnvelopeCorrection: Boolean = false
    ) : AIAssistantRemoteExecutionResult()
    data class EmptyRemoteReply(
        val retried: Boolean = false
    ) : AIAssistantRemoteExecutionResult()
    data object IncompleteResponseAfterRetry : AIAssistantRemoteExecutionResult()
    data class RemoteReply(val reply: String) : AIAssistantRemoteExecutionResult()
}
