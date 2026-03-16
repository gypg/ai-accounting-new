package com.example.aiaccounting.utils

/**
 * Classify remote model ids into a small set of user-facing categories.
 *
 * We prefer parsing OpenRouter/OpenAI-compatible ids formatted as `vendor/model`.
 * This avoids false positives from naive substring matching (e.g. matching `yi` inside `tiny`).
 */
object ModelIdCategorizer {

    fun categorizeModelId(modelId: String): String {
        val trimmed = modelId.trim()
        if (trimmed.isBlank()) return "其他"

        val lower = trimmed.lowercase()

        // Prefer vendor prefix when present: vendor/model
        val vendor = lower.substringBefore('/', missingDelimiterValue = "")
        if (vendor.isNotBlank() && vendor != lower) {
            return when (vendor) {
                "openai" -> "OpenAI"
                "anthropic" -> "Claude"
                "google" -> "Gemini"
                "meta", "meta-llama" -> "Llama"
                "mistralai" -> "Mistral"
                "qwen", "alibaba" -> "通义千问"
                "deepseek" -> "DeepSeek"
                "zhipu", "zhipuai", "glm" -> "ChatGLM"
                "01-ai", "yi" -> "Yi"
                else -> "其他"
            }
        }

        // Fallback: best-effort keyword matching.
        return when {
            "claude" in lower -> "Claude"
            "gemini" in lower -> "Gemini"
            "qwen" in lower -> "通义千问"
            "deepseek" in lower -> "DeepSeek"
            // `yi` should match token boundaries only to avoid false positives (e.g. tiny)
            Regex("(^|[^a-z0-9])yi([^a-z0-9]|$)").containsMatchIn(lower) -> "Yi"
            "glm" in lower -> "ChatGLM"
            "mistral" in lower -> "Mistral"
            "llama" in lower || "meta" in lower -> "Llama"
            "gpt" in lower || "openai" in lower -> "OpenAI"
            else -> "其他"
        }
    }
}
