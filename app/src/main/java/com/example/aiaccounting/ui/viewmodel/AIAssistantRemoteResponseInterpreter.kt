package com.example.aiaccounting.ui.viewmodel

internal sealed class RemoteResponseDecision {
    data class ExecuteActions(val rawResponse: String) : RemoteResponseDecision()
    data class FallbackToLocalTransaction(val remoteReply: String) : RemoteResponseDecision()
    data class ReturnRemoteReply(val reply: String) : RemoteResponseDecision()
}

internal class AIAssistantRemoteResponseInterpreter {

    private val actionTypeRegex = Regex(
        "\"type\"\\s*:\\s*\"(add_transaction|create_account|query|query_accounts|query_categories|query_transactions|create_category)\""
    )

    private val transactionKeywords = listOf(
        "花了", "收入", "支出", "消费", "买", "卖", "转账", "付", "赚", "工资", "奖金", "红包", "退款", "报销"
    )
    private val transactionPhrases = listOf("记账", "记个账", "记一笔")

    fun interpret(userMessage: String, remoteResponse: String): RemoteResponseDecision {
        return if (isActionCommand(remoteResponse)) {
            RemoteResponseDecision.ExecuteActions(remoteResponse)
        } else if (isTransactionRequest(userMessage)) {
            RemoteResponseDecision.FallbackToLocalTransaction(remoteResponse)
        } else {
            RemoteResponseDecision.ReturnRemoteReply(remoteResponse)
        }
    }

    fun combineRemoteAndLocalReply(remoteReply: String, localResult: String): String {
        return when {
            localResult.contains("❌") -> localResult
            remoteReply.isBlank() -> localResult
            else -> "$remoteReply\n\n$localResult"
        }
    }

    private fun isActionCommand(response: String): Boolean {
        val trimmed = response.trim()
        val candidate = extractJsonCandidate(trimmed) ?: return false

        return candidate.contains("\"action\"") ||
            candidate.contains("\"actions\"") ||
            actionTypeRegex.containsMatchIn(candidate)
    }

    private fun extractJsonCandidate(response: String): String? {
        val fencedJson = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
            .find(response)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        if (!fencedJson.isNullOrBlank()) {
            return fencedJson
        }

        val firstBrace = response.indexOf('{')
        val lastBrace = response.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return response.substring(firstBrace, lastBrace + 1).trim()
        }

        return null
    }

    private fun isTransactionRequest(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return transactionPhrases.any { lowerMessage.contains(it) } ||
            transactionKeywords.any { lowerMessage.contains(it) }
    }
}
