package com.example.aiaccounting.utils

/**
 * Utilities for building OpenAI-compatible endpoints.
 *
 * NOTE: This app persists `apiUrl` as either:
 * - gateway base: https://host (InviteGatewayService uses /bootstrap)
 * - OpenAI base: https://host/v1
 * - or legacy full endpoint: https://host/v1/chat/completions
 */
object OpenAiUrlUtils {

    fun normalizeBase(apiUrl: String): String {
        val trimmed = apiUrl.trim().removeSuffix("/")
        return when {
            trimmed.endsWith("/v1/chat/completions") -> trimmed.removeSuffix("/chat/completions")
            trimmed.endsWith("/chat/completions") -> trimmed.removeSuffix("/chat/completions")
            trimmed.endsWith("/v1") -> trimmed
            else -> "$trimmed/v1"
        }
    }

    fun chatCompletions(apiUrl: String): String {
        return "${normalizeBase(apiUrl)}/chat/completions"
    }

    fun models(apiUrl: String): String {
        return "${normalizeBase(apiUrl)}/models"
    }

    fun whisperTranscriptions(apiUrl: String): String {
        return "${normalizeBase(apiUrl)}/audio/transcriptions"
    }
}
