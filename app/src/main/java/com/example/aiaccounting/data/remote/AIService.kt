package com.example.aiaccounting.data.remote

import com.example.aiaccounting.data.remote.dto.AIRequest
import com.example.aiaccounting.data.remote.dto.AIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AIService {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: AIRequest
    ): Response<AIResponse>
}
