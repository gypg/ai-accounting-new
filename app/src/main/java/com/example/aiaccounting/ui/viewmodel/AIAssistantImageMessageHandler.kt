package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.ImageAction
import com.example.aiaccounting.data.service.ImageProcessingService

internal sealed class ImagePromptResult {
    data class Success(val prompt: String) : ImagePromptResult()
    data class Error(val message: String) : ImagePromptResult()
}

internal sealed class ImageMessageProcessingResult {
    data class TextReply(val message: String) : ImageMessageProcessingResult()
    data class ExecuteEnvelope(val envelope: AIAssistantActionEnvelope) : ImageMessageProcessingResult()
    data class RemoteBookkeepingPrompt(val prompt: String) : ImageMessageProcessingResult()
    data class Error(val message: String) : ImageMessageProcessingResult()
}

internal class AIAssistantImageMessageHandler(
    private val aiService: AIService,
    private val imageProcessingService: ImageProcessingService,
    private val aiUsageRepository: AIUsageRepository
) {
    private companion object {
        const val OCR_BATCH_TIMEOUT_MS = 15_000L
        const val OCR_SINGLE_TIMEOUT_MS = 10_000L
    }
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

        val nativeImageSupported = isNativeImageSupported(config)
        if (nativeImageSupported) {
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
            is ImagePromptResult.Success -> ImageMessageProcessingResult.RemoteBookkeepingPrompt(promptResult.prompt)
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

        val typedActions = mutableListOf<AIAssistantTypedAction>()
        val replies = mutableListOf<String>()

        imageUris.forEach { imageUri ->
            val analysisResult = aiService.analyzeImageAndRecord(
                imageUri = imageUri,
                config = config,
                context = context,
                promptContext = promptContext
            )
            analysisResult.actions
                ?.mapNotNull(::toTypedAction)
                ?.let { typedActions.addAll(it) }
            analysisResult.message.takeIf { it.isNotBlank() }?.let(replies::add)
        }

        if (typedActions.isNotEmpty()) {
            return ImageMessageProcessingResult.ExecuteEnvelope(
                envelope = AIAssistantActionEnvelope(
                    actions = typedActions,
                    reply = replies.joinToString("\n\n")
                )
            )
        }

        if (replies.isEmpty()) {
            return ImageMessageProcessingResult.Error("😥 没有在图片里识别到有效内容，请尝试更清晰的图片。")
        }

        return ImageMessageProcessingResult.TextReply(replies.joinToString("\n\n"))
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
            imageUris, context, timeoutMs = OCR_BATCH_TIMEOUT_MS
        )
        val effectiveResults = if (analysisResults.any { it.hasContent }) {
            analysisResults
        } else {
            imageUris.map { imageUri ->
                imageProcessingService.analyzeImage(imageUri, context, timeoutMs = OCR_SINGLE_TIMEOUT_MS)
            }
        }

        val resultsForPrompt = effectiveResults.filter { it.hasContent }

        if (resultsForPrompt.isEmpty()) {
            return ImagePromptResult.Error("😥 没有在图片里识别到文字或标签，可能图片过模糊或无法读取。请尝试更清晰的账单照片再试一次。")
        }

        return ImagePromptResult.Success(
            imageProcessingService.generateCompactPrompt(resultsForPrompt, message)
        )
    }


    private fun toTypedAction(action: ImageAction): AIAssistantTypedAction? {
        if (action.action.trim().lowercase() != "add_transaction") {
            return null
        }
        if (action.amount <= 0) {
            return null
        }

        val transactionTypeRaw = when (action.type.trim().lowercase()) {
            "income", "收入" -> "income"
            "transfer", "转账" -> "transfer"
            else -> "expense"
        }

        return AIAssistantTypedAction.AddTransaction(
            amount = action.amount,
            transactionTypeRaw = transactionTypeRaw,
            categoryRef = AIAssistantEntityReference(
                id = null,
                name = action.category,
                rawIdText = "",
                kind = "category"
            ),
            accountRef = AIAssistantEntityReference(
                id = null,
                name = action.account,
                rawIdText = "",
                kind = "account"
            ),
            transferAccountRef = null,
            note = action.note,
            dateTimestamp = System.currentTimeMillis()
        )
    }
}
