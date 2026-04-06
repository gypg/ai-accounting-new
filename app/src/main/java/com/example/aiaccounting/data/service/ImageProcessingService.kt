package com.example.aiaccounting.data.service

import android.content.Context
import android.net.Uri
import com.example.aiaccounting.data.service.image.ImageQualityMetrics
import com.example.aiaccounting.data.service.image.OcrAgreementLevel
import com.example.aiaccounting.data.service.image.OcrPassAnalysis
import com.example.aiaccounting.data.service.image.OcrPreprocessedVariant
import com.example.aiaccounting.data.service.image.OcrPreprocessingProfile
import com.example.aiaccounting.data.service.image.OcrPreprocessingUtils
import com.example.aiaccounting.data.service.image.OcrResultSelector
import com.example.aiaccounting.data.service.image.ReceiptTextHeuristics
import com.example.aiaccounting.logging.AppLogLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
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
class ImageProcessingService @Inject constructor(
    private val appLogLogger: AppLogLogger
) {

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
        val hasContent: Boolean,
        val agreementLevel: OcrAgreementLevel = OcrAgreementLevel.NONE,
        val selectedProfile: OcrPreprocessingProfile = OcrPreprocessingProfile.BASE,
        val imageQualityMetrics: ImageQualityMetrics = ImageQualityMetrics()
    )

    /**
     * 分析单张图片 - 带超时保护
     */
    suspend fun analyzeImage(uri: Uri, context: Context, timeoutMs: Long = 5000): ImageAnalysisResult {
        return try {
            withTimeout(timeoutMs) {
                val labelsDeferred = async { extractLabelsFromImage(uri, context) }
                val passDeferred = async { extractBestPassAnalysis(uri, context) }

                val labels = labelsDeferred.await().take(5)
                val passSelection = passDeferred.await()
                if (passSelection == null) {
                    val fallbackText = runCatching { extractTextFromFile(uri, context) }.getOrDefault("").trim()
                    val fallbackKeyLines = ReceiptTextHeuristics.extractKeyLines(fallbackText)
                    val fallbackSignals = ReceiptTextHeuristics.extractReceiptSignals(fallbackText)
                    val fallbackHasText = fallbackText.isNotBlank() || fallbackKeyLines.isNotEmpty() || fallbackSignals.hasStrongReceiptSignals
                    appLogLogger.info(
                        source = "AI",
                        category = "image_ocr",
                        message = "图片OCR未选出候选结果",
                        details = "labels=${labels.size},fallbackTextLength=${fallbackText.length},fallbackKeyLineCount=${fallbackKeyLines.size},fallbackAmountCandidates=${fallbackSignals.amounts.size},fallbackHasText=$fallbackHasText"
                    )
                    val hasLabelContent = labels.isNotEmpty()
                    return@withTimeout if (fallbackHasText || hasLabelContent) {
                        val promptText = buildPromptText(fallbackKeyLines, fallbackText)
                        ImageAnalysisResult(
                            rawText = fallbackText,
                            text = promptText,
                            keyLines = fallbackKeyLines,
                            labels = labels,
                            receiptSignals = fallbackSignals,
                            qualityScore = ReceiptTextHeuristics.calculateQualityScore(
                                rawText = fallbackText,
                                keyLines = fallbackKeyLines,
                                labels = labels,
                                signals = fallbackSignals
                            ),
                            confidence = if (fallbackHasText) OcrConfidence.LOW else OcrConfidence.NONE,
                            hasContent = true
                        )
                    } else {
                        emptyResult()
                    }
                }

                val best = passSelection.best
                val promptText = buildPromptText(best.keyLines, best.rawText)
                val finalScore = ReceiptTextHeuristics.calculateQualityScore(
                    rawText = best.rawText,
                    keyLines = best.keyLines,
                    labels = labels,
                    signals = best.receiptSignals,
                    imageQualityMetrics = best.imageQualityMetrics,
                    agreementLevel = passSelection.agreementLevel
                )
                val finalConfidence = ReceiptTextHeuristics.toConfidence(finalScore)
                val hasContent = best.rawText.isNotBlank() || best.keyLines.isNotEmpty() || best.receiptSignals.hasStrongReceiptSignals || labels.isNotEmpty()

                appLogLogger.info(
                    source = "AI",
                    category = "image_ocr",
                    message = "图片OCR分析完成",
                    details = "rawLineCount=${best.rawText.lines().count { it.isNotBlank() }},keyLineCount=${best.keyLines.size},promptLineCount=${promptText.lines().count { it.isNotBlank() }},promptLength=${promptText.length},amountCandidates=${best.receiptSignals.amounts.size},dateCandidates=${best.receiptSignals.dates.size},merchantCandidates=${best.receiptSignals.merchants.size},qualityScore=$finalScore,confidence=${finalConfidence.name}"
                )

                ImageAnalysisResult(
                    rawText = best.rawText,
                    text = promptText,
                    keyLines = best.keyLines,
                    labels = labels,
                    receiptSignals = best.receiptSignals,
                    qualityScore = finalScore,
                    confidence = finalConfidence,
                    hasContent = hasContent,
                    agreementLevel = passSelection.agreementLevel,
                    selectedProfile = best.profile,
                    imageQualityMetrics = best.imageQualityMetrics
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
     * 图像标签识别 - 优化版
     */
    private suspend fun extractLabelsFromImage(uri: Uri, context: Context): List<String> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val labeler = ImageLabeling.getClient(
                ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.6f)
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

    private suspend fun extractBestPassAnalysis(uri: Uri, context: Context): com.example.aiaccounting.data.service.image.OcrSelectionResult? {
        val variants = OcrPreprocessingUtils.preprocessVariants(context, uri)
        val analyses = if (variants.isNotEmpty()) {
            variants.mapNotNull { variant -> analyzePass(variant) }
        } else {
            listOfNotNull(analyzeOriginalPass(uri, context))
        }

        if (analyses.isEmpty()) return null

        val initialSelection = OcrResultSelector.selectBestCandidate(analyses)
        val rescoredAnalyses = analyses.map { candidate ->
            candidate.copy(
                qualityScore = ReceiptTextHeuristics.calculateQualityScore(
                    rawText = candidate.rawText,
                    keyLines = candidate.keyLines,
                    labels = emptyList(),
                    signals = candidate.receiptSignals,
                    imageQualityMetrics = candidate.imageQualityMetrics,
                    agreementLevel = initialSelection.agreementLevel
                ),
                confidence = ReceiptTextHeuristics.toConfidence(
                    ReceiptTextHeuristics.calculateQualityScore(
                        rawText = candidate.rawText,
                        keyLines = candidate.keyLines,
                        labels = emptyList(),
                        signals = candidate.receiptSignals,
                        imageQualityMetrics = candidate.imageQualityMetrics,
                        agreementLevel = initialSelection.agreementLevel
                    )
                )
            )
        }
        return OcrResultSelector.selectBestCandidate(rescoredAnalyses)
    }

    private suspend fun analyzePass(variant: OcrPreprocessedVariant): OcrPassAnalysis? {
        val rawText = extractTextFromBitmap(variant.bitmap)
        if (rawText.isBlank()) return null

        val keyLines = ReceiptTextHeuristics.extractKeyLines(rawText)
        val signals = ReceiptTextHeuristics.extractReceiptSignals(rawText)
        val qualityScore = ReceiptTextHeuristics.calculateQualityScore(
            rawText = rawText,
            keyLines = keyLines,
            labels = emptyList(),
            signals = signals,
            imageQualityMetrics = variant.imageQualityMetrics
        )

        return OcrPassAnalysis(
            profile = variant.profile,
            rawText = rawText,
            keyLines = keyLines,
            receiptSignals = signals,
            qualityScore = qualityScore,
            confidence = ReceiptTextHeuristics.toConfidence(qualityScore),
            imageQualityMetrics = variant.imageQualityMetrics
        )
    }

    private suspend fun analyzeOriginalPass(uri: Uri, context: Context): OcrPassAnalysis? {
        val rawText = extractTextFromFile(uri, context)
        if (rawText.isBlank()) return null

        val keyLines = ReceiptTextHeuristics.extractKeyLines(rawText)
        val signals = ReceiptTextHeuristics.extractReceiptSignals(rawText)
        val qualityScore = ReceiptTextHeuristics.calculateQualityScore(
            rawText = rawText,
            keyLines = keyLines,
            labels = emptyList(),
            signals = signals
        )

        return OcrPassAnalysis(
            profile = OcrPreprocessingProfile.BASE,
            rawText = rawText,
            keyLines = keyLines,
            receiptSignals = signals,
            qualityScore = qualityScore,
            confidence = ReceiptTextHeuristics.toConfidence(qualityScore),
            imageQualityMetrics = ImageQualityMetrics()
        )
    }

    private suspend fun extractTextFromBitmap(bitmap: android.graphics.Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            processTextRecognition(image)
        } catch (e: Exception) {
            ""
        }
    }
    private suspend fun extractTextFromFile(uri: Uri, context: Context): String {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            processTextRecognition(image)
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun processTextRecognition(image: InputImage): String {
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        return suspendCancellableCoroutine { continuation ->
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
                appendLine("预处理方案：${describeProfile(result.selectedProfile)}")
                if (result.agreementLevel != OcrAgreementLevel.NONE) {
                    appendLine("多轮识别一致性：${describeAgreement(result.agreementLevel)}")
                }

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

                if (result.text.isNotBlank()) {
                    appendLine("识别文字：")
                    appendLine(result.text)
                } else if (result.keyLines.isNotEmpty()) {
                    appendLine("关键文字：${result.keyLines.joinToString(" | ")}")
                }

                if (!result.hasContent) {
                    appendLine("（未识别到内容）")
                }

                if (result.confidence == OcrConfidence.LOW || result.confidence == OcrConfidence.NONE) {
                    appendLine("注意：该图片识别结果不稳定，请谨慎推断，不要臆造缺失字段。")
                } else if (result.confidence == OcrConfidence.MEDIUM && result.agreementLevel == OcrAgreementLevel.NONE) {
                    appendLine("注意：该图片已通过本地门控，但多轮识别一致性较弱，请对不确定字段保守处理。")
                }

                appendLine()
            }

            appendLine("如果图片是多笔账单或流水，请尽量逐条提取每一笔交易，不要只总结前几笔。")
            appendLine("请优先依据高置信度图片内容进行分析；如果字段不完整，请明确说明不确定项。如果是账单，请提取金额、类型、类别、备注。")
        }
    }

    private fun trimTextForPrompt(keyLines: List<String>, rawText: String): String {
        return buildPromptText(keyLines, rawText)
    }

    private fun buildPromptText(keyLines: List<String>, rawText: String): String {
        val sanitizedKeyLines = keyLines.map { it.trim() }.filter { it.isNotBlank() }
        val sanitizedRawLines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (sanitizedKeyLines.isEmpty() && sanitizedRawLines.isEmpty()) return ""

        val chosenLines = if (sanitizedKeyLines.size >= sanitizedRawLines.size / 2 && sanitizedKeyLines.isNotEmpty()) {
            mergeReceiptCoverageLines(sanitizedKeyLines)
        } else {
            mergeReceiptCoverageLines(sanitizedRawLines)
        }

        val joined = chosenLines.joinToString("\n").trim()
        return if (joined.length > 1800) {
            joined.take(1800) + "..."
        } else {
            joined
        }
    }

    private fun mergeReceiptCoverageLines(lines: List<String>): List<String> {
        if (lines.isEmpty()) return emptyList()
        if (lines.size <= 18) return lines.distinct()

        val header = lines.take(4)
        val tail = lines.takeLast(4)
        val middle = lines.drop(4).dropLast(4)
        val sampledMiddle = middle.filterIndexed { index, _ -> index % 2 == 0 }.take(12)

        return (header + sampledMiddle + tail).distinct()
    }

    private fun describeConfidence(confidence: OcrConfidence): String {
        return when (confidence) {
            OcrConfidence.HIGH -> "高"
            OcrConfidence.MEDIUM -> "中"
            OcrConfidence.LOW -> "低"
            OcrConfidence.NONE -> "无"
        }
    }

    private fun describeAgreement(agreementLevel: OcrAgreementLevel): String {
        return when (agreementLevel) {
            OcrAgreementLevel.STRONG -> "强"
            OcrAgreementLevel.WEAK -> "弱"
            OcrAgreementLevel.NONE -> "无"
        }
    }

    private fun describeProfile(profile: OcrPreprocessingProfile): String {
        return when (profile) {
            OcrPreprocessingProfile.BASE -> "基础增强"
            OcrPreprocessingProfile.DETAIL -> "细节增强"
            OcrPreprocessingProfile.DOCUMENT -> "文档增强"
            OcrPreprocessingProfile.SCREENSHOT -> "截图增强"
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
