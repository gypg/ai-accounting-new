package com.example.aiaccounting.ai

import com.example.aiaccounting.data.model.ButlerPersonaRegistry
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
    fun detectIdentityQuery(message: String, activeButlerName: String? = null): IdentityQueryResult {
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
        val knownNames = ButlerPersonaRegistry.knownNames(activeButlerName)
        fun findMentionedKnownName(): String? {
            return knownNames.firstOrNull { lowerMessage.contains(it.lowercase()) }
        }

        for (name in knownNames) {
            val normalizedName = name.lowercase()
            // 检查是否是特定身份确认
            for (pattern in specificPatterns) {
                val formattedPattern = pattern.format(normalizedName)
                if (lowerMessage.contains(formattedPattern) ||
                    lowerMessage == "你是$normalizedName" ||
                    lowerMessage == "你是${normalizedName}吗" ||
                    lowerMessage == "你是${normalizedName}？" ||
                    lowerMessage == "你是${normalizedName}?") {
                    return IdentityQueryResult(
                        isIdentityQuery = true,
                        queryType = IdentityQueryType.SPECIFIC_IDENTITY_CHECK,
                        mentionedName = name,
                        confidence = 0.95f
                    )
                }
            }

            // 检查是否只是提及名字（如"苏浅在哪里"）
            if (lowerMessage.contains(normalizedName) &&
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
                val mentionedName = findMentionedKnownName()
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
            val mentionedName = findMentionedKnownName()
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
        queryResult: IdentityQueryResult,
        activeButlerName: String? = null
    ): String {
        return ButlerPersonaRegistry.buildIdentityReply(
            butlerId = currentButlerId,
            queryType = queryResult.queryType,
            mentionedName = queryResult.mentionedName,
            activeButlerName = activeButlerName
        )
    }


    /**
     * 检查消息是否同时包含身份询问和功能请求
     */
    fun hasMixedIntent(message: String, activeButlerName: String? = null): Boolean {
        val identityResult = detectIdentityQuery(message, activeButlerName)
        
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
