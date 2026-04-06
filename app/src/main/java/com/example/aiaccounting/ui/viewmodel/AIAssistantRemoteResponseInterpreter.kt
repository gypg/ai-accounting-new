package com.example.aiaccounting.ui.viewmodel

import org.json.JSONArray
import org.json.JSONObject

internal sealed class RemoteResponseDecision {
    data class QueryBeforeExecute(val envelope: AIAssistantActionEnvelope) : RemoteResponseDecision()
    data class ExecuteActions(val envelope: AIAssistantActionEnvelope) : RemoteResponseDecision()
    data class ReturnRemoteReply(val reply: String) : RemoteResponseDecision()
}

internal class AIAssistantRemoteResponseInterpreter {

    private val actionTypeRegex = Regex(
        "\"(?:type|action)\"\\s*:\\s*\"(add_transaction|transfer|create_account|query|query_accounts|query_categories|query_transactions|create_category)\""
    )

    private val fencedJsonRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
    private val dirtyNullWrapperRegex = Regex("^(?:null\\s*)+")
    private val trailingDirtyNullWrapperRegex = Regex("(?:\\s*null)+$")
    private val ignorableUnknownActionNames = setOf("income", "expense", "transfer", "收入", "支出", "转账")

    fun interpret(remoteResponse: String): RemoteResponseDecision {
        val sanitizedResponse = sanitizeRemoteResponse(remoteResponse)
        val trimmed = sanitizedResponse.trim()

        val actionEnvelope = extractActionEnvelope(sanitizedResponse)
        if (actionEnvelope != null) {
            return if (requiresQueryBeforeExecution(actionEnvelope)) {
                RemoteResponseDecision.QueryBeforeExecute(actionEnvelope)
            } else {
                RemoteResponseDecision.ExecuteActions(actionEnvelope)
            }
        }

        val wrappedChoicesContent = extractOpenAIChoicesContent(trimmed)
        if (!wrappedChoicesContent.isNullOrBlank()) {
            val wrappedEnvelope = extractActionEnvelope(wrappedChoicesContent)
            if (wrappedEnvelope != null) {
                return if (requiresQueryBeforeExecution(wrappedEnvelope)) {
                    RemoteResponseDecision.QueryBeforeExecute(wrappedEnvelope)
                } else {
                    RemoteResponseDecision.ExecuteActions(wrappedEnvelope)
                }
            }
        }

        if (trimmed.startsWith("{")) {
            extractReplyField(trimmed)?.let { reply ->
                if (reply.isNotBlank()) {
                    return RemoteResponseDecision.ReturnRemoteReply(reply)
                }
            }
        }

        return RemoteResponseDecision.ReturnRemoteReply(extractDisplayReply(sanitizedResponse))
    }

    /**
     * 从 JSON 中提取 reply 字段
     * 用于提取 AI 助手的回复文本（如 {"thinking": "...", "reply": "你好！"}）
     */
    private fun extractReplyField(jsonText: String): String? {
        return runCatching {
            val json = JSONObject(jsonText)
            val reply = json.optString("reply", "").trim()
            reply.takeIf { it.isNotEmpty() }
        }.getOrNull()
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
                    }.filterNot(::isIgnorableUnknownAction)
                )
            } else {
                val jsonObject = JSONObject(jsonText)
                if (jsonObject.has("actions")) {
                    val actionsArray = jsonObject.getJSONArray("actions")
                    AIAssistantActionEnvelope(
                        actions = List(actionsArray.length()) { index ->
                            parseTypedAction(actionsArray.getJSONObject(index))
                        }.filterNot(::isIgnorableUnknownAction),
                        reply = jsonObject.optString("reply").trim().removeDirtyNullWrappers()
                    )
                } else {
                    AIAssistantActionEnvelope(
                        actions = listOf(parseTypedAction(jsonObject)).filterNot(::isIgnorableUnknownAction),
                        reply = jsonObject.optString("reply").trim().removeDirtyNullWrappers()
                    )
                }
            }
        }.getOrNull()?.takeIf { it.actions.isNotEmpty() }
    }

    private fun parseTypedAction(actionJson: JSONObject): AIAssistantTypedAction {
        val actionName = normalizeActionName(actionJson)
        return when (actionName) {
            "add_transaction", "transfer" -> {
                val accountRef = parseEntityReference(
                    actionJson = actionJson,
                    objectKey = "accountRef",
                    legacyNameKey = "account",
                    legacyIdKey = "accountId",
                    defaultKind = "account"
                )
                val legacyTransferRef = parseOptionalEntityReference(
                    actionJson = actionJson,
                    objectKey = "transferAccountRef",
                    legacyNameKey = "transferAccount",
                    legacyIdKey = "transferAccountId",
                    defaultKind = "account"
                )
                val fromAccountId = actionJson.optLong("fromAccountId").takeIf { it > 0 }
                val toAccountId = actionJson.optLong("toAccountId").takeIf { it > 0 }
                val normalizedAccountRef = if (fromAccountId != null && accountRef.id == null) {
                    accountRef.copy(id = fromAccountId, rawIdText = actionJson.optString("fromAccountId", accountRef.rawIdText))
                } else {
                    accountRef
                }
                val transferAccountRef = legacyTransferRef ?: toAccountId?.let {
                    AIAssistantEntityReference(
                        id = it,
                        name = "",
                        rawIdText = actionJson.optString("toAccountId", ""),
                        kind = "account"
                    )
                }

                val dateValue = actionJson.opt("date")
                val hasExplicitDate = actionJson.has("date")
                AIAssistantTypedAction.AddTransaction(
                    amount = actionJson.optDouble("amount", 0.0),
                    transactionTypeRaw = actionJson.optString(
                        "transactionType",
                        actionJson.optString(
                            "type",
                            if (actionName == "transfer") "transfer" else "expense"
                        )
                    ),
                    categoryRef = parseEntityReference(
                        actionJson = actionJson,
                        objectKey = "categoryRef",
                        legacyNameKey = "category",
                        legacyIdKey = "categoryId",
                        defaultKind = "category"
                    ),
                    accountRef = normalizedAccountRef,
                    transferAccountRef = transferAccountRef,
                    note = actionJson.optString("note", ""),
                    dateTimestamp = actionJson.optLong("date", System.currentTimeMillis()),
                    rawDate = when (dateValue) {
                        null -> null
                        is Number -> dateValue.toString()
                        else -> dateValue.toString()
                    },
                    hasExplicitDate = hasExplicitDate
                )
            }
            "create_account" -> AIAssistantTypedAction.CreateAccount(
                name = actionJson.optString("name", actionJson.optString("accountName", "")),
                accountTypeRaw = actionJson.optString("accountType", actionJson.optString("type", "OTHER")),
                balance = actionJson.optDouble("balance", actionJson.optDouble("initialBalance", 0.0))
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
                target = actionJson.optString("target", when {
                    actionJson.optString("type", "").trim() == "query_accounts" || actionJson.optString("action", "").trim() == "query_accounts" -> "accounts"
                    actionJson.optString("type", "").trim() == "query_categories" || actionJson.optString("action", "").trim() == "query_categories" -> "categories"
                    actionJson.optString("type", "").trim() == "query_transactions" || actionJson.optString("action", "").trim() == "query_transactions" -> "transactions"
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
            return when (explicitAction.lowercase()) {
                "query_accounts", "query_categories", "query_transactions" -> "query"
                else -> explicitAction
            }
        }

        val rawType = actionJson.optString("type", "").trim()
        return when (rawType.lowercase()) {
            "query_accounts" -> "query"
            "query_categories" -> "query"
            "query_transactions" -> "query"
            "add_transaction" -> "add_transaction"
            "income", "expense", "收入", "支出" -> {
                if (looksLikeTransactionPayload(actionJson)) {
                    "add_transaction"
                } else {
                    rawType
                }
            }
            "transfer", "转账" -> {
                if (looksLikeTransactionPayload(actionJson)) {
                    "transfer"
                } else {
                    rawType
                }
            }
            else -> rawType
        }
    }

    private fun looksLikeTransactionPayload(actionJson: JSONObject): Boolean {
        return actionJson.has("amount") ||
            actionJson.has("account") ||
            actionJson.has("accountId") ||
            actionJson.has("accountRef") ||
            actionJson.has("category") ||
            actionJson.has("categoryId") ||
            actionJson.has("categoryRef") ||
            actionJson.has("transactionType")
    }

    private fun isIgnorableUnknownAction(action: AIAssistantTypedAction): Boolean {
        return action is AIAssistantTypedAction.Unknown &&
            ignorableUnknownActionNames.contains(action.rawAction.trim().lowercase())
    }

    private fun containsActionMarkers(candidate: String): Boolean {
        return candidate.contains("\"action\"") ||
            candidate.contains("\"actions\"") ||
            actionTypeRegex.containsMatchIn(candidate)
    }

    private fun extractDisplayReply(response: String): String {
        val trimmed = response.trim().removeDirtyNullWrappers()
        if (trimmed.isEmpty()) {
            return trimmed
        }

        // Try to extract from OpenAI-compatible JSON format first
        extractOpenAIChoicesContent(trimmed)?.let { return it }

        val withoutFencedJson = trimmed.replace(fencedJsonRegex, " ").trim()
        val withoutInlineJson = stripDisplayJsonLikeSegments(withoutFencedJson)
            .removeDirtyNullWrappers()
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        if (withoutInlineJson.isNotBlank()) {
            return withoutInlineJson
        }

        val likelyJsonGarbage = trimmed.contains("```") ||
            trimmed.contains("\"action\"") ||
            trimmed.contains("\"actions\"") ||
            trimmed.contains('{') ||
            trimmed.contains('[')
        return if (likelyJsonGarbage) "" else trimmed
    }

    /**
     * Extract display content from OpenAI-compatible JSON responses like:
     * {"choices": [{"message": {"content": "您好！"}}]}
     */
    private fun extractOpenAIChoicesContent(jsonText: String): String? {
        return runCatching {
            val jsonObject = JSONObject(jsonText)
            val choices = jsonObject.optJSONArray("choices") ?: return@runCatching null
            if (choices.length() == 0) return@runCatching null
            val firstChoice = choices.optJSONObject(0) ?: return@runCatching null
            val message = firstChoice.optJSONObject("message") ?: return@runCatching null
            val contentValue = message.opt("content") ?: return@runCatching null
            val content = when (contentValue) {
                is String -> contentValue
                is JSONArray -> {
                    buildString {
                        for (index in 0 until contentValue.length()) {
                            val part = contentValue.opt(index)
                            when (part) {
                                is JSONObject -> {
                                    val text = part.optString("text", "")
                                    if (text.isNotBlank()) {
                                        append(text)
                                    }
                                }
                                is String -> append(part)
                            }
                        }
                    }
                }
                else -> ""
            }
            content.trim().takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    private fun requiresQueryBeforeExecution(envelope: AIAssistantActionEnvelope): Boolean {
        return envelope.actions.any { it is AIAssistantTypedAction.AddTransaction }
    }

    internal fun countAddTransactionActions(envelope: AIAssistantActionEnvelope): Int {
        return envelope.actions.count { it is AIAssistantTypedAction.AddTransaction }
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

    private fun stripDisplayJsonLikeSegments(text: String): String {
        if (text.isBlank()) {
            return text
        }

        val builder = StringBuilder()
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            if (ch == '{' || ch == '[') {
                val closeChar = if (ch == '{') '}' else ']'
                val segment = extractBracketedJson(text.substring(index), ch, closeChar)
                if (segment != null) {
                    index += segment.length
                    continue
                }
            }
            builder.append(ch)
            index += 1
        }
        return builder.toString()
    }

    private fun String.removeDirtyNullWrappers(): String {
        return trim()
            .replace(Regex("^(?:null\\s*)+"), "")
            .replace(Regex("(?:\\s*null)+$"), "")
            .trim()
    }

}
