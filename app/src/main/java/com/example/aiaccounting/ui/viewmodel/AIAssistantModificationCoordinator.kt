package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.model.ButlerPersonaRegistry

internal data class PendingModificationState(
    val intent: TransactionModificationHandler.ModificationIntent,
    val confirmation: TransactionModificationHandler.ModificationConfirmation
)

internal sealed class ModificationFlowResult {
    data class StartConfirmation(
        val pendingState: PendingModificationState,
        val reply: String
    ) : ModificationFlowResult()

    data class Finish(
        val reply: String,
        val shouldClearPending: Boolean = false
    ) : ModificationFlowResult()
}

internal class AIAssistantModificationCoordinator(
    private val transactionModificationHandler: TransactionModificationHandler
) {
    suspend fun beginModification(
        message: String,
        butlerId: String
    ): ModificationFlowResult {
        val modificationRequest = transactionModificationHandler.detectModificationIntent(message)
        if (modificationRequest.targetTransaction == null) {
            return ModificationFlowResult.Finish(
                ButlerPersonaRegistry.buildModificationNotFoundReply()
            )
        }

        val confirmation = transactionModificationHandler.generateModificationConfirmation(modificationRequest)
            ?: return ModificationFlowResult.Finish(ButlerPersonaRegistry.buildModificationCannotGenerateReply())

        return ModificationFlowResult.StartConfirmation(
            pendingState = PendingModificationState(
                intent = modificationRequest.intent,
                confirmation = confirmation
            ),
            reply = transactionModificationHandler.generatePersonalityConfirmationMessage(butlerId, confirmation)
        )
    }

    suspend fun continueModification(
        message: String,
        butlerId: String,
        pendingState: PendingModificationState
    ): ModificationFlowResult {
        return when {
            transactionModificationHandler.isCancellation(message) -> {
                val reply = ButlerPersonaRegistry.buildModificationCancellationReply(butlerId)
                ModificationFlowResult.Finish(reply, shouldClearPending = true)
            }

            transactionModificationHandler.isConfirmation(message) -> {
                val result = when (pendingState.intent) {
                    TransactionModificationHandler.ModificationIntent.DELETE_LAST_TRANSACTION,
                    TransactionModificationHandler.ModificationIntent.DELETE_SPECIFIC_TRANSACTION -> {
                        transactionModificationHandler.executeDelete(pendingState.confirmation.transaction)
                    }
                    else -> {
                        transactionModificationHandler.executeModification(pendingState.confirmation)
                    }
                }
                if (result.success) {
                    ModificationFlowResult.Finish(
                        transactionModificationHandler.generatePersonalitySuccessMessage(butlerId, result),
                        shouldClearPending = true
                    )
                } else {
                    ModificationFlowResult.Finish(ButlerPersonaRegistry.buildModificationFailureReply())
                }
            }

            else -> ModificationFlowResult.Finish(ButlerPersonaRegistry.buildModificationInstructionReply())
        }
    }
}
