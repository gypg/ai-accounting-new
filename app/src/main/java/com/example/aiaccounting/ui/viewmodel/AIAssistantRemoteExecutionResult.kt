package com.example.aiaccounting.ui.viewmodel

internal sealed class AIAssistantRemoteExecutionResult {
    data object Timeout : AIAssistantRemoteExecutionResult()
    data class TransportFailure(val message: String) : AIAssistantRemoteExecutionResult()
    data object IncompleteResponse : AIAssistantRemoteExecutionResult()
    data class QueryBeforeExecutionRequested(val envelope: AIAssistantActionEnvelope) : AIAssistantRemoteExecutionResult()
    data class ActionExecutionRequested(val envelope: AIAssistantActionEnvelope) : AIAssistantRemoteExecutionResult()
    data object TransactionActionMissing : AIAssistantRemoteExecutionResult()
    data class RemoteReply(val reply: String) : AIAssistantRemoteExecutionResult()
}
