package com.example.aiaccounting.ui.viewmodel

internal class AIAssistantPendingModificationLifecycle(
    private val modificationCoordinator: AIAssistantModificationCoordinator
) {
    private var pendingState: PendingModificationState? = null

    fun currentState(): PendingModificationState? = pendingState

    suspend fun begin(message: String, butlerId: String): String {
        return when (val result = modificationCoordinator.beginModification(message, butlerId)) {
            is ModificationFlowResult.StartConfirmation -> {
                pendingState = result.pendingState
                result.reply
            }
            is ModificationFlowResult.Finish -> result.reply
        }
    }

    suspend fun continuePending(message: String, butlerId: String): String {
        val currentPendingState = pendingState ?: return "抱歉，没有待确认的操作。"
        val result = modificationCoordinator.continueModification(message, butlerId, currentPendingState)
        if (result is ModificationFlowResult.Finish && result.shouldClearPending) {
            pendingState = null
        }
        return when (result) {
            is ModificationFlowResult.Finish -> result.reply
            is ModificationFlowResult.StartConfirmation -> result.reply
        }
    }

    internal fun seedForTest(state: PendingModificationState?) {
        pendingState = state
    }
}
