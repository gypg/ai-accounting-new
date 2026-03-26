package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 操作留痕实体
 * 用于审计 AI 创建 / 修改 / 删除 / 自动补建等动作
 */
@Entity(
    tableName = "ai_operation_traces",
    indices = [
        Index(value = ["traceId"]),
        Index(value = ["entityType", "entityId"]),
        Index(value = ["sourceType"]),
        Index(value = ["actionType"]),
        Index(value = ["timestamp"])
    ]
)
data class AIOperationTrace(
    @PrimaryKey
    val id: String,
    val traceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceType: String,
    val actionType: String,
    val entityType: String,
    val entityId: String? = null,
    val relatedTransactionId: Long? = null,
    val summary: String = "",
    val details: String? = null,
    val success: Boolean = true,
    val errorMessage: String? = null
)
