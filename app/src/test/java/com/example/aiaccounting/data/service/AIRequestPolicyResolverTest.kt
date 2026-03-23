package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.model.AIConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIRequestPolicyResolverTest {

    private val resolver = AIRequestPolicyResolver()

    @Test
    fun resolve_returnsTwoAttemptsAndAllowsFallback_forAutoConnectionTest() {
        val policy = resolver.resolve(
            kind = AIRequestKind.CONNECTION_TEST,
            config = AIConfig(model = "")
        )

        assertEquals(2, policy.maxAttempts)
        assertTrue(policy.allowModelFallback)
        assertFalse(policy.allowRetry)
    }

    @Test
    fun resolve_returnsSingleAttempt_forFixedConnectionTest() {
        val policy = resolver.resolve(
            kind = AIRequestKind.CONNECTION_TEST,
            config = AIConfig(model = "fixed-model")
        )

        assertEquals(1, policy.maxAttempts)
        assertFalse(policy.allowModelFallback)
        assertFalse(policy.allowRetry)
    }

    @Test
    fun resolve_keepsStreamChatSingleAttemptWithoutFallback() {
        val policy = resolver.resolve(
            kind = AIRequestKind.STREAM_CHAT,
            config = AIConfig(model = "")
        )

        assertEquals(1, policy.maxAttempts)
        assertFalse(policy.allowModelFallback)
        assertFalse(policy.allowRetry)
    }
}
