package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

/**
 * Transaction entity representing a financial transaction
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["date"]),
        Index(value = ["type"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val categoryId: Long,
    val type: TransactionType,
    val amount: Double,
    val date: Long,  // Unix timestamp
    val note: String = "",
    val tags: String = "",  // Comma-separated tags
    val transferAccountId: Long? = null,  // For transfers
    val attachmentPath: String? = null,  // Receipt image path
    val isRecurring: Boolean = false,
    val recurringInterval: String? = null,  // "daily", "weekly", "monthly", "yearly"
    val recurringEndDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
