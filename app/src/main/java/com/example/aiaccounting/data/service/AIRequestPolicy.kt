package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.model.AIConfig

internal enum class AIRequestKind {
    CONNECTION_TEST,
    MODEL_FETCH,
    NON_STREAM_CHAT,
    STREAM_CHAT
}

internal data class AIRequestPolicy(
    val kind: AIRequestKind,
    val maxAttempts: Int,
    val allowRetry: Boolean,
    val allowModelFallback: Boolean
)

internal class AIRequestPolicyResolver {

    fun resolve(kind: AIRequestKind, config: AIConfig): AIRequestPolicy {
        return when (kind) {
            AIRequestKind.CONNECTION_TEST -> AIRequestPolicy(
                kind = kind,
                maxAttempts = 2,
                allowRetry = true,
                allowModelFallback = config.model.isBlank()
            )
            AIRequestKind.MODEL_FETCH -> AIRequestPolicy(
                kind = kind,
                maxAttempts = 1,
                allowRetry = false,
                allowModelFallback = false
            )
            AIRequestKind.NON_STREAM_CHAT -> AIRequestPolicy(
                kind = kind,
                maxAttempts = 2,
                allowRetry = true,
                allowModelFallback = true
            )
            AIRequestKind.STREAM_CHAT -> AIRequestPolicy(
                kind = kind,
                maxAttempts = 1,
                allowRetry = false,
                allowModelFallback = false
            )
        }
    }
}
