package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI分类推断器
 * 根据用户消息关键词推断交易分类（子分类优先，父分类兜底）
 */
@Singleton
class AICategoryInferrer @Inject constructor() {

    fun inferCategory(message: String, type: TransactionType): String? {
        val lowerMessage = message.lowercase()

        for ((subCategory, keywords) in subCategoryKeywords) {
            if (keywords.any { lowerMessage.contains(it) }) {
                return subCategory
            }
        }

        for ((category, keywords) in parentCategoryKeywords) {
            if (keywords.any { lowerMessage.contains(it) }) {
                return category
            }
        }

        return null
    }

    fun parseSubCategoryIntent(message: String): Pair<String, String>? {
        val p1 = Regex("在[「「]?(.+?)[」」]?下[面]?(?:创建|添加|新建)[「「]?(.+?)[」」]?(?:分类|子分类)?$")
        p1.find(message)?.let { return it.groupValues[1].trim() to it.groupValues[2].trim() }

        val p2 = Regex("给[「「]?(.+?)[」」]?(?:添加|创建|新建)子分类[「「]?(.+?)[」」]?$")
        p2.find(message)?.let { return it.groupValues[1].trim() to it.groupValues[2].trim() }

        val p3 = Regex("[「「]?(.+?)[」」]?下(?:创建|添加|新建)[「「]?(.+?)[」」]?(?:分类|子分类)?$")
        p3.find(message)?.let { return it.groupValues[1].trim() to it.groupValues[2].trim() }

        val p4 = Regex("把[「「]?(.+?)[」」]?归[到入][「「]?(.+?)[」」]?下")
        p4.find(message)?.let { return it.groupValues[2].trim() to it.groupValues[1].trim() }

        return null
    }

    fun parseCategoryName(message: String): String? {
        val patterns = listOf(
            Regex("(?:创建|添加|新建)[「「]?(.+?)[」」]?分类"),
            Regex("分类[「「]?(.+?)[」」]?$"),
            Regex("(?:创建|添加|新建)(?:一个)?[「「]?(.+?)[」」]?(?:的)?(?:支出|收入)?分类")
        )
        for (pattern in patterns) {
            pattern.find(message)?.let {
                val name = it.groupValues[1].trim()
                if (name.isNotEmpty() && name.length <= 10) return name
            }
        }
        return null
    }

    private val subCategoryKeywords = mapOf(
        "火锅" to listOf("火锅"),
        "烧烤" to listOf("烧烤", "撸串"),
        "奶茶" to listOf("奶茶", "茶饮"),
        "咖啡" to listOf("咖啡", "星巴克", "瑞幸"),
        "外卖" to listOf("外卖", "美团", "饿了么"),
        "聚餐" to listOf("聚餐", "聚会", "请客"),
        "早餐" to listOf("早餐", "早饭", "早点"),
        "午餐" to listOf("午餐", "午饭", "中饭"),
        "晚餐" to listOf("晚餐", "晚饭"),
        "夜宵" to listOf("夜宵", "宵夜"),
        "零食" to listOf("零食", "小吃", "甜品", "蛋糕"),
        "水果" to listOf("水果"),
        "打车" to listOf("打车", "滴滴", "出租车", "网约车"),
        "地铁" to listOf("地铁"),
        "公交" to listOf("公交"),
        "加油" to listOf("加油", "油费"),
        "停车" to listOf("停车"),
        "高铁" to listOf("高铁", "火车"),
        "机票" to listOf("机票", "飞机"),
        "衣服" to listOf("衣服", "裤子", "裙子", "外套"),
        "鞋子" to listOf("鞋子", "鞋"),
        "化妆品" to listOf("化妆品", "口红", "面膜"),
        "数码" to listOf("手机", "电脑", "耳机", "平板"),
        "电影" to listOf("电影", "影院"),
        "游戏" to listOf("游戏", "充值"),
        "健身" to listOf("健身", "运动", "跑步", "游泳"),
        "旅游" to listOf("旅游", "旅行", "景点", "门票"),
        "书籍" to listOf("书", "书籍"),
        "课程" to listOf("课程", "网课", "培训"),
        "药品" to listOf("药", "药品", "药店"),
        "体检" to listOf("体检"),
        "房租" to listOf("房租", "租金"),
        "水电" to listOf("水电", "电费", "水费", "燃气"),
        "物业" to listOf("物业", "物业费")
    )

    private val parentCategoryKeywords = mapOf(
        "餐饮" to listOf("吃", "饭", "餐厅", "餐", "食", "菜", "肉", "面", "粉", "饮"),
        "交通" to listOf("车", "交通", "出行"),
        "购物" to listOf("买", "淘宝", "京东", "购物", "拼多多"),
        "娱乐" to listOf("玩", "KTV", "唱歌", "娱乐"),
        "住房" to listOf("房", "住", "居住"),
        "医疗" to listOf("医院", "看病", "医疗"),
        "教育" to listOf("学", "教育"),
        "通讯" to listOf("话费", "流量", "宽带"),
        "工资" to listOf("工资", "薪水", "薪资"),
        "奖金" to listOf("奖金", "奖励", "红包")
    )
}
