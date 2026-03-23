package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AIAssistantPendingModificationLifecycleTest {

    private val modificationCoordinator = mockk<AIAssistantModificationCoordinator>()
    private val lifecycle = AIAssistantPendingModificationLifecycle(modificationCoordinator)

    @Test
    fun begin_storesPendingState_whenCoordinatorStartsConfirmation() = runTest {
        val pendingState = pendingState()
        coEvery { modificationCoordinator.beginModification("把上一笔改成20", "xiaocainiang") } returns
            ModificationFlowResult.StartConfirmation(
                pendingState = pendingState,
                reply = "请确认修改"
            )

        val reply = lifecycle.begin("把上一笔改成20", "xiaocainiang")

        assertEquals("请确认修改", reply)
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun begin_keepsStateEmpty_whenCoordinatorFinishesImmediately() = runTest {
        coEvery { modificationCoordinator.beginModification("改一下", "xiaocainiang") } returns
            ModificationFlowResult.Finish("没有找到相关交易")

        val reply = lifecycle.begin("改一下", "xiaocainiang")

        assertEquals("没有找到相关交易", reply)
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_returnsFallback_whenNoPendingStateExists() = runTest {
        val reply = lifecycle.continuePending("确认", "xiaocainiang")

        assertEquals("抱歉，没有待确认的操作。", reply)
    }

    @Test
    fun continuePending_clearsState_whenUserConfirms() = runTest {
        lifecycle.seedForTest(pendingState())
        coEvery {
            modificationCoordinator.continueModification("确认", "xiaocainiang", any())
        } returns ModificationFlowResult.Finish("已确认", shouldClearPending = true)

        val reply = lifecycle.continuePending("确认", "xiaocainiang")

        assertEquals("已确认", reply)
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_clearsState_whenUserCancels() = runTest {
        lifecycle.seedForTest(pendingState())
        coEvery {
            modificationCoordinator.continueModification("取消", "xiaocainiang", any())
        } returns ModificationFlowResult.Finish("已取消", shouldClearPending = true)

        val reply = lifecycle.continuePending("取消", "xiaocainiang")

        assertEquals("已取消", reply)
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_keepsState_whenConfirmationFails() = runTest {
        lifecycle.seedForTest(pendingState())
        coEvery {
            modificationCoordinator.continueModification("确认", "xiaocainiang", any())
        } returns ModificationFlowResult.Finish("修改失败：数据库忙，请稍后重试", shouldClearPending = false)

        val reply = lifecycle.continuePending("确认", "xiaocainiang")

        assertEquals("修改失败：数据库忙，请稍后重试", reply)
        assertNotNull(lifecycle.currentState())
    }

    private fun pendingState() = PendingModificationState(
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
