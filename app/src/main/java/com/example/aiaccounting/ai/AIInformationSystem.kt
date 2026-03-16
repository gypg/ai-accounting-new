package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI智能信息系统
 * 提供全面的信息访问、分析和摘要能力
 */
@Singleton
class AIInformationSystem @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * 信息查询类型
     */
    enum class QueryType {
        ACCOUNT_INFO,           // 账户信息
        CATEGORY_INFO,          // 分类信息
        TRANSACTION_LIST,       // 交易记录列表
        TRANSACTION_SUMMARY,    // 交易摘要统计
        EXPENSE_ANALYSIS,       // 支出分析
        INCOME_ANALYSIS,        // 收入分析
        BUDGET_STATUS,          // 预算状态
        TREND_ANALYSIS,         // 趋势分析
        COMPARISON_ANALYSIS,    // 对比分析
        CUSTOM_QUERY            // 自定义查询
    }

    /**
     * 查询请求
     */
    data class QueryRequest(
        val queryType: QueryType,
        val startDate: Long? = null,
        val endDate: Long? = null,
        val accountId: Long? = null,
        val categoryId: Long? = null,
        val limit: Int? = null,
        val customParams: Map<String, String> = emptyMap()
    )

    /**
     * 查询结果
     */
    data class QueryResult(
        val success: Boolean,
        val data: Any?,
        val summary: String,
        val details: String,
        val errorMessage: String? = null
    )

    private companion object {
        const val DEFAULT_QUERY_LIMIT = 50
    }

    suspend fun executeQuery(request: QueryRequest): QueryResult {
        return try {
            when (request.queryType) {
                QueryType.ACCOUNT_INFO -> getAccountInfo()
                QueryType.CATEGORY_INFO -> getCategoryInfo()
                QueryType.TRANSACTION_LIST -> getTransactionList(request)
                QueryType.TRANSACTION_SUMMARY -> getTransactionSummary(request)
                QueryType.EXPENSE_ANALYSIS -> getExpenseAnalysis(request)
                QueryType.INCOME_ANALYSIS -> getIncomeAnalysis(request)
                QueryType.BUDGET_STATUS -> getBudgetStatus(request)
                QueryType.TREND_ANALYSIS -> getTrendAnalysis(request)
                QueryType.COMPARISON_ANALYSIS -> getComparisonAnalysis(request)
                QueryType.CUSTOM_QUERY -> executeCustomQuery(request)
            }
        } catch (e: Exception) {
            QueryResult(
                success = false,
                data = null,
                summary = "查询失败",
                details = "",
                errorMessage = e.message
            )
        }
    }

    /**
     * 获取账户信息
     */
    private suspend fun getAccountInfo(): QueryResult {
        val accounts = accountRepository.getAllAccountsList()
        val totalBalance = accounts.sumOf { it.balance }
        
        val summary = buildString {
            append("共 ${accounts.size} 个账户，")
            append("总资产 ¥${String.format("%.2f", totalBalance)}")
        }
        
        val details = buildString {
            appendLine("【账户详情】")
            accounts.forEach { account ->
                appendLine("• ${account.name}: ¥${String.format("%.2f", account.balance)} (${account.type})")
            }
        }
        
        return QueryResult(
            success = true,
            data = accounts,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取分类信息
     */
    private suspend fun getCategoryInfo(): QueryResult {
        val categories = categoryRepository.getAllCategoriesList()
        val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
        val incomeCategories = categories.filter { it.type == TransactionType.INCOME }
        
        val summary = "共 ${categories.size} 个分类（${expenseCategories.size} 个支出，${incomeCategories.size} 个收入）"
        
        val details = buildString {
            appendLine("【支出分类】")
            expenseCategories.forEach { 
                appendLine("• ${it.name}")
            }
            appendLine()
            appendLine("【收入分类】")
            incomeCategories.forEach { 
                appendLine("• ${it.name}")
            }
        }
        
        return QueryResult(
            success = true,
            data = categories,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取交易记录列表
     */
    private suspend fun getTransactionList(request: QueryRequest): QueryResult {
        val transactions = if (request.startDate != null && request.endDate != null) {
            val limit = request.limit ?: DEFAULT_QUERY_LIMIT
            transactionRepository.getTransactionsByDateRangeList(
                request.startDate,
                request.endDate,
                limit
            )
        } else {
            val limit = request.limit ?: DEFAULT_QUERY_LIMIT
            transactionRepository.getRecentTransactionsList(limit)
        }

        val limitedList = transactions

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val summary = "共找到 ${transactions.size} 笔交易记录" +
                if (request.limit != null && transactions.size >= request.limit)
                    "，显示前 ${request.limit} 笔" else ""
        
        val details = buildString {
            appendLine("【交易记录】")
            limitedList.forEach { transaction ->
                val typeStr = when (transaction.type) {
                    TransactionType.INCOME -> "收入"
                    TransactionType.EXPENSE -> "支出"
                    TransactionType.TRANSFER -> "转账"
                }
                val dateStr = dateFormat.format(Date(transaction.date))
                appendLine("• $dateStr | $typeStr | ¥${String.format("%.2f", transaction.amount)} | ${transaction.note}")
            }
        }
        
        return QueryResult(
            success = true,
            data = limitedList,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取交易摘要统计
     */
    private suspend fun getTransactionSummary(request: QueryRequest): QueryResult {
        val (startDate, endDate) = getDateRange(request)
        
        val transactions = transactionRepository.getTransactionsByDateRange(
            startDate, endDate
        ).first()
        
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        
        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val netAmount = totalIncome - totalExpense
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val periodStr = "${dateFormat.format(Date(startDate))} 至 ${dateFormat.format(Date(endDate))}"
        
        val summary = buildString {
            append("期间收支：收入 ¥${String.format("%.2f", totalIncome)}，")
            append("支出 ¥${String.format("%.2f", totalExpense)}，")
            append("净${if (netAmount >= 0) "盈余" else "亏损"} ¥${String.format("%.2f", kotlin.math.abs(netAmount))}")
        }
        
        val details = buildString {
            appendLine("【收支摘要】($periodStr)")
            appendLine("• 总收入: ¥${String.format("%.2f", totalIncome)} (${transactions.count { it.type == TransactionType.INCOME }} 笔)")
            appendLine("• 总支出: ¥${String.format("%.2f", totalExpense)} (${transactions.count { it.type == TransactionType.EXPENSE }} 笔)")
            appendLine("• 净${if (netAmount >= 0) "盈余" else "亏损"}: ¥${String.format("%.2f", kotlin.math.abs(netAmount))}")
            appendLine("• 交易笔数: ${transactions.size} 笔")
        }
        
        val data = mapOf(
            "totalIncome" to totalIncome,
            "totalExpense" to totalExpense,
            "netAmount" to netAmount,
            "transactionCount" to transactions.size,
            "incomeCount" to transactions.count { it.type == TransactionType.INCOME },
            "expenseCount" to transactions.count { it.type == TransactionType.EXPENSE }
        )
        
        return QueryResult(
            success = true,
            data = data,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取支出分析
     */
    private suspend fun getExpenseAnalysis(request: QueryRequest): QueryResult {
        val (startDate, endDate) = getDateRange(request)

        val transactions = transactionRepository.getTransactionsByDateRange(
            startDate, endDate
        ).first()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenses.sumOf { it.amount }

        // 避免 N+1：一次性加载分类映射
        val categoryMap = categoryRepository.getAllCategoriesList().associateBy { it.id }

        // 按分类统计
        val categoryExpenses = expenses.groupBy { it.categoryId }
            .map { (categoryId, trans) ->
                val categoryName = categoryMap[categoryId]?.name ?: "未分类"
                Pair(categoryName, trans.sumOf { it.amount })
            }
            .sortedByDescending { it.second }
        
        val topCategories = categoryExpenses.take(5)
        
        val summary = buildString {
            append("总支出 ¥${String.format("%.2f", totalExpense)}，")
            append("共 ${expenses.size} 笔，")
            append("最大支出分类：${topCategories.firstOrNull()?.first ?: "无"}")
        }
        
        val details = buildString {
            appendLine("【支出分析】")
            appendLine("• 总支出: ¥${String.format("%.2f", totalExpense)}")
            appendLine("• 交易笔数: ${expenses.size} 笔")
            appendLine("• 平均单笔: ¥${String.format("%.2f", if (expenses.isNotEmpty()) totalExpense / expenses.size else 0.0)}")
            appendLine()
            appendLine("【支出分类 TOP${topCategories.size}】")
            topCategories.forEachIndexed { index, (name, amount) ->
                val percentage = if (totalExpense > 0) (amount / totalExpense * 100) else 0.0
                appendLine("${index + 1}. $name: ¥${String.format("%.2f", amount)} (${String.format("%.1f", percentage)}%)")
            }
        }
        
        return QueryResult(
            success = true,
            data = categoryExpenses,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取收入分析
     */
    private suspend fun getIncomeAnalysis(request: QueryRequest): QueryResult {
        val (startDate, endDate) = getDateRange(request)

        val transactions = transactionRepository.getTransactionsByDateRange(
            startDate, endDate
        ).first()

        val incomes = transactions.filter { it.type == TransactionType.INCOME }
        val totalIncome = incomes.sumOf { it.amount }

        // 避免 N+1：一次性加载分类映射
        val categoryMap = categoryRepository.getAllCategoriesList().associateBy { it.id }

        // 按分类统计
        val categoryIncomes = incomes.groupBy { it.categoryId }
            .map { (categoryId, trans) ->
                val categoryName = categoryMap[categoryId]?.name ?: "未分类"
                Pair(categoryName, trans.sumOf { it.amount })
            }
            .sortedByDescending { it.second }
        
        val summary = buildString {
            append("总收入 ¥${String.format("%.2f", totalIncome)}，")
            append("共 ${incomes.size} 笔")
        }
        
        val details = buildString {
            appendLine("【收入分析】")
            appendLine("• 总收入: ¥${String.format("%.2f", totalIncome)}")
            appendLine("• 交易笔数: ${incomes.size} 笔")
            appendLine("• 平均单笔: ¥${String.format("%.2f", if (incomes.isNotEmpty()) totalIncome / incomes.size else 0.0)}")
            appendLine()
            appendLine("【收入来源】")
            categoryIncomes.forEachIndexed { index, (name, amount) ->
                val percentage = if (totalIncome > 0) (amount / totalIncome * 100) else 0.0
                appendLine("${index + 1}. $name: ¥${String.format("%.2f", amount)} (${String.format("%.1f", percentage)}%)")
            }
        }
        
        return QueryResult(
            success = true,
            data = categoryIncomes,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取预算状态
     */
    private suspend fun getBudgetStatus(request: QueryRequest): QueryResult {
        // 获取本月数据
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis
        val endOfMonth = System.currentTimeMillis()
        
        val transactions = transactionRepository.getTransactionsByDateRange(
            startOfMonth, endOfMonth
        ).first()
        
        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        // 这里可以添加预算设置和比较逻辑
        val summary = "本月已支出 ¥${String.format("%.2f", totalExpense)}"
        
        val details = buildString {
            appendLine("【本月支出概况】")
            appendLine("• 总支出: ¥${String.format("%.2f", totalExpense)}")
            appendLine("• 交易笔数: ${transactions.filter { it.type == TransactionType.EXPENSE }.size} 笔")
        }
        
        return QueryResult(
            success = true,
            data = totalExpense,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取趋势分析
     */
    private suspend fun getTrendAnalysis(request: QueryRequest): QueryResult {
        val (startDate, endDate) = getDateRange(request)
        
        val transactions = transactionRepository.getTransactionsByDateRange(
            startDate, endDate
        ).first()
        
        // 按天统计
        val dailyData = transactions.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
        }.map { (date, trans) ->
            val income = trans.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = trans.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            Triple(date, income, expense)
        }.sortedBy { it.first }
        
        val summary = "分析了 ${dailyData.size} 天的数据，共 ${transactions.size} 笔交易"
        
        val details = buildString {
            appendLine("【每日收支趋势】")
            dailyData.forEach { (date, income, expense) ->
                appendLine("• $date: 收入 ¥${String.format("%.2f", income)}, 支出 ¥${String.format("%.2f", expense)}")
            }
        }
        
        return QueryResult(
            success = true,
            data = dailyData,
            summary = summary,
            details = details
        )
    }

    /**
     * 获取对比分析
     */
    private suspend fun getComparisonAnalysis(request: QueryRequest): QueryResult {
        // 获取本月和上月数据
        val calendar = Calendar.getInstance()
        
        // 本月
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val thisMonthStart = calendar.timeInMillis
        val thisMonthEnd = System.currentTimeMillis()
        
        // 上月
        calendar.add(Calendar.MONTH, -1)
        val lastMonthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DATE, -1)
        val lastMonthEnd = calendar.timeInMillis
        
        val thisMonthTrans = transactionRepository.getTransactionsByDateRange(
            thisMonthStart, thisMonthEnd
        ).first()
        
        val lastMonthTrans = transactionRepository.getTransactionsByDateRange(
            lastMonthStart, lastMonthEnd
        ).first()
        
        val thisMonthExpense = thisMonthTrans
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val lastMonthExpense = lastMonthTrans
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val change = thisMonthExpense - lastMonthExpense
        val changePercent = if (lastMonthExpense > 0) (change / lastMonthExpense * 100) else 0.0
        
        val summary = buildString {
            append("本月支出 ¥${String.format("%.2f", thisMonthExpense)}，")
            append("比上月${if (change >= 0) "增加" else "减少"} ¥${String.format("%.2f", kotlin.math.abs(change))}")
        }
        
        val details = buildString {
            appendLine("【月度对比】")
            appendLine("• 本月支出: ¥${String.format("%.2f", thisMonthExpense)}")
            appendLine("• 上月支出: ¥${String.format("%.2f", lastMonthExpense)}")
            appendLine("• 变化: ${if (change >= 0) "+" else "-"}¥${String.format("%.2f", kotlin.math.abs(change))} (${String.format("%.1f", changePercent)}%)")
        }
        
        return QueryResult(
            success = true,
            data = mapOf(
                "thisMonth" to thisMonthExpense,
                "lastMonth" to lastMonthExpense,
                "change" to change,
                "changePercent" to changePercent
            ),
            summary = summary,
            details = details
        )
    }

    /**
     * 执行自定义查询
     */
    private suspend fun executeCustomQuery(request: QueryRequest): QueryResult {
        // 根据自定义参数执行查询
        val queryDescription = request.customParams["description"] ?: "自定义查询"
        
        return QueryResult(
            success = true,
            data = null,
            summary = "执行了: $queryDescription",
            details = "自定义查询结果"
        )
    }

    /**
     * 获取日期范围
     */
    private fun getDateRange(request: QueryRequest): Pair<Long, Long> {
        val startDate = request.startDate ?: getDefaultStartDate()
        val endDate = request.endDate ?: System.currentTimeMillis()
        return Pair(startDate, endDate)
    }

    /**
     * 获取默认开始日期（30天前）
     */
    private fun getDefaultStartDate(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        return calendar.timeInMillis
    }

    /**
     * 生成智能摘要
     */
    suspend fun generateSmartSummary(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.timeInMillis
        val endOfMonth = System.currentTimeMillis()
        
        val transactions = transactionRepository.getTransactionsByDateRange(
            startOfMonth, endOfMonth
        ).first()
        
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        
        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val netAmount = totalIncome - totalExpense
        
        return buildString {
            appendLine("【本月财务概况】")
            appendLine("💰 总收入: ¥${String.format("%.2f", totalIncome)}")
            appendLine("💸 总支出: ¥${String.format("%.2f", totalExpense)}")
            appendLine("📊 净${if (netAmount >= 0) "盈余" else "亏损"}: ¥${String.format("%.2f", kotlin.math.abs(netAmount))}")
            appendLine("📝 交易笔数: ${transactions.size} 笔")
            
            if (totalExpense > 0) {
                val topExpense = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .maxByOrNull { it.amount }
                
                if (topExpense != null) {
                    appendLine()
                    appendLine("【最大单笔支出】")
                    appendLine("¥${String.format("%.2f", topExpense.amount)} - ${topExpense.note}")
                }
            }
        }
    }
}
