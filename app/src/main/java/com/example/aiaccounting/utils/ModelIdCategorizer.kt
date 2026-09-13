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
        // If vendor is not recognized (gateway/aggregator prefix like 2api/openrouter), fall back to keyword matching
        // on the full id to keep category chips useful.
        val vendor = lower.substringBefore('/', missingDelimiterValue = "")
        if (vendor.isNotBlank() && vendor != lower) {
            when (vendor) {
                "openai" -> return "OpenAI"
                "anthropic" -> return "Claude"
                "google" -> return "Gemini"
                "meta", "meta-llama" -> return "Llama"
                "mistralai" -> return "Mistral"
                "qwen", "alibaba" -> return "通义千问"
                "deepseek" -> return "DeepSeek"
                "zhipu", "zhipuai", "glm" -> return "ChatGLM"
                "01-ai", "yi" -> return "Yi"
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
