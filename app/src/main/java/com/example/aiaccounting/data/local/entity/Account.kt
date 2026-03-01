package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Account entity representing a user's account (bank account, cash, credit card, etc.)
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val icon: String = "💳",
    val color: String = "#2196F3",
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Account type enum
 */
enum class AccountType {
    CASH,           // 现金
    BANK,           // 银行账户
    CREDIT_CARD,    // 信用卡
    DEBIT_CARD,     // 借记卡
    ALIPAY,         // 支付宝
    WECHAT,         // 微信
    OTHER           // 其他
}
