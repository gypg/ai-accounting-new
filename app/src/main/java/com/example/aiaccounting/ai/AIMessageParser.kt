package com.example.aiaccounting.ai

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI消息解析工具
 * 从用户消息中提取金额、日期、备注、查询类型等结构化信息
 */
@Singleton
class AIMessageParser @Inject constructor() {

    fun extractAmount(message: String): Double? {
        val match = Regex("""\d+\.?\d*\s*[元块]?""").find(message)
        return match?.groupValues?.get(0)?.replace(Regex("[元块\\s]"), "")?.toDoubleOrNull()
    }

    fun containsAmount(message: String): Boolean {
        return Regex("""\d+\.?\d*\s*[元块]?""").containsMatchIn(message)
    }

    fun extractNote(message: String): String? {
        val patterns = listOf(
            Regex("""买了(.+?)(?:[，。]|$)"""),
            Regex("""花了.+?买(.+?)(?:[，。]|$)"""),
            Regex("""消费(.+?)(?:[，。]|$)"""),
            Regex("""用于(.+?)(?:[，。]|$)""")
        )
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    fun extractLimit(message: String): Int? {
        return Regex("""(\d+)""").find(message)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun inferQueryType(message: String): AIInformationSystem.QueryType {
        return when {
            message.contains("账户") || message.contains("余额") || message.contains("资产") ->
                AIInformationSystem.QueryType.ACCOUNT_INFO
            message.contains("分类") || message.contains("类别") ->
                AIInformationSystem.QueryType.CATEGORY_INFO
            message.contains("明细") || message.contains("记录") || message.contains("账单") ->
                AIInformationSystem.QueryType.TRANSACTION_LIST
            message.contains("支出分析") || message.contains("消费分析") || message.contains("钱花") ->
                AIInformationSystem.QueryType.EXPENSE_ANALYSIS
            message.contains("收入分析") ->
                AIInformationSystem.QueryType.INCOME_ANALYSIS
            message.contains("趋势") || message.contains("走势") ->
                AIInformationSystem.QueryType.TREND_ANALYSIS
            message.contains("对比") || message.contains("比较") || message.contains("环比") ->
                AIInformationSystem.QueryType.COMPARISON_ANALYSIS
            message.contains("预算") ->
                AIInformationSystem.QueryType.BUDGET_STATUS
            message.contains("收支") || message.contains("汇总") || message.contains("总结") ->
                AIInformationSystem.QueryType.TRANSACTION_SUMMARY
            else -> AIInformationSystem.QueryType.ACCOUNT_INFO
        }
    }

    fun extractDateRange(message: String): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        return when {
            message.contains("今天") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            message.contains("昨天") -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            message.contains("本周") || message.contains("这周") -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            message.contains("本月") || message.contains("这个月") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            message.contains("上月") || message.contains("上个月") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                Pair(start, calendar.timeInMillis)
            }
            message.contains("最近") || message.contains("近") -> {
                val days = Regex("""(\d+)""").find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 7
                calendar.add(Calendar.DAY_OF_MONTH, -days)
                Pair(calendar.timeInMillis, now)
            }
            else -> Pair(null, null)
        }
    }

    fun extractTransactionDate(message: String): Long {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        val specificDate = extractSpecificDateFromMessage(message, currentYear)
        if (specificDate != null) return specificDate

        when {
            message.contains("昨天") -> calendar.add(Calendar.DAY_OF_MONTH, -1)
            message.contains("前天") -> calendar.add(Calendar.DAY_OF_MONTH, -2)
            message.contains("大前天") -> calendar.add(Calendar.DAY_OF_MONTH, -3)
            message.contains("上周") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            message.contains("上月") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun extractSpecificDateFromMessage(message: String, defaultYear: Int): Long? {
        val patterns = listOf(
            "(\\d{4})年(\\d{1,2})月(\\d{1,2})[日号]?" to listOf(1, 2, 3),
            "(\\d{1,2})月(\\d{1,2})[日号]?" to listOf(-1, 1, 2),
            "(\\d{1,2})\\.(\\d{1,2})" to listOf(-1, 1, 2),
            "(\\d{1,2})-(\\d{1,2})" to listOf(-1, 1, 2),
            "(\\d{1,2})/(\\d{1,2})" to listOf(-1, 1, 2)
        )

        for ((pattern, groups) in patterns) {
            val matcher = java.util.regex.Pattern.compile(pattern).matcher(message)
            if (matcher.find()) {
                val year = if (groups[0] == -1) defaultYear else matcher.group(groups[0])?.toIntOrNull() ?: defaultYear
                val month = matcher.group(groups[1])?.toIntOrNull()
                val day = matcher.group(groups[2])?.toIntOrNull()

                if (month != null && day != null && month in 1..12 && day in 1..31) {
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month - 1, day, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    return calendar.timeInMillis
                }
            }
        }
        return null
    }

    fun extractAnalysisScope(message: String): AIReasoningEngine.AnalysisScope {
        return when {
            message.contains("今天") -> AIReasoningEngine.AnalysisScope.TODAY
            message.contains("昨天") -> AIReasoningEngine.AnalysisScope.YESTERDAY
            message.contains("本周") || message.contains("这周") -> AIReasoningEngine.AnalysisScope.THIS_WEEK
            message.contains("本月") || message.contains("这个月") -> AIReasoningEngine.AnalysisScope.THIS_MONTH
            message.contains("上月") || message.contains("上个月") -> AIReasoningEngine.AnalysisScope.LAST_MONTH
            message.contains("今年") || message.contains("本年") -> AIReasoningEngine.AnalysisScope.THIS_YEAR
            message.contains("最近7天") || message.contains("近7天") -> AIReasoningEngine.AnalysisScope.LAST_7_DAYS
            message.contains("最近30天") || message.contains("近30天") -> AIReasoningEngine.AnalysisScope.LAST_30_DAYS
            else -> AIReasoningEngine.AnalysisScope.THIS_MONTH
        }
    }

    fun getScopeDateRange(scope: AIReasoningEngine.AnalysisScope): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        val startDate = when (scope) {
            AIReasoningEngine.AnalysisScope.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1); calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_MONTH, -7); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_MONTH, -30); calendar.timeInMillis
            }
            AIReasoningEngine.AnalysisScope.CUSTOM -> calendar.timeInMillis
        }

        return Pair(startDate, endDate)
    }
}