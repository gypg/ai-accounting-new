package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Test

class AIAssistantPendingStateResetTest {

    @Test
    fun clear_removesPendingModificationState() {
        val modificationLifecycle = AIAssistantPendingModificationLifecycle(
            modificationCoordinator = AIAssistantModificationCoordinator(
                transactionModificationHandler = mockk()
            )
        )
        val clarificationLifecycle = AIAssistantPendingClarificationLifecycle()
        val lifecycle = AIAssistantPendingInteractionLifecycle(modificationLifecycle, clarificationLifecycle)
        modificationLifecycle.seedForTest(
            PendingModificationState(
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
        )

        lifecycle.clear()

        assertNull(lifecycle.currentState())
        assertNull(lifecycle.currentModificationState())
    }

    @Test
    fun clear_removesPendingClarificationState() {
        val modificationLifecycle = AIAssistantPendingModificationLifecycle(
            modificationCoordinator = AIAssistantModificationCoordinator(
                transactionModificationHandler = mockk()
            )
        )
        val clarificationLifecycle = AIAssistantPendingClarificationLifecycle()
        val lifecycle = AIAssistantPendingInteractionLifecycle(modificationLifecycle, clarificationLifecycle)
        clarificationLifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        lifecycle.clear()

        assertNull(lifecycle.currentState())
        assertNull(lifecycle.currentClarificationState())
    }
}
