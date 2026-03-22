package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ChatMessage
import com.example.aiaccounting.data.model.MessageRole
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.ImageProcessingService
import kotlinx.coroutines.withTimeoutOrNull

internal sealed class ImagePromptResult {
    data class Success(val prompt: String) : ImagePromptResult()
    data class Error(val message: String) : ImagePromptResult()
}

internal sealed class ImageMessageProcessingResult {
    data class Success(val message: String) : ImageMessageProcessingResult()
    data class Error(val message: String) : ImageMessageProcessingResult()
}

internal class AIAssistantImageMessageHandler(
    private val aiService: AIService,
    private val imageProcessingService: ImageProcessingService,
    private val aiUsageRepository: AIUsageRepository
) {
    fun isNativeImageSupported(config: AIConfig): Boolean {
        return aiService.isImageSupported(config)
    }

    suspend fun processImageMessage(
        message: String,
        imageUris: List<Uri>,
        context: Context,
        currentButler: Butler,
        config: AIConfig,
        isNetworkAvailable: Boolean
    ): ImageMessageProcessingResult {
        if (!isNetworkAvailable) {
            return ImageMessageProcessingResult.Error("📡 网络不可用，无法识别图片。请检查网络连接后重试。")
        }

        if (!config.isEnabled || config.apiKey.isBlank()) {
            return ImageMessageProcessingResult.Error("🔑 请先配置支持图片的模型和 API 密钥，才能使用图片识别功能。")
        }

        val isNativeImageSupported = isNativeImageSupported(config)
        if (isNativeImageSupported) {
            return processNativeImageMessage(
                message = message,
                imageUris = imageUris,
                context = context,
                currentButler = currentButler,
                config = config
            )
        }

        return when (
            val promptResult = buildImagePrompt(
                message = message,
                imageUris = imageUris,
                context = context,
                currentButler = currentButler,
                isNativeImageSupported = false
            )
        ) {
            is ImagePromptResult.Error -> ImageMessageProcessingResult.Error(promptResult.message)
            is ImagePromptResult.Success -> ImageMessageProcessingResult.Success(
                requestImageReply(
                    prompt = promptResult.prompt,
                    config = config
                )
            )
        }
    }

    private suspend fun processNativeImageMessage(
        message: String,
        imageUris: List<Uri>,
        context: Context,
        currentButler: Butler,
        config: AIConfig
    ): ImageMessageProcessingResult {
        val promptContext = buildString {
            appendLine(currentButler.systemPrompt)
            appendLine()
            appendLine("【图片补充说明】")
            if (message.isNotBlank()) {
                appendLine("用户补充：$message")
            } else {
                appendLine("用户未补充额外文字，请直接根据图片内容分析。")
            }
        }.trim()

        val replies = imageUris.mapNotNull { imageUri ->
            val analysisResult = aiService.analyzeImageAndRecord(
                imageUri = imageUri,
                config = config,
                context = context,
                promptContext = promptContext
            )
            analysisResult.message.takeIf { it.isNotBlank() }
        }

        if (replies.isEmpty()) {
            return ImageMessageProcessingResult.Error("😥 没有在图片里识别到有效内容，请尝试更清晰的图片。")
        }

        return ImageMessageProcessingResult.Success(replies.joinToString("\n\n"))
    }

    suspend fun buildImagePrompt(
        message: String,
        imageUris: List<Uri>,
        context: Context,
        currentButler: Butler,
        isNativeImageSupported: Boolean
    ): ImagePromptResult {
        if (isNativeImageSupported) {
            return ImagePromptResult.Success(
                buildString {
                    appendLine(currentButler.systemPrompt)
                    appendLine()
                    if (message.isNotBlank()) {
                        appendLine("用户说：$message")
                        appendLine()
                    }
                    appendLine("用户发了${imageUris.size}张图片，请分析其中的消费信息并返回JSON格式执行记账。")
                }
            )
        }

        val analysisResults = imageProcessingService.analyzeMultipleImages(
            imageUris, context, timeoutMs = 8000
        )
        val hasContent = analysisResults.any { it.hasContent }
        if (!hasContent) {
            return ImagePromptResult.Error("😥 没有在图片里识别到文字或标签，可能图片过模糊或无法读取。请尝试更清晰的账单照片再试一次。")
        }

        val acceptedResults = analysisResults.filter {
            it.confidence == ImageProcessingService.OcrConfidence.HIGH ||
                it.confidence == ImageProcessingService.OcrConfidence.MEDIUM
        }
        if (acceptedResults.isEmpty()) {
            return ImagePromptResult.Error("😥 这次图片识别置信度过低，暂时不会发送到云端 AI。请尽量保持图片清晰、端正、完整后再试一次。")
        }

        return ImagePromptResult.Success(
            imageProcessingService.generateCompactPrompt(acceptedResults, message)
        )
    }

    suspend fun requestImageReply(
        prompt: String,
        config: AIConfig
    ): String {
        val chatMessages = listOf(
            ChatMessage(
                role = MessageRole.USER,
                content = prompt
            )
        )

        val aiResponse = withTimeoutOrNull(90000L) {
            aiService.chat(chatMessages, config)
        }

        aiUsageRepository.recordCall(success = aiResponse != null)
        return aiResponse ?: "图片处理超时，请稍后重试。"
    }
}
