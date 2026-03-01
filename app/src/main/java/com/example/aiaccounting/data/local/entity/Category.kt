package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Category entity for transaction categorization
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val icon: String = "📁",
    val color: String = "#2196F3",
    val isDefault: Boolean = false,
    val parentId: Long? = null,  // For sub-categories
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Transaction type enum
 */
enum class TransactionType {
    INCOME,     // 收入
    EXPENSE,    // 支出
    TRANSFER    // 转账
}