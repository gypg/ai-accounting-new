package com.example.aiaccounting.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val icon: String = "",
    val color: String = "",
    val isAsset: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class AccountType {
    CASH,
    BANK_CARD,
    CREDIT_CARD,
    ALIPAY,
    WECHAT,
    OTHER
}
