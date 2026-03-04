package com.example.aiaccounting.widget

import android.content.Context
import android.content.SharedPreferences
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.BudgetRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 小组件更新服务
 * 在应用内更新小组件显示的统计数据
 */
@Singleton
class WidgetUpdateService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) {

    /**
     * 更新小组件统计数据
     */
    suspend fun updateWidgetStats(context: Context) {
        try {
            // 获取本月日期范围
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis

            // 查询本月交易
            val transactions = transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth).first()

            var totalIncome = 0.0
            var totalExpense = 0.0

            transactions.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.INCOME -> totalIncome += transaction.amount
                    TransactionType.EXPENSE -> totalExpense += transaction.amount
                    else -> {}
                }
            }

            // 获取本月预算
            val budgetProgress = budgetRepository.getTotalBudgetProgress(year, month).first()
            val budgetLimit = budgetProgress?.budget?.amount ?: 0.0

            // 保存到SharedPreferences
            val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putFloat("month_income", totalIncome.toFloat())
                putFloat("month_expense", totalExpense.toFloat())
                putFloat("month_budget", budgetLimit.toFloat())
                apply()
            }

            // 通知所有小组件更新
            WidgetProvider1x1.updateAllWidgets(context)
            WidgetProvider2x1.updateAllWidgets(context)
            WidgetProvider3x1.updateAllWidgets(context)
            WidgetProvider3x2.updateAllWidgets(context)
            WidgetProvider4x3.updateAllWidgets(context)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * 清除小组件数据
         */
        fun clearWidgetStats(context: Context) {
            val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            WidgetProvider1x1.updateAllWidgets(context)
            WidgetProvider2x1.updateAllWidgets(context)
            WidgetProvider3x1.updateAllWidgets(context)
            WidgetProvider3x2.updateAllWidgets(context)
            WidgetProvider4x3.updateAllWidgets(context)
        }
    }
}
