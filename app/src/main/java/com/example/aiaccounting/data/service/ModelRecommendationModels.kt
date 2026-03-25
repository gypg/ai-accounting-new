package com.example.aiaccounting.data.service

enum class ModelSelectionStrategy {
    AUTO,
    FIXED
}

enum class ModelRecommendationSource {
    PERFORMANCE,
    REMOTE_ORDER,
    MANUAL_FIXED,
    NONE
}

enum class ModelPerformanceFailureCategory {
    TIMEOUT,
    MODEL_UNAVAILABLE,
    OTHER
}

data class ModelPerformanceRecord(
    val modelId: String,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val timeoutCount: Int = 0,
    val lastLatencyMs: Long? = null,
    val averageLatencyMs: Long? = null,
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
    val lastUsedAt: Long? = null
)

data class ModelPerformanceSnapshot(
    val configIdentity: String,
    val records: List<ModelPerformanceRecord>,
    val updatedAtMillis: Long? = null
)

data class RecommendedModelSummary(
    val modelId: String,
    val reason: String,
    val source: ModelRecommendationSource,
    val latencyMs: Long?,
    val updatedAtMillis: Long
)

data class ModelExecutionPlan(
    val strategy: ModelSelectionStrategy,
    val primaryModelId: String?,
    val fallbackModelIds: List<String>,
    val recommendationSource: ModelRecommendationSource
)
