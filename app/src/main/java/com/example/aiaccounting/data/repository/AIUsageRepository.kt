package com.example.aiaccounting.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.aiaccounting.utils.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiUsageDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_usage")

/**
 * AI使用统计仓库
 */
@Singleton
class AIUsageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.aiUsageDataStore

    companion object {
        private val TOTAL_CALLS = longPreferencesKey("total_calls")
        private val TOTAL_TOKENS = longPreferencesKey("total_tokens")
        private val TOTAL_COST = doublePreferencesKey("total_cost")
        private val LAST_CALL_TIME = longPreferencesKey("last_call_time")
        private val SUCCESS_CALLS = longPreferencesKey("success_calls")
        private val FAILED_CALLS = longPreferencesKey("failed_calls")
        private val FIRST_USE_TIME = longPreferencesKey("first_use_time")
    }

    /**
     * 获取使用统计流
     */
    fun getUsageStats(): Flow<AIUsageStats> {
        return dataStore.data.map { preferences ->
            AIUsageStats(
                totalCalls = preferences[TOTAL_CALLS] ?: 0L,
                totalTokens = preferences[TOTAL_TOKENS] ?: 0L,
                totalCost = preferences[TOTAL_COST] ?: 0.0,
                lastCallTime = preferences[LAST_CALL_TIME] ?: 0L,
                successCalls = preferences[SUCCESS_CALLS] ?: 0L,
                failedCalls = preferences[FAILED_CALLS] ?: 0L,
                firstUseTime = preferences[FIRST_USE_TIME] ?: 0L
            )
        }
    }

    /**
     * 记录一次API调用
     */
    suspend fun recordCall(
        tokens: Int = 0,
        cost: Double = 0.0,
        success: Boolean = true
    ) {
        dataStore.edit { preferences ->
            // 记录首次使用时间
            if (preferences[FIRST_USE_TIME] == null) {
                preferences[FIRST_USE_TIME] = System.currentTimeMillis()
            }
            
            // 更新总调用次数
            preferences[TOTAL_CALLS] = (preferences[TOTAL_CALLS] ?: 0L) + 1
            
            // 更新Token数
            preferences[TOTAL_TOKENS] = (preferences[TOTAL_TOKENS] ?: 0L) + tokens
            
            // 更新费用
            preferences[TOTAL_COST] = (preferences[TOTAL_COST] ?: 0.0) + cost
            
            // 更新时间
            preferences[LAST_CALL_TIME] = System.currentTimeMillis()
            
            // 更新成功/失败次数
            if (success) {
                preferences[SUCCESS_CALLS] = (preferences[SUCCESS_CALLS] ?: 0L) + 1
            } else {
                preferences[FAILED_CALLS] = (preferences[FAILED_CALLS] ?: 0L) + 1
            }
        }
    }

    /**
     * 重置统计
     */
    suspend fun resetStats() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * 计算预估费用（基于OpenAI价格）
     */
    fun calculateEstimatedCost(tokens: Int, model: String): Double {
        return when {
            model.contains("gpt-4o-mini") -> tokens * 0.00015 / 1000  // $0.15 per 1M tokens
            model.contains("gpt-4o") -> tokens * 0.005 / 1000  // $5 per 1M tokens
            model.contains("gpt-4") -> tokens * 0.03 / 1000  // $30 per 1M tokens
            model.contains("gpt-3.5") -> tokens * 0.0005 / 1000  // $0.50 per 1M tokens
            model.contains("claude") -> tokens * 0.003 / 1000  // $3 per 1M tokens (approximate)
            model.contains("gemini") -> tokens * 0.0005 / 1000  // $0.50 per 1M tokens (approximate)
            else -> tokens * 0.002 / 1000  // default
        }
    }
}

/**
 * AI使用统计数据类
 */
data class AIUsageStats(
    val totalCalls: Long = 0L,
    val totalTokens: Long = 0L,
    val totalCost: Double = 0.0,
    val lastCallTime: Long = 0L,
    val successCalls: Long = 0L,
    val failedCalls: Long = 0L,
    val firstUseTime: Long = 0L
) {
    /**
     * 成功率
     */
    val successRate: Float
        get() = if (totalCalls > 0) {
            (successCalls.toFloat() / totalCalls.toFloat()) * 100
        } else 0f

    /**
     * 格式化费用显示
     */
    fun formattedCost(): String {
        return String.format("$%.4f", totalCost)
    }

    /**
     * 格式化人民币费用（假设汇率7.2）
     */
    fun formattedCostCNY(): String {
        return String.format("¥%.4f", totalCost * 7.2)
    }

    /**
     * 获取最后调用时间的格式化字符串
     */
    fun lastCallTimeFormatted(): String {
        return if (lastCallTime > 0) {
            DateUtils.formatDateTime(lastCallTime)
        } else "从未使用"
    }

    /**
     * 获取首次使用时间的格式化字符串
     */
    fun firstUseTimeFormatted(): String {
        return if (firstUseTime > 0) {
            DateUtils.formatDateTime(firstUseTime)
        } else "从未使用"
    }
}
