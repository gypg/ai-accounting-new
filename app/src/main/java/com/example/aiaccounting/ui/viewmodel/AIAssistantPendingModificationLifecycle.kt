package com.example.aiaccounting.ui.viewmodel

internal class AIAssistantPendingModificationLifecycle(
    private val modificationCoordinator: AIAssistantModificationCoordinator
) {
    private var pendingState: PendingModificationState? = null

    fun currentState(): PendingModificationState? = pendingState

    suspend fun begin(message: String, butlerId: String): ModificationFlowResult {
        return when (val result = modificationCoordinator.beginModification(message, butlerId)) {
            is ModificationFlowResult.StartConfirmation -> {
                pendingState = result.pendingState
                result
            }
            is ModificationFlowResult.Finish -> result
        }
    }

    suspend fun continuePending(message: String, butlerId: String): ModificationFlowResult {
        val currentPendingState = pendingState
            ?: return ModificationFlowResult.Finish("抱歉，没有待确认的操作。")
        return when (val result = modificationCoordinator.continueModification(message, butlerId, currentPendingState)) {
            is ModificationFlowResult.Finish -> {
                if (result.shouldClearPending) {
                    pendingState = null
                }
                result
            }
            is ModificationFlowResult.StartConfirmation -> {
                pendingState = result.pendingState
                result
            }
        }
    }

    internal fun seedForTest(state: PendingModificationState?) {
        pendingState = state
    }
}
