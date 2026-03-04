package com.example.aiaccounting.widget

import android.content.Context
import android.graphics.Bitmap
import com.example.aiaccounting.data.local.entity.TransactionType
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 小组件数据同步工具类
 * 用于在主应用内更新小组件数据
 */
object WidgetDataSync {

    /**
     * 更新小组件统计数据
     * 在每次交易变更后调用
     */
    suspend fun updateWidgetStats(
        context: Context,
        transactionRepository: com.example.aiaccounting.data.repository.TransactionRepository,
        budgetRepository: com.example.aiaccounting.data.repository.BudgetRepository
    ) {
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
            val categoryMap = mutableMapOf<String, Float>()
            val dailyExpenses = MutableList(7) { 0f }

            transactions.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.INCOME -> totalIncome += transaction.amount
                    TransactionType.EXPENSE -> {
                        totalExpense += transaction.amount
                        // 统计分类
                        val categoryName = transaction.categoryId.toString() // 简化处理，实际应该查询分类名称
                        categoryMap[categoryName] = categoryMap.getOrDefault(categoryName, 0f) + transaction.amount.toFloat()
                    }
                    else -> {}
                }
            }

            // 获取最近7天的支出数据
            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L
            for (i in 0 until 7) {
                val dayStart = now - (6 - i) * oneDay
                val dayEnd = dayStart + oneDay
                val dayTransactions = transactionRepository.getTransactionsByDateRange(dayStart, dayEnd).first()
                dailyExpenses[i] = dayTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                    .toFloat()
            }

            // 获取本月预算
            val budgetProgress = budgetRepository.getTotalBudgetProgress(year, month).first()
            val budgetLimit = budgetProgress?.budget?.amount ?: 0.0

            // 生成图表
            val trendChartBase64 = WidgetChartGenerator.generateTrendChart(context, dailyExpenses)
            val pieChartBase64 = if (categoryMap.isNotEmpty()) {
                WidgetChartGenerator.generatePieChart(context, categoryMap.keys.toList(), categoryMap.values.toList())
            } else null

            // 保存到SharedPreferences
            val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putFloat("month_income", totalIncome.toFloat())
                putFloat("month_expense", totalExpense.toFloat())
                putFloat("month_budget", budgetLimit.toFloat())
                trendChartBase64?.let { putString("trend_chart", it) }
                pieChartBase64?.let { putString("pie_chart", it) }
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

    /**
     * 同步小组件中的待处理交易到数据库
     * 在主应用启动时调用
     */
    suspend fun syncPendingTransactions(
        context: Context,
        transactionRepository: com.example.aiaccounting.data.repository.TransactionRepository
    ) {
        try {
            val prefs = context.getSharedPreferences("widget_pending_transactions", Context.MODE_PRIVATE)
            val pendingData = prefs.getString("pending", "[]") ?: "[]"

            // 解析待处理交易
            val jsonArray = org.json.JSONArray(pendingData)
            if (jsonArray.length() == 0) return

            // 插入到数据库
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val transaction = com.example.aiaccounting.data.local.entity.Transaction(
                    id = 0, // 自动生成
                    accountId = json.getLong("accountId"),
                    categoryId = json.getLong("categoryId"),
                    type = com.example.aiaccounting.data.local.entity.TransactionType.valueOf(json.getString("type")),
                    amount = json.getDouble("amount"),
                    date = json.getLong("date"),
                    note = json.getString("note")
                )
                transactionRepository.insertTransaction(transaction)
            }

            // 清空待处理列表
            prefs.edit().putString("pending", "[]").apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
