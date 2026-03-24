package com.example.aiaccounting.ui.viewmodel

internal sealed class PendingInteractionState {
    data class Modification(val state: PendingModificationState) : PendingInteractionState()
    data class Clarification(val state: PendingClarificationState) : PendingInteractionState()
}

internal class AIAssistantPendingInteractionLifecycle(
    private val modificationLifecycle: AIAssistantPendingModificationLifecycle,
    private val clarificationLifecycle: AIAssistantPendingClarificationLifecycle
) {
    fun currentState(): PendingInteractionState? {
        return clarificationLifecycle.currentState()?.let { PendingInteractionState.Clarification(it) }
            ?: modificationLifecycle.currentState()?.let { PendingInteractionState.Modification(it) }
    }

    fun currentModificationState(): PendingModificationState? = modificationLifecycle.currentState()

    fun currentClarificationState(): PendingClarificationState? = clarificationLifecycle.currentState()

    fun clear() {
        modificationLifecycle.clear()
        clarificationLifecycle.clear()
    }

    suspend fun beginModification(message: String, butlerId: String): ModificationFlowResult {
        return modificationLifecycle.begin(message, butlerId)
    }

    suspend fun continueModification(message: String, butlerId: String): ModificationFlowResult {
        return modificationLifecycle.continuePending(message, butlerId)
    }

    fun beginClarification(originalMessage: String, question: String): ClarificationFlowResult.RequestClarification {
        return clarificationLifecycle.begin(originalMessage, question)
    }

    fun continueClarification(message: String, butlerId: String): ClarificationFlowResult {
        return clarificationLifecycle.continuePending(message, butlerId)
    }

    fun clearClarificationAfterSuccessfulContinuation() {
        clarificationLifecycle.clearAfterSuccessfulContinuation()
    }

    fun restoreClarification(state: PendingClarificationState) {
        clarificationLifecycle.restore(state)
    }
}
