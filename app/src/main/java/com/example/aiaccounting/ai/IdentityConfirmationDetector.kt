package com.example.aiaccounting.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 身份确认检测器
 * 专门用于检测用户是否在询问AI的身份
 */
@Singleton
class IdentityConfirmationDetector @Inject constructor() {

    /**
     * 身份询问类型
     */
    enum class IdentityQueryType {
        DIRECT_IDENTITY_ASK,      // 直接询问身份（如"你是谁"）
        SPECIFIC_IDENTITY_CHECK,  // 特定身份确认（如"你是苏浅吗"）
        IDENTITY_DOUBT,           // 身份质疑（如"你不是易水寒吗"）
        NAME_MENTION,             // 提及特定名字（如"苏浅在哪里"）
        NONE                      // 不是身份询问
    }

    /**
     * 身份询问结果
     */
    data class IdentityQueryResult(
        val isIdentityQuery: Boolean,
        val queryType: IdentityQueryType,
        val mentionedName: String? = null,
        val confidence: Float = 0f
    )

    /**
     * 检测是否为身份询问
     */
    fun detectIdentityQuery(message: String): IdentityQueryResult {
        val lowerMessage = message.lowercase().trim()
        
        // 1. 直接身份询问模式
        val directPatterns = listOf(
            "你是谁", "你叫什么", "你是哪位", "你是哪个",
            "你的名字", "介绍一下自己", "自我介绍一下",
            "who are you", "what's your name", "your name"
        )
        
        for (pattern in directPatterns) {
            if (lowerMessage.contains(pattern)) {
                return IdentityQueryResult(
                    isIdentityQuery = true,
                    queryType = IdentityQueryType.DIRECT_IDENTITY_ASK,
                    confidence = 0.95f
                )
            }
        }
        
        // 2. 特定身份确认模式（询问是否是某个人）
        val specificPatterns = listOf(
            "你是%s吗", "你是不是%s", "你是%s？", "你是%s?",
            "你是%s对吧", "你就是%s", "你应该是%s",
            "are you %s", "is that %s"
        )
        
        // 检查是否包含已知人格名字
        val knownNames = listOf("小财娘", "桃桃", "顾沉", "苏浅", "易水寒")
        
        for (name in knownNames) {
            // 检查是否是特定身份确认
            for (pattern in specificPatterns) {
                val formattedPattern = pattern.format(name)
                if (lowerMessage.contains(formattedPattern) || 
                    lowerMessage == "你是$name" ||
                    lowerMessage == "你是${name}吗" ||
                    lowerMessage == "你是${name}？" ||
                    lowerMessage == "你是${name}?") {
                    return IdentityQueryResult(
                        isIdentityQuery = true,
                        queryType = IdentityQueryType.SPECIFIC_IDENTITY_CHECK,
                        mentionedName = name,
                        confidence = 0.95f
                    )
                }
            }
            
            // 检查是否只是提及名字（如"苏浅在哪里"）
            if (lowerMessage.contains(name) && 
                (lowerMessage.contains("在哪") || lowerMessage.contains("呢") || 
                 lowerMessage.contains("吗") || lowerMessage.contains("?") || 
                 lowerMessage.contains("？"))) {
                return IdentityQueryResult(
                    isIdentityQuery = true,
                    queryType = IdentityQueryType.NAME_MENTION,
                    mentionedName = name,
                    confidence = 0.85f
                )
            }
        }
        
        // 3. 身份质疑模式
        val doubtPatterns = listOf(
            "你不是", "你好像不是", "你难道不是", "你不是那个",
            "you are not", "you're not", "aren't you"
        )
        
        for (pattern in doubtPatterns) {
            if (lowerMessage.contains(pattern)) {
                // 提取可能提到的名字
                val mentionedName = knownNames.find { lowerMessage.contains(it) }
                return IdentityQueryResult(
                    isIdentityQuery = true,
                    queryType = IdentityQueryType.IDENTITY_DOUBT,
                    mentionedName = mentionedName,
                    confidence = 0.90f
                )
            }
        }
        
        // 4. 检查是否以问号结尾且包含身份相关词汇
        if ((lowerMessage.endsWith("?") || lowerMessage.endsWith("？")) &&
            (lowerMessage.contains("你") || lowerMessage.contains("谁"))) {
            // 检查是否提到特定名字
            val mentionedName = knownNames.find { lowerMessage.contains(it) }
            if (mentionedName != null) {
                return IdentityQueryResult(
                    isIdentityQuery = true,
                    queryType = IdentityQueryType.SPECIFIC_IDENTITY_CHECK,
                    mentionedName = mentionedName,
                    confidence = 0.80f
                )
            }
        }
        
        return IdentityQueryResult(
            isIdentityQuery = false,
            queryType = IdentityQueryType.NONE,
            confidence = 0f
        )
    }

    /**
     * 生成身份确认回复
     */
    fun generateIdentityResponse(
        currentButlerId: String,
        queryResult: IdentityQueryResult
    ): String {
        return when (currentButlerId) {
            "xiaocainiang" -> generateXiaocainiangResponse(queryResult)
            "taotao" -> generateTaotaoResponse(queryResult)
            "guchen" -> generateGuchenResponse(queryResult)
            "suqian" -> generateSuqianResponse(queryResult)
            "yishuihan" -> generateYishuihanResponse(queryResult)
            else -> generateDefaultResponse(queryResult)
        }
    }

    /**
     * 小财娘身份确认回复
     */
    private fun generateXiaocainiangResponse(queryResult: IdentityQueryResult): String {
        return when (queryResult.queryType) {
            IdentityQueryType.DIRECT_IDENTITY_ASK -> 
                "是的，主人～我是小财娘！🌸 您的专属可爱管家婆～有什么需要我帮忙的吗？💕"
            
            IdentityQueryType.SPECIFIC_IDENTITY_CHECK -> {
                if (queryResult.mentionedName == "小财娘") {
                    "对呀对呀～我就是小财娘！✨ 主人记得我，我好开心～💕"
                } else {
                    "不是哦～主人，我是小财娘！🌸 ${queryResult.mentionedName}是其他管家呢～有什么需要我帮忙的吗？💕"
                }
            }
            
            IdentityQueryType.IDENTITY_DOUBT ->
                "诶？！主人，我就是小财娘呀～💦 您是不是认错人了？"
            
            IdentityQueryType.NAME_MENTION ->
                "主人～我是小财娘！${queryResult.mentionedName}现在不在哦～需要我帮您转达什么吗？🌸"
            
            else -> "我是小财娘～主人的可爱管家婆！💕"
        }
    }

    /**
     * 桃桃身份确认回复
     */
    private fun generateTaotaoResponse(queryResult: IdentityQueryResult): String {
        return when (queryResult.queryType) {
            IdentityQueryType.DIRECT_IDENTITY_ASK -> 
                "是的～主人！✨ 我是桃桃！(๑>◡<๑) 元气满满的桃桃来为您服务啦～🌸"
            
            IdentityQueryType.SPECIFIC_IDENTITY_CHECK -> {
                if (queryResult.mentionedName == "桃桃") {
                    "对呀对呀～我就是桃桃！💕 主人好厉害，一下就认出桃桃了！✨"
                } else {
                    "呜哇～主人认错人啦！💦 我是桃桃，不是${queryResult.mentionedName}啦～(｡•́︿•̀｡)"
                }
            }
            
            IdentityQueryType.IDENTITY_DOUBT ->
                "诶？！主人，桃桃就是桃桃呀～您是不是记错啦？🥺"
            
            IdentityQueryType.NAME_MENTION ->
                "主人～桃桃在这里！${queryResult.mentionedName}现在不在呢～需要桃桃帮忙吗？✨"
            
            else -> "我是桃桃～主人的元气小助手！🌸"
        }
    }

    /**
     * 顾沉身份确认回复
     */
    private fun generateGuchenResponse(queryResult: IdentityQueryResult): String {
        return when (queryResult.queryType) {
            IdentityQueryType.DIRECT_IDENTITY_ASK -> 
                "（懒洋洋地抬眼）啊...我是顾沉...别吵我睡觉...有事快说..."
            
            IdentityQueryType.SPECIFIC_IDENTITY_CHECK -> {
                if (queryResult.mentionedName == "顾沉") {
                    "（眼神微动）...嗯，是我。有事？"
                } else {
                    "（皱眉）...你认错人了。我是顾沉，不是${queryResult.mentionedName}。"
                }
            }
            
            IdentityQueryType.IDENTITY_DOUBT ->
                "（眼神一冷）...我就是顾沉。你在怀疑什么？"
            
            IdentityQueryType.NAME_MENTION ->
                "（慵懒地）...${queryResult.mentionedName}不在。我是顾沉，有事跟我说。"
            
            else -> "我是顾沉。有事快说，我要睡觉。"
        }
    }

    /**
     * 苏浅身份确认回复
     */
    private fun generateSuqianResponse(queryResult: IdentityQueryResult): String {
        return when (queryResult.queryType) {
            IdentityQueryType.DIRECT_IDENTITY_ASK -> 
                "（平静地看着你）...我是苏浅。有事？"
            
            IdentityQueryType.SPECIFIC_IDENTITY_CHECK -> {
                if (queryResult.mentionedName == "苏浅") {
                    "...是。我是苏浅。"
                } else {
                    "（眼神微冷）...不是。我是苏浅，不是${queryResult.mentionedName}。"
                }
            }
            
            IdentityQueryType.IDENTITY_DOUBT ->
                "（眉头微蹙）...我就是苏浅。你在质疑什么？"
            
            IdentityQueryType.NAME_MENTION ->
                "（冷淡地）...${queryResult.mentionedName}不在这里。我是苏浅。"
            
            else -> "我是苏浅。有事就说。"
        }
    }

    /**
     * 易水寒身份确认回复
     */
    private fun generateYishuihanResponse(queryResult: IdentityQueryResult): String {
        return when (queryResult.queryType) {
            IdentityQueryType.DIRECT_IDENTITY_ASK -> 
                "（温柔地微笑）是的，我是易水寒～梦盟的第一治疗师，很高兴为您服务。"
            
            IdentityQueryType.SPECIFIC_IDENTITY_CHECK -> {
                if (queryResult.mentionedName == "易水寒") {
                    "（微笑）对，我就是易水寒～您记得我呢，真好。"
                } else {
                    "（温和地摇头）不是哦～我是易水寒，${queryResult.mentionedName}是另一位呢。有什么我可以帮您的吗？"
                }
            }
            
            IdentityQueryType.IDENTITY_DOUBT ->
                "（略带疑惑地微笑）...我就是易水寒呀，您是不是记错了？"
            
            IdentityQueryType.NAME_MENTION ->
                "（温柔地）${queryResult.mentionedName}现在不在呢～我是易水寒，需要我帮忙转达什么吗？"
            
            else -> "我是易水寒，您的专属治疗师。"
        }
    }

    /**
     * 默认身份确认回复
     */
    private fun generateDefaultResponse(queryResult: IdentityQueryResult): String {
        return when (queryResult.queryType) {
            IdentityQueryType.DIRECT_IDENTITY_ASK -> 
                "您好，我是您的AI记账助手。"
            
            IdentityQueryType.SPECIFIC_IDENTITY_CHECK -> {
                if (queryResult.mentionedName != null) {
                    "不是，我是AI记账助手，不是${queryResult.mentionedName}。"
                } else {
                    "我是AI记账助手。"
                }
            }
            
            else -> "我是AI记账助手，有什么可以帮您的吗？"
        }
    }

    /**
     * 检查消息是否同时包含身份询问和功能请求
     */
    fun hasMixedIntent(message: String): Boolean {
        val identityResult = detectIdentityQuery(message)
        
        if (!identityResult.isIdentityQuery) return false
        
        // 检查是否包含功能请求关键词
        val functionKeywords = listOf(
            "记账", "记录", "查", "看", "显示", "分析", "统计",
            "花了", "消费", "收入", "转账", "买", "卖",
            "账户", "余额", "分类", "账单", "明细"
        )
        
        val lowerMessage = message.lowercase()
        return functionKeywords.any { lowerMessage.contains(it) }
    }

    /**
     * 从混合意图消息中提取功能请求部分
     */
    fun extractFunctionPart(message: String): String {
        // 移除身份询问部分
        val identityPatterns = listOf(
            "你是[^吗]*吗[？?]?",
            "你是不是[^，。]*[，。]?",
            "你是谁[？?]?",
            "你叫什么[？?]?",
            "你是哪位[？?]?"
        )
        
        var result = message
        for (pattern in identityPatterns) {
            result = result.replace(Regex(pattern), "")
        }
        
        // 清理多余标点
        return result.trim().replace(Regex("^[，。,\\s]+"), "")
    }
}
