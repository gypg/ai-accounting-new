package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记账模板实体
 */
@Entity(tableName = "transaction_templates")
data class TransactionTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,           // 模板名称
    val amount: Double,         // 金额
    val type: TransactionType,  // 收入/支出
    val categoryId: Long,       // 分类ID
    val accountId: Long,        // 账户ID
    val note: String = "",      // 备注
    val icon: String = "💰",    // 图标
    val sortOrder: Int = 0,     // 排序
    val isActive: Boolean = true // 是否启用
)
