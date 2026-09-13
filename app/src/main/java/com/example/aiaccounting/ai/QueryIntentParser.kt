package com.example.aiaccounting.ai

import java.util.Calendar
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 查询意图解析器
 * 将自然语言查询解析为结构化的查询请求
 */
@Singleton
class QueryIntentParser @Inject constructor() {

    /**
     * 解析用户查询
     */
    fun parseQuery(message: String): AIInformationSystem.QueryRequest? {
        val lowerMessage = message.lowercase()
        
        // 识别查询类型
        val queryType = identifyQueryType(lowerMessage)
        
        // 如果不是查询类型，返回null
        if (queryType == null) {
            return null
        }
        
        // 提取日期范围
        val (startDate, endDate) = extractDateRange(lowerMessage)
        
        // 提取限制数量
        val limit = extractLimit(lowerMessage)
        
        return AIInformationSystem.QueryRequest(
            queryType = queryType,
            startDate = startDate,
            endDate = endDate,
            limit = limit
        )
    }
    
    /**
     * 识别查询类型
     */
    private fun identifyQueryType(message: String): AIInformationSystem.QueryType? {
        return when {
            // 账户信息查询
            containsAny(message, listOf("账户", "余额", "资产", "有多少钱", "总资产", "存款")) -> {
                AIInformationSystem.QueryType.ACCOUNT_INFO
            }
            
            // 分类信息查询
            containsAny(message, listOf("分类", "类别", "有哪些分类")) -> {
                AIInformationSystem.QueryType.CATEGORY_INFO
            }
            
            // 交易记录查询
            containsAny(message, listOf("交易记录", "明细", "账单", "消费记录", "最近消费")) -> {
                AIInformationSystem.QueryType.TRANSACTION_LIST
            }
            
            // 收支摘要
            containsAny(message, listOf("收支", "收入支出", "汇总", "总结", "概况")) && 
            !containsAny(message, listOf("分析", "趋势")) -> {
                AIInformationSystem.QueryType.TRANSACTION_SUMMARY
            }
            
            // 支出分析
            containsAny(message, listOf("支出分析", "消费分析", "钱花哪了", "消费结构")) -> {
                AIInformationSystem.QueryType.EXPENSE_ANALYSIS
            }
            
            // 收入分析
            containsAny(message, listOf("收入分析", "收入来源")) -> {
                AIInformationSystem.QueryType.INCOME_ANALYSIS
            }
            
            // 预算状态
            containsAny(message, listOf("预算", "还剩多少", "额度")) -> {
                AIInformationSystem.QueryType.BUDGET_STATUS
            }
            
            // 趋势分析
            containsAny(message, listOf("趋势", "走势", "变化", "统计")) -> {
                AIInformationSystem.QueryType.TREND_ANALYSIS
            }
            
            // 对比分析
            containsAny(message, listOf("对比", "比较", "比上月", "比上月", "环比")) -> {
                AIInformationSystem.QueryType.COMPARISON_ANALYSIS
            }
            
            else -> null
        }
    }
    
    /**
     * 提取日期范围
     */
    private fun extractDateRange(message: String): Pair<Long?, Long?> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // 检查特定日期关键词
        when {
            // 今天
            message.contains("今天") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, now)
            }
            
            // 昨天
            message.contains("昨天") -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val end = calendar.timeInMillis
                return Pair(start, end)
            }
            
            // 本周
            message.contains("本周") || message.contains("这周") -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, now)
            }
            
            // 上周
            message.contains("上周") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                val end = calendar.timeInMillis
                return Pair(start, end)
            }
            
            // 本月
            message.contains("本月") || message.contains("这个月") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, now)
            }
            
            // 上月
            message.contains("上月") || message.contains("上个月") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.timeInMillis
                return Pair(start, end)
            }
            
            // 今年
            message.contains("今年") || message.contains("本年") -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, now)
            }
            
            // 最近N天
            message.contains("最近") || message.contains("近") -> {
                val days = extractNumber(message) ?: 7
                calendar.add(Calendar.DAY_OF_MONTH, -days)
                val start = calendar.timeInMillis
                return Pair(start, now)
            }
        }
        
        // 默认返回null，使用系统默认值
        return Pair(null, null)
    }
    
    /**
     * 提取限制数量
     */
    private fun extractLimit(message: String): Int? {
        // 匹配 "前N条"、"最近N笔"、"N条" 等
        val patterns = listOf(
            Pattern.compile("(前|最近|显示|看)(\\d+)(条|笔|个|项)"),
            Pattern.compile("(\\d+)(条|笔|个|项)(记录|数据|交易)")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(2)?.toIntOrNull()
            }
        }
        
        return null
    }
    
    /**
     * 提取数字
     */
    private fun extractNumber(message: String): Int? {
        val pattern = Pattern.compile("(\\d+)")
        val matcher = pattern.matcher(message)
        if (matcher.find()) {
            return matcher.group(1)?.toIntOrNull()
        }
        return null
    }
    
    /**
     * 检查是否包含任意关键词
     */
    private fun containsAny(message: String, keywords: List<String>): Boolean {
        return keywords.any { message.contains(it) }
    }
    
    /**
     * 判断是否为信息查询类消息
     */
    fun isInformationQuery(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // 查询类关键词
        val queryKeywords = listOf(
            "查", "看", "显示", "告诉", "多少", "余额", "资产",
            "账户", "分类", "记录", "明细", "账单", "统计",
            "分析", "趋势", "对比", "比较", "汇总", "总结",
            "概况", "预算", "额度", "还剩", "收入", "支出"
        )
        
        // 排除记账类关键词
        val excludeKeywords = listOf(
            "记", "花了", "消费", "收入", "支出", "转账", "买", "卖",
            "添加", "创建", "新建", "删除", "修改", "更新"
        )
        
        // 如果包含查询关键词且不包含排除关键词，认为是查询
        val hasQueryKeyword = queryKeywords.any { lowerMessage.contains(it) }
        val hasExcludeKeyword = excludeKeywords.any { 
            lowerMessage.contains(it) && !lowerMessage.contains("查询") && !lowerMessage.contains("查看")
        }
        
        return hasQueryKeyword && !hasExcludeKeyword
    }
}
