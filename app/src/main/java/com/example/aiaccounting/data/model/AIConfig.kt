package com.example.aiaccounting.data.model

/**
 * AI配置数据类
 */
data class AIConfig(
    val provider: AIProvider = AIProvider.QWEN,
    val apiKey: String = "",
    val apiUrl: String = "",
    val model: String = "",
    val isEnabled: Boolean = false
) {
    companion object {
        const val KEY_PROVIDER = "ai_provider"
        const val KEY_API_KEY = "ai_api_key"
        const val KEY_API_URL = "ai_api_url"
        const val KEY_MODEL = "ai_model"
        const val KEY_ENABLED = "ai_enabled"
        const val KEY_USE_BUILTIN = "ai_use_builtin" // 是否使用内置配置
        const val KEY_GATEWAY_BASE_URL = "ai_gateway_base_url" // 邀请码网关地址
        const val KEY_INVITE_BOUND = "ai_invite_bound" // 是否通过邀请码绑定（用于隐藏 token / apiUrl 等敏感信息）

        // 邀请码绑定专用存储（避免与用户自定义 API Key 相互覆盖）
        const val KEY_INVITE_TOKEN = "ai_invite_token"
        const val KEY_INVITE_CODE = "ai_invite_code"
        const val KEY_INVITE_API_URL = "ai_invite_api_url"
        const val KEY_INVITE_MODEL = "ai_invite_model"
        const val KEY_INVITE_RPM = "ai_invite_rpm"

        // 模型选择模式（用于 Auto/手动选择）
        const val KEY_MODEL_MODE = "ai_model_mode" // AUTO | FIXED
        const val KEY_INVITE_MODEL_MODE = "ai_invite_model_mode" // AUTO | FIXED

        // 内置默认配置 - 预置的AI服务
        // 红月API - 无需用户手动配置，开箱即用
        val BUILTIN_CONFIG = AIConfig(
            provider = AIProvider.CUSTOM,
            apiKey = "",
            apiUrl = "https://hongyue.cloud/v1",
            model = "openai/gpt-oss-120b",
            isEnabled = true
        )

        // 默认配置
        fun defaultFor(provider: AIProvider): AIConfig {
            return when (provider) {
                AIProvider.QWEN -> AIConfig(
                    provider = AIProvider.QWEN,
                    apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    model = "qwen-turbo"
                )
                AIProvider.DEEPSEEK -> AIConfig(
                    provider = AIProvider.DEEPSEEK,
                    apiUrl = "https://api.deepseek.com/v1",
                    model = "deepseek-chat"
                )
                AIProvider.ZHIPU -> AIConfig(
                    provider = AIProvider.ZHIPU,
                    apiUrl = "https://open.bigmodel.cn/api/paas/v4",
                    model = "glm-4-flash"
                )
                AIProvider.BAIDU -> AIConfig(
                    provider = AIProvider.BAIDU,
                    apiUrl = "https://qianfan.baidubce.com/v2",
                    model = "ernie-speed"
                )
                AIProvider.CUSTOM -> AIConfig(
                    provider = AIProvider.CUSTOM,
                    apiUrl = "",
                    model = ""
                )
            }
        }

        // 获取实际使用的配置（优先使用内置配置）
        fun getEffectiveConfig(userConfig: AIConfig, useBuiltin: Boolean): AIConfig {
            return if (useBuiltin) {
                userConfig.copy(
                    provider = BUILTIN_CONFIG.provider,
                    apiKey = BUILTIN_CONFIG.apiKey,
                    apiUrl = BUILTIN_CONFIG.apiUrl,
                    model = BUILTIN_CONFIG.model,
                    isEnabled = true
                )
            } else {
                userConfig
            }
        }
    }
}

/**
 * AI提供商枚举 - 只保留国内知名厂商
 */
enum class AIProvider(
    val displayName: String,
    val description: String,
    val models: List<AIModel>,
    val apiType: ApiType
) {
    QWEN(
        "通义千问",
        "阿里云大模型",
        listOf(
            AIModel("qwen-turbo", "通义千问 Turbo", "速度快，成本低", true, "对话"),
            AIModel("qwen-plus", "通义千问 Plus", "平衡性能", false, "对话"),
            AIModel("qwen-max", "通义千问 Max", "最强能力", false, "对话"),
            AIModel("qwen-coder-turbo", "通义千问 Coder", "代码专用", true, "代码"),
            AIModel("qwen-vl-max", "通义千问 VL Max", "视觉理解", false, "视觉"),
            AIModel("qwen-vl-plus", "通义千问 VL Plus", "视觉增强", false, "视觉"),
            AIModel("qwen-math-plus", "通义千问 Math", "数学推理", false, "推理"),
            AIModel("qwen-long", "通义千问 Long", "长文本", false, "长文本")
        ),
        ApiType.OPENAI_COMPATIBLE
    ),
    DEEPSEEK(
        "DeepSeek",
        "深度求索",
        listOf(
            AIModel("deepseek-chat", "DeepSeek Chat", "通用对话", true, "对话"),
            AIModel("deepseek-coder", "DeepSeek Coder", "代码专用", true, "代码"),
            AIModel("deepseek-reasoner", "DeepSeek R1", "推理模型", true, "推理")
        ),
        ApiType.OPENAI_COMPATIBLE
    ),
    ZHIPU(
        "智谱AI",
        "ChatGLM大模型",
        listOf(
            AIModel("glm-4-flash", "GLM-4 Flash", "免费快速", true, "对话"),
            AIModel("glm-4-air", "GLM-4 Air", "高性价比", false, "对话"),
            AIModel("glm-4-airx", "GLM-4 AirX", "极速响应", false, "对话"),
            AIModel("glm-4", "GLM-4", "旗舰模型", false, "对话"),
            AIModel("glm-4v", "GLM-4V", "视觉理解", false, "视觉"),
            AIModel("glm-4-alltools", "GLM-4 All Tools", "工具调用", false, "工具"),
            AIModel("codegeex-4", "CodeGeeX-4", "代码生成", true, "代码"),
            AIModel("charglm-3", "CharGLM-3", "角色扮演", true, "角色")
        ),
        ApiType.OPENAI_COMPATIBLE
    ),
    BAIDU(
        "百度千帆",
        "文心一言",
        listOf(
            AIModel("ernie-speed", "ERNIE Speed", "免费快速", true, "对话"),
            AIModel("ernie-lite", "ERNIE Lite", "轻量免费", true, "对话"),
            AIModel("ernie-tiny", "ERNIE Tiny", "极速免费", true, "对话"),
            AIModel("ernie-4.0", "ERNIE 4.0", "旗舰模型", false, "对话"),
            AIModel("ernie-4.0-turbo", "ERNIE 4.0 Turbo", "增强版", false, "对话"),
            AIModel("ernie-3.5", "ERNIE 3.5", "标准版", false, "对话"),
            AIModel("ernie-character", "ERNIE Character", "角色扮演", true, "角色")
        ),
        ApiType.OPENAI_COMPATIBLE
    ),
    CUSTOM(
        "自定义",
        "接入其他OpenAI兼容API",
        emptyList(),
        ApiType.OPENAI_COMPATIBLE
    );

    companion object {
        fun fromString(value: String): AIProvider {
            return values().find { it.name == value } ?: QWEN
        }
    }
}

enum class ApiType {
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    GEMINI
}

/**
 * AI模型信息
 */
data class AIModel(
    val id: String,
    val displayName: String,
    val description: String,
    val isFree: Boolean = false,
    val category: String = "对话"
)

/**
 * 自定义模型信息（用于自定义提供商）
 */
data class CustomModel(
    val id: String,
    val displayName: String,
    val group: String,
    val apiType: String,
    val description: String = "",
    val isFree: Boolean = false
)

/**
 * AI对话消息
 */
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

/**
 * AI分析结果
 */
data class AIAnalysisResult(
    val summary: String,
    val suggestions: List<String>,
    val insights: List<String>
)
