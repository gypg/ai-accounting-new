package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

/**
 * Budget entity for spending limits
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["period"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val period: BudgetPeriod,  // Monthly, weekly, yearly
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val alertThreshold: Double = 0.8,  // Alert when 80% used
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Budget period enum
 */
enum class BudgetPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}
