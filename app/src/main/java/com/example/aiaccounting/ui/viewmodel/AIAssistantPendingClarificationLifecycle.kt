package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIMessageParser

internal enum class ClarificationTrigger {
    TRANSACTION_AMOUNT,
    TRANSACTION_TYPE,
    GENERIC
}

internal data class PendingClarificationState(
    val originalMessage: String,
    val question: String,
    val trigger: ClarificationTrigger
)

internal sealed class ClarificationFlowResult {
    data class RequestClarification(
        val pendingState: PendingClarificationState,
        val reply: String
    ) : ClarificationFlowResult()

    data class ContinueWithMessage(val message: String) : ClarificationFlowResult()

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

        pendingState = null
        return ClarificationFlowResult.ContinueWithMessage(
            message = "${currentPendingState.originalMessage} $message".trim()
        )
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
            else -> ClarificationTrigger.GENERIC
        }
    }

    private fun shouldReask(state: PendingClarificationState, message: String): Boolean {
        return when (state.trigger) {
            ClarificationTrigger.TRANSACTION_AMOUNT -> !messageParser.containsAmount(message)
            ClarificationTrigger.TRANSACTION_TYPE -> !containsTransactionType(message)
            ClarificationTrigger.GENERIC -> message.isBlank()
        }
    }

    private fun containsTransactionType(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return listOf("收入", "支出", "转账", "花了", "消费", "赚", "收到", "工资", "奖金").any {
            lowerMessage.contains(it)
        }
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
