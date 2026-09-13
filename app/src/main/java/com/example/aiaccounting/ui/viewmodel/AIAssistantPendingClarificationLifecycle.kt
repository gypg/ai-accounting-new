package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIMessageParser
import com.example.aiaccounting.data.model.ButlerPersonaRegistry

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
            ?: return ClarificationFlowResult.Finish(ButlerPersonaRegistry.buildClarificationNoPendingReply())

        if (isCancellation(message)) {
            pendingState = null
            return ClarificationFlowResult.Finish(
                reply = ButlerPersonaRegistry.buildClarificationCancellationReply(butlerId),
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
        if (knownAccountKeywords().any { lowerMessage.contains(it) }) {
            return true
        }
        return isLikelyCustomAccountReply(message)
    }

    private fun containsTransactionCategory(message: String): Boolean {
        val lowerMessage = message.lowercase()
        if (knownCategoryKeywords().any { lowerMessage.contains(it) }) {
            return true
        }
        return isLikelyCustomCategoryReply(message)
    }

    private fun isLikelyCustomAccountReply(message: String): Boolean {
        val normalized = message.trim()
        val lowerMessage = normalized.lowercase()
        if (normalized.isBlank()) {
            return false
        }
        if (isNonSpecificEntityReply(lowerMessage) || isWeakAcknowledgementReply(lowerMessage)) {
            return false
        }
        if (messageParser.containsAmount(normalized) || containsTransactionType(normalized) || containsTransactionDate(normalized)) {
            return false
        }
        if (knownCategoryKeywords().any { lowerMessage.contains(it) }) {
            return false
        }
        if (lowerMessage.contains("分类") || lowerMessage.contains("类别") || lowerMessage.contains("归类")) {
            return false
        }
        val hasExplicitAccountHint = listOf(
            "账户", "卡", "钱包", "资金", "零钱", "备用", "日常", "生活", "bank", "wallet", "account"
        ).any { lowerMessage.contains(it) }
        return hasExplicitAccountHint || isLikelyConcreteCustomEntityName(normalized)
    }

    private fun isLikelyCustomCategoryReply(message: String): Boolean {
        val normalized = message.trim()
        val lowerMessage = normalized.lowercase()
        if (normalized.isBlank()) {
            return false
        }
        if (isNonSpecificEntityReply(lowerMessage) || isWeakAcknowledgementReply(lowerMessage)) {
            return false
        }
        if (messageParser.containsAmount(normalized) || containsTransactionType(normalized) || containsTransactionDate(normalized)) {
            return false
        }
        if (knownAccountKeywords().any { lowerMessage.contains(it) }) {
            return false
        }
        if (lowerMessage.contains("账户") || lowerMessage.contains("卡") || lowerMessage.contains("wallet") || lowerMessage.contains("account")) {
            return false
        }
        return isLikelyConcreteCustomEntityName(normalized)
    }

    private fun isWeakAcknowledgementReply(lowerMessage: String): Boolean {
        val normalized = lowerMessage.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").replace(Regex("\\s+"), " ").trim()
        return listOf(
            "ok", "okay", "好的", "好", "行", "可以", "嗯", "嗯嗯", "收到", "明白", "yes", "yep", "sure"
        ).any { normalized == it || normalized.startsWith("$it ") }
    }

    private fun isLikelyConcreteCustomEntityName(message: String): Boolean {
        val normalized = message.trim()
        if (normalized.isBlank()) {
            return false
        }
        if (normalized.length < 2) {
            return false
        }
        val compact = normalized.replace(Regex("\\s+"), "")
        if (compact.length < 2) {
            return false
        }
        if (compact.length > 20) {
            return false
        }
        if (!compact.any { it.isLetterOrDigit() }) {
            return false
        }
        if (isWeakAcknowledgementReply(compact.lowercase())) {
            return false
        }
        val normalizedLower = compact.lowercase()
        if (isNonSpecificEntityReply(normalizedLower)) {
            return false
        }
        return true
    }

    private fun isNonSpecificEntityReply(lowerMessage: String): Boolean {
        return listOf(
            "不知道", "不清楚", "不确定", "随便", "都行", "都可以", "随意", "你决定", "你定",
            "先记", "先这样", "之后再说", "晚点再说", "这笔先记一下"
        ).any { lowerMessage.contains(it) }
    }

    private fun knownAccountKeywords(): List<String> {
        return listOf(
            "微信", "wechat", "wx", "支付宝", "alipay", "花呗", "余额宝", "现金", "cash",
            "信用卡", "银行卡", "银行", "工行", "建行", "农行", "中行", "招行", "招商", "交行",
            "储蓄卡", "借记卡", "debit", "credit"
        )
    }

    private fun knownCategoryKeywords(): List<String> {
        return listOf(
            "餐饮", "交通", "购物", "娱乐", "住房", "医疗", "教育", "通讯",
            "工资", "奖金", "午饭", "午餐", "晚饭", "早餐", "咖啡", "奶茶",
            "打车", "地铁", "公交", "房租", "水电"
        )
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

}
