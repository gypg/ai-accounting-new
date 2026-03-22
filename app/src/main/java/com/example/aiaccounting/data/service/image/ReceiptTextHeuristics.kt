package com.example.aiaccounting.data.service.image

import com.example.aiaccounting.data.service.ImageProcessingService
import kotlin.math.max
import kotlin.math.min

internal object ReceiptTextHeuristics {
    private val amountRegex = Regex("(?:(?:¥|￥|RMB)\\s*([0-9]{1,6}(?:\\.[0-9]{1,2})?))|([0-9]{2,6}\\.[0-9]{1,2})")
    private val dateRegex = Regex("(?:20\\d{2}[-/.年]\\d{1,2}[-/.月]\\d{1,2}(?:日)?(?:\\s+\\d{1,2}:\\d{2})?)")
    private val paymentMethodKeywords = listOf(
        "微信支付", "支付宝", "信用卡", "借记卡", "银行卡", "云闪付", "现金", "Apple Pay", "银联"
    )
    private val merchantKeywords = listOf(
        "店", "超市", "商场", "餐厅", "茶", "咖啡", "便利店", "会员店", "药房", "酒店", "外卖"
    )
    private val receiptKeywords = listOf(
        "总计", "合计", "实付", "订单号", "交易时间", "支付方式", "金额", "收款", "消费"
    )

    fun extractReceiptSignals(text: String): ImageProcessingService.ReceiptSignals {
        val normalized = text.trim()
        if (normalized.isBlank()) {
            return ImageProcessingService.ReceiptSignals()
        }

        val amounts = amountRegex.findAll(normalized)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                    ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            }
            .filter { value -> value.isNotBlank() && value != "0" }
            .distinct()
            .take(5)
            .toList()

        val dates = dateRegex.findAll(normalized)
            .map { it.value.trim() }
            .distinct()
            .take(3)
            .toList()

        val paymentMethods = paymentMethodKeywords.filter { normalized.contains(it, ignoreCase = true) }
        val merchants = normalized.lines()
            .map { it.trim() }
            .filter { line ->
                line.length >= 3 && merchantKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) }
            }
            .distinct()
            .take(3)

        return ImageProcessingService.ReceiptSignals(
            amounts = amounts,
            dates = dates,
            paymentMethods = paymentMethods,
            merchants = merchants
        )
    }

    fun extractKeyLines(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        return text.lines()
            .map { it.trim() }
            .filter { line -> line.length > 1 }
            .filter { line -> line.any { it.isLetterOrDigit() || it == '¥' || it == '￥' } }
            .filter { line ->
                receiptKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) } ||
                    merchantKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) } ||
                    amountRegex.containsMatchIn(line) ||
                    paymentMethodKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) } ||
                    dateRegex.containsMatchIn(line) ||
                    line.length >= 4
            }
            .distinct()
            .take(10)
    }

    fun calculateQualityScore(
        rawText: String,
        keyLines: List<String>,
        labels: List<String>,
        signals: ImageProcessingService.ReceiptSignals
    ): Int {
        if (rawText.isBlank() && labels.isEmpty()) return 0

        var score = 0
        val normalizedText = rawText.trim()

        if (normalizedText.isNotBlank()) {
            score += min(normalizedText.length / 12, 20)
        }
        score += min(keyLines.size * 8, 30)
        score += min(labels.size * 4, 8)
        score += min(signals.amounts.size * 15, 30)
        score += min(signals.dates.size * 10, 10)
        score += min(signals.paymentMethods.size * 10, 10)
        score += min(signals.merchants.size * 12, 12)

        val noisyShortLines = normalizedText.lines().count { line -> line.trim().length in 1..2 }
        score -= min(noisyShortLines * 5, 20)

        val suspiciousOnlySymbols = normalizedText.lines().count { line ->
            val trimmed = line.trim()
            trimmed.isNotBlank() && trimmed.none { it.isLetterOrDigit() }
        }
        score -= min(suspiciousOnlySymbols * 6, 18)

        val boundedScore = max(0, min(score, 100))
        return if (boundedScore == 0 && normalizedText.isNotBlank()) 5 else boundedScore
    }

    fun toConfidence(score: Int): ImageProcessingService.OcrConfidence {
        return when {
            score >= 70 -> ImageProcessingService.OcrConfidence.HIGH
            score >= 40 -> ImageProcessingService.OcrConfidence.MEDIUM
            score > 0 -> ImageProcessingService.OcrConfidence.LOW
            else -> ImageProcessingService.OcrConfidence.NONE
        }
    }
}
