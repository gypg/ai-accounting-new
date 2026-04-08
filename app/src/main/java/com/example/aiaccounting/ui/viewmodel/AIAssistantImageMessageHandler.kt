package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.ImageAction
import com.example.aiaccounting.data.service.ImageProcessingService
import com.example.aiaccounting.logging.AppLogLogger
import java.util.UUID

internal sealed class ImagePromptResult {
    data class Success(val prompt: String) : ImagePromptResult()
    data class ChatPrompt(val prompt: String) : ImagePromptResult()
    data class Error(val message: String) : ImagePromptResult()
}

internal sealed class ImageMessageProcessingResult {
    data class TextReply(val message: String) : ImageMessageProcessingResult()
    data class ExecuteEnvelope(val envelope: AIAssistantActionEnvelope) : ImageMessageProcessingResult()
    data class RemoteBookkeepingPrompt(val prompt: String) : ImageMessageProcessingResult()
    data class RemoteChatPrompt(val prompt: String) : ImageMessageProcessingResult()
    data class Clarification(val message: String) : ImageMessageProcessingResult()
    data class Error(val message: String) : ImageMessageProcessingResult()
}

internal class AIAssistantImageMessageHandler(
    private val aiService: AIService,
    private val imageProcessingService: ImageProcessingService,
    private val aiUsageRepository: AIUsageRepository,
    private val appLogLogger: AppLogLogger
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
        isNetworkAvailable: Boolean,
        traceId: String? = null
    ): ImageMessageProcessingResult {
        val effectiveTraceId = traceId ?: UUID.randomUUID().toString()
        appLogLogger.info(
            source = "AI",
            category = "image_process_start",
            message = "图片消息处理开始",
            details = "imageCount=${imageUris.size},userMessageLength=${message.length},provider=${config.provider.name},model=${config.model.ifBlank { "AUTO" }},networkAvailable=$isNetworkAvailable",
            traceId = effectiveTraceId
        )
        if (!isNetworkAvailable) {
            appLogLogger.warning(
                source = "AI",
                category = "image_process_mode_selected",
                message = "图片消息处理终止",
                details = "reason=network_unavailable,imageCount=${imageUris.size}",
                traceId = effectiveTraceId
            )
            return ImageMessageProcessingResult.Error("📡 网络不可用，无法识别图片。请检查网络连接后重试。")
        }

        if (!config.isEnabled || config.apiKey.isBlank()) {
            appLogLogger.warning(
                source = "AI",
                category = "image_process_mode_selected",
                message = "图片消息处理终止",
                details = "reason=ai_not_configured,imageCount=${imageUris.size}",
                traceId = effectiveTraceId
            )
            return ImageMessageProcessingResult.Error("🔑 请先配置支持图片的模型和 API 密钥，才能使用图片识别功能。")
        }

        val nativeImageSupported = isNativeImageSupported(config)
        val describeOnly = shouldDescribeImageContent(message)
        appLogLogger.info(
            source = "AI",
            category = "image_process_mode_selected",
            message = "图片消息处理模式已选择",
            details = "nativeImageSupported=$nativeImageSupported,describeOnly=$describeOnly,imageCount=${imageUris.size}",
            traceId = effectiveTraceId
        )
        if (nativeImageSupported) {
            val nativeResult = processNativeImageMessage(
                message = message,
                imageUris = imageUris,
                context = context,
                currentButler = currentButler,
                config = config,
                traceId = effectiveTraceId
            )
            val shouldFallbackToOcr = when (nativeResult) {
                is ImageMessageProcessingResult.Error -> true
                is ImageMessageProcessingResult.TextReply -> nativeResult.message.isBlank()
                else -> false
            }
            if (!shouldFallbackToOcr) {
                return nativeResult
            }
            appLogLogger.warning(
                source = "AI",
                category = "image_process_mode_selected",
                message = "原生图片主链失败，回退OCR链路",
                details = "nativeResult=${nativeResult::class.java.simpleName},imageCount=${imageUris.size}",
                traceId = effectiveTraceId
            )
        }

        if (describeOnly) {
            return processImageUnderstandingMessage(
                message = message,
                imageUris = imageUris,
                context = context,
                currentButler = currentButler,
                traceId = effectiveTraceId
            )
        }

        return when (
            val promptResult = buildImagePrompt(
                message = message,
                imageUris = imageUris,
                context = context,
                currentButler = currentButler,
                isNativeImageSupported = false,
                traceId = effectiveTraceId
            )
        ) {
            is ImagePromptResult.Error -> ImageMessageProcessingResult.Error(promptResult.message)
            is ImagePromptResult.ChatPrompt -> ImageMessageProcessingResult.RemoteChatPrompt(promptResult.prompt)
            is ImagePromptResult.Success -> ImageMessageProcessingResult.RemoteBookkeepingPrompt(promptResult.prompt)
        }
    }

    private suspend fun processNativeImageMessage(
        message: String,
        imageUris: List<Uri>,
        context: Context,
        currentButler: Butler,
        config: AIConfig,
        traceId: String
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
            appLogLogger.info(
                source = "AI",
                category = "image_native_request_start",
                message = "原生图片识别开始",
                details = "uriTail=${imageUri.toString().takeLast(64)},userMessageLength=${message.length}",
                traceId = traceId
            )
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

        appLogLogger.info(
            source = "AI",
            category = "image_action_parse",
            message = "图片记账动作解析完成",
            details = "images=${imageUris.size},typedActions=${typedActions.size},replies=${replies.size},missingAccountOrCategory=${typedActions.count { action -> action is AIAssistantTypedAction.AddTransaction && (action.accountRef.name.isBlank() || action.categoryRef.name.isBlank()) }}",
            traceId = traceId
        )

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

    private suspend fun processImageUnderstandingMessage(
        message: String,
        imageUris: List<Uri>,
        context: Context,
        currentButler: Butler,
        traceId: String
    ): ImageMessageProcessingResult {
        val analysisResults = imageProcessingService.analyzeMultipleImages(
            imageUris,
            context,
            timeoutMs = OCR_BATCH_TIMEOUT_MS
        )
        val effectiveResults = if (analysisResults.any { it.hasContent }) {
            analysisResults
        } else {
            imageUris.map { imageUri ->
                imageProcessingService.analyzeImage(
                    imageUri,
                    context,
                    timeoutMs = OCR_SINGLE_TIMEOUT_MS
                )
            }
        }
        val resultsWithContent = effectiveResults.filter { it.hasContent }
        if (resultsWithContent.isEmpty()) {
            appLogLogger.warning(
                source = "AI",
                category = "image_prompt_empty_result",
                message = "图片内容理解未识别到可用内容",
                details = "imageCount=${imageUris.size},userMessageLength=${message.length}",
                traceId = traceId
            )
            return ImageMessageProcessingResult.Clarification(
                "😥 没有在图片里识别到文字或标签，可能图片过模糊或无法读取。请尝试更清晰的图片再试一次。"
            )
        }

        val summary = buildString {
            appendLine(currentButler.systemPrompt)
            appendLine()
            appendLine("【图片内容理解】")
            appendLine("用户问题：${message.ifBlank { "请描述图片内容" }}")
            appendLine()
            resultsWithContent.forEachIndexed { index, result ->
                appendLine("【图${index + 1}】")
                if (result.labels.isNotEmpty()) {
                    appendLine("类型：${result.labels.joinToString(", ")}")
                }
                if (result.text.isNotBlank()) {
                    appendLine("识别文字：")
                    appendLine(result.text.lines().filter { it.isNotBlank() }.take(8).joinToString("\n"))
                } else if (result.keyLines.isNotEmpty()) {
                    appendLine("关键文字：${result.keyLines.take(8).joinToString(" | ")}")
                }
                appendLine()
            }
            appendLine("请直接根据以上内容，用管家口吻回答用户这张图片里是什么、写了什么，不要执行记账动作。")
        }.trim()
        return ImageMessageProcessingResult.RemoteChatPrompt(summary)
    }

    private fun shouldDescribeImageContent(message: String): Boolean {
        val normalized = message.trim().lowercase()
        if (normalized.isBlank()) return false
        val descriptionKeywords = listOf(
            "图片的内容是什么", "这张图是什么内容", "图片里是什么", "图里是什么", "图片写了什么", "图里写了什么",
            "帮我看看图片", "帮我看下图片", "看看这张图", "看图", "识别图片内容", "描述一下这张图"
        )
        val bookkeepingKeywords = listOf(
            "记账", "记一笔", "入账", "小票", "账单", "消费", "支出", "收入", "流水", "报销"
        )
        return descriptionKeywords.any { normalized.contains(it) } && bookkeepingKeywords.none { normalized.contains(it) }
    }

    suspend fun buildImagePrompt(
        message: String,
        imageUris: List<Uri>,
        context: Context,
        currentButler: Butler,
        isNativeImageSupported: Boolean,
        traceId: String? = null
    ): ImagePromptResult {
        val effectiveTraceId = traceId ?: UUID.randomUUID().toString()
        if (isNativeImageSupported) {
            return ImagePromptResult.Success(
                buildString {
                    appendLine(currentButler.systemPrompt)
                    appendLine()
                    if (message.isNotBlank()) {
                        appendLine("用户说：$message")
                        appendLine()
                    }
                    appendLine("用户发了${imageUris.size}张图片，请分析图片内容；如果是账单或消费凭证，请尽可能提取消费信息并返回可执行结果。")
                }
            )
        }

        val analysisResults = imageProcessingService.analyzeMultipleImages(
            imageUris, context, timeoutMs = OCR_BATCH_TIMEOUT_MS
        )
        appLogLogger.info(
            source = "AI",
            category = "image_prompt_batch_summary",
            message = "图片Prompt批量OCR摘要",
            details = "imageCount=${imageUris.size},resultsWithSignal=${analysisResults.count { it.hasContent || it.labels.isNotEmpty() || it.keyLines.isNotEmpty() || it.text.isNotBlank() }}",
            traceId = effectiveTraceId
        )
        val effectiveResults = if (analysisResults.any { it.hasContent || it.labels.isNotEmpty() || it.keyLines.isNotEmpty() || it.text.isNotBlank() }) {
            analysisResults
        } else {
            imageUris.map { imageUri ->
                imageProcessingService.analyzeImage(
                    imageUri,
                    context,
                    timeoutMs = OCR_SINGLE_TIMEOUT_MS
                )
            }.also { fallbackResults ->
                appLogLogger.info(
                    source = "AI",
                    category = "image_prompt_single_fallback_summary",
                    message = "图片Prompt单图回退摘要",
                    details = "imageCount=${imageUris.size},resultsWithSignal=${fallbackResults.count { it.hasContent || it.labels.isNotEmpty() || it.keyLines.isNotEmpty() || it.text.isNotBlank() }}",
                    traceId = effectiveTraceId
                )
            }
        }

        val resultsWithSignal = effectiveResults.count {
            it.hasContent || it.labels.isNotEmpty() || it.keyLines.isNotEmpty() || it.text.isNotBlank()
        }
        if (resultsWithSignal == 0) {
            appLogLogger.warning(
                source = "AI",
                category = "image_prompt_empty_result",
                message = "图片记账Prompt为空",
                details = "images=${imageUris.size},userMessageLength=${message.length},reason=no_usable_ocr_result",
                traceId = effectiveTraceId
            )
            return ImagePromptResult.Error("😥 没有在图片里识别到文字或标签，可能图片过模糊或无法读取。请尝试更清晰的图片再试一次。")
        }

        val prompt = imageProcessingService.generateCompactPrompt(effectiveResults, message)

        appLogLogger.info(
            source = "AI",
            category = "image_prompt_ready",
            message = "图片记账Prompt构建",
            details = "images=${imageUris.size},resultsForPrompt=$resultsWithSignal,userMessageLength=${message.length},native=false,promptLength=${prompt.length}",
            traceId = effectiveTraceId
        )

        if (prompt.isBlank()) {
            appLogLogger.warning(
                source = "AI",
                category = "image_prompt_empty_result",
                message = "图片记账Prompt为空",
                details = "images=${imageUris.size},userMessageLength=${message.length}",
                traceId = effectiveTraceId
            )
            return ImagePromptResult.Error("😥 没有在图片里识别到文字或标签，可能图片过模糊或无法读取。请尝试更清晰的图片再试一次。")
        }

        val normalizedMessage = message.trim()
        val shouldUseChatPrompt = normalizedMessage.isBlank() || shouldDescribeImageContent(normalizedMessage)
        return if (shouldUseChatPrompt) {
            ImagePromptResult.ChatPrompt(
                buildImageUnderstandingPrompt(
                    message = if (normalizedMessage.isBlank()) "请描述图片内容" else normalizedMessage,
                    resultsWithContent = effectiveResults,
                    currentButler = currentButler
                )
            )
        } else {
            ImagePromptResult.Success(prompt)
        }
    }

    private fun buildImageUnderstandingPrompt(
        message: String,
        resultsWithContent: List<ImageProcessingService.ImageAnalysisResult>,
        currentButler: Butler
    ): String {
        return buildString {
            appendLine(currentButler.systemPrompt)
            appendLine()
            appendLine("【图片内容理解】")
            appendLine("用户问题：${message.ifBlank { "请描述图片内容" }}")
            appendLine()
            resultsWithContent.forEachIndexed { index, result ->
                appendLine("【图${index + 1}】")
                if (result.labels.isNotEmpty()) {
                    appendLine("类型：${result.labels.joinToString(", ")}")
                }
                if (result.text.isNotBlank()) {
                    appendLine("识别文字：")
                    appendLine(result.text.lines().filter { it.isNotBlank() }.take(8).joinToString("\n"))
                } else if (result.keyLines.isNotEmpty()) {
                    appendLine("关键文字：${result.keyLines.take(8).joinToString(" | ")}")
                }
                appendLine()
            }
            appendLine("请直接根据以上内容，用管家口吻回答用户这张图片里是什么、写了什么；如果内容不完整，请明确说明你能看清和看不清的部分。不要执行记账动作。")
        }.trim()
    }

    private fun isReadyForStrictBookkeepingPrompt(
        result: ImageProcessingService.ImageAnalysisResult
    ): Boolean {
        return result.receiptSignals.hasStrongReceiptSignals ||
            result.receiptSignals.dates.isNotEmpty() ||
            result.confidence == ImageProcessingService.OcrConfidence.HIGH
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
