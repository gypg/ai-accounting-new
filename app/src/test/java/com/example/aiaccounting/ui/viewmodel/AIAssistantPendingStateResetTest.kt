package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Test

class AIAssistantPendingStateResetTest {

    private val pendingModificationLifecycle = AIAssistantPendingModificationLifecycle(
        modificationCoordinator = AIAssistantModificationCoordinator(
            transactionModificationHandler = mockk()
        )
    )
    private val pendingClarificationLifecycle = AIAssistantPendingClarificationLifecycle()

    @Test
    fun clear_removesPendingModificationState() {
        pendingModificationLifecycle.seedForTest(
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

        pendingModificationLifecycle.clear()

        assertNull(pendingModificationLifecycle.currentState())
    }

    @Test
    fun clear_removesPendingClarificationState() {
        pendingClarificationLifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        pendingClarificationLifecycle.clear()

        assertNull(pendingClarificationLifecycle.currentState())
    }
}
