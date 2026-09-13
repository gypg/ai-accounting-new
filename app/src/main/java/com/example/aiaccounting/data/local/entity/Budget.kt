package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 预算周期枚举
 */
enum class BudgetPeriod {
    MONTHLY,
    YEARLY
}

/**
 * 预算实体
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // 预算名称
    val amount: Double, // 预算金额
    val categoryId: Long? = null, // 关联的分类ID（null表示总预算）
    val period: BudgetPeriod = BudgetPeriod.MONTHLY, // 预算周期
    val year: Int, // 预算年份
    val month: Int? = null, // 预算月份（年度预算时为null）
    val alertThreshold: Double = 0.8, // 超支提醒阈值（80%）
    val isActive: Boolean = true, // 是否启用
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 预算进度数据类（用于UI展示）
 */
data class BudgetProgress(
    val budget: Budget,
    val spent: Double, // 已支出金额
    val remaining: Double, // 剩余金额
    val percentage: Float, // 使用百分比
    val isOverBudget: Boolean, // 是否超支
    val shouldAlert: Boolean // 是否应提醒
) {
    companion object {
        fun calculate(budget: Budget, spent: Double): BudgetProgress {
            val remaining = budget.amount - spent
            val percentage = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
            val isOverBudget = spent > budget.amount
            val shouldAlert = percentage >= budget.alertThreshold || isOverBudget
            
            return BudgetProgress(
                budget = budget,
                spent = spent,
                remaining = remaining,
                percentage = percentage.coerceIn(0f, 1f),
                isOverBudget = isOverBudget,
                shouldAlert = shouldAlert
            )
        }
    }
}
