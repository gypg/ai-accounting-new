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
    fun begin_storesPendingState_withTransactionAccountTrigger() {
        val result = lifecycle.begin(
            originalMessage = "帮我记一笔午饭 25 元 支出",
            question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。"
        )

        assertEquals("这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。", result.reply)
        assertEquals(ClarificationTrigger.TRANSACTION_ACCOUNT, result.pendingState.trigger)
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun begin_storesPendingState_withTransactionCategoryTrigger() {
        val result = lifecycle.begin(
            originalMessage = "帮我记一笔 25 元 支出 今天",
            question = "这笔记到哪个分类呢？比如餐饮、交通或购物。"
        )

        assertEquals("这笔记到哪个分类呢？比如餐饮、交通或购物。", result.reply)
        assertEquals(ClarificationTrigger.TRANSACTION_CATEGORY, result.pendingState.trigger)
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun begin_storesPendingState_withTransactionDateTrigger() {
        val result = lifecycle.begin(
            originalMessage = "帮我记一笔午饭 25 元 支出",
            question = "这笔是哪天发生的呢？比如今天、昨天或 3 月 15 日。"
        )

        assertEquals("这笔是哪天发生的呢？比如今天、昨天或 3 月 15 日。", result.reply)
        assertEquals(ClarificationTrigger.TRANSACTION_DATE, result.pendingState.trigger)
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_returnsFallbackFinish_whenNoPendingStateExists() {
        val result = lifecycle.continuePending("25元", "xiaocainiang")

        assertEquals(ClarificationFlowResult.Finish("当前没有进行中的补充问题，我们继续聊你想做的事吧。"), result)
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
    fun continuePending_reasksQuestion_whenTransactionAccountIsStillMissing() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
            )
        )

        val result = lifecycle.continuePending("午饭", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                    trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
                ),
                reply = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。"
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_reasksQuestion_whenTransactionCategoryIsStillMissing() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔 25 元 支出 今天",
                question = "这笔记到哪个分类呢？比如餐饮、交通或购物。",
                trigger = ClarificationTrigger.TRANSACTION_CATEGORY
            )
        )

        val result = lifecycle.continuePending("这笔先记一下", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔 25 元 支出 今天",
                    question = "这笔记到哪个分类呢？比如餐饮、交通或购物。",
                    trigger = ClarificationTrigger.TRANSACTION_CATEGORY
                ),
                reply = "这笔记到哪个分类呢？比如餐饮、交通或购物。"
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_reasksQuestion_whenTransactionDateIsStillMissing() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔是哪天发生的呢？比如今天、昨天或 3 月 15 日。",
                trigger = ClarificationTrigger.TRANSACTION_DATE
            )
        )

        val result = lifecycle.continuePending("午饭", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    question = "这笔是哪天发生的呢？比如今天、昨天或 3 月 15 日。",
                    trigger = ClarificationTrigger.TRANSACTION_DATE
                ),
                reply = "这笔是哪天发生的呢？比如今天、昨天或 3 月 15 日。"
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesTransactionAccount() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
            )
        )

        val result = lifecycle.continuePending("微信", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    resumedMessage = "帮我记一笔午饭 25 元 支出 微信",
                    trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
                )
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
        lifecycle.clearAfterSuccessfulContinuation()
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesBankAliasAccount() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出 今天",
                question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
            )
        )

        val result = lifecycle.continuePending("工行", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔午饭 25 元 支出 今天",
                    resumedMessage = "帮我记一笔午饭 25 元 支出 今天 工行",
                    trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
                )
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
        lifecycle.clearAfterSuccessfulContinuation()
        assertNull(lifecycle.currentState())
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
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔午饭",
                    resumedMessage = "帮我记一笔午饭 25元",
                    trigger = ClarificationTrigger.TRANSACTION_AMOUNT
                )
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
        lifecycle.clearAfterSuccessfulContinuation()
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
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔 25 元",
                    resumedMessage = "帮我记一笔 25 元 支出",
                    trigger = ClarificationTrigger.TRANSACTION_TYPE
                )
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
        lifecycle.clearAfterSuccessfulContinuation()
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesTransactionCategory() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔 25 元 支出 今天",
                question = "这笔记到哪个分类呢？比如餐饮、交通或购物。",
                trigger = ClarificationTrigger.TRANSACTION_CATEGORY
            )
        )

        val result = lifecycle.continuePending("餐饮", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔 25 元 支出 今天",
                    resumedMessage = "帮我记一笔 25 元 支出 今天 餐饮",
                    trigger = ClarificationTrigger.TRANSACTION_CATEGORY
                )
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
        lifecycle.clearAfterSuccessfulContinuation()
        assertNull(lifecycle.currentState())
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesCustomAccountName() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
            )
        )

        val result = lifecycle.continuePending("旅行卡", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    resumedMessage = "帮我记一笔午饭 25 元 支出 旅行卡",
                    trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
                )
            ),
            result
        )
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesCustomCategoryName() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔 25 元 支出 今天",
                question = "这笔记到哪个分类呢？比如餐饮、交通或购物。",
                trigger = ClarificationTrigger.TRANSACTION_CATEGORY
            )
        )

        val result = lifecycle.continuePending("宠物用品", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔 25 元 支出 今天",
                    resumedMessage = "帮我记一笔 25 元 支出 今天 宠物用品",
                    trigger = ClarificationTrigger.TRANSACTION_CATEGORY
                )
            ),
            result
        )
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesCustomAccountNameWithoutKeyword() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
            )
        )

        val result = lifecycle.continuePending("旅游基金", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    resumedMessage = "帮我记一笔午饭 25 元 支出 旅游基金",
                    trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
                )
            ),
            result
        )
    }

    @Test
    fun continuePending_reasksQuestion_whenCategoryReplyLooksLikeAccount() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔 25 元 支出 今天",
                question = "这笔记到哪个分类呢？比如餐饮、交通或购物。",
                trigger = ClarificationTrigger.TRANSACTION_CATEGORY
            )
        )

        val result = lifecycle.continuePending("微信", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔 25 元 支出 今天",
                    question = "这笔记到哪个分类呢？比如餐饮、交通或购物。",
                    trigger = ClarificationTrigger.TRANSACTION_CATEGORY
                ),
                reply = "这笔记到哪个分类呢？比如餐饮、交通或购物。"
            ),
            result
        )
    }

    @Test
    fun continuePending_reasksQuestion_whenAccountReplyIsWeakAcknowledgement() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
            )
        )

        val result = lifecycle.continuePending("ok", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                    trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
                ),
                reply = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。"
            ),
            result
        )
    }

    @Test
    fun continuePending_reasksQuestion_whenAccountReplyIsWeakAcknowledgementWithPunctuation() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
            )
        )

        val result = lifecycle.continuePending("好的。", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.RequestClarification(
                pendingState = PendingClarificationState(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    question = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
                    trigger = ClarificationTrigger.TRANSACTION_ACCOUNT
                ),
                reply = "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。"
            ),
            result
        )
    }

    @Test
    fun continuePending_mergesOriginalMessage_whenUserProvidesTransactionDate() {
        lifecycle.seedForTest(
            PendingClarificationState(
                originalMessage = "帮我记一笔午饭 25 元 支出",
                question = "这笔是哪天发生的呢？比如今天、昨天或 3 月 15 日。",
                trigger = ClarificationTrigger.TRANSACTION_DATE
            )
        )

        val result = lifecycle.continuePending("昨天", "xiaocainiang")

        assertEquals(
            ClarificationFlowResult.ContinueWithPayload(
                ClarificationContinuationRequest(
                    originalMessage = "帮我记一笔午饭 25 元 支出",
                    resumedMessage = "帮我记一笔午饭 25 元 支出 昨天",
                    trigger = ClarificationTrigger.TRANSACTION_DATE
                )
            ),
            result
        )
        assertNotNull(lifecycle.currentState())
        lifecycle.clearAfterSuccessfulContinuation()
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
