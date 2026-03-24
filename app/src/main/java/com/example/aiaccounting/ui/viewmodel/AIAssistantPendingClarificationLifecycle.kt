package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIMessageParser

internal enum class ClarificationTrigger {
    TRANSACTION_AMOUNT,
    TRANSACTION_TYPE,
    TRANSACTION_ACCOUNT,
    TRANSACTION_CATEGORY,
    TRANSACTION_DATE,
    GENERIC
}

internal data class PendingClarificationState(
    val originalMessage: String,
    val question: String,
    val trigger: ClarificationTrigger
)

internal data class ClarificationContinuationRequest(
    val originalMessage: String,
    val resumedMessage: String,
    val trigger: ClarificationTrigger
)

internal sealed class ClarificationFlowResult {
    data class RequestClarification(
        val pendingState: PendingClarificationState,
        val reply: String
    ) : ClarificationFlowResult()

    data class ContinueWithPayload(val payload: ClarificationContinuationRequest) : ClarificationFlowResult()

    data class Finish(
        val reply: String,
        val shouldClearPending: Boolean = false
    ) : ClarificationFlowResult()
}

internal class AIAssistantPendingClarificationLifecycle(
    private val messageParser: AIMessageParser = AIMessageParser()
) {
    private var pendingState: PendingClarificationState? = null

    fun currentState(): PendingClarificationState? = pendingState

    fun clear() {
        pendingState = null
    }

    fun begin(originalMessage: String, question: String): ClarificationFlowResult.RequestClarification {
        val nextState = PendingClarificationState(
            originalMessage = originalMessage,
            question = question,
            trigger = inferTrigger(question)
        )
        pendingState = nextState
        return ClarificationFlowResult.RequestClarification(
            pendingState = nextState,
            reply = question
        )
    }

    fun continuePending(message: String, butlerId: String): ClarificationFlowResult {
        val currentPendingState = pendingState
            ?: return ClarificationFlowResult.Finish("抱歉，没有待补充的信息。")

        if (isCancellation(message)) {
            pendingState = null
            return ClarificationFlowResult.Finish(
                reply = cancellationReply(butlerId),
                shouldClearPending = true
            )
        }

        if (shouldReask(currentPendingState, message)) {
            return ClarificationFlowResult.RequestClarification(
                pendingState = currentPendingState,
                reply = currentPendingState.question
            )
        }

        return ClarificationFlowResult.ContinueWithPayload(
            payload = ClarificationContinuationRequest(
                originalMessage = currentPendingState.originalMessage,
                resumedMessage = "${currentPendingState.originalMessage} $message".trim(),
                trigger = currentPendingState.trigger
            )
        )
    }

    fun clearAfterSuccessfulContinuation() {
        pendingState = null
    }

    fun restore(state: PendingClarificationState) {
        pendingState = state
    }

    internal fun seedForTest(state: PendingClarificationState?) {
        pendingState = state
    }

    private fun inferTrigger(question: String): ClarificationTrigger {
        return when {
            question.contains("金额") -> ClarificationTrigger.TRANSACTION_AMOUNT
            question.contains("收入") || question.contains("支出") || question.contains("转账") -> {
                ClarificationTrigger.TRANSACTION_TYPE
            }
            question.contains("账户") || question.contains("微信") || question.contains("支付宝") || question.contains("银行卡") -> {
                ClarificationTrigger.TRANSACTION_ACCOUNT
            }
            question.contains("分类") || question.contains("类别") || question.contains("归类") -> {
                ClarificationTrigger.TRANSACTION_CATEGORY
            }
            question.contains("日期") || question.contains("时间") || question.contains("哪天") || question.contains("几号") -> {
                ClarificationTrigger.TRANSACTION_DATE
            }
            else -> ClarificationTrigger.GENERIC
        }
    }

    private fun shouldReask(state: PendingClarificationState, message: String): Boolean {
        return when (state.trigger) {
            ClarificationTrigger.TRANSACTION_AMOUNT -> !messageParser.containsAmount(message)
            ClarificationTrigger.TRANSACTION_TYPE -> !containsTransactionType(message)
            ClarificationTrigger.TRANSACTION_ACCOUNT -> !containsTransactionAccount(message)
            ClarificationTrigger.TRANSACTION_CATEGORY -> !containsTransactionCategory(message)
            ClarificationTrigger.TRANSACTION_DATE -> !containsTransactionDate(message)
            ClarificationTrigger.GENERIC -> message.isBlank()
        }
    }

    private fun containsTransactionType(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return listOf("收入", "支出", "转账", "花了", "消费", "赚", "收到", "工资", "奖金").any {
            lowerMessage.contains(it)
        }
    }

    private fun containsTransactionAccount(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return listOf(
            "微信", "wechat", "wx", "支付宝", "alipay", "花呗", "余额宝", "现金", "cash",
            "信用卡", "银行卡", "银行", "工行", "建行", "农行", "中行", "招行", "招商", "交行",
            "储蓄卡", "借记卡", "debit", "credit"
        ).any { lowerMessage.contains(it) }
    }

    private fun containsTransactionCategory(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return listOf(
            "餐饮", "交通", "购物", "娱乐", "住房", "医疗", "教育", "通讯",
            "工资", "奖金", "午饭", "午餐", "晚饭", "早餐", "咖啡", "奶茶",
            "打车", "地铁", "公交", "房租", "水电"
        ).any { lowerMessage.contains(it) }
    }

    private fun containsTransactionDate(message: String): Boolean {
        val lowerMessage = message.lowercase()
        if (listOf("今天", "昨天", "前天", "大前天", "刚才", "刚刚", "本周", "这周", "上周", "本月", "上月").any {
                lowerMessage.contains(it)
            }) {
            return true
        }
        return listOf(
            Regex("""\d{4}年\d{1,2}月\d{1,2}[日号]?"""),
            Regex("""\d{1,2}月\d{1,2}[日号]?"""),
            Regex("""\d{1,2}[./-]\d{1,2}""")
        ).any { it.containsMatchIn(message) }
    }

    private fun isCancellation(message: String): Boolean {
        return listOf("取消", "算了", "不用了", "不记了", "停止").any { message.contains(it) }
    }

    private fun cancellationReply(butlerId: String): String {
        return when (butlerId) {
            "xiaocainiang" -> "好的主人～已取消这次补充。🌸"
            "taotao" -> "好的～这次先不继续啦！✨"
            "guchen" -> "（懒洋洋地）...行，那就先这样。"
            "suqian" -> "（平静地）...已取消。"
            "yishuihan" -> "（微笑）好的，已为您取消这次补充。"
            else -> "已取消这次补充。"
        }
    }
}
