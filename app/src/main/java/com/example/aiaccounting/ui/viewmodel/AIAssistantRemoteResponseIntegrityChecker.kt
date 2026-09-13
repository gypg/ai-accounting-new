package com.example.aiaccounting.ui.viewmodel

import org.json.JSONArray
import org.json.JSONObject

internal class AIAssistantRemoteResponseIntegrityChecker {

    private val fencedJsonRegex = Regex("^```json\\s*([\\s\\S]*?)\\s*```$", RegexOption.IGNORE_CASE)
    private val trailingCommaRegex = Regex(",\\s*[}\\]]")

    fun isComplete(response: String): Boolean {
        val trimmed = response.trim()
        if (trimmed.isBlank()) {
            return false
        }

        val structuredPayload = extractStructuredPayload(trimmed) ?: return true
        if (!isValidJsonPayload(structuredPayload)) {
            return false
        }

        val wrappedContent = extractOpenAIWrappedContent(structuredPayload)
        if (wrappedContent.isNullOrBlank()) {
            return true
        }

        val wrappedStructuredPayload = extractStructuredPayload(wrappedContent.trim()) ?: return true
        return isValidJsonPayload(wrappedStructuredPayload)
    }

    private fun extractStructuredPayload(response: String): String? {
        val fencedJsonMatch = fencedJsonRegex.find(response)
        if (fencedJsonMatch != null) {
            return fencedJsonMatch.groupValues
                .getOrNull(1)
                ?.trim()
                ?: ""
        }

        return when {
            response.startsWith("```json", ignoreCase = true) -> ""
            response.startsWith("{") || response.startsWith("[") -> response
            else -> null
        }
    }

    private fun isValidJsonPayload(payload: String): Boolean {
        if (payload.isBlank()) {
            return false
        }

        if (trailingCommaRegex.containsMatchIn(payload)) {
            return false
        }

        return when {
            payload.startsWith("{") -> isValidJsonObject(payload)
            payload.startsWith("[") -> isValidJsonArray(payload)
            else -> false
        }
    }

    private fun isValidJsonObject(payload: String): Boolean {
        if (!payload.endsWith("}")) {
            return false
        }

        return try {
            JSONObject(payload)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidJsonArray(payload: String): Boolean {
        if (!payload.endsWith("]")) {
            return false
        }

        return try {
            JSONArray(payload)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun extractOpenAIWrappedContent(payload: String): String? {
        return runCatching {
            val json = JSONObject(payload)
            val choices = json.optJSONArray("choices") ?: return@runCatching null
            if (choices.length() == 0) {
                return@runCatching null
            }
            val firstChoice = choices.optJSONObject(0) ?: return@runCatching null
            val message = firstChoice.optJSONObject("message") ?: return@runCatching null
            if (!message.has("content")) {
                return@runCatching null
            }
            val contentValue = message.opt("content")
            when (contentValue) {
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
                else -> null
            }
        }.getOrNull()
    }
}
