package com.example.aiaccounting.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * AI Request DTO
 */
data class AIRequest(
    @SerializedName("model")
    val model: String,
    
    @SerializedName("messages")
    val messages: List<AIMessage>,
    
    @SerializedName("temperature")
    val temperature: Double = 0.7,
    
    @SerializedName("max_tokens")
    val maxTokens: Int = 1000,
    
    @SerializedName("stream")
    val stream: Boolean = false
)

/**
 * AI Message DTO
 */
data class AIMessage(
    @SerializedName("role")
    val role: String,  // "system", "user", "assistant"
    
    @SerializedName("content")
    val content: String
)

/**
 * AI Response DTO
 */
data class AIResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("object")
    val objectType: String,
    
    @SerializedName("created")
    val created: Long,
    
    @SerializedName("model")
    val model: String,
    
    @SerializedName("choices")
    val choices: List<AIChoice>,
    
    @SerializedName("usage")
    val usage: AIUsage?
)

/**
 * AI Choice DTO
 */
data class AIChoice(
    @SerializedName("index")
    val index: Int,
    
    @SerializedName("message")
    val message: AIMessage,
    
    @SerializedName("finish_reason")
    val finishReason: String
)

/**
 * AI Usage DTO
 */
data class AIUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    
    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * AI Error Response DTO
 */
data class AIErrorResponse(
    @SerializedName("error")
    val error: AIError
)

/**
 * AI Error DTO
 */
data class AIError(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("type")
    val type: String?,
    
    @SerializedName("param")
    val param: String?,
    
    @SerializedName("code")
    val code: String?
)
