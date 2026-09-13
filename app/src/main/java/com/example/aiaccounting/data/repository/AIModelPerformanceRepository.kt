package com.example.aiaccounting.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.service.ModelPerformanceFailureCategory
import com.example.aiaccounting.data.service.ModelPerformanceRecord
import com.example.aiaccounting.data.service.ModelPerformanceSnapshot
import com.example.aiaccounting.data.service.ModelRecommendationEngine
import com.example.aiaccounting.data.service.RecommendedModelSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiModelPerformanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_model_performance")

@Singleton
class AIModelPerformanceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.aiModelPerformanceDataStore
    private val recommendationEngine = ModelRecommendationEngine()
    private val snapshotPrefix = "model_performance_snapshot::"

    fun createConfigIdentity(config: AIConfig): String {
        val provider = config.provider.name
        val apiUrl = config.apiUrl.trim().trimEnd('/')
        return "$provider|$apiUrl"
    }

    fun getSnapshot(config: AIConfig): Flow<ModelPerformanceSnapshot?> {
        val identity = createConfigIdentity(config)
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(snapshotKey(identity))]?.let { decodeSnapshot(identity, it) }
        }
    }

    fun getRecommendation(config: AIConfig, remoteModelIds: List<String>): Flow<RecommendedModelSummary?> {
        val identity = createConfigIdentity(config)
        return dataStore.data.map { preferences ->
            val snapshot = preferences[stringPreferencesKey(snapshotKey(identity))]?.let { decodeSnapshot(identity, it) }
            recommendationEngine.recommend(
                remoteModelIds = remoteModelIds,
                snapshot = snapshot,
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    suspend fun recordSuccess(
        config: AIConfig,
        modelId: String,
        latencyMs: Long,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        updateRecord(config, modelId, nowMillis) { current ->
            val nextSuccessCount = current.successCount + 1
            val nextAverage = when (val previousAverage = current.averageLatencyMs) {
                null -> latencyMs
                else -> ((previousAverage * current.successCount) + latencyMs) / nextSuccessCount
            }
            current.copy(
                successCount = nextSuccessCount,
                lastLatencyMs = latencyMs,
                averageLatencyMs = nextAverage,
                lastSuccessAt = nowMillis,
                lastUsedAt = nowMillis
            )
        }
    }

    suspend fun recordFailure(
        config: AIConfig,
        modelId: String,
        category: ModelPerformanceFailureCategory,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        updateRecord(config, modelId, nowMillis) { current ->
            current.copy(
                failureCount = current.failureCount + 1,
                timeoutCount = current.timeoutCount + if (category == ModelPerformanceFailureCategory.TIMEOUT) 1 else 0,
                lastFailureAt = nowMillis,
                lastUsedAt = nowMillis
            )
        }
    }

    suspend fun clearForConfig(config: AIConfig) {
        val identity = createConfigIdentity(config)
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(snapshotKey(identity)))
        }
    }

    private suspend fun updateRecord(
        config: AIConfig,
        modelId: String,
        nowMillis: Long,
        transform: (ModelPerformanceRecord) -> ModelPerformanceRecord
    ) {
        val trimmedModelId = modelId.trim()
        if (trimmedModelId.isBlank()) return
        val identity = createConfigIdentity(config)
        dataStore.edit { preferences ->
            val existingSnapshot = preferences[stringPreferencesKey(snapshotKey(identity))]
                ?.let { decodeSnapshot(identity, it) }
                ?: ModelPerformanceSnapshot(identity, emptyList(), null)
            val updatedRecords = existingSnapshot.records.toMutableList()
            val index = updatedRecords.indexOfFirst { it.modelId == trimmedModelId }
            val currentRecord = if (index >= 0) {
                updatedRecords[index]
            } else {
                ModelPerformanceRecord(modelId = trimmedModelId)
            }
            val nextRecord = transform(currentRecord)
            if (index >= 0) {
                updatedRecords[index] = nextRecord
            } else {
                updatedRecords.add(nextRecord)
            }
            val nextSnapshot = existingSnapshot.copy(
                records = updatedRecords.toList(),
                updatedAtMillis = nowMillis
            )
            preferences[stringPreferencesKey(snapshotKey(identity))] = encodeSnapshot(nextSnapshot)
        }
    }

    private fun snapshotKey(identity: String): String = "$snapshotPrefix$identity"

    private fun encodeSnapshot(snapshot: ModelPerformanceSnapshot): String {
        return JSONObject().apply {
            put("updatedAtMillis", snapshot.updatedAtMillis)
            put(
                "records",
                JSONArray().apply {
                    snapshot.records.forEach { record ->
                        put(JSONObject().apply {
                            put("modelId", record.modelId)
                            put("successCount", record.successCount)
                            put("failureCount", record.failureCount)
                            put("timeoutCount", record.timeoutCount)
                            put("lastLatencyMs", record.lastLatencyMs)
                            put("averageLatencyMs", record.averageLatencyMs)
                            put("lastSuccessAt", record.lastSuccessAt)
                            put("lastFailureAt", record.lastFailureAt)
                            put("lastUsedAt", record.lastUsedAt)
                        })
                    }
                }
            )
        }.toString()
    }

    private fun decodeSnapshot(identity: String, raw: String): ModelPerformanceSnapshot {
        val json = JSONObject(raw)
        val recordsArray = json.optJSONArray("records") ?: JSONArray()
        val records = buildList {
            for (index in 0 until recordsArray.length()) {
                val item = recordsArray.optJSONObject(index) ?: continue
                val modelId = item.optString("modelId").trim()
                if (modelId.isBlank()) continue
                add(
                    ModelPerformanceRecord(
                        modelId = modelId,
                        successCount = item.optInt("successCount"),
                        failureCount = item.optInt("failureCount"),
                        timeoutCount = item.optInt("timeoutCount"),
                        lastLatencyMs = item.optLongOrNull("lastLatencyMs"),
                        averageLatencyMs = item.optLongOrNull("averageLatencyMs"),
                        lastSuccessAt = item.optLongOrNull("lastSuccessAt"),
                        lastFailureAt = item.optLongOrNull("lastFailureAt"),
                        lastUsedAt = item.optLongOrNull("lastUsedAt")
                    )
                )
            }
        }
        return ModelPerformanceSnapshot(
            configIdentity = identity,
            records = records,
            updatedAtMillis = json.optLongOrNull("updatedAtMillis")
        )
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    return if (has(key) && !isNull(key)) optLong(key) else null
}
