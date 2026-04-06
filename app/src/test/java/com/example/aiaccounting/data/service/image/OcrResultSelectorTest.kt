package com.example.aiaccounting.data.service.image

import com.example.aiaccounting.data.service.ImageProcessingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrResultSelectorTest {

    @Test
    fun selectBestCandidate_prefersHigherScoreAndStrongerSignals() {
        val weakCandidate = OcrPassAnalysis(
            profile = OcrPreprocessingProfile.BASE,
            rawText = "总计 45",
            keyLines = listOf("总计 45"),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("45")),
            qualityScore = 52,
            confidence = ImageProcessingService.OcrConfidence.MEDIUM,
            imageQualityMetrics = ImageQualityMetrics(width = 800, height = 600)
        )
        val strongCandidate = OcrPassAnalysis(
            profile = OcrPreprocessingProfile.DOCUMENT,
            rawText = "霸王茶姬\n总计 ￥45.00\n微信支付",
            keyLines = listOf("霸王茶姬", "总计 ￥45.00", "微信支付"),
            receiptSignals = ImageProcessingService.ReceiptSignals(
                amounts = listOf("45.00"),
                paymentMethods = listOf("微信支付"),
                merchants = listOf("霸王茶姬")
            ),
            qualityScore = 81,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            imageQualityMetrics = ImageQualityMetrics(width = 1200, height = 900)
        )

        val selection = OcrResultSelector.selectBestCandidate(listOf(weakCandidate, strongCandidate))

        assertEquals(OcrPreprocessingProfile.DOCUMENT, selection.best.profile)
        assertTrue(selection.agreementLevel == OcrAgreementLevel.WEAK || selection.agreementLevel == OcrAgreementLevel.NONE)
    }

    @Test
    fun selectBestCandidate_marksStrongAgreementWhenSignalsOverlapAcrossPasses() {
        val baseCandidate = OcrPassAnalysis(
            profile = OcrPreprocessingProfile.BASE,
            rawText = "霸王茶姬\n总计 ￥45.00\n微信支付",
            keyLines = listOf("霸王茶姬", "总计 ￥45.00", "微信支付"),
            receiptSignals = ImageProcessingService.ReceiptSignals(
                amounts = listOf("45.00"),
                paymentMethods = listOf("微信支付"),
                merchants = listOf("霸王茶姬")
            ),
            qualityScore = 76,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            imageQualityMetrics = ImageQualityMetrics(width = 1200, height = 900)
        )
        val detailCandidate = OcrPassAnalysis(
            profile = OcrPreprocessingProfile.DETAIL,
            rawText = "霸王茶姬\n总计 ￥45.00\n微信支付",
            keyLines = listOf("霸王茶姬", "总计 ￥45.00", "微信支付"),
            receiptSignals = ImageProcessingService.ReceiptSignals(
                amounts = listOf("45.00"),
                paymentMethods = listOf("微信支付"),
                merchants = listOf("霸王茶姬")
            ),
            qualityScore = 79,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            imageQualityMetrics = ImageQualityMetrics(width = 1200, height = 900)
        )

        val selection = OcrResultSelector.selectBestCandidate(listOf(baseCandidate, detailCandidate))

        assertEquals(OcrAgreementLevel.STRONG, selection.agreementLevel)
        assertEquals(OcrPreprocessingProfile.DETAIL, selection.best.profile)
    }

    @Test
    fun selectBestCandidate_canPreferScreenshotProfileForReceiptScreenshot() {
        val documentCandidate = OcrPassAnalysis(
            profile = OcrPreprocessingProfile.DOCUMENT,
            rawText = "2026年4月1日\n-6.00 餐饮\n-38.00 餐饮",
            keyLines = listOf("2026年4月1日", "-6.00 餐饮", "-38.00 餐饮"),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("6.00", "38.00")),
            qualityScore = 82,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            imageQualityMetrics = ImageQualityMetrics(width = 1280, height = 720)
        )
        val screenshotCandidate = OcrPassAnalysis(
            profile = OcrPreprocessingProfile.SCREENSHOT,
            rawText = "2026年4月1日\n-6.00 餐饮（早餐：煎饼果子）——现金\n-38.00 餐饮（午餐：日式定食）——微信\n-9.90 购物（淘宝：愚人节整蛊玩具）——支付宝",
            keyLines = listOf(
                "2026年4月1日",
                "-6.00 餐饮（早餐：煎饼果子）——现金",
                "-38.00 餐饮（午餐：日式定食）——微信",
                "-9.90 购物（淘宝：愚人节整蛊玩具）——支付宝"
            ),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("6.00", "38.00", "9.90")),
            qualityScore = 92,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            imageQualityMetrics = ImageQualityMetrics(width = 1280, height = 720)
        )

        val selection = OcrResultSelector.selectBestCandidate(listOf(documentCandidate, screenshotCandidate))

        assertEquals(OcrPreprocessingProfile.SCREENSHOT, selection.best.profile)
    }
}
