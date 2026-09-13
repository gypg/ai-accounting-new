package com.example.aiaccounting.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.BudgetRepository
import com.example.aiaccounting.data.repository.CategoryRepository
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
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * 更新小组件统计数据
     * 修复：添加趋势图和饼图数据生成
     */
    suspend fun updateWidgetStats(context: Context) {
        try {
            Log.d("WidgetUpdateService", "开始更新小组件统计数据")
            
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
            Log.d("WidgetUpdateService", "本月交易数量: ${transactions.size}")

            var totalIncome = 0.0
            var totalExpense = 0.0
            
            // 按分类统计支出
            val expenseByCategory = mutableMapOf<Long, Double>()
            
            // 按日期统计支出（用于趋势图）
            val expenseByDate = mutableMapOf<Int, Double>()

            transactions.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.INCOME -> totalIncome += transaction.amount
                    TransactionType.EXPENSE -> {
                        totalExpense += transaction.amount
                        // 按分类统计
                        val categoryId = transaction.categoryId
                        expenseByCategory[categoryId] = (expenseByCategory[categoryId] ?: 0.0) + transaction.amount
                        
                        // 按日期统计（用于趋势图）
                        val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                        expenseByDate[dayOfMonth] = (expenseByDate[dayOfMonth] ?: 0.0) + transaction.amount
                    }
                    else -> {}
                }
            }

            // 获取本月预算
            val budgetProgress = budgetRepository.getTotalBudgetProgress(year, month).first()
            val budgetLimit = budgetProgress?.budget?.amount ?: 0.0

            // 生成7日趋势图数据（最近7天）
            val trendData = generate7DayTrendData(expenseByDate)
            val trendChartBase64 = if (trendData.isNotEmpty()) {
                WidgetChartGenerator.generateTrendChart(context, trendData)
            } else null
            Log.d("WidgetUpdateService", "趋势图生成结果: ${trendChartBase64 != null}")

            // 生成分类饼图数据
            val (categoryNames, categoryValues) = generateCategoryPieData(expenseByCategory, categoryRepository)
            val pieChartBase64 = if (categoryValues.isNotEmpty() && categoryValues.sum() > 0) {
                WidgetChartGenerator.generatePieChart(context, categoryNames, categoryValues)
            } else null
            Log.d("WidgetUpdateService", "饼图生成结果: ${pieChartBase64 != null}")

            // 保存到SharedPreferences
            val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putFloat("month_income", totalIncome.toFloat())
                putFloat("month_expense", totalExpense.toFloat())
                putFloat("month_budget", budgetLimit.toFloat())
                // 保存图表数据
                if (trendChartBase64 != null) {
                    putString("trend_chart", trendChartBase64)
                }
                if (pieChartBase64 != null) {
                    putString("pie_chart", pieChartBase64)
                }
                apply()
            }
            Log.d("WidgetUpdateService", "数据已保存到SharedPreferences")

            // 通知所有小组件更新
            WidgetProvider1x1.updateAllWidgets(context)
            WidgetProvider2x1.updateAllWidgets(context)
            WidgetProvider3x1.updateAllWidgets(context)
            WidgetProvider3x2.updateAllWidgets(context)
            WidgetProvider4x3.updateAllWidgets(context)
            Log.d("WidgetUpdateService", "小组件更新通知已发送")

        } catch (e: Exception) {
            Log.e("WidgetUpdateService", "更新小组件数据失败: ${e.message}", e)
        }
    }
    
    /**
     * 生成7日趋势数据
     */
    private fun generate7DayTrendData(expenseByDate: Map<Int, Double>): List<Float> {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // 获取最近7天的数据
        val trendData = mutableListOf<Float>()
        for (i in 6 downTo 0) {
            val day = currentDay - i
            if (day > 0 && day <= daysInMonth) {
                val amount = expenseByDate[day]?.toFloat() ?: 0f
                trendData.add(amount)
            } else {
                trendData.add(0f)
            }
        }
        return trendData
    }
    
    /**
     * 生成分类饼图数据
     */
    private suspend fun generateCategoryPieData(
        expenseByCategory: Map<Long, Double>,
        categoryRepository: CategoryRepository
    ): Pair<List<String>, List<Float>> {
        val categoryNames = mutableListOf<String>()
        val categoryValues = mutableListOf<Float>()
        
        // 获取所有分类信息
        val categories = categoryRepository.getAllCategories().first()
        val categoryMap = categories.associateBy { it.id }
        
        // 按金额排序，取前6个分类
        val sortedCategories = expenseByCategory.entries
            .sortedByDescending { it.value }
            .take(6)
        
        sortedCategories.forEach { (categoryId, amount) ->
            val categoryName = categoryMap[categoryId]?.name ?: "其他"
            categoryNames.add(categoryName)
            categoryValues.add(amount.toFloat())
        }
        
        return categoryNames to categoryValues
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
