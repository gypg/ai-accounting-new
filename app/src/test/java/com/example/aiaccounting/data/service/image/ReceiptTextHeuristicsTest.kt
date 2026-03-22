package com.example.aiaccounting.data.service.image

import com.example.aiaccounting.data.service.ImageProcessingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptTextHeuristicsTest {

    @Test
    fun extractReceiptSignals_detectsAmountsDatesPaymentMethodsAndMerchants() {
        val text = """
            霸王茶姬
            2026-03-23 18:22
            总计 ￥45.00
            微信支付
        """.trimIndent()

        val signals = ReceiptTextHeuristics.extractReceiptSignals(text)

        assertTrue(signals.amounts.contains("45.00"))
        assertTrue(signals.dates.any { it.contains("2026-03-23") })
        assertTrue(signals.paymentMethods.contains("微信支付"))
        assertTrue(signals.merchants.contains("霸王茶姬"))
        assertTrue(signals.hasStrongReceiptSignals)
    }

    @Test
    fun calculateQualityScore_rewardsReceiptSignalsAndUsefulLabels() {
        val text = """
            霸王茶姬
            2026-03-23 18:22
            总计 ￥45.00
            微信支付
        """.trimIndent()
        val keyLines = listOf("霸王茶姬", "总计 ￥45.00", "微信支付")
        val labels = listOf("Receipt", "Document")
        val signals = ReceiptTextHeuristics.extractReceiptSignals(text)

        val score = ReceiptTextHeuristics.calculateQualityScore(
            rawText = text,
            keyLines = keyLines,
            labels = labels,
            signals = signals
        )

        assertTrue(score >= 70)
        assertEquals(ImageProcessingService.OcrConfidence.HIGH, ReceiptTextHeuristics.toConfidence(score))
    }

    @Test
    fun calculateQualityScore_penalizesNoisyText() {
        val noisyText = "a\n1\n--\n?\n"
        val score = ReceiptTextHeuristics.calculateQualityScore(
            rawText = noisyText,
            keyLines = emptyList(),
            labels = emptyList(),
            signals = ReceiptTextHeuristics.extractReceiptSignals(noisyText)
        )

        assertTrue(score < 40)
        assertEquals(ImageProcessingService.OcrConfidence.LOW, ReceiptTextHeuristics.toConfidence(score))
    }

    @Test
    fun extractKeyLines_prioritizesReceiptSignalsAndDropsTinyNoise() {
        val text = """
            a
            山姆会员店
            总计 ￥199.00
            订单号 123456
            x
            信用卡
        """.trimIndent()

        val keyLines = ReceiptTextHeuristics.extractKeyLines(text)

        assertEquals(listOf("山姆会员店", "总计 ￥199.00", "订单号 123456", "信用卡"), keyLines)
        assertFalse(keyLines.contains("a"))
        assertFalse(keyLines.contains("x"))
    }
}
