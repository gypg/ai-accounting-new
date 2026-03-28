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
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantPendingModificationLifecycleTest {

    private val modificationCoordinator = mockk<AIAssistantModificationCoordinator>()
    private val lifecycle = AIAssistantPendingModificationLifecycle(modificationCoordinator)

    @Test
    fun begin_returnsConfirmationResult_andStoresPendingState_whenCoordinatorStartsConfirmation() = runTest {
        val pendingState = pendingState()
        coEvery { modificationCoordinator.beginModification("把上一笔改成20", "xiaocainiang") } returns
            ModificationFlowResult.StartConfirmation(
                pendingState = pendingState,
                reply = "请确认修改"
            )

        val result = lifecycle.begin("把上一笔改成20", "xiaocainiang")

        assertEquals(
            ModificationFlowResult.StartConfirmation(
                pendingState = pendingState,
                reply = "请确认修改"
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun begin_returnsFinish_whenCoordinatorFinishesImmediately() = runTest {
        coEvery { modificationCoordinator.beginModification("改一下", "xiaocainiang") } returns
            ModificationFlowResult.Finish("没有找到相关交易")

        val result = lifecycle.begin("改一下", "xiaocainiang")

        assertEquals(ModificationFlowResult.Finish("没有找到相关交易"), result)
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_returnsFallbackFinish_whenNoPendingStateExists() = runTest {
        val result = lifecycle.continuePending("确认", "xiaocainiang")

        assertEquals(ModificationFlowResult.Finish("当前没有进行中的确认操作，我们可以继续处理新的需求。"), result)
    }

    @Test
    fun continuePending_clearsState_whenUserConfirms() = runTest {
        lifecycle.seedForTest(pendingState())
        coEvery {
            modificationCoordinator.continueModification("确认", "xiaocainiang", any())
        } returns ModificationFlowResult.Finish("已确认", shouldClearPending = true)

        val result = lifecycle.continuePending("确认", "xiaocainiang")

        assertEquals(ModificationFlowResult.Finish("已确认", shouldClearPending = true), result)
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_clearsState_whenUserCancels() = runTest {
        lifecycle.seedForTest(pendingState())
        coEvery {
            modificationCoordinator.continueModification("取消", "xiaocainiang", any())
        } returns ModificationFlowResult.Finish("已取消", shouldClearPending = true)

        val result = lifecycle.continuePending("取消", "xiaocainiang")

        assertEquals(ModificationFlowResult.Finish("已取消", shouldClearPending = true), result)
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_keepsState_whenConfirmationFails() = runTest {
        lifecycle.seedForTest(pendingState())
        coEvery {
            modificationCoordinator.continueModification("确认", "xiaocainiang", any())
        } returns ModificationFlowResult.Finish("修改没有成功，请稍后重试。", shouldClearPending = false)

        val result = lifecycle.continuePending("确认", "xiaocainiang")

        assertEquals(
            ModificationFlowResult.Finish("修改没有成功，请稍后重试。", shouldClearPending = false),
            result
        )
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_keepsState_whenCoordinatorRequestsAnotherConfirmationStep() = runTest {
        val pendingState = pendingState()
        lifecycle.seedForTest(pendingState)
        coEvery {
            modificationCoordinator.continueModification("确认", "xiaocainiang", any())
        } returns ModificationFlowResult.StartConfirmation(
            pendingState = pendingState,
            reply = "请再确认一次"
        )

        val result = lifecycle.continuePending("确认", "xiaocainiang")

        assertTrue(result is ModificationFlowResult.StartConfirmation)
        assertEquals("请再确认一次", (result as ModificationFlowResult.StartConfirmation).reply)
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
