package com.example.aiaccounting.ui.viewmodel

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
        return isValidJsonObject(structuredPayload)
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
            response.startsWith("{") -> response
            else -> null
        }
    }

    private fun isValidJsonObject(payload: String): Boolean {
        if (payload.isBlank() || !payload.endsWith("}")) {
            return false
        }

        if (trailingCommaRegex.containsMatchIn(payload)) {
            return false
        }

        return try {
            JSONObject(payload)
            true
        } catch (_: Exception) {
            false
        }
    }
}
