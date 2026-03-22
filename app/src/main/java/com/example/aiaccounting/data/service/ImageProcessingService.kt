package com.example.aiaccounting.data.service

import android.content.Context
import android.net.Uri
import com.example.aiaccounting.data.service.image.OcrPreprocessingUtils
import com.example.aiaccounting.data.service.image.ReceiptTextHeuristics
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 图像处理服务 - 为普通语言模型提供图片理解能力
 * 优化版本：并行处理、精简数据、快速响应
 */
@Singleton
class ImageProcessingService @Inject constructor() {

    enum class OcrConfidence {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }

    data class ReceiptSignals(
        val amounts: List<String> = emptyList(),
        val dates: List<String> = emptyList(),
        val paymentMethods: List<String> = emptyList(),
        val merchants: List<String> = emptyList()
    ) {
        val hasStrongReceiptSignals: Boolean
            get() = amounts.isNotEmpty() || paymentMethods.isNotEmpty() || merchants.isNotEmpty()
    }

    /**
     * 图片处理结果 - 精简版
     */
    data class ImageAnalysisResult(
        val rawText: String,
        val text: String,
        val keyLines: List<String>,
        val labels: List<String>,
        val receiptSignals: ReceiptSignals,
        val qualityScore: Int,
        val confidence: OcrConfidence,
        val hasContent: Boolean
    )

    /**
     * 分析单张图片 - 带超时保护
     */
    suspend fun analyzeImage(uri: Uri, context: Context, timeoutMs: Long = 5000): ImageAnalysisResult {
        return try {
            withTimeout(timeoutMs) {
                // 并行执行OCR和标签识别
                val textDeferred = async { extractTextFromImage(uri, context) }
                val labelsDeferred = async { extractLabelsFromImage(uri, context) }

                val rawText = textDeferred.await()
                val labels = labelsDeferred.await().take(5)
                val keyLines = ReceiptTextHeuristics.extractKeyLines(rawText)
                val signals = ReceiptTextHeuristics.extractReceiptSignals(rawText)
                val compactText = trimTextForPrompt(keyLines, rawText)
                val qualityScore = ReceiptTextHeuristics.calculateQualityScore(
                    rawText = rawText,
                    keyLines = keyLines,
                    labels = labels,
                    signals = signals
                )
                val confidence = ReceiptTextHeuristics.toConfidence(qualityScore)
                val hasContent = rawText.isNotBlank() || labels.isNotEmpty()

                ImageAnalysisResult(
                    rawText = rawText,
                    text = compactText,
                    keyLines = keyLines,
                    labels = labels,
                    receiptSignals = signals,
                    qualityScore = qualityScore,
                    confidence = confidence,
                    hasContent = hasContent
                )
            }
        } catch (e: TimeoutCancellationException) {
            emptyResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyResult()
        }
    }

    /**
     * 并行分析多张图片
     */
    suspend fun analyzeMultipleImages(
        uris: List<Uri>,
        context: Context,
        timeoutMs: Long = 8000
    ): List<ImageAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMs) {
                // 并行处理所有图片
                val deferredResults = uris.map { uri ->
                    async { analyzeImage(uri, context, 5000) }
                }
                deferredResults.awaitAll()
            }
        } catch (e: TimeoutCancellationException) {
            uris.map { emptyResult() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uris.map { emptyResult() }
        }
    }

    /**
     * OCR文字识别 - 优化版
     */
    private suspend fun extractTextFromImage(uri: Uri, context: Context): String {
        return try {
            val preprocessedBytes = OcrPreprocessingUtils.preprocessImage(context, uri)
            val image = preprocessedBytes?.let { InputImage.fromByteArray(it, 0, it.size, 0, android.graphics.ImageFormat.JPEG) }
                ?: InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

            val result = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text.trim()
                        recognizer.close()
                        continuation.resume(text)
                    }
                    .addOnFailureListener { e ->
                        recognizer.close()
                        continuation.resumeWithException(e)
                    }
            }
            result
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 图像标签识别 - 优化版
     */
    private suspend fun extractLabelsFromImage(uri: Uri, context: Context): List<String> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val labeler = ImageLabeling.getClient(
                ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.6f) // 提高阈值，减少标签数量
                    .build()
            )

            val result = suspendCancellableCoroutine<List<String>> { continuation ->
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        val labelList = labels
                            .sortedByDescending { it.confidence }
                            .take(3)
                            .map { it.text }
                        labeler.close()
                        continuation.resume(labelList)
                    }
                    .addOnFailureListener { e ->
                        labeler.close()
                        continuation.resumeWithException(e)
                    }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 生成精简的AI提示词
     */
    fun generateCompactPrompt(
        results: List<ImageAnalysisResult>,
        userMessage: String
    ): String {
        return buildString {
            appendLine("你是小财娘，活泼可爱的管家婆AI助手！")
            appendLine()

            if (userMessage.isNotBlank()) {
                appendLine("用户说：$userMessage")
                appendLine()
            }

            appendLine("用户发了${results.size}张图片，内容如下：")
            appendLine()

            results.forEachIndexed { index, result ->
                appendLine("【图${index + 1}】")
                appendLine("识别置信度：${describeConfidence(result.confidence)}（${result.qualityScore}分）")

                if (result.labels.isNotEmpty()) {
                    appendLine("类型：${result.labels.joinToString(", ")}")
                }

                if (result.receiptSignals.merchants.isNotEmpty()) {
                    appendLine("商户：${result.receiptSignals.merchants.joinToString(", ")}")
                }
                if (result.receiptSignals.amounts.isNotEmpty()) {
                    appendLine("金额候选：${result.receiptSignals.amounts.joinToString(", ")}")
                }
                if (result.receiptSignals.paymentMethods.isNotEmpty()) {
                    appendLine("支付方式：${result.receiptSignals.paymentMethods.joinToString(", ")}")
                }
                if (result.receiptSignals.dates.isNotEmpty()) {
                    appendLine("时间候选：${result.receiptSignals.dates.joinToString(", ")}")
                }

                if (result.keyLines.isNotEmpty()) {
                    appendLine("关键文字：${result.keyLines.joinToString(" | ")}")
                } else if (result.text.isNotBlank()) {
                    appendLine("文字：${result.text}")
                }

                if (!result.hasContent) {
                    appendLine("（未识别到内容）")
                }

                if (result.confidence == OcrConfidence.LOW || result.confidence == OcrConfidence.NONE) {
                    appendLine("注意：该图片识别结果不稳定，请谨慎推断，不要臆造缺失字段。")
                }

                appendLine()
            }

            appendLine("请优先依据高置信度图片内容进行分析；如果字段不完整，请明确说明不确定项。如果是账单，请提取金额、类型、类别、备注。")
        }
    }

    private fun trimTextForPrompt(keyLines: List<String>, rawText: String): String {
        val preferredText = keyLines.joinToString("\n").trim()
        val baseText = if (preferredText.isNotBlank()) preferredText else rawText.trim()
        if (baseText.isBlank()) return ""
        return if (baseText.length > 500) {
            baseText.take(500) + "..."
        } else {
            baseText
        }
    }

    private fun describeConfidence(confidence: OcrConfidence): String {
        return when (confidence) {
            OcrConfidence.HIGH -> "高"
            OcrConfidence.MEDIUM -> "中"
            OcrConfidence.LOW -> "低"
            OcrConfidence.NONE -> "无"
        }
    }

    private fun emptyResult(): ImageAnalysisResult {
        return ImageAnalysisResult(
            rawText = "",
            text = "",
            keyLines = emptyList(),
            labels = emptyList(),
            receiptSignals = ReceiptSignals(),
            qualityScore = 0,
            confidence = OcrConfidence.NONE,
            hasContent = false
        )
    }
}
