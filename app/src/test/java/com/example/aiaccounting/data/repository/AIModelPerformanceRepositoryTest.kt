package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.service.ModelPerformanceFailureCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AIModelPerformanceRepositoryTest {

    @Test
    fun recordSuccess_andRecommendation_areScopedByProviderAndApiUrl() = runTest {
        val repository = InMemoryModelPerformanceStore()
        val config = AIConfig(
            provider = AIProvider.CUSTOM,
            apiUrl = "https://example.com/v1",
            model = "",
            isEnabled = true
        )

        repository.clearForConfig(config)
        repository.recordSuccess(config, "model-a", latencyMs = 120, nowMillis = 1000L)
        repository.recordFailure(config, "model-b", ModelPerformanceFailureCategory.TIMEOUT, nowMillis = 1100L)

        val snapshot = repository.getSnapshot(config).first()
        val recommendation = repository.getRecommendation(config, listOf("model-a", "model-b")).first()

        assertEquals(2, snapshot?.records?.size)
        assertEquals("model-a", recommendation?.modelId)
        assertEquals(120L, recommendation?.latencyMs)
    }

    @Test
    fun clearForConfig_removesOnlyMatchingIdentity() = runTest {
        val repository = InMemoryModelPerformanceStore()
        val configA = AIConfig(provider = AIProvider.CUSTOM, apiUrl = "https://a.example/v1")
        val configB = AIConfig(provider = AIProvider.CUSTOM, apiUrl = "https://b.example/v1")

        repository.clearForConfig(configA)
        repository.clearForConfig(configB)
        repository.recordSuccess(configA, "model-a", latencyMs = 100, nowMillis = 1000L)
        repository.recordSuccess(configB, "model-b", latencyMs = 200, nowMillis = 1000L)

        repository.clearForConfig(configA)

        assertNull(repository.getSnapshot(configA).first())
        assertEquals("model-b", repository.getSnapshot(configB).first()?.records?.single()?.modelId)
    }
}

private class InMemoryModelPerformanceStore {
    private val repository = mutableMapOf<String, MutableStateFlow<com.example.aiaccounting.data.service.ModelPerformanceSnapshot?>>()
    private val recommendationEngine = com.example.aiaccounting.data.service.ModelRecommendationEngine()

    fun createConfigIdentity(config: AIConfig): String {
        return "${config.provider.name}|${config.apiUrl.trim().trimEnd('/')}"
    }

    fun getSnapshot(config: AIConfig): Flow<com.example.aiaccounting.data.service.ModelPerformanceSnapshot?> {
        val identity = createConfigIdentity(config)
        return repository.getOrPut(identity) { MutableStateFlow(null) }
    }

    fun getRecommendation(config: AIConfig, remoteModelIds: List<String>): Flow<com.example.aiaccounting.data.service.RecommendedModelSummary?> {
        val identity = createConfigIdentity(config)
        return repository.getOrPut(identity) { MutableStateFlow(null) }.let { state ->
            MutableStateFlow(
                recommendationEngine.recommend(
                    remoteModelIds = remoteModelIds,
                    snapshot = state.value,
                    nowMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun recordSuccess(config: AIConfig, modelId: String, latencyMs: Long, nowMillis: Long) {
        update(config) { snapshot ->
            val records = snapshot.records.toMutableList()
            val index = records.indexOfFirst { it.modelId == modelId }
            val current = records.getOrNull(index) ?: com.example.aiaccounting.data.service.ModelPerformanceRecord(modelId = modelId)
            val nextSuccessCount = current.successCount + 1
            val averageLatency = current.averageLatencyMs?.let { ((it * current.successCount) + latencyMs) / nextSuccessCount } ?: latencyMs
            val updated = current.copy(
                successCount = nextSuccessCount,
                lastLatencyMs = latencyMs,
                averageLatencyMs = averageLatency,
                lastSuccessAt = nowMillis,
                lastUsedAt = nowMillis
            )
            if (index >= 0) records[index] = updated else records.add(updated)
            snapshot.copy(records = records, updatedAtMillis = nowMillis)
        }
    }

    suspend fun recordFailure(config: AIConfig, modelId: String, category: ModelPerformanceFailureCategory, nowMillis: Long) {
        update(config) { snapshot ->
            val records = snapshot.records.toMutableList()
            val index = records.indexOfFirst { it.modelId == modelId }
            val current = records.getOrNull(index) ?: com.example.aiaccounting.data.service.ModelPerformanceRecord(modelId = modelId)
            val updated = current.copy(
                failureCount = current.failureCount + 1,
                timeoutCount = current.timeoutCount + if (category == ModelPerformanceFailureCategory.TIMEOUT) 1 else 0,
                lastFailureAt = nowMillis,
                lastUsedAt = nowMillis
            )
            if (index >= 0) records[index] = updated else records.add(updated)
            snapshot.copy(records = records, updatedAtMillis = nowMillis)
        }
    }

    suspend fun clearForConfig(config: AIConfig) {
        repository[createConfigIdentity(config)] = MutableStateFlow(null)
    }

    private suspend fun update(
        config: AIConfig,
        transform: (com.example.aiaccounting.data.service.ModelPerformanceSnapshot) -> com.example.aiaccounting.data.service.ModelPerformanceSnapshot
    ) {
        val identity = createConfigIdentity(config)
        val state = repository.getOrPut(identity) {
            MutableStateFlow(
                com.example.aiaccounting.data.service.ModelPerformanceSnapshot(
                    configIdentity = identity,
                    records = emptyList(),
                    updatedAtMillis = null
                )
            )
        }
        state.value = transform(
            state.value ?: com.example.aiaccounting.data.service.ModelPerformanceSnapshot(
                configIdentity = identity,
                records = emptyList(),
                updatedAtMillis = null
            )
        )
    }
}
