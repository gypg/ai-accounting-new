package com.example.aiaccounting.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelExecutionPlannerTest {

    private val planner = ModelExecutionPlanner()

    @Test
    fun plan_returnsFixedSingleModelWithoutFallback() {
        val plan = planner.plan(
            strategy = ModelSelectionStrategy.FIXED,
            configuredModelId = "fixed-model",
            remoteModelIds = listOf("fixed-model", "other-model"),
            snapshot = null
        )

        assertEquals("fixed-model", plan.primaryModelId)
        assertEquals(emptyList<String>(), plan.fallbackModelIds)
        assertEquals(ModelRecommendationSource.MANUAL_FIXED, plan.recommendationSource)
    }

    @Test
    fun plan_returnsOrderedAutoCandidates() {
        val plan = planner.plan(
            strategy = ModelSelectionStrategy.AUTO,
            configuredModelId = "",
            remoteModelIds = listOf("model-a", "model-b"),
            snapshot = ModelPerformanceSnapshot(
                configIdentity = "id",
                records = listOf(
                    ModelPerformanceRecord(modelId = "model-b", successCount = 2, averageLatencyMs = 100),
                    ModelPerformanceRecord(modelId = "model-a", successCount = 1, averageLatencyMs = 500)
                )
            )
        )

        assertEquals("model-b", plan.primaryModelId)
        assertEquals(listOf("model-a"), plan.fallbackModelIds)
        assertEquals(ModelRecommendationSource.PERFORMANCE, plan.recommendationSource)
    }

    @Test
    fun plan_returnsNoPrimaryWhenFixedConfiguredModelBlank() {
        val plan = planner.plan(
            strategy = ModelSelectionStrategy.FIXED,
            configuredModelId = "",
            remoteModelIds = emptyList(),
            snapshot = null
        )

        assertNull(plan.primaryModelId)
        assertEquals(ModelRecommendationSource.NONE, plan.recommendationSource)
    }
}
