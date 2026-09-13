package com.example.aiaccounting.data.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRecommendationEngineTest {

    private val engine = ModelRecommendationEngine()

    @Test
    fun recommend_prefersSuccessfulLowLatencyModel() {
        val snapshot = ModelPerformanceSnapshot(
            configIdentity = "CUSTOM|https://example.com/v1",
            records = listOf(
                ModelPerformanceRecord(
                    modelId = "slow-model",
                    successCount = 1,
                    failureCount = 2,
                    timeoutCount = 1,
                    averageLatencyMs = 900
                ),
                ModelPerformanceRecord(
                    modelId = "fast-model",
                    successCount = 3,
                    failureCount = 0,
                    timeoutCount = 0,
                    averageLatencyMs = 120
                )
            )
        )

        val recommendation = engine.recommend(
            remoteModelIds = listOf("slow-model", "fast-model"),
            snapshot = snapshot,
            nowMillis = 123L
        )

        assertEquals("fast-model", recommendation?.modelId)
        assertEquals(ModelRecommendationSource.PERFORMANCE, recommendation?.source)
    }

    @Test
    fun recommend_fallsBackToRemoteOrderWhenNoStatsExist() {
        val recommendation = engine.recommend(
            remoteModelIds = listOf("model-a", "model-b"),
            snapshot = null,
            nowMillis = 123L
        )

        assertEquals("model-a", recommendation?.modelId)
        assertEquals(ModelRecommendationSource.REMOTE_ORDER, recommendation?.source)
    }

    @Test
    fun rankCandidates_filtersExcludedAndMissingModels() {
        val snapshot = ModelPerformanceSnapshot(
            configIdentity = "id",
            records = listOf(
                ModelPerformanceRecord(modelId = "old-model", successCount = 10),
                ModelPerformanceRecord(modelId = "model-b", successCount = 2)
            )
        )

        val ranked = engine.rankCandidates(
            remoteModelIds = listOf("model-a", "model-b"),
            snapshot = snapshot,
            exclude = setOf("model-b")
        )

        assertEquals(listOf("model-a"), ranked)
    }
}
