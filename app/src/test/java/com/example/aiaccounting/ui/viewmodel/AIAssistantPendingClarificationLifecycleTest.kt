package com.example.aiaccounting.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AIAssistantPendingClarificationLifecycleTest {

    private val lifecycle = AIAssistantPendingClarificationLifecycle()

    @Test
    fun begin_storesPendingState_withAmountTrigger() {
        val result = lifecycle.begin(
            originalMessage = "帮我记一笔午饭",
            question = "请问这笔交易的金额是多少呢？"
        )

        assertEquals("请问这笔交易的金额是多少呢？", result.reply)
        assertEquals(ClarificationTrigger.TRANSACTION_AMOUNT, result.pendingState.trigger)
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun begin_storesPendingState_withTransactionTypeTrigger() {
        val result = lifecycle.begin(
            originalMessage = "帮我记一笔 25 元",
            question = "这笔是收入、支出还是转账呢？"
        )

        assertEquals("这笔是收入、支出还是转账呢？", result.reply)
        assertEquals(ClarificationTrigger.TRANSACTION_TYPE, result.pendingState.trigger)
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_returnsFallbackFinish_whenNoPendingStateExists() {
        val result = lifecycle.continuePending("25元", "xiaocainiang")

        assertEquals(ClarificationFlowResult.Finish("抱歉，没有待补充的信息。"), result)
    }

    @Test
    fun continuePending_reasksQuestion_whenAmountIsStillMissing() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        val result = lifecycle.continuePending("午饭", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔午饭",
                    question = "请问这笔交易的金额是多少呢？",
                    trigger = ClarificationTrigger.TRANSACTION_AMOUNT
                ),
                reply = "请问这笔交易的金额是多少呢？"
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_reasksQuestion_whenTransactionTypeIsStillMissing() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔 25 元",
                question = "这笔是收入、支出还是转账呢？",
                trigger = ClarificationTrigger.TRANSACTION_TYPE
            )
        )

        val result = lifecycle.continuePending("午饭", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔 25 元",
                    question = "这笔是收入、支出还是转账呢？",
                    trigger = ClarificationTrigger.TRANSACTION_TYPE
                ),
                reply = "这笔是收入、支出还是转账呢？"
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesAmount() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        val result = lifecycle.continuePending("25元", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithMessage("帮我记一笔午饭 25元"),
            result
        )
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesTransactionType() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔 25 元",
                question = "这笔是收入、支出还是转账呢？",
                trigger = ClarificationTrigger.TRANSACTION_TYPE
            )
        )

        val result = lifecycle.continuePending("支出", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithMessage("帮我记一笔 25 元 支出"),
            result
        )
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_clearsState_whenUserCancels() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        val result = lifecycle.continuePending("取消", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.Finish(
                reply = "好的主人～已取消这次补充。🌸",
                shouldClearPending = true
            ),
            result
        )
        assertNull(lifecycle.currentState())
    }

    @Test
    fun clear_removesPendingState() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭",
                question = "请问这笔交易的金额是多少呢？",
                trigger = ClarificationTrigger.TRANSACTION_AMOUNT
            )
        )

        lifecycle.clear()

        assertNull(lifecycle.currentState())
    }
}
