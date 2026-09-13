package com.example.aiaccounting.data.service

class ModelRecommendationEngine {

    fun rankCandidates(
        remoteModelIds: List<String>,
        snapshot: ModelPerformanceSnapshot?,
        exclude: Set<String> = emptySet()
    ): List<String> {
        val remoteCandidates = remoteModelIds
            .map { it.trim() }
            .filter { it.isNotBlank() && it !in exclude }

        if (remoteCandidates.isEmpty()) return emptyList()
        val recordMap = snapshot?.records?.associateBy { it.modelId }.orEmpty()

        return remoteCandidates.sortedWith(
            compareByDescending<String> { recordMap[it]?.successCount ?: 0 }
                .thenBy { (recordMap[it]?.timeoutCount ?: 0) + (recordMap[it]?.failureCount ?: 0) }
                .thenBy { recordMap[it]?.averageLatencyMs ?: Long.MAX_VALUE }
                .thenBy { remoteCandidates.indexOf(it) }
        )
    }

    fun recommend(
        remoteModelIds: List<String>,
        snapshot: ModelPerformanceSnapshot?,
        nowMillis: Long,
        exclude: Set<String> = emptySet()
    ): RecommendedModelSummary? {
        val ranked = rankCandidates(
            remoteModelIds = remoteModelIds,
            snapshot = snapshot,
            exclude = exclude
        )
        val recommendedId = ranked.firstOrNull() ?: return null
        val record = snapshot?.records?.firstOrNull { it.modelId == recommendedId }
        val source = if (record == null) {
            ModelRecommendationSource.REMOTE_ORDER
        } else {
            ModelRecommendationSource.PERFORMANCE
        }
        val reason = if (source == ModelRecommendationSource.PERFORMANCE) {
            "根据最近成功率与延迟推荐"
        } else {
            "根据当前模型列表顺序推荐"
        }
        return RecommendedModelSummary(
            modelId = recommendedId,
            reason = reason,
            source = source,
            latencyMs = record?.averageLatencyMs ?: record?.lastLatencyMs,
            updatedAtMillis = nowMillis
        )
    }
}
