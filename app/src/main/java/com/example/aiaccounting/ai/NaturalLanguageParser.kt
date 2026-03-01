package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import java.util.*
import java.util.regex.Pattern

/**
 * 自然语言解析器
 * 用于从用户输入中提取记账信息
 */
class NaturalLanguageParser {

    /**
     * 解析结果数据类
     */
    data class ParsedResult(
        val type: TransactionType? = null,
        val amount: Double? = null,
        val category: String? = null,
        val date: Date? = null,
        val remark: String? = null,
        val confidence: Float = 0f
    )

    /**
     * 主解析函数
     * @param input 用户输入文本
     * @param categories 可用分类列表（用于匹配）
     * @return 解析结果
     */
    fun parse(input: String, categories: List<Category> = emptyList()): ParsedResult {
        val normalizedInput = normalizeInput(input)
        
        // 提取金额
        val amount = extractAmount(normalizedInput)
        
        // 提取日期
        val date = extractDate(normalizedInput)
        
        // 判断交易类型
        val type = determineTransactionType(normalizedInput)
        
        // 匹配分类
        val category = matchCategory(normalizedInput, categories, type)
        
        // 提取备注
        val remark = extractRemark(normalizedInput, amount, category)
        
        // 计算置信度
        val confidence = calculateConfidence(amount, type, category)
        
        return ParsedResult(
            type = type,
            amount = amount,
            category = category,
            date = date,
            remark = remark,
            confidence = confidence
        )
    }

    /**
     * 标准化输入文本
     */
    private fun normalizeInput(input: String): String {
        return input
            .trim()
            .replace("，", ",")
            .replace("。", ".")
            .replace("！", "!")
            .replace("？", "?")
            .replace("块", "元")
            .replace("块钱", "元")
            .replace("大洋", "元")
    }

    /**
     * 提取金额
     * 支持格式：25元、25.5元、¥25、25块、25
     */
    fun extractAmount(input: String): Double? {
        // 匹配模式：数字+元/块/¥符号
        val patterns = listOf(
            "(\\d+\\.?\\d*)\\s*[元块]",           // 25元、25.5元、25块
            "[¥￥]\\s*(\\d+\\.?\\d*)",           // ¥25、￥25
            "(\\d+\\.?\\d*)\\s*元",              // 25 元
            "花了\\s*(\\d+\\.?\\d*)",            // 花了25
            "收入\\s*(\\d+\\.?\\d*)",            // 收入25
            "(?:是|为|了)\\s*(\\d+\\.?\\d*)",   // 是25、为25
        )

        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(input)
            if (matcher.find()) {
                return matcher.group(1)?.toDoubleOrNull()
            }
        }

        // 如果没有找到带单位的数字，尝试提取任意数字
        val numberPattern = "(\\d+\\.?\\d*)"
        val matcher = Pattern.compile(numberPattern).matcher(input)
        if (matcher.find()) {
            val number = matcher.group(1)?.toDoubleOrNull()
            // 如果数字太大（可能是日期），忽略
            if (number != null && number < 1000000) {
                return number
            }
        }

        return null
    }

    /**
     * 提取日期
     * 支持：今天、昨天、前天、本周、上周、本月、上月、具体日期
     */
    fun extractDate(input: String): Date? {
        val calendar = Calendar.getInstance()
        
        return when {
            // 今天
            input.contains("今天") || input.contains("今日") -> {
                calendar.time
            }
            // 昨天
            input.contains("昨天") || input.contains("昨日") -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.time
            }
            // 前天
            input.contains("前天") -> {
                calendar.add(Calendar.DAY_OF_MONTH, -2)
                calendar.time
            }
            // 大前天
            input.contains("大前天") -> {
                calendar.add(Calendar.DAY_OF_MONTH, -3)
                calendar.time
            }
            // 本周
            input.contains("本周") || input.contains("这周") -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.time
            }
            // 上周
            input.contains("上周") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.time
            }
            // 本月
            input.contains("本月") || input.contains("这个月") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            // 上月
            input.contains("上月") || input.contains("上个月") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            // 今年
            input.contains("今年") -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            // 去年
            input.contains("去年") -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            // 具体日期匹配（如：3月15日、3.15、3-15）
            else -> {
                extractSpecificDate(input)
            }
        }
    }

    /**
     * 提取具体日期
     */
    private fun extractSpecificDate(input: String): Date? {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        
        // 匹配模式：3月15日、3.15、3-15、3/15
        val patterns = listOf(
            "(\\d{1,2})月(\\d{1,2})[日号]?",
            "(\\d{1,2})\\.(\\d{1,2})",
            "(\\d{1,2})-(\\d{1,2})",
            "(\\d{1,2})/(\\d{1,2})"
        )
        
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(input)
            if (matcher.find()) {
                val month = matcher.group(1)?.toIntOrNull()
                val day = matcher.group(2)?.toIntOrNull()
                
                if (month != null && day != null) {
                    // 验证日期有效性
                    if (month in 1..12 && day in 1..31) {
                        calendar.set(currentYear, month - 1, day)
                        // 如果日期在未来，可能是去年的
                        if (calendar.time.after(Date())) {
                            calendar.add(Calendar.YEAR, -1)
                        }
                        return calendar.time
                    }
                }
            }
        }
        
        return null
    }

    /**
     * 判断交易类型
     */
    fun determineTransactionType(input: String): TransactionType? {
        // 收入关键词
        val incomeKeywords = listOf(
            "收入", "收到", "到账", "工资", "奖金", "红包", "转账", "退款",
            "收益", "利息", "分红", "报销", "补贴", "津贴", "赚", "挣",
            "发了", "给了", "打款", "入账", "进账", "来钱"
        )
        
        // 支出关键词
        val expenseKeywords = listOf(
            "支出", "花费", "花了", "消费", "买", "购", "付", "支付",
            "缴费", "充值", "还款", "借出", "打赏", "捐款", "缴", "交",
            "吃了", "喝了", "玩了", "用了", "打了", "坐了", "住了"
        )
        
        // 转账关键词
        val transferKeywords = listOf(
            "转账", "转给", "转给", "转到", "划转", "调拨"
        )
        
        val lowerInput = input.toLowerCase()
        
        return when {
            incomeKeywords.any { lowerInput.contains(it) } -> TransactionType.INCOME
            expenseKeywords.any { lowerInput.contains(it) } -> TransactionType.EXPENSE
            transferKeywords.any { lowerInput.contains(it) } -> TransactionType.TRANSFER
            // 默认根据语境判断
            lowerInput.contains("给") || lowerInput.contains("借") -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE // 默认支出
        }
    }

    /**
     * 匹配分类
     */
    fun matchCategory(
        input: String,
        categories: List<Category>,
        type: TransactionType?
    ): String? {
        if (categories.isEmpty()) return null
        
        // 根据类型筛选分类
        val filteredCategories = if (type != null) {
            categories.filter { it.type == type }
        } else {
            categories
        }
        
        // 精确匹配
        for (category in filteredCategories) {
            if (input.contains(category.name)) {
                return category.name
            }
        }
        
        // 模糊匹配（关键词）
        val categoryKeywords = mapOf(
            "餐饮" to listOf("吃", "饭", "餐", "美食", "餐厅", "外卖", "食堂", "早餐", "午餐", "晚餐", "宵夜", "火锅", "烧烤", "奶茶", "咖啡"),
            "交通" to listOf("车", "地铁", "公交", "打车", "出租", "滴滴", "高铁", "火车", "飞机", "油费", "停车", "过路费"),
            "购物" to listOf("买", "购", "淘宝", "京东", "拼多多", "天猫", "超市", "商场", "衣服", "鞋子", "包包", "化妆品"),
            "娱乐" to listOf("玩", "电影", "游戏", "KTV", "唱歌", "旅游", "旅行", "门票", "会员", "充值", "视频", "音乐"),
            "居住" to listOf("房", "租", "水电", "物业", "煤气", "宽带", "维修", "家具", "家电", "装修"),
            "医疗" to listOf("药", "医院", "看病", "体检", "医保", "诊所", "牙医", "眼镜"),
            "教育" to listOf("书", "学费", "培训", "课程", "学习", "考试", "资料", "文具"),
            "通讯" to listOf("话费", "流量", "宽带", "手机", "电话"),
            "人情" to listOf("礼", "红包", "请客", "聚会", "聚餐", "送礼", "份子钱"),
            "工资" to listOf("工资", "薪水", "薪酬", "月薪", "年薪", "收入", "发工资"),
            "投资" to listOf("股票", "基金", "理财", "投资", "收益", "利息", "分红"),
            "兼职" to listOf("兼职", "副业", "外快", "稿费", "设计费", "咨询费")
        )
        
        for ((categoryName, keywords) in categoryKeywords) {
            if (keywords.any { input.contains(it) }) {
                // 检查这个分类是否在可用分类中
                val matchedCategory = filteredCategories.find { it.name == categoryName }
                if (matchedCategory != null) {
                    return matchedCategory.name
                }
            }
        }
        
        return null
    }

    /**
     * 提取备注
     */
    private fun extractRemark(input: String, amount: Double?, category: String?): String? {
        var remark = input
        
        // 移除金额信息
        amount?.let {
            remark = remark.replace(it.toString(), "")
            remark = remark.replace("${it}元", "")
            remark = remark.replace("${it}块", "")
        }
        
        // 移除分类信息
        category?.let {
            remark = remark.replace(it, "")
        }
        
        // 移除常见词汇
        val wordsToRemove = listOf(
            "花了", "收入", "收到", "支出", "消费", "买了", "支付了",
            "今天", "昨天", "前天", "本周", "上周", "本月", "上月",
            "元", "块", "钱", "的", "了", "在", "去", "来", "用"
        )
        
        for (word in wordsToRemove) {
            remark = remark.replace(word, "")
        }
        
        // 清理多余空格
        remark = remark.trim()
        
        return if (remark.isNotEmpty()) remark else null
    }

    /**
     * 计算置信度
     */
    private fun calculateConfidence(
        amount: Double?,
        type: TransactionType?,
        category: String?
    ): Float {
        var confidence = 0f
        
        if (amount != null) confidence += 0.4f
        if (type != null) confidence += 0.3f
        if (category != null) confidence += 0.3f
        
        return confidence
    }

    /**
     * 生成确认消息
     */
    fun generateConfirmationMessage(result: ParsedResult): String {
        val sb = StringBuilder()
        sb.append("识别到以下信息：\n\n")
        
        result.type?.let {
            sb.append("类型：${if (it == TransactionType.INCOME) "收入" else "支出"}\n")
        }
        
        result.amount?.let {
            sb.append("金额：${it}元\n")
        }
        
        result.category?.let {
            sb.append("分类：$it\n")
        }
        
        result.date?.let {
            val sdf = java.text.SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
            sb.append("日期：${sdf.format(it)}\n")
        }
        
        result.remark?.let {
            if (it.isNotBlank()) {
                sb.append("备注：$it\n")
            }
        }
        
        sb.append("\n确认要记录这笔账吗？")
        
        return sb.toString()
    }

    /**
     * 快速记账模式
     * 用于简单的记账场景
     */
    fun quickParse(input: String): ParsedResult {
        return parse(input, emptyList())
    }
}
