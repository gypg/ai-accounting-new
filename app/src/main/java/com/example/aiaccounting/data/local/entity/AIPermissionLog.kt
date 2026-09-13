package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI权限操作日志实体
 * 记录所有AI权限判断和操作执行
 */
@Entity(tableName = "ai_permission_logs")
data class AIPermissionLog(
    @PrimaryKey
    val id: String,
    
    val timestamp: Long = System.currentTimeMillis(),
    
    // 操作信息
    val operationType: String,
    val permissionLevel: String,
    
    // AI判断信息
    val riskScore: Float = 0f,
    val confidence: Float = 0f,
    val granted: Boolean = false,
    val reason: String = "",
    
    // 人工干预
    val requiresHumanIntervention: Boolean = false,
    val interventionReason: String? = null,
    val humanApproved: Boolean? = null,
    val humanApprovalReason: String? = null,
    val humanApprovalTime: Long? = null,
    
    // 执行状态
    val executed: Boolean = false,
    val executionSuccess: Boolean? = null,
    val executionResult: String? = null,
    
    // 上下文信息
    val userId: String = "default_user",
    val deviceTrusted: Boolean = true,
    val networkSecure: Boolean = true,
    val operationDetails: String? = null
)
