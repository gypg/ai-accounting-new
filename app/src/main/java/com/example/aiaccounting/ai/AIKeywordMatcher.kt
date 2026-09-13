package com.example.aiaccounting.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI关键词匹配器
 * 负责意图分析中的关键词检测辅助方法
 */
@Singleton
class AIKeywordMatcher @Inject constructor() {

    fun containsActionKeywords(message: String): Boolean {
        val actionKeywords = listOf("记", "添加", "创建", "新建", "删除", "修改", "更新")
        return actionKeywords.any { message.contains(it) }
    }

    fun containsAnalysisKeywords(message: String): Boolean {
        val analysisKeywords = listOf("分析", "统计", "趋势", "对比", "比较", "结构", "报告")
        return analysisKeywords.any { message.contains(it) }
    }

    fun containsAccountManagementKeywords(message: String): Boolean {
        val keywords = listOf("添加账户", "新建账户", "创建账户", "删除账户")
        return keywords.any { message.contains(it) }
    }

    fun containsCategoryManagementKeywords(message: String): Boolean {
        val keywords = listOf(
            "添加分类", "新建分类", "创建分类", "删除分类",
            "添加子分类", "新建子分类", "创建子分类",
            "子分类", "下面添加", "下面创建", "下新建",
            "下添加", "下创建", "归到", "归入"
        )
        return keywords.any { message.contains(it) }
    }

    fun isGreetingOrSimpleConversation(message: String): Boolean {
        val greetings = listOf("你好", "您好", "hi", "hello", "在吗", "在不在", "有人吗")
        return greetings.any { message.contains(it) }
    }
}
