package com.example.aiaccounting.ui.viewmodel

import org.json.JSONArray
import org.json.JSONObject

internal sealed class RemoteResponseDecision {
    data class ExecuteActions(val envelope: AIAssistantActionEnvelope) : RemoteResponseDecision()
    data class FallbackToLocalTransaction(val remoteReply: String) : RemoteResponseDecision()
    data class ReturnRemoteReply(val reply: String) : RemoteResponseDecision()
}

internal class AIAssistantRemoteResponseInterpreter {

    private val actionTypeRegex = Regex(
        "\"type\"\\s*:\\s*\"(add_transaction|transfer|create_account|query|query_accounts|query_categories|query_transactions|create_category)\""
    )

    private val transactionKeywords = listOf(
        "花了", "收入", "支出", "消费", "买", "卖", "转账", "付", "赚", "工资", "奖金", "红包", "退款", "报销"
    )
    private val transactionPhrases = listOf("记账", "记个账", "记一笔", "记下", "记录一笔")
    private val fencedJsonRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
    private val dirtyNullWrapperRegex = Regex("^(?:null\\s*)+")
    private val trailingDirtyNullWrapperRegex = Regex("(?:\\s*null)+$")

    fun interpret(userMessage: String, remoteResponse: String): RemoteResponseDecision {
        val sanitizedResponse = sanitizeRemoteResponse(remoteResponse)
        val actionEnvelope = extractActionEnvelope(sanitizedResponse)
        return if (actionEnvelope != null) {
            RemoteResponseDecision.ExecuteActions(actionEnvelope)
        } else if (isTransactionRequest(userMessage)) {
            RemoteResponseDecision.FallbackToLocalTransaction(extractDisplayReply(sanitizedResponse))
        } else {
            RemoteResponseDecision.ReturnRemoteReply(extractDisplayReply(sanitizedResponse))
        }
    }

    fun combineRemoteAndLocalReply(remoteReply: String, localResult: String): String {
        return when {
            isLocalFailure(localResult) -> localResult
            remoteReply.isBlank() -> localResult
            else -> "$remoteReply\n\n$localResult"
        }
    }

    private fun isActionCommand(response: String): Boolean {
        val candidate = extractExecutableJsonCandidate(response.trim()) ?: return false

        return candidate.contains("\"action\"") ||
            candidate.contains("\"actions\"") ||
            actionTypeRegex.containsMatchIn(candidate)
    }

    private fun extractExecutableJsonCandidate(response: String): String? {
        fencedJsonRegex.find(response)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { candidate ->
                return unwrapExecutableJsonCandidate(candidate)
            }

        return unwrapExecutableJsonCandidate(response)
    }

    private fun unwrapExecutableJsonCandidate(text: String): String? {
        val directCandidate = extractActionJsonCandidate(text)
        if (directCandidate != null) {
            return maybeUnwrapKnownWrapper(directCandidate) ?: directCandidate
        }
        return null
    }

    private fun maybeUnwrapKnownWrapper(candidate: String): String? {
        val trimmed = candidate.trim()
        val wrapperKeys = listOf("data", "result", "payload")
        return wrapperKeys.firstNotNullOfOrNull { key ->
            extractWrappedField(trimmed, key)?.takeIf(::containsActionMarkers)
        }
    }

    private fun extractActionJsonCandidate(text: String): String? {
        val objectStart = text.indexOf('{')
        val arrayStart = text.indexOf('[')

        val firstCandidate = when {
            objectStart == -1 && arrayStart == -1 -> null
            objectStart == -1 -> extractBracketedJson(text, '[', ']')
            arrayStart == -1 -> extractBracketedJson(text, '{', '}')
            arrayStart < objectStart -> extractBracketedJson(text, '[', ']')
            else -> extractBracketedJson(text, '{', '}')
        }
        if (firstCandidate != null && containsActionMarkers(firstCandidate)) {
            return firstCandidate
        }

        val fallbackCandidate = when {
            firstCandidate?.startsWith("[") == true -> extractBracketedJson(text, '{', '}')
            firstCandidate?.startsWith("{") == true -> extractBracketedJson(text, '[', ']')
            else -> null
        }

        return fallbackCandidate?.takeIf(::containsActionMarkers)
    }

    internal fun parseActionEnvelopeFromResponse(response: String): AIAssistantActionEnvelope {
        val sanitizedResponse = sanitizeRemoteResponse(response)
        return extractActionEnvelope(sanitizedResponse)
            ?: throw IllegalArgumentException("无法解析动作语义")
    }

    internal fun parseActionEnvelopeForExecutor(jsonText: String): AIAssistantActionEnvelope {
        return parseActionEnvelope(jsonText)
            ?: throw IllegalArgumentException("无法解析动作语义")
    }

    private fun extractActionEnvelope(response: String): AIAssistantActionEnvelope? {
        val executableJson = extractExecutableJsonCandidate(response) ?: return null
        return parseActionEnvelope(executableJson)
    }

    private fun parseActionEnvelope(jsonText: String): AIAssistantActionEnvelope? {
        return runCatching {
            if (jsonText.trim().startsWith("[")) {
                val actionsArray = JSONArray(jsonText)
                AIAssistantActionEnvelope(
                    actions = List(actionsArray.length()) { index ->
                        parseTypedAction(actionsArray.getJSONObject(index))
                    }
                )
            } else {
                val jsonObject = JSONObject(jsonText)
                if (jsonObject.has("actions")) {
                    val actionsArray = jsonObject.getJSONArray("actions")
                    AIAssistantActionEnvelope(
                        actions = List(actionsArray.length()) { index ->
                            parseTypedAction(actionsArray.getJSONObject(index))
                        },
                        reply = jsonObject.optString("reply").trim().removeDirtyNullWrappers()
                    )
                } else {
                    AIAssistantActionEnvelope(
                        actions = listOf(parseTypedAction(jsonObject)),
                        reply = jsonObject.optString("reply").trim().removeDirtyNullWrappers()
                    )
                }
            }
        }.getOrNull()?.takeIf { it.actions.isNotEmpty() }
    }

    private fun parseTypedAction(actionJson: JSONObject): AIAssistantTypedAction {
        val actionName = normalizeActionName(actionJson)
        return when (actionName) {
            "add_transaction", "transfer" -> AIAssistantTypedAction.AddTransaction(
                amount = actionJson.optDouble("amount", 0.0),
                transactionTypeRaw = actionJson.optString("transactionType", actionJson.optString("type", "expense")),
                categoryRef = parseEntityReference(
                    actionJson = actionJson,
                    objectKey = "categoryRef",
                    legacyNameKey = "category",
                    legacyIdKey = "categoryId",
                    defaultKind = "category"
                ),
                accountRef = parseEntityReference(
                    actionJson = actionJson,
                    objectKey = "accountRef",
                    legacyNameKey = "account",
                    legacyIdKey = "accountId",
                    defaultKind = "account"
                ),
                transferAccountRef = parseOptionalEntityReference(
                    actionJson = actionJson,
                    objectKey = "transferAccountRef",
                    legacyNameKey = "transferAccount",
                    legacyIdKey = "transferAccountId",
                    defaultKind = "account"
                ),
                note = actionJson.optString("note", ""),
                dateTimestamp = actionJson.optLong("date", System.currentTimeMillis())
            )
            "create_account" -> AIAssistantTypedAction.CreateAccount(
                name = actionJson.optString("name", ""),
                accountTypeRaw = actionJson.optString("accountType", actionJson.optString("type", "OTHER")),
                balance = actionJson.optDouble("balance", 0.0)
            )
            "create_category" -> AIAssistantTypedAction.CreateCategory(
                name = actionJson.optString("name", actionJson.optString("categoryName", "")),
                categoryTypeRaw = actionJson.optString(
                    "categoryType",
                    actionJson.optString("transactionType", actionJson.optString("type", ""))
                ),
                parentId = when {
                    actionJson.has("parentId") -> actionJson.optLong("parentId").takeIf { it > 0 }
                    actionJson.has("parentCategoryId") -> actionJson.optLong("parentCategoryId").takeIf { it > 0 }
                    else -> null
                }
            )
            "query" -> AIAssistantTypedAction.Query(
                target = actionJson.optString("target", when (actionJson.optString("type", "").trim()) {
                    "query_accounts" -> "accounts"
                    "query_categories" -> "categories"
                    "query_transactions" -> "transactions"
                    else -> ""
                })
            )
            else -> AIAssistantTypedAction.Unknown(actionName)
        }
    }

    private fun parseEntityReference(
        actionJson: JSONObject,
        objectKey: String,
        legacyNameKey: String,
        legacyIdKey: String,
        defaultKind: String
    ): AIAssistantEntityReference {
        return parseOptionalEntityReference(actionJson, objectKey, legacyNameKey, legacyIdKey, defaultKind)
            ?: AIAssistantEntityReference(
                id = actionJson.optLong(legacyIdKey).takeIf { it > 0 },
                name = actionJson.optString(legacyNameKey, ""),
                rawIdText = actionJson.optString(legacyIdKey, ""),
                kind = defaultKind
            )
    }

    private fun parseOptionalEntityReference(
        actionJson: JSONObject,
        objectKey: String,
        legacyNameKey: String,
        legacyIdKey: String,
        defaultKind: String
    ): AIAssistantEntityReference? {
        val objectRef = actionJson.optJSONObject(objectKey)
        if (objectRef != null) {
            return AIAssistantEntityReference(
                id = objectRef.optLong("id").takeIf { it > 0 },
                name = objectRef.optString("name", actionJson.optString(legacyNameKey, "")),
                rawIdText = objectRef.optString("id", actionJson.optString(legacyIdKey, "")),
                kind = objectRef.optString("kind", defaultKind)
            )
        }

        val legacyId = actionJson.optLong(legacyIdKey).takeIf { it > 0 }
        val legacyName = actionJson.optString(legacyNameKey, "")
        val rawIdText = actionJson.optString(legacyIdKey, "")
        if (legacyId != null || legacyName.isNotBlank() || rawIdText.isNotBlank()) {
            return AIAssistantEntityReference(
                id = legacyId,
                name = legacyName,
                rawIdText = rawIdText,
                kind = defaultKind
            )
        }

        return null
    }

    private fun normalizeActionName(actionJson: JSONObject): String {
        val explicitAction = actionJson.optString("action", "").trim()
        if (explicitAction.isNotBlank()) {
            return explicitAction
        }

        return when (actionJson.optString("type", "").trim()) {
            "query_accounts" -> "query"
            "query_categories" -> "query"
            "query_transactions" -> "query"
            else -> actionJson.optString("type", "").trim()
        }
    }

    private fun containsActionMarkers(candidate: String): Boolean {
        return candidate.contains("\"action\"") ||
            candidate.contains("\"actions\"") ||
            actionTypeRegex.containsMatchIn(candidate)
    }

    private fun extractDisplayReply(response: String): String {
        val trimmed = response.trim()
        if (trimmed.isEmpty()) {
            return trimmed
        }

        val jsonCandidate = extractActionJsonCandidate(trimmed)
        return if (jsonCandidate != null && trimmed == jsonCandidate) {
            trimmed
        } else {
            trimmed
        }
    }

    private fun sanitizeRemoteResponse(response: String): String {
        val trimmed = response.trim()
        if (trimmed.isEmpty()) {
            return trimmed
        }

        val cleanedWrapper = trimmed
            .replace(dirtyNullWrapperRegex, "")
            .replace(trailingDirtyNullWrapperRegex, "")
            .trim()

        return if (cleanedWrapper.isNotEmpty()) cleanedWrapper else trimmed
    }

    private fun extractBracketedJson(text: String, openChar: Char, closeChar: Char): String? {
        val startIndex = text.indexOf(openChar)
        if (startIndex == -1) {
            return null
        }

        var depth = 0
        var inString = false
        var escaped = false

        for (index in startIndex until text.length) {
            val ch = text[index]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\') {
                escaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (inString) {
                continue
            }
            if (ch == openChar) {
                depth += 1
            } else if (ch == closeChar) {
                depth -= 1
                if (depth == 0) {
                    return text.substring(startIndex, index + 1)
                }
            }
        }

        return null
    }

    private fun extractWrappedField(text: String, key: String): String? {
        val keyRegex = Regex("\"$key\"\\s*:\\s*")
        val match = keyRegex.find(text) ?: return null
        val valueStart = match.range.last + 1
        if (valueStart >= text.length) {
            return null
        }

        val candidateText = text.substring(valueStart).trimStart()
        return when {
            candidateText.startsWith("{") -> extractBracketedJson(candidateText, '{', '}')
            candidateText.startsWith("[") -> extractBracketedJson(candidateText, '[', ']')
            else -> null
        }
    }

    private fun isLocalFailure(localResult: String): Boolean {
        val normalized = localResult.trim()
        return normalized.contains("❌") ||
            normalized.startsWith("错误") ||
            normalized.startsWith("记账失败") ||
            normalized.startsWith("创建账户失败") ||
            normalized.startsWith("创建分类失败") ||
            normalized.startsWith("执行操作时出错")
    }

    private fun String.removeDirtyNullWrappers(): String {
        return trim()
            .replace(Regex("^(?:null\\s*)+"), "")
            .replace(Regex("(?:\\s*null)+$"), "")
            .trim()
    }

    private fun isTransactionRequest(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return transactionPhrases.any { lowerMessage.contains(it) } ||
            transactionKeywords.any { lowerMessage.contains(it) }
    }
}
