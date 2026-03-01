package com.example.aiaccounting.data.remote

import com.example.aiaccounting.data.remote.dto.AIRequest
import com.example.aiaccounting.data.remote.dto.AIResponse
import com.example.aiaccounting.data.remote.dto.AIMessage
import com.example.aiaccounting.data.remote.dto.AIErrorResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for AI service interactions
 */
@Singleton
class AIRepository @Inject constructor() {

    private var apiKey: String = ""
    private var baseUrl: String = "https://api.openai.com/v1/"
    private var model: String = "gpt-3.5-turbo"
    private var temperature: Double = 0.7
    private var maxTokens: Int = 1000

    private var aiService: AIService? = null
    private val gson = Gson()

    /**
     * Configure AI service
     */
    fun configure(
        apiKey: String,
        baseUrl: String = "https://api.api.openai.com/v1/",
        model: String = "gpt-3.5-turbo",
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ) {
        this.apiKey = apiKey
        this.baseUrl = baseUrl
        this.model = model
        this.temperature = temperature
        this.maxTokens = maxTokens

        // Create new service instance
        createService()
    }

    /**
     * Create AI service instance
     */
    private fun createService() {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        aiService = retrofit.create(AIService::class.java)
    }

    /**
     * Check if AI service is configured
     */
    fun isConfigured(): Boolean {
        return apiKey.isNotEmpty() && aiService != null
    }

    /**
     * Send message to AI
     */
    suspend fun sendMessage(
        messages: List<AIMessage>,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("AI服务未配置"))
            }

            val finalMessages = if (systemPrompt != null) {
                listOf(AIMessage("system", systemPrompt)) + messages
            } else {
                messages
            }

            val request = AIRequest(
                model = model,
                messages = finalMessages,
                temperature = temperature,
                maxTokens = maxTokens
            )

            val response = aiService!!.chatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val aiResponse = response.body()!!
                val assistantMessage = aiResponse.choices.firstOrNull()?.message?.content
                    ?: "抱歉，我无法理解您的问题。"

                Result.success(assistantMessage)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val errorResponse = gson.fromJson(errorBody, AIErrorResponse::class.java)
                    errorResponse.error.message
                } catch (e: Exception) {
                    "API请求失败: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse transaction from natural language
     */
    suspend fun parseTransaction(userInput: String): Result<ParsedTransaction> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("AI服务未配置"))
            }

            val prompt = """
                你是一个记账助手。请从用户的输入中提取交易信息，并以JSON格式返回。
                
                用户输入：$userInput
                
                请提取以下信息：
                - type: 交易类型（"income"或"expense"）
                - amount: 金额（数字）
                - category: 分类（如"餐饮"、"交通"、"购物"等）
                - date: 日期（如"今天"、"昨天"、"2024-02-28"等，如果未指定则为"今天"）
                - note: 备注（如果有的话）
                
                返回格式：
                {
                    "type": "expense",
                    "amount": 25.0,
                    "category": "餐饮",
                    "date": "今天",
                    "note": "午饭"
                }
                
                如果无法识别，返回：
                {
                    "error": "无法识别交易信息"
                }
            """.trimIndent()

            val messages = listOf(AIMessage("user", prompt))
            val response = sendMessage(messages)

            response.fold(
                onSuccess = { aiResponse ->
                    try {
                        val jsonStart = aiResponse.indexOf("{")
                        val jsonEnd = aiResponse.lastIndexOf("}") + 1
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            val json = aiResponse.substring(jsonStart, jsonEnd)
                            val parsed = gson.fromJson(json, ParsedTransaction::class.java)
                            
                            if (parsed.error != null) {
                                Result.failure(Exception(parsed.error))
                            } else {
                                Result.success(parsed)
                            }
                        } else {
                            Result.failure(Exception("无法解析AI响应"))
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get default system prompt
     */
    fun getDefaultSystemPrompt(): String {
        return """
            你是一个智能记账助手。你的任务是帮助用户记录和管理财务信息。
            
            你可以：
            1. 帮助用户记录收入和支出
            2. 回答关于财务状况的问题
            3. 提供财务建议
            4. 分析消费习惯
            
            当用户说类似"今天午饭花了25元"时，你应该识别出：
            - 类型：支出
            - 金额：25
            - 分类：餐饮
            - 备注：午饭
            
            当用户问"这个月花了多少钱"时，你应该帮助查询相关数据。
            
            请用简洁、友好的方式回复用户。
        """.trimIndent()
    }

    /**
     * Get current configuration
     */
    fun getConfiguration(): AIConfiguration {
        return AIConfiguration(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens
        )
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-3.5-turbo"
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1/"
        const val DEFAULT_TEMPERATURE = 0.7
        const val DEFAULT_MAX_TOKENS = 1000
    }
}

/**
 * Parsed transaction data class
AI解析的交易信息
 */
data class ParsedTransaction(
    val type: String? = null,  // "income" or "expense"
    val amount: Double? = null,
    val category: String? = null,
    val date: String? = null,
    val note: String? = null,
    val error: String? = null
)

/**
 * AI Configuration data class
 */
data class AIConfiguration(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val temperature: Double,
    val maxTokens: Int
)
