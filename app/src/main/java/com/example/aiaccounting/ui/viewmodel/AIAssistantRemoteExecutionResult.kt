package com.example.aiaccounting.ui.viewmodel

internal sealed class AIAssistantRemoteExecutionResult {
    data object Timeout : AIAssistantRemoteExecutionResult()
    data class TransportFailure(val message: String) : AIAssistantRemoteExecutionResult()
    data object IncompleteResponse : AIAssistantRemoteExecutionResult()
    data class ActionExecutionRequested(val rawResponse: String) : AIAssistantRemoteExecutionResult()
    data class LocalFallbackRequested(val remoteReply: String) : AIAssistantRemoteExecutionResult()
    data class RemoteReply(val reply: String) : AIAssistantRemoteExecutionResult()
}
