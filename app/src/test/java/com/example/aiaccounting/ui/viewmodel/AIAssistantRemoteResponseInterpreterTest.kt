package com.example.aiaccounting.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantRemoteResponseInterpreterTest {

    private val interpreter = AIAssistantRemoteResponseInterpreter()

    @Test
    fun interpret_returnsExecuteActions_whenResponseContainsActionField() {
        val decision = interpreter.interpret(
            remoteResponse = "{\"action\":\"add_transaction\",\"amount\":25}"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single()
        assertTrue(action is AIAssistantTypedAction.AddTransaction)
    }

    @Test
    fun interpret_returnsExecuteActions_whenDirtyNullWrapperContainsJsonAction() {
        val decision = interpreter.interpret(
            remoteResponse = "nullnull {\"action\":\"add_transaction\",\"amount\":25} null"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals(25.0, action.amount, 0.0)
    }

    @Test
    fun interpret_returnsExecuteActions_whenJsonActionHasShortTextPreamble() {
        val decision = interpreter.interpret(
            remoteResponse = "好的，下面是执行结果：{\"action\":\"add_transaction\",\"amount\":25}"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals(25.0, action.amount, 0.0)
    }

    @Test
    fun interpret_returnsExecuteActions_whenJsonActionHasPreambleAndTrailingText() {
        val decision = interpreter.interpret(
            remoteResponse = "好的，我来处理。{\"action\":\"add_transaction\",\"amount\":25} 已帮你生成动作"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals(25.0, action.amount, 0.0)
    }

    @Test
    fun interpret_returnsExecuteActions_whenFencedJsonHasLeadingText() {
        val decision = interpreter.interpret(
            remoteResponse = "我会按你的要求执行：\n```json\n{\"action\":\"add_transaction\",\"amount\":25}\n```"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals(25.0, action.amount, 0.0)
    }

    @Test
    fun interpret_returnsExecuteActions_whenResponseContainsArrayRootActions() {
        val decision = interpreter.interpret(
            remoteResponse = "[{\"action\":\"add_transaction\",\"amount\":25}]"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        assertTrue((decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() is AIAssistantTypedAction.AddTransaction)
    }

    @Test
    fun interpret_returnsExecuteActions_whenResponseWrapsActionInsideDataObject() {
        val decision = interpreter.interpret(
            remoteResponse = "{\"data\":{\"action\":\"add_transaction\",\"amount\":25}}"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals(25.0, action.amount, 0.0)
    }

    @Test
    fun interpret_returnsExecuteActions_whenResponseWrapsActionsInsidePayloadObject() {
        val decision = interpreter.interpret(
            remoteResponse = "{\"payload\":{\"actions\":[{\"action\":\"query\",\"target\":\"accounts\"},{\"action\":\"query\",\"target\":\"categories\"}]}}"
        )

        assertTrue(decision is RemoteResponseDecision.ExecuteActions)
        val actions = (decision as RemoteResponseDecision.ExecuteActions).envelope.actions
        assertEquals(2, actions.size)
        assertTrue(actions[0] is AIAssistantTypedAction.Query)
        assertTrue(actions[1] is AIAssistantTypedAction.Query)
    }

    @Test
    fun interpret_returnsRemoteReply_whenPlainTextMentionsActionWord() {
        val decision = interpreter.interpret(
            remoteResponse = "我可以根据你的需求决定下一步 action。"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        assertEquals("我可以根据你的需求决定下一步 action。", (decision as RemoteResponseDecision.ReturnRemoteReply).reply)
    }

    @Test
    fun interpret_returnsRemoteReply_whenDirtyNullWrapperContainsPlainReply() {
        val decision = interpreter.interpret(
            remoteResponse = "nullnull 你好呀，我在呢 null"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        assertEquals("你好呀，我在呢", (decision as RemoteResponseDecision.ReturnRemoteReply).reply)
    }

    @Test
    fun interpret_returnsRemoteReply_whenNoExecutableEnvelope_evenIfMessageLooksBookkeeping() {
        val decision = interpreter.interpret(
            remoteResponse = "好的，我来帮你处理这笔记录。"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        assertEquals("好的，我来帮你处理这笔记录。", (decision as RemoteResponseDecision.ReturnRemoteReply).reply)
    }

    @Test
    fun interpret_returnsRemoteReply_whenMessageOnlyContainsGenericRecordWord() {
        val decision = interpreter.interpret(
            remoteResponse = "好的，我记住了。"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        assertEquals("好的，我记住了。", (decision as RemoteResponseDecision.ReturnRemoteReply).reply)
    }

    @Test
    fun interpret_returnsRemoteReply_forBuyWithoutAmount_whenNoActionJson() {
        val decision = interpreter.interpret(
            remoteResponse = "好的，我可以给你推荐几家店。"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        assertEquals("好的，我可以给你推荐几家店。", (decision as RemoteResponseDecision.ReturnRemoteReply).reply)
    }

    @Test
    fun interpret_mapsQueryTypeAliases_toTypedQueryTargets() {
        val decision = interpreter.interpret(
            remoteResponse = "{\"type\":\"query_accounts\"}"
        )

        assertTrue(decision is RemoteResponseDecision.ExecuteActions)
        val action = (decision as RemoteResponseDecision.ExecuteActions).envelope.actions.single() as AIAssistantTypedAction.Query
        assertEquals("accounts", action.target)
    }

    @Test
    fun interpret_mapsQueryActionAliases_toTypedQueryTargets() {
        val decision = interpreter.interpret(
            remoteResponse = "{\"action\":\"query_accounts\"}"
        )

        assertTrue(decision is RemoteResponseDecision.ExecuteActions)
        val action = (decision as RemoteResponseDecision.ExecuteActions).envelope.actions.single() as AIAssistantTypedAction.Query
        assertEquals("accounts", action.target)
    }

    @Test
    fun interpret_parsesLegacyCreateAccountShape_withTypeAndAccountName() {
        val decision = interpreter.interpret(
            remoteResponse = "{\"type\":\"create_account\",\"accountName\":\"微信\",\"accountType\":\"WECHAT\",\"balance\":500}"
        )

        assertTrue(decision is RemoteResponseDecision.ExecuteActions)
        val action = (decision as RemoteResponseDecision.ExecuteActions).envelope.actions.single() as AIAssistantTypedAction.CreateAccount
        assertEquals("微信", action.name)
        assertEquals("WECHAT", action.accountTypeRaw)
        assertEquals(500.0, action.balance, 0.0)
    }

    @Test
    fun interpret_parsesLegacyTransferShape_withFromToAccountIds() {
        val decision = interpreter.interpret(
            remoteResponse = "{\"type\":\"transfer\",\"amount\":100,\"fromAccountId\":1,\"toAccountId\":2,\"note\":\"转账\"}"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals("transfer", action.transactionTypeRaw)
        assertEquals(1L, action.accountRef.id)
        assertEquals(2L, action.transferAccountRef?.id)
    }

    @Test
    fun interpret_parsesTypedEntityReferences_forAddTransactionAction() {
        val decision = interpreter.interpret(
            remoteResponse = """
                {
                  "action":"add_transaction",
                  "amount":25,
                  "transactionType":"expense",
                  "accountRef":{"id":12,"name":"日常卡","kind":"account"},
                  "categoryRef":{"id":34,"name":"餐饮","kind":"category"},
                  "note":"午饭"
                }
            """.trimIndent()
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals(12L, action.accountRef.id)
        assertEquals("日常卡", action.accountRef.name)
        assertEquals(34L, action.categoryRef.id)
        assertEquals("餐饮", action.categoryRef.name)
    }

    @Test
    fun interpret_parsesTransferAction_withTypedEntityReferences() {
        val decision = interpreter.interpret(
            remoteResponse = """
                {
                  "action":"add_transaction",
                  "amount":100,
                  "transactionType":"transfer",
                  "accountRef":{"id":1,"name":"微信","kind":"account"},
                  "transferAccountRef":{"id":2,"name":"支付宝","kind":"account"},
                  "note":"转到支付宝"
                }
            """.trimIndent()
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals("transfer", action.transactionTypeRaw)
        assertEquals(1L, action.accountRef.id)
        assertEquals(2L, action.transferAccountRef?.id)
        assertEquals("支付宝", action.transferAccountRef?.name)
    }

    @Test
    fun interpret_parsesTransferAction_withExplicitTransferType() {
        val decision = interpreter.interpret(
            remoteResponse = """
                {
                  "type":"transfer",
                  "amount":100,
                  "accountRef":{"id":1,"name":"微信","kind":"account"},
                  "transferAccountRef":{"id":2,"name":"支付宝","kind":"account"},
                  "note":"转到支付宝"
                }
            """.trimIndent()
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals("transfer", action.transactionTypeRaw)
        assertEquals(1L, action.accountRef.id)
        assertEquals(2L, action.transferAccountRef?.id)
    }

    @Test
    fun interpret_parsesTransferAlias_whenActionIsTransfer_withoutTypeField() {
        val decision = interpreter.interpret(
            remoteResponse = """
                {
                  "action":"transfer",
                  "amount":100,
                  "accountRef":{"id":1,"name":"微信","kind":"account"},
                  "transferAccountRef":{"id":2,"name":"支付宝","kind":"account"},
                  "note":"转到支付宝"
                }
            """.trimIndent()
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute)
        val action = (decision as RemoteResponseDecision.QueryBeforeExecute).envelope.actions.single() as AIAssistantTypedAction.AddTransaction
        assertEquals("transfer", action.transactionTypeRaw)
        assertEquals(1L, action.accountRef.id)
        assertEquals(2L, action.transferAccountRef?.id)
    }

    @Test
    fun interpret_malformedJsonGarbage_keepsSafeDecisionSemantics() {
        val decision = interpreter.interpret(
            remoteResponse = "好的 ```json {\"action\":\"add_transaction\",\"amount\":25,} ``` 我来帮你处理"
        )

        assertTrue(decision is RemoteResponseDecision.QueryBeforeExecute || decision is RemoteResponseDecision.ReturnRemoteReply)
        if (decision is RemoteResponseDecision.ReturnRemoteReply) {
            val remoteReply = decision.reply
            assertTrue(!remoteReply.contains("{") && !remoteReply.contains("}") && !remoteReply.contains("\"action\"") && !remoteReply.contains("```"))
            assertTrue(remoteReply.contains("好的") || remoteReply.contains("我来帮你处理"))
        }
    }

    @Test
    fun interpret_returnRemoteReply_stripsEmbeddedJsonGarbage_forNonTransactionMessage() {
        val decision = interpreter.interpret(
            remoteResponse = "当然可以，{\"foo\":\"bar\"} 我在呢"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        val reply = (decision as RemoteResponseDecision.ReturnRemoteReply).reply
        assertTrue(!reply.contains("{\"foo\":\"bar\"}"))
        assertTrue(reply.contains("当然可以") || reply.contains("我在呢"))
    }

    @Test
    fun interpret_returnsBlankRemoteReply_whenDisplayContentIsOnlyJsonGarbage() {
        val decision = interpreter.interpret(
            remoteResponse = "```json\n{\"foo\":\"bar\"}\n```"
        )

        assertTrue(decision is RemoteResponseDecision.ReturnRemoteReply)
        assertEquals("", (decision as RemoteResponseDecision.ReturnRemoteReply).reply)
    }
}
