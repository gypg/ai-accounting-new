package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantPendingInteractionLifecycleTest {

    private val modificationCoordinator = mockk<AIAssistantModificationCoordinator>()
    private val modificationLifecycle = AIAssistantPendingModificationLifecycle(modificationCoordinator)
    private val clarificationLifecycle = AIAssistantPendingClarificationLifecycle()
    private val interactionLifecycle = AIAssistantPendingInteractionLifecycle(
        modificationLifecycle = modificationLifecycle,
        clarificationLifecycle = clarificationLifecycle
    )

    @Test
    fun currentState_returnsClarification_whenClarificationIsPending() {
        clarificationLifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        val state = interactionLifecycle.currentState()

        assertTrue(state is PendingInteractionState.Clarification)
    }

    @Test
    fun currentState_returnsModification_whenModificationIsPending() {
        modificationLifecycle.seedForTest(pendingModificationState())

        val state = interactionLifecycle.currentState()

        assertTrue(state is PendingInteractionState.Modification)
    }

    @Test
    fun clear_removesBothPendingStates() {
        modificationLifecycle.seedForTest(pendingModificationState())
        clarificationLifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        interactionLifecycle.clear()

        assertNull(interactionLifecycle.currentState())
        assertNull(interactionLifecycle.currentModificationState())
        assertNull(interactionLifecycle.currentClarificationState())
    }

    @Test
    fun beginModification_delegatesToModificationLifecycle() = runTest {
        coEvery { modificationCoordinator.beginModification("把上一笔改成20", "xiaocainiang") } returns
            ModificationFlowResult.StartConfirmation(
                pendingState = pendingModificationState(),
                reply = "请确认修改"
            )

        val result = interactionLifecycle.beginModification("把上一笔改成20", "xiaocainiang")

        assertTrue(result is ModificationFlowResult.StartConfirmation)
        assertTrue(interactionLifecycle.currentState() is PendingInteractionState.Modification)
    }

    @Test
    fun beginClarification_delegatesToClarificationLifecycle() {
        val result = interactionLifecycle.beginClarification(
            originalMessage = "帮我记一笔午饭",
            question = "请问这笔交易的金额是多少呢？"
        )

        assertEquals("请问这笔交易的金额是多少呢？", result.reply)
        assertTrue(interactionLifecycle.currentState() is PendingInteractionState.Clarification)
    }

    @Test
    fun continueClarification_keepsClarificationStateUntilContinuationIsConfirmedSuccessful() {
        interactionLifecycle.beginClarification(
            originalMessage = "帮我记一笔午饭",
            question = "请问这笔交易的金额是多少呢？"
        )

        val result = interactionLifecycle.continueClarification("25元", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔午饭",
                    resumedMessage = "帮我记一笔午饭 25元",
                    trigger = ClarificationTrigger.TRANSACTION_AMOUNT
                )
            ),
            result
        )
        assertTrue(interactionLifecycle.currentState() is PendingInteractionState.Clarification)
        interactionLifecycle.clearClarificationAfterSuccessfulContinuation()
        assertNull(interactionLifecycle.currentClarificationState())
    }

    private fun pendingModificationState() = PendingModificationState(
        intent = TransactionModificationHandler.ModificationIntent.MODIFY_LAST_TRANSACTION,
        confirmation = TransactionModificationHandler.ModificationConfirmation(
            transaction = Transaction(
                id = 1,
                accountId = 1,
                categoryId = 1,
                type = TransactionType.EXPENSE,
                amount = 10.0,
                date = 0L,
                note = "午饭"
            ),
            originalValues = emptyMap(),
            newValues = emptyMap(),
            confirmationMessage = "确认修改",
            requiresConfirmation = true
        )
    )
}
