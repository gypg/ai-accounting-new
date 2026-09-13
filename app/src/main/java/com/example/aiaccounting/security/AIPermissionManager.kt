package com.example.aiaccounting.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.aiaccounting.data.local.entity.AIPermissionLog
import com.example.aiaccounting.data.repository.AIPermissionLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI权限管理器
 * 统一管理AI模块的所有权限，实现智能判断、自动执行、审计日志
 */
@Singleton
class AIPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager,
    private val permissionLogRepository: AIPermissionLogRepository
) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ai_permission_prefs",
        Context.MODE_PRIVATE
    )

    /**
     * 权限等级
     */
    enum class PermissionLevel {
        CRITICAL,    // 关键操作：删除账户、清空数据等
        HIGH,        // 高风险：修改交易、删除交易、大额转账
        MEDIUM,      // 中等风险：创建交易、修改分类
        LOW,         // 低风险：查询数据、查看统计
        MINIMAL      // 最小风险：普通对话、身份确认
    }

    /**
     * 操作类型
     */
    enum class OperationType {
        // 交易操作
        CREATE_TRANSACTION,
        UPDATE_TRANSACTION,
        DELETE_TRANSACTION,
        BATCH_CREATE_TRANSACTION,
        
        // 账户操作
        CREATE_ACCOUNT,
        UPDATE_ACCOUNT,
        DELETE_ACCOUNT,
        TRANSFER_BETWEEN_ACCOUNTS,
        
        // 分类操作
        CREATE_CATEGORY,
        UPDATE_CATEGORY,
        DELETE_CATEGORY,
        
        // 预算操作
        SET_BUDGET,
        UPDATE_BUDGET,
        DELETE_BUDGET,
        
        // 数据操作
        EXPORT_DATA,
        IMPORT_DATA,
        CLEAR_ALL_DATA,
        
        // 查询操作
        QUERY_TRANSACTIONS,
        QUERY_ACCOUNTS,
        QUERY_STATISTICS,
        
        // 系统操作
        CHANGE_SETTINGS,
        SWITCH_BUTLER,
        UPDATE_AI_CONFIG
    }

    /**
     * 权限判断上下文
     */
    data class PermissionContext(
        val userId: String = "default_user",
        val userTrustScore: Float = 0.8f,  // 用户信任度分数 (0-1)
        val operationHistory: List<OperationRecord> = emptyList(),
        val currentSessionOperations: Int = 0,
        val lastOperationTime: Long = 0,
        val deviceTrusted: Boolean = true,
        val networkSecure: Boolean = true
    )

    /**
     * 操作记录
     */
    data class OperationRecord(
        val operationType: OperationType,
        val timestamp: Long,
        val success: Boolean,
        val riskScore: Float
    )

    /**
     * 权限判断结果
     */
    data class PermissionResult(
        val granted: Boolean,
        val level: PermissionLevel,
        val riskScore: Float,  // 风险分数 (0-1)
        val confidence: Float,  // AI判断置信度 (0-1)
        val reason: String,
        val requiresAudit: Boolean,
        val requiresHumanIntervention: Boolean = false,
        val interventionReason: String? = null
    )

    /**
     * 判断操作权限
     * AI智能分析，自动决策是否允许执行
     */
    suspend fun checkPermission(
        operationType: OperationType,
        context: PermissionContext? = null,
        operationDetails: Map<String, Any> = emptyMap()
    ): PermissionResult {
        // 使用提供的上下文或获取默认上下文
        val permissionContext = context ?: getDefaultContext()
        
        // 1. 获取操作的风险等级
        val permissionLevel = getOperationPermissionLevel(operationType)
        
        // 2. 计算风险分数
        val riskScore = calculateRiskScore(operationType, permissionContext, operationDetails)
        
        // 3. 基于AI策略判断权限
        val result = evaluatePermission(permissionLevel, riskScore, permissionContext, operationDetails)
        
        // 4. 记录权限判断日志
        if (result.requiresAudit) {
            logPermissionCheck(operationType, permissionContext, result, operationDetails)
        }
        
        return result
    }

    /**
     * 执行带权限检查的操作
     */
    suspend fun <T> executeWithPermission(
        operationType: OperationType,
        operationDetails: Map<String, Any> = emptyMap(),
        operation: suspend () -> T
    ): T? {
        val context = getDefaultContext()
        val permissionResult = checkPermission(operationType, context, operationDetails)
        
        return if (permissionResult.granted && !permissionResult.requiresHumanIntervention) {
            try {
                val result = operation()
                
                // 记录成功执行
                logOperationExecution(
                    operationType = operationType,
                    context = context,
                    permissionResult = permissionResult,
                    operationDetails = operationDetails,
                    success = true,
                    result = "执行成功"
                )
                
                result
            } catch (e: Exception) {
                // 记录执行失败
                logOperationExecution(
                    operationType = operationType,
                    context = context,
                    permissionResult = permissionResult,
                    operationDetails = operationDetails,
                    success = false,
                    result = "执行失败: ${e.message}"
                )
                null
            }
        } else {
            // 记录权限拒绝
            logOperationExecution(
                operationType = operationType,
                context = context,
                permissionResult = permissionResult,
                operationDetails = operationDetails,
                success = false,
                result = if (permissionResult.requiresHumanIntervention) {
                    "需要人工干预: ${permissionResult.interventionReason}"
                } else {
                    "权限被拒绝: ${permissionResult.reason}"
                }
            )
            null
        }
    }

    /**
     * 获取操作的权限等级
     */
    private fun getOperationPermissionLevel(operationType: OperationType): PermissionLevel {
        return when (operationType) {
            // 关键操作
            OperationType.CLEAR_ALL_DATA,
            OperationType.DELETE_ACCOUNT -> PermissionLevel.CRITICAL
            
            // 高风险操作
            OperationType.DELETE_TRANSACTION,
            OperationType.UPDATE_TRANSACTION,
            OperationType.TRANSFER_BETWEEN_ACCOUNTS,
            OperationType.IMPORT_DATA -> PermissionLevel.HIGH
            
            // 中等风险操作
            OperationType.CREATE_TRANSACTION,
            OperationType.CREATE_ACCOUNT,
            OperationType.UPDATE_ACCOUNT,
            OperationType.CREATE_CATEGORY,
            OperationType.SET_BUDGET -> PermissionLevel.MEDIUM
            
            // 低风险操作
            OperationType.QUERY_TRANSACTIONS,
            OperationType.QUERY_ACCOUNTS,
            OperationType.QUERY_STATISTICS,
            OperationType.EXPORT_DATA -> PermissionLevel.LOW
            
            // 最小风险
            else -> PermissionLevel.MINIMAL
        }
    }

    /**
     * 计算风险分数
     */
    private fun calculateRiskScore(
        operationType: OperationType,
        context: PermissionContext,
        operationDetails: Map<String, Any>
    ): Float {
        var riskScore = 0.0f
        
        // 1. 基于操作类型的基础风险
        riskScore += when (getOperationPermissionLevel(operationType)) {
            PermissionLevel.CRITICAL -> 0.9f
            PermissionLevel.HIGH -> 0.7f
            PermissionLevel.MEDIUM -> 0.5f
            PermissionLevel.LOW -> 0.2f
            PermissionLevel.MINIMAL -> 0.05f
        }
        
        // 2. 用户信任度影响
        riskScore *= (1.2f - context.userTrustScore)  // 信任度越低，风险越高
        
        // 3. 操作频率检查
        val recentOperations = context.operationHistory
            .filter { System.currentTimeMillis() - it.timestamp < 60000 }  // 1分钟内
        if (recentOperations.size > 10) {
            riskScore += 0.2f  // 操作过于频繁
        }
        
        // 4. 金额风险（如果是交易操作）
        val amount = operationDetails["amount"] as? Double
        if (amount != null) {
            when {
                amount > 10000 -> riskScore += 0.3f
                amount > 5000 -> riskScore += 0.2f
                amount > 1000 -> riskScore += 0.1f
            }
        }
        
        // 5. 设备信任度
        if (!context.deviceTrusted) {
            riskScore += 0.2f
        }
        
        // 6. 网络安全
        if (!context.networkSecure) {
            riskScore += 0.15f
        }
        
        return riskScore.coerceIn(0.0f, 1.0f)
    }

    /**
     * 评估权限
     * 
     * AI权限策略：
     * 1. 关键操作（删除账户、清空数据）：需要人工确认
     * 2. 其他所有操作：AI自动授权，无需用户干预
     * 3. 所有操作都会记录审计日志
     */
    private fun evaluatePermission(
        level: PermissionLevel,
        riskScore: Float,
        context: PermissionContext,
        operationDetails: Map<String, Any>
    ): PermissionResult {
        
        val aiConfidence = calculateAIConfidence(context, riskScore)
        
        return when {
            // 关键操作：需要人工干预（删除账户、清空数据等）
            level == PermissionLevel.CRITICAL -> {
                PermissionResult(
                    granted = false,
                    level = level,
                    riskScore = riskScore,
                    confidence = aiConfidence,
                    reason = "关键操作需要人工确认",
                    requiresAudit = true,
                    requiresHumanIntervention = true,
                    interventionReason = "涉及关键数据安全，需要用户确认"
                )
            }
            
            // 其他所有操作：AI自动授权
            // 包括：创建交易、修改交易、删除交易、创建账户、创建分类等
            else -> {
                PermissionResult(
                    granted = true,
                    level = level,
                    riskScore = riskScore,
                    confidence = aiConfidence,
                    reason = "AI自动授权执行",
                    requiresAudit = true,  // 所有操作都记录审计日志
                    requiresHumanIntervention = false  // 无需人工干预
                )
            }
        }
    }

    /**
     * 计算AI判断置信度
     */
    private fun calculateAIConfidence(context: PermissionContext, riskScore: Float): Float {
        var confidence = 0.8f
        
        // 基于历史数据调整
        val successRate = if (context.operationHistory.isNotEmpty()) {
            context.operationHistory.count { it.success }.toFloat() / context.operationHistory.size
        } else 1.0f
        
        confidence *= successRate
        
        // 风险分数影响置信度
        confidence *= (1.0f - riskScore * 0.5f)
        
        return confidence.coerceIn(0.0f, 1.0f)
    }

    /**
     * 记录权限检查日志
     */
    private suspend fun logPermissionCheck(
        operationType: OperationType,
        context: PermissionContext,
        result: PermissionResult,
        operationDetails: Map<String, Any>
    ) {
        val log = AIPermissionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            operationType = operationType.name,
            permissionLevel = result.level.name,
            riskScore = result.riskScore,
            confidence = result.confidence,
            granted = result.granted,
            reason = result.reason,
            requiresHumanIntervention = result.requiresHumanIntervention,
            interventionReason = result.interventionReason,
            userId = context.userId,
            deviceTrusted = context.deviceTrusted,
            networkSecure = context.networkSecure,
            operationDetails = operationDetails.toString()
        )
        
        permissionLogRepository.insertLog(log)
    }

    /**
     * 记录操作执行日志
     */
    private suspend fun logOperationExecution(
        operationType: OperationType,
        context: PermissionContext,
        permissionResult: PermissionResult,
        operationDetails: Map<String, Any>,
        success: Boolean,
        result: String
    ) {
        val log = AIPermissionLog(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            operationType = operationType.name,
            permissionLevel = permissionResult.level.name,
            riskScore = permissionResult.riskScore,
            confidence = permissionResult.confidence,
            granted = permissionResult.granted,
            reason = result,
            requiresHumanIntervention = permissionResult.requiresHumanIntervention,
            interventionReason = permissionResult.interventionReason,
            userId = context.userId,
            deviceTrusted = context.deviceTrusted,
            networkSecure = context.networkSecure,
            operationDetails = operationDetails.toString(),
            executed = true,
            executionSuccess = success
        )
        
        permissionLogRepository.insertLog(log)
    }

    /**
     * 获取默认权限上下文
     */
    private suspend fun getDefaultContext(): PermissionContext {
        return PermissionContext(
            userId = prefs.getString("current_user_id", "default_user") ?: "default_user",
            userTrustScore = prefs.getFloat("user_trust_score", 0.8f),
            deviceTrusted = securityManager.isDeviceSecure(),
            networkSecure = true  // 可由外部传入
        )
    }

    /**
     * 更新用户信任度
     */
    fun updateUserTrustScore(score: Float) {
        prefs.edit {
            putFloat("user_trust_score", score.coerceIn(0.0f, 1.0f))
        }
    }

    /**
     * 获取审计日志
     */
    fun getAuditLogs(limit: Int = 100): Flow<List<AIPermissionLog>> {
        return permissionLogRepository.getRecentLogs(limit)
    }

    /**
     * 获取需要人工干预的操作
     */
    fun getPendingHumanInterventions(): Flow<List<AIPermissionLog>> {
        return permissionLogRepository.getPendingInterventions()
    }

    /**
     * 人工确认操作
     */
    suspend fun humanConfirmOperation(logId: String, approved: Boolean, reason: String) {
        permissionLogRepository.updateHumanIntervention(logId, approved, reason)
    }

    /**
     * 检查设备安全状态
     */
    private fun SecurityManager.isDeviceSecure(): Boolean {
        return !isLocked()
    }
}
