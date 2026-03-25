package com.example.aiaccounting.data.service

class ModelExecutionPlanner(
    private val recommendationEngine: ModelRecommendationEngine = ModelRecommendationEngine()
) {

    fun plan(
        strategy: ModelSelectionStrategy,
        configuredModelId: String,
        remoteModelIds: List<String>,
        snapshot: ModelPerformanceSnapshot?,
        exclude: Set<String> = emptySet(),
        nowMillis: Long = System.currentTimeMillis()
    ): ModelExecutionPlan {
        return when (strategy) {
            ModelSelectionStrategy.FIXED -> {
                val modelId = configuredModelId.trim()
                ModelExecutionPlan(
                    strategy = strategy,
                    primaryModelId = modelId.ifBlank { null },
                    fallbackModelIds = emptyList(),
                    recommendationSource = if (modelId.isBlank()) {
                        ModelRecommendationSource.NONE
                    } else {
                        ModelRecommendationSource.MANUAL_FIXED
                    }
                )
            }

            ModelSelectionStrategy.AUTO -> {
                val ordered = recommendationEngine.rankCandidates(
                    remoteModelIds = remoteModelIds,
                    snapshot = snapshot,
                    exclude = exclude
                )
                val recommendation = recommendationEngine.recommend(
                    remoteModelIds = remoteModelIds,
                    snapshot = snapshot,
                    nowMillis = nowMillis,
                    exclude = exclude
                )
                ModelExecutionPlan(
                    strategy = strategy,
                    primaryModelId = ordered.firstOrNull(),
                    fallbackModelIds = ordered.drop(1),
                    recommendationSource = recommendation?.source ?: ModelRecommendationSource.NONE
                )
            }
        }
    }
}
