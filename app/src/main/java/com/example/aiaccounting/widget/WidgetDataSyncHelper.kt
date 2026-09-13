package com.example.aiaccounting.widget

import android.content.Context
import android.content.SharedPreferences
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import java.util.Calendar

/**
 * 小组件数据同步辅助类
 * 简化版 - 直接在交易操作时更新小组件数据
 */
object WidgetDataSyncHelper {

    private const val PREFS_NAME = "widget_stats"
    private const val KEY_INCOME = "month_income"
    private const val KEY_EXPENSE = "month_expense"

    /**
     * 在插入交易时更新小组件数据
     */
    fun onTransactionInserted(context: Context, transaction: Transaction) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            when (transaction.type) {
                TransactionType.INCOME -> {
                    val currentIncome = prefs.getFloat(KEY_INCOME, 0f)
                    editor.putFloat(KEY_INCOME, currentIncome + transaction.amount.toFloat())
                }
                TransactionType.EXPENSE -> {
                    val currentExpense = prefs.getFloat(KEY_EXPENSE, 0f)
                    editor.putFloat(KEY_EXPENSE, currentExpense + transaction.amount.toFloat())
                }
                else -> {}
            }
            editor.apply()

            // 触发小组件更新
            updateWidgets(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 在删除交易时更新小组件数据
     */
    fun onTransactionDeleted(context: Context, transaction: Transaction) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            when (transaction.type) {
                TransactionType.INCOME -> {
                    val currentIncome = prefs.getFloat(KEY_INCOME, 0f)
                    editor.putFloat(KEY_INCOME, (currentIncome - transaction.amount.toFloat()).coerceAtLeast(0f))
                }
                TransactionType.EXPENSE -> {
                    val currentExpense = prefs.getFloat(KEY_EXPENSE, 0f)
                    editor.putFloat(KEY_EXPENSE, (currentExpense - transaction.amount.toFloat()).coerceAtLeast(0f))
                }
                else -> {}
            }
            editor.apply()

            // 触发小组件更新
            updateWidgets(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 完全重新计算并更新小组件数据
     */
    fun recalculateAndUpdate(context: Context, transactions: List<Transaction>) {
        try {
            var totalIncome = 0f
            var totalExpense = 0f

            // 获取本月时间范围
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            transactions.forEach { transaction ->
                val transactionCalendar = Calendar.getInstance().apply {
                    timeInMillis = transaction.date
                }
                val transactionMonth = transactionCalendar.get(Calendar.MONTH)
                val transactionYear = transactionCalendar.get(Calendar.YEAR)

                // 只统计本月数据
                if (transactionMonth == currentMonth && transactionYear == currentYear) {
                    when (transaction.type) {
                        TransactionType.INCOME -> totalIncome += transaction.amount.toFloat()
                        TransactionType.EXPENSE -> totalExpense += transaction.amount.toFloat()
                        else -> {}
                    }
                }
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putFloat(KEY_INCOME, totalIncome)
                putFloat(KEY_EXPENSE, totalExpense)
                apply()
            }

            // 触发小组件更新
            updateWidgets(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 触发所有小组件更新
     */
    private fun updateWidgets(context: Context) {
        try {
            WidgetProvider1x1.updateAllWidgets(context)
            WidgetProvider2x1.updateAllWidgets(context)
            WidgetProvider3x1.updateAllWidgets(context)
            WidgetProvider3x2.updateAllWidgets(context)
            WidgetProvider4x3.updateAllWidgets(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
