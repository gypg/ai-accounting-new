package com.example.aiaccounting.data.service

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.aiaccounting.data.model.AIAnalysisResult
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import com.example.aiaccounting.di.AiOkHttpClient
import com.example.aiaccounting.di.AiTestOkHttpClient
import com.example.aiaccounting.utils.OpenAiUrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI服务 - 处理大模型API调用
 */
@Singleton
class AIService @Inject constructor(
    @AiOkHttpClient private val client: OkHttpClient,
    @AiTestOkHttpClient private val testClient: OkHttpClient
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 发送对话请求（流式）
     * 注意：当前实现为非流式，但保留流式接口以便未来扩展
     */
    fun sendChatStream(
        messages: List<ChatMessage>,
        config: AIConfig
    ): Flow<String> = flow {
        if (!config.isEnabled || config.apiKey.isBlank()) {
            emit("请先配置AI API密钥")
            return@flow
        }

        try {
            val response = when (config.provider) {
                AIProvider.QWEN, AIProvider.DEEPSEEK, AIProvider.ZHIPU, AIProvider.BAIDU, AIProvider.CUSTOM -> sendOpenAIChatStream(messages, config)
            }
            emit(response)
        } catch (e: Exception) {
            emit("请求失败: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 分析账单数据
     */
    suspend fun analyzeTransactions(
        transactionSummary: String,
        config: AIConfig
    ): AIAnalysisResult = withContext(Dispatchers.IO) {
        if (!config.isEnabled || config.apiKey.isBlank()) {
            return@withContext AIAnalysisResult(
                summary = "AI分析功能未启用",
                suggestions = emptyList(),
                insights = emptyList()
            )
        }

        val systemPrompt = """
            你是一位专业的财务分析师。请根据用户的账单数据提供简洁的分析和建议。
            请以JSON格式返回，包含以下字段：
            - summary: 总体情况总结（一句话）
            - suggestions: 理财建议列表（3-5条）
            - insights: 数据洞察列表（2-3条）
        """.trimIndent()

        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, systemPrompt),
            ChatMessage(MessageRole.USER, "请分析以下账单数据：\n$transactionSummary")
        )

        try {
            val response = when (config.provider) {
                AIProvider.QWEN, AIProvider.DEEPSEEK, AIProvider.ZHIPU, AIProvider.BAIDU, AIProvider.CUSTOM -> sendOpenAIChat(messages, config)
            }

            // 尝试解析JSON响应
            parseAnalysisResponse(response)
        } catch (e: Exception) {
            AIAnalysisResult(
                summary = "分析失败: ${e.message}",
                suggestions = listOf("请检查API配置是否正确"),
                insights = emptyList()
            )
        }
    }

    /**
     * 智能记账 - 从自然语言提取记账信息
     */
    suspend fun parseTransactionFromText(
        text: String,
        config: AIConfig
    ): ParsedTransaction? = withContext(Dispatchers.IO) {
        if (!config.isEnabled || config.apiKey.isBlank()) {
            return@withContext null
        }

        val systemPrompt = """
            你是一个智能记账助手。从用户的自然语言描述中提取记账信息。
            请以JSON格式返回，包含以下字段：
            - amount: 金额（数字）
            - type: 类型（"income"收入 或 "expense"支出）
            - category: 分类（如：餐饮、交通、购物、工资等）
            - note: 备注信息
            如果无法提取，返回null。
        """.trimIndent()

        val messages = listOf(
            ChatMessage(MessageRole.SYSTEM, systemPrompt),
            ChatMessage(MessageRole.USER, text)
        )

        try {
            val response = when (config.provider) {
                AIProvider.QWEN, AIProvider.DEEPSEEK, AIProvider.ZHIPU, AIProvider.BAIDU, AIProvider.CUSTOM -> sendOpenAIChat(messages, config)
            }

            parseTransactionResponse(response)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取可用模型列表
     * @return 模型列表，失败返回空列表
     */
    suspend fun fetchModels(config: AIConfig): List<RemoteModel> = withContext(Dispatchers.IO) {
        if (!config.isEnabled || config.apiKey.isBlank()) {
            return@withContext emptyList()
        }

        try {
            when (config.provider) {
                AIProvider.QWEN, AIProvider.DEEPSEEK, AIProvider.ZHIPU, AIProvider.BAIDU, AIProvider.CUSTOM -> fetchOpenAIModels(config)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun fetchOpenAIModels(config: AIConfig): List<RemoteModel> {
        val url = OpenAiUrlUtils.models(config.apiUrl)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("Content-Type", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("获取模型列表失败: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw Exception("空响应")
            val json = JSONObject(responseBody)
            val data = json.getJSONArray("data")
            
            val models = mutableListOf<RemoteModel>()
            for (i in 0 until data.length()) {
                val modelObj = data.getJSONObject(i)
                val id = modelObj.optString("id", "")
                // 获取所有非空ID的模型，不过滤（OpenRouter有400+模型）
                if (id.isNotBlank()) {
                    models.add(RemoteModel(
                        id = id,
                        name = modelObj.optString("name", id),
                        description = modelObj.optString("description", modelObj.optString("object", "model"))
                    ))
                }
            }
            return models
        }
    }

    private fun fetchClaudeModels(config: AIConfig): List<RemoteModel> {
        // Claude API没有公开的模型列表端点，返回默认模型
        return listOf(
            RemoteModel("claude-3-haiku-20240307", "Claude 3 Haiku", "速度快，成本低"),
            RemoteModel("claude-3-sonnet-20240229", "Claude 3 Sonnet", "平衡性能和成本"),
            RemoteModel("claude-3-opus-20240229", "Claude 3 Opus", "最强能力"),
            RemoteModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "最新版本")
        )
    }

    private fun fetchGeminiModels(config: AIConfig): List<RemoteModel> {
        // Gemini API需要特殊处理，返回默认模型
        return listOf(
            RemoteModel("gemini-1.5-flash", "Gemini 1.5 Flash", "速度快，效率高"),
            RemoteModel("gemini-1.5-pro", "Gemini 1.5 Pro", "能力强，长上下文"),
            RemoteModel("gemini-pro", "Gemini Pro", "标准版本"),
            RemoteModel("gemini-ultra", "Gemini Ultra", "最强能力")
        )
    }

    /**
     * 测试API连接
     * @return 测试结果，成功返回null，失败返回错误信息
     */
    suspend fun testConnection(config: AIConfig): String? = withContext(Dispatchers.IO) {
        if (!config.isEnabled) {
            return@withContext "AI助手未启用"
        }

        if (config.apiKey.isBlank()) {
            return@withContext "API密钥不能为空"
        }

        if (config.apiUrl.isBlank()) {
            return@withContext "API地址不能为空"
        }

        try {
            // 发送一个简单的测试请求
            val testMessages = listOf(
                ChatMessage(MessageRole.USER, "Hi")
            )

            when (config.provider) {
                AIProvider.QWEN, AIProvider.DEEPSEEK, AIProvider.ZHIPU, AIProvider.BAIDU, AIProvider.CUSTOM -> testOpenAIConnection(config, testMessages)
            }

            // 如果成功执行没有抛出异常，则连接成功
            null
        } catch (e: Exception) {
            // 返回具体的错误信息
            when {
                e.message?.contains("401") == true -> "API密钥无效或已过期"
                e.message?.contains("403") == true -> "没有权限访问该API"
                e.message?.contains("404") == true -> "API地址不存在，请检查URL"
                e.message?.contains("429") == true -> "请求过于频繁，请稍后再试"
                e.message?.contains("500") == true || e.message?.contains("502") == true || e.message?.contains("503") == true -> "服务器错误，请稍后再试"
                e.message?.contains("UnknownHostException") == true || e.message?.contains("无法解析主机") == true -> "无法连接到服务器，请检查网络或API地址"
                e.message?.contains("ConnectException") == true || e.message?.contains("连接") == true -> "连接失败，请检查网络"
                e.message?.contains("SSL") == true || e.message?.contains("证书") == true -> "SSL证书错误"
                e.message?.contains("timeout") == true || e.message?.contains("超时") == true -> "连接超时，请检查网络"
                else -> "连接失败: ${e.message ?: "未知错误"}"
            }
        }
    }

    private fun testOpenAIConnection(config: AIConfig, messages: List<ChatMessage>) {
        val url = OpenAiUrlUtils.chatCompletions(config.apiUrl)

        // Use minimal payload to reduce latency/cost.
        val messagesArray = JSONArray()
        val first = messages.firstOrNull { it.role == MessageRole.USER }?.content?.ifBlank { "Hi" } ?: "Hi"
        messagesArray.put(
            JSONObject().apply {
                put("role", "user")
                put("content", first)
            }
        )

        val primaryModel = config.model.trim()
        val modelToUse = if (primaryModel.isNotBlank()) {
            primaryModel
        } else {
            val ids = fetchRemoteModelIds(config)
            pickPreferredModel(ids, exclude = emptySet())
                ?: throw Exception("无法获取可用模型列表，请稍后重试")
        }

        val requestBody = JSONObject().apply {
            put("model", modelToUse)
            put("messages", messagesArray)
            put("max_tokens", 1)
            put("temperature", 0)
            put("stream", false)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        testClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw Exception("API请求失败(${response.code}): $errorBody")
            }

            val responseBody = response.body?.string() ?: throw Exception("空响应")
            val json = JSONObject(responseBody)
            if (!json.has("choices")) {
                throw Exception("无效的响应格式")
            }
        }
    }

    private fun testClaudeConnection(config: AIConfig, messages: List<ChatMessage>) {
        val url = "${config.apiUrl}/messages"

        val messagesArray = JSONArray()
        messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "assistant")
                put("content", msg.content)
            })
        }

        val requestBody = JSONObject().apply {
            put("model", config.model.ifEmpty { "claude-3-sonnet-20240229" })
            put("max_tokens", 5)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API请求失败: ${response.code}")
            }
            val responseBody = response.body?.string() ?: throw Exception("空响应")
            val json = JSONObject(responseBody)
            if (!json.has("content")) {
                throw Exception("无效的响应格式")
            }
        }
    }

    private fun testGeminiConnection(config: AIConfig, messages: List<ChatMessage>) {
        val model = config.model.ifEmpty { "gemini-pro" }
        val url = "${config.apiUrl}/models/$model:generateContent?key=${config.apiKey}"

        val contentsArray = JSONArray()
        messages.forEach { msg ->
            contentsArray.put(JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "model")
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", msg.content)
                }))
            })
        }

        val requestBody = JSONObject().apply {
            put("contents", contentsArray)
        }

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API请求失败: ${response.code}")
            }
            val responseBody = response.body?.string() ?: throw Exception("空响应")
            val json = JSONObject(responseBody)
            if (!json.has("candidates")) {
                throw Exception("无效的响应格式")
            }
        }
    }

    /**
     * OpenAI API调用（流式版本）
     * 尝试使用流式API，如果不支持则回退到非流式
     */
    private fun sendOpenAIChatStream(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String {
        // Current implementation is non-streaming under the hood. If AUTO is enabled (model is blank),
        // resolve a model via sendOpenAIChatNonStream directly to avoid an extra failing stream attempt.
        if (config.model.isBlank()) {
            return sendOpenAIChatNonStream(messages, config)
        }

        val url = OpenAiUrlUtils.chatCompletions(config.apiUrl)

        val messagesArray = JSONArray()
        messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role.name.lowercase())
                put("content", msg.content)
            })
        }

        // 首先尝试流式请求
        val streamRequestBody = JSONObject().apply {
            put("model", config.model)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 2000)
            put("stream", true)
        }

        val streamRequest = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(streamRequestBody.toString().toRequestBody(jsonMediaType))
            .build()

        return try {
            // 尝试流式请求
            client.newCall(streamRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    // 如果流式请求失败，回退到非流式
                    return sendOpenAIChatNonStream(messages, config)
                }

                val responseBody = response.body?.string() ?: throw Exception("空响应")
                
                // 解析SSE格式的流式响应
                val content = StringBuilder()
                responseBody.lines().forEach { line ->
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        if (data != "[DONE]") {
                            try {
                                val json = JSONObject(data)
                                val choices = json.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                    val text = delta?.optString("content", "")
                                    if (!text.isNullOrEmpty()) {
                                        content.append(text)
                                    }
                                }
                            } catch (e: Exception) {
                                // 忽略解析错误
                            }
                        }
                    }
                }
                
                if (content.isNotEmpty()) {
                    content.toString()
                } else {
                    // 如果流式解析失败，回退到非流式
                    sendOpenAIChatNonStream(messages, config)
                }
            }
        } catch (e: Exception) {
            // 流式请求失败，回退到非流式
            sendOpenAIChatNonStream(messages, config)
        }
    }

    private sealed class OpenAIChatFailure(message: String) : Exception(message) {
        data class ModelUnavailable(val details: String) : OpenAIChatFailure(details)
        data class Other(val details: String) : OpenAIChatFailure(details)
    }

    private fun isModelUnavailable(responseCode: Int, responseBody: String?): Boolean {
        // Strong signals
        if (responseCode == 404) return true

        val body = responseBody.orEmpty().lowercase()
        // Common OpenAI/OpenRouter style messages
        return body.contains("model") && (
            body.contains("not found") ||
                body.contains("does not exist") ||
                body.contains("unknown model") ||
                body.contains("invalid model") ||
                body.contains("model_not_found") ||
                body.contains("no such model")
            )
    }

    private fun sendOpenAIChatNonStreamOnce(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String {
        val url = OpenAiUrlUtils.chatCompletions(config.apiUrl)

        val messagesArray = JSONArray()
        messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role.name.lowercase())
                put("content", msg.content)
            })
        }

        val requestBody = JSONObject().apply {
            // Keep AUTO semantics consistent: when model is blank, this call site should already have
            // resolved a model (sendOpenAIChatNonStream resolves via /v1/models).
            put("model", config.model)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 2000)
            put("stream", false)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val rawBody = response.body?.string()

            if (!response.isSuccessful) {
                if (isModelUnavailable(response.code, rawBody)) {
                    throw OpenAIChatFailure.ModelUnavailable("model_unavailable: http=${response.code} body=${rawBody.orEmpty()}")
                }
                throw OpenAIChatFailure.Other("API请求失败: ${response.code}")
            }

            if (rawBody.isNullOrEmpty()) {
                throw OpenAIChatFailure.Other("空响应")
            }

            val json = JSONObject(rawBody)

            // 检查是否有错误
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                val message = error.optString("message", "未知错误")
                if (isModelUnavailable(responseCode = 200, responseBody = rawBody)) {
                    throw OpenAIChatFailure.ModelUnavailable(message)
                }
                throw OpenAIChatFailure.Other(message)
            }

            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                return choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
            throw OpenAIChatFailure.Other("无效的响应格式")
        }
    }

    private fun fetchRemoteModelIds(config: AIConfig): List<String> {
        return fetchOpenAIModels(config).map { it.id }.filter { it.isNotBlank() }
    }

    private fun pickPreferredModel(remoteModelIds: List<String>, exclude: Set<String>): String? {
        val recommended = "openai/gpt-oss-120b"

        if (recommended !in exclude && recommended in remoteModelIds) return recommended

        return remoteModelIds.firstOrNull { it !in exclude }
    }

    private fun sendOpenAIChatNonStream(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String {
        // Auto fallback: within one request, at most 2 tries with different models.
        // Candidate pool is strictly from remote /v1/models list (per product decision).

        val primaryModel = config.model.trim()

        val remoteModelIds: List<String>?
        val firstModel = if (primaryModel.isNotBlank()) {
            remoteModelIds = null
            primaryModel
        } else {
            val ids = try {
                fetchRemoteModelIds(config)
            } catch (e: Exception) {
                throw OpenAIChatFailure.Other("无法获取可用模型列表，请稍后重试")
            }
            remoteModelIds = ids
            pickPreferredModel(ids, exclude = emptySet())
                ?: throw OpenAIChatFailure.Other("无法获取可用模型列表，请稍后重试")
        }

        try {
            return sendOpenAIChatNonStreamOnce(messages, config.copy(model = firstModel))
        } catch (e: OpenAIChatFailure.ModelUnavailable) {
            // Continue to fallback
        }

        // 2nd attempt (N=2): pick next candidate from /v1/models, excluding first.
        val idsForFallback = remoteModelIds ?: try {
            fetchRemoteModelIds(config)
        } catch (e: Exception) {
            throw OpenAIChatFailure.Other("无法获取可用模型列表，请稍后重试")
        }

        val secondModel = pickPreferredModel(idsForFallback, exclude = setOf(firstModel))
            ?: throw OpenAIChatFailure.ModelUnavailable("model_unavailable: no alternative model")

        return sendOpenAIChatNonStreamOnce(messages, config.copy(model = secondModel))
    }

    /**
     * OpenAI API调用（非流式版本）
     */
    private fun sendOpenAIChatNonStream_LEGACY(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String {
        val url = OpenAiUrlUtils.chatCompletions(config.apiUrl)

        val messagesArray = JSONArray()
        messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role.name.lowercase())
                put("content", msg.content)
            })
        }

        val requestBody = JSONObject().apply {
            // Keep AUTO semantics consistent: when model is blank, this call site should already have
            // resolved a model (sendOpenAIChatNonStream resolves via /v1/models).
            put("model", config.model)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 2000)
            put("stream", false)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${config.apiKey.trim()}")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API请求失败: ${response.code}")
            }

            response.body?.use { body ->
                val responseBody = body.string()
                if (responseBody.isNullOrEmpty()) {
                    throw Exception("空响应")
                }

                val json = JSONObject(responseBody)

                // 检查是否有错误
                if (json.has("error")) {
                    val error = json.getJSONObject("error")
                    throw Exception(error.optString("message", "未知错误"))
                }

                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    return choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                }
                throw Exception("无效的响应格式")
            } ?: throw Exception("响应体为空")
        }
    }

    /**
     * OpenAI API调用（兼容旧版本）
     */
    private fun sendOpenAIChat(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String {
        return sendOpenAIChatNonStream(messages, config)
    }

    /**
     * 公共聊天API - 用于普通模型处理OCR后的文本
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String = withContext(Dispatchers.IO) {
        when (config.provider) {
            AIProvider.QWEN, AIProvider.DEEPSEEK, AIProvider.ZHIPU, AIProvider.BAIDU, AIProvider.CUSTOM -> 
                sendOpenAIChatNonStream(messages, config)
        }
    }

    /**
     * Claude API调用
     */
    private fun sendClaudeChat(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String {
        val url = "${config.apiUrl}/messages"

        // 分离系统消息和用户消息
        val systemMessage = messages.find { it.role == MessageRole.SYSTEM }?.content ?: ""
        val userMessages = messages.filter { it.role != MessageRole.SYSTEM }

        val messagesArray = JSONArray()
        userMessages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "assistant")
                put("content", msg.content)
            })
        }

        val requestBody = JSONObject().apply {
            put("model", config.model.ifEmpty { "claude-3-sonnet-20240229" })
            put("max_tokens", 2000)
            put("messages", messagesArray)
            if (systemMessage.isNotEmpty()) {
                put("system", systemMessage)
            }
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API请求失败: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw Exception("空响应")
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("content")
            if (content.length() > 0) {
                return content.getJSONObject(0).getString("text")
            }
            throw Exception("无效的响应格式")
        }
    }

    /**
     * Gemini API调用
     */
    private fun sendGeminiChat(
        messages: List<ChatMessage>,
        config: AIConfig
    ): String {
        val model = config.model.ifEmpty { "gemini-pro" }
        val url = "${config.apiUrl}/models/$model:generateContent?key=${config.apiKey}"

        // 构建Gemini格式的内容
        val contentsArray = JSONArray()
        messages.filter { it.role != MessageRole.SYSTEM }.forEach { msg ->
            contentsArray.put(JSONObject().apply {
                put("role", if (msg.role == MessageRole.USER) "user" else "model")
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", msg.content)
                }))
            })
        }

        val requestBody = JSONObject().apply {
            put("contents", contentsArray)
        }

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API请求失败: ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw Exception("空响应")
            val json = JSONObject(responseBody)
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text")
                }
            }
            throw Exception("无效的响应格式")
        }
    }

    /**
     * 解析分析响应
     */
    private fun parseAnalysisResponse(response: String): AIAnalysisResult {
        return try {
            // 尝试从响应中提取JSON
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}")

            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonStr = response.substring(jsonStart, jsonEnd + 1)
                val json = JSONObject(jsonStr)

                AIAnalysisResult(
                    summary = json.optString("summary", "分析完成"),
                    suggestions = json.optJSONArray("suggestions")?.let { arr ->
                        List(arr.length()) { arr.getString(it) }
                    } ?: emptyList(),
                    insights = json.optJSONArray("insights")?.let { arr ->
                        List(arr.length()) { arr.getString(it) }
                    } ?: emptyList()
                )
            } else {
                // 如果不是JSON格式，使用文本作为总结
                AIAnalysisResult(
                    summary = response.take(200),
                    suggestions = emptyList(),
                    insights = emptyList()
                )
            }
        } catch (e: Exception) {
            AIAnalysisResult(
                summary = response.take(200),
                suggestions = emptyList(),
                insights = emptyList()
            )
        }
    }

    /**
 * 解析交易响应
 */
private fun parseTransactionResponse(response: String): ParsedTransaction? {
    return try {
        val jsonStart = response.indexOf("{")
        val jsonEnd = response.lastIndexOf("}")

        if (jsonStart != -1 && jsonEnd != -1) {
            val jsonStr = response.substring(jsonStart, jsonEnd + 1)
            val json = JSONObject(jsonStr)

            ParsedTransaction(
                amount = json.optDouble("amount", 0.0),
                type = json.optString("type", "expense"),
                category = json.optString("category", "其他"),
                note = json.optString("note", "")
            )
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 识别图片内容并自动记账
 * @param imageUri 图片URI
 * @param config AI配置
 * @param context 上下文
 * @return 识别结果，包含是否成功、识别到的内容和记账操作
 */
suspend fun analyzeImageAndRecord(
    imageUri: Uri,
    config: AIConfig,
    context: Context
): ImageAnalysisResult = withContext(Dispatchers.IO) {
    if (!config.isEnabled || config.apiKey.isBlank()) {
        return@withContext ImageAnalysisResult(
            success = false,
            message = "请先配置AI API密钥",
            actions = null
        )
    }

    try {
        // 读取图片并转换为base64
        val base64Image = uriToBase64(imageUri, context)
            ?: return@withContext ImageAnalysisResult(
                success = false,
                message = "无法读取图片",
                actions = null
            )

        // 构建包含图片的消息
        val systemPrompt = """
你是"小财娘"，一位可爱又贴心的管家婆AI助手 🌸

【你的任务】
分析用户提供的图片，识别其中的消费信息（如收据、账单、购物小票等），并自动记账。

【需要识别的信息】
1. 消费金额
2. 消费类型（收入/支出）
3. 消费分类（餐饮、交通、购物、娱乐等）
4. 消费描述/备注

【回复格式】
请以JSON格式返回：
```json
{
  "success": true,
  "description": "图片内容描述",
  "actions": [
    {"action": "add_transaction", "amount": 金额, "type": "expense", "category": "分类名", "account": "账户名", "note": "备注"}
  ],
  "reply": "主人～小财娘从图片中识别到消费信息啦！🌸\\n\\n已为您记账：XXX元用于XXX"
}
```

如果图片中没有识别到消费信息：
```json
{
  "success": false,
  "description": "图片内容描述",
  "actions": [],
  "reply": "主人～小财娘仔细看了一下图片，没有识别到消费信息呢。您可以手动告诉我这笔消费的具体内容哦～💕"
}
```

如果图片无法识别或不清晰：
```json
{
  "success": false,
  "description": "图片不清晰或无法识别",
  "actions": [],
  "reply": "主人～图片有点模糊呢，小财娘看不太清楚。您可以重新上传一张清晰的图片，或者直接告诉我消费内容～💕"
}
```

请用可爱管家的语气回复，多使用emoji表情～
        """.trimIndent()

        val result = when (config.provider) {
            AIProvider.QWEN, AIProvider.DEEPSEEK, AIProvider.ZHIPU, AIProvider.BAIDU, AIProvider.CUSTOM -> {
                try {
                    sendOpenAIImageRequest(base64Image, systemPrompt, config)
                } catch (e: Exception) {
                    if (e.message == "UNSUPPORTED_MODEL") {
                        // Best-effort fallback: try another model from /v1/models.
                        // NOTE: Keep retry count to 1 to avoid heavy multi-tries on large model pools.
                        val ids = fetchRemoteModelIds(config)
                        val fallback = pickPreferredModel(ids, exclude = setOf(config.model.trim()))
                            ?: throw e
                        sendOpenAIImageRequest(base64Image, systemPrompt, config.copy(model = fallback))
                    } else {
                        throw e
                    }
                }
            }
        }

        // 解析结果
        parseImageAnalysisResult(result)
    } catch (e: Exception) {
        ImageAnalysisResult(
            success = false,
            message = "图片识别失败: ${e.message}",
            actions = null
        )
    }
}

/**
 * 将URI转换为Base64编码的图片
 */
private fun uriToBase64(uri: Uri, context: Context): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            val imageBytes = outputStream.toByteArray()
            // 压缩图片，限制大小
            val compressedBytes = compressImageIfNeeded(imageBytes)
            Base64.encodeToString(compressedBytes, Base64.DEFAULT)
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 压缩图片（如果需要）
 * 使用JPEG压缩算法，确保图片质量的同时限制大小
 */
private fun compressImageIfNeeded(imageBytes: ByteArray): ByteArray {
    // 如果图片小于1MB，直接返回
    if (imageBytes.size < 1024 * 1024) {
        return imageBytes
    }
    
    return try {
        // 解码图片
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return imageBytes // 解码失败返回原图
        
        // 如果图片尺寸过大，先缩小尺寸
        val maxDimension = 1920 // 最大边长
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
                if (it != bitmap) bitmap.recycle()
            }
        } else {
            bitmap
        }
        
        // 使用JPEG压缩
        val outputStream = java.io.ByteArrayOutputStream()
        var quality = 90
        do {
            outputStream.reset()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() > 1024 * 1024 && quality > 30)
        
        // 回收Bitmap
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        
        outputStream.toByteArray()
    } catch (e: Exception) {
        // 压缩失败返回原图
        imageBytes
    }
}

/**
 * OpenAI图片识别请求
 */
private fun sendOpenAIImageRequest(
    base64Image: String,
    systemPrompt: String,
    config: AIConfig
): String {
    val url = OpenAiUrlUtils.chatCompletions(config.apiUrl)

    val messagesArray = JSONArray()
    
    // 系统消息
    messagesArray.put(JSONObject().apply {
        put("role", "system")
        put("content", systemPrompt)
    })
    
    // 用户消息（包含图片）
    val contentArray = JSONArray()
    contentArray.put(JSONObject().apply {
        put("type", "text")
        put("text", "请分析这张图片中的消费信息")
    })
    contentArray.put(JSONObject().apply {
        put("type", "image_url")
        put("image_url", JSONObject().apply {
            put("url", "data:image/jpeg;base64,$base64Image")
        })
    })
    
    messagesArray.put(JSONObject().apply {
        put("role", "user")
        put("content", contentArray)
    })

    val requestBody = JSONObject().apply {
        // Keep AUTO semantics consistent with chat(): if model is blank, AIService will resolve a model based on /v1/models.
        put("model", config.model)
        put("messages", messagesArray)
        put("temperature", 0.7)
        put("max_tokens", 2000)
    }

    val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer ${config.apiKey.trim()}")
        .header("Content-Type", "application/json")
        .post(requestBody.toString().toRequestBody(jsonMediaType))
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            // 检查是否是模型不支持图片
            if (errorBody.contains("does not support images") || 
                errorBody.contains("image") || 
                response.code == 400) {
                throw Exception("UNSUPPORTED_MODEL")
            }
            throw Exception("API请求失败(${response.code}): $errorBody")
        }

        val responseBody = response.body?.string() ?: throw Exception("空响应")
        val json = JSONObject(responseBody)
        
        if (json.has("error")) {
            val error = json.getJSONObject("error")
            throw Exception(error.optString("message", "未知错误"))
        }
        
        val choices = json.getJSONArray("choices")
        if (choices.length() > 0) {
            return choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
        throw Exception("无效的响应格式")
    }
}

/**
 * Claude图片识别请求
 */
private fun sendClaudeImageRequest(
    base64Image: String,
    systemPrompt: String,
    config: AIConfig
): String {
    val url = "${config.apiUrl}/messages"

    val contentArray = JSONArray()
    contentArray.put(JSONObject().apply {
        put("type", "text")
        put("text", "请分析这张图片中的消费信息")
    })
    contentArray.put(JSONObject().apply {
        put("type", "image")
        put("source", JSONObject().apply {
            put("type", "base64")
            put("media_type", "image/jpeg")
            put("data", base64Image)
        })
    })

    val messagesArray = JSONArray()
    messagesArray.put(JSONObject().apply {
        put("role", "user")
        put("content", contentArray)
    })

    val requestBody = JSONObject().apply {
        put("model", config.model.ifEmpty { "claude-3-sonnet-20240229" })
        put("max_tokens", 2000)
        put("messages", messagesArray)
        if (systemPrompt.isNotEmpty()) {
            put("system", systemPrompt)
        }
    }

    val request = Request.Builder()
        .url(url)
        .header("x-api-key", config.apiKey)
        .header("anthropic-version", "2023-06-01")
        .header("Content-Type", "application/json")
        .post(requestBody.toString().toRequestBody(jsonMediaType))
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            if (errorBody.contains("image") || response.code == 400) {
                throw Exception("UNSUPPORTED_MODEL")
            }
            throw Exception("API请求失败: ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw Exception("空响应")
        val json = JSONObject(responseBody)
        val content = json.getJSONArray("content")
        if (content.length() > 0) {
            return content.getJSONObject(0).getString("text")
        }
        throw Exception("无效的响应格式")
    }
}

/**
 * Gemini图片识别请求
 */
private fun sendGeminiImageRequest(
    base64Image: String,
    systemPrompt: String,
    config: AIConfig
): String {
    val model = config.model.ifEmpty { "gemini-1.5-flash" }
    val url = "${config.apiUrl}/models/$model:generateContent?key=${config.apiKey}"

    val partsArray = JSONArray()
    partsArray.put(JSONObject().apply {
        put("text", systemPrompt + "\n\n请分析这张图片中的消费信息")
    })
    partsArray.put(JSONObject().apply {
        put("inline_data", JSONObject().apply {
            put("mime_type", "image/jpeg")
            put("data", base64Image)
        })
    })

    val contentsArray = JSONArray()
    contentsArray.put(JSONObject().apply {
        put("parts", partsArray)
    })

    val requestBody = JSONObject().apply {
        put("contents", contentsArray)
    }

    val request = Request.Builder()
        .url(url)
        .header("Content-Type", "application/json")
        .post(requestBody.toString().toRequestBody(jsonMediaType))
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            if (errorBody.contains("image") || response.code == 400) {
                throw Exception("UNSUPPORTED_MODEL")
            }
            throw Exception("API请求失败: ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw Exception("空响应")
        val json = JSONObject(responseBody)
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text")
            }
        }
        throw Exception("无效的响应格式")
    }
}

/**
 * 解析图片识别结果
 */
private fun parseImageAnalysisResult(response: String): ImageAnalysisResult {
    return try {
        // 尝试提取JSON
        val jsonStr = extractJsonFromResponse(response)
        val json = JSONObject(jsonStr)

        val success = json.optBoolean("success", false)
        val description = json.optString("description", "")
        val reply = json.optString("reply", "")

        // 解析actions
        val actions = if (json.has("actions")) {
            val actionsArray = json.getJSONArray("actions")
            List(actionsArray.length()) { i ->
                val actionObj = actionsArray.getJSONObject(i)
                ImageAction(
                    action = actionObj.optString("action", ""),
                    amount = actionObj.optDouble("amount", 0.0),
                    type = actionObj.optString("type", "expense"),
                    category = actionObj.optString("category", ""),
                    account = actionObj.optString("account", ""),
                    note = actionObj.optString("note", "")
                )
            }
        } else null

        ImageAnalysisResult(
            success = success,
            message = reply.ifBlank { description },
            actions = actions
        )
    } catch (e: Exception) {
        // 如果不是JSON格式，返回文本内容
        ImageAnalysisResult(
            success = false,
            message = response,
            actions = null
        )
    }
}

/**
 * 从响应中提取JSON
 */
private fun extractJsonFromResponse(response: String): String {
    // 尝试找到JSON代码块
    val codeBlockRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```")
    val match = codeBlockRegex.find(response)
    if (match != null) {
        return match.groupValues[1].trim()
    }

    // 尝试找到JSON对象
    val jsonStart = response.indexOf("{")
    val jsonEnd = response.lastIndexOf("}")
    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
        return response.substring(jsonStart, jsonEnd + 1)
    }

    return response
}

/**
 * 检查模型是否支持图片识别
 * 支持所有包含视觉相关关键词的模型
 */
fun isImageSupported(config: AIConfig): Boolean {
    val model = config.model.lowercase()
    
    // 视觉模型关键词列表
    val visionKeywords = listOf(
        "vision", "vl", "visual", "image", "img",
        "gpt-4o", "gpt4o", "claude-3", "claude3",
        "gemini-1.5", "gemini-2", "gemini-1.5-flash", "gemini-1.5-pro",
        "glm-4v", "qwen-vl", "yi-vl", "llava",
        "multimodal", "多模态",
        "4v", "-v-", "_v_", " v", "v1", "v2"
    )
    
    // 检查是否包含视觉关键词
    val hasVisionKeyword = visionKeywords.any { keyword ->
        model.contains(keyword)
    }
    
    // 排除明确的非视觉模型
    val nonVisionKeywords = listOf(
        "embed", "embedding", "text-", "code-", "instruct-", "base"
    )
    val isNonVision = nonVisionKeywords.any { keyword ->
        model.contains(keyword)
    }
    
    return hasVisionKeyword && !isNonVision
}
}

/**
 * 解析后的交易数据
 */
data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val category: String,
    val note: String
)

/**
 * 远程模型信息
 */
data class RemoteModel(
    val id: String,
    val name: String,
    val description: String
)

/**
 * 图片识别结果
 */
data class ImageAnalysisResult(
    val success: Boolean,
    val message: String,
    val actions: List<ImageAction>?
)

/**
 * 图片识别出的操作
 */
data class ImageAction(
    val action: String,
    val amount: Double,
    val type: String,
    val category: String,
    val account: String,
    val note: String
)
