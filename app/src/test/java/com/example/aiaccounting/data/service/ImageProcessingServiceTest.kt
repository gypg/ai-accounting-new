package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.service.image.OcrPreprocessingProfile
import com.example.aiaccounting.logging.AppLogLogger
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageProcessingServiceTest {

    @Test
    fun generateCompactPrompt_returnsEmptyString_whenAllResultsAreEmpty() {
        val service = ImageProcessingService(mockk<AppLogLogger>(relaxed = true))
        val emptyResult = ImageProcessingService.ImageAnalysisResult(
            rawText = "",
            text = "",
            keyLines = emptyList(),
            labels = emptyList(),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 0,
            confidence = ImageProcessingService.OcrConfidence.NONE,
            hasContent = false
        )

        val prompt = service.generateCompactPrompt(listOf(emptyResult), "")

        assertEquals("", prompt)
    }

    @Test
    fun generateCompactPrompt_keepsDenseScreenshotText_whenResultHasContent() {
        val service = ImageProcessingService(mockk<AppLogLogger>(relaxed = true))
        val screenshotResult = ImageProcessingService.ImageAnalysisResult(
            rawText = "2026年4月7日\n-12.00 餐饮（早餐：咖啡+三明治）——瑞幸（微信）\n-42.00 餐饮（午餐：麻辣香锅外卖）——美团外卖",
            text = "2026年4月7日\n-12.00 餐饮（早餐：咖啡+三明治）——瑞幸（微信）\n-42.00 餐饮（午餐：麻辣香锅外卖）——美团外卖",
            keyLines = listOf(
                "2026年4月7日",
                "-12.00 餐饮（早餐：咖啡+三明治）——瑞幸（微信）",
                "-42.00 餐饮（午餐：麻辣香锅外卖）——美团外卖"
            ),
            labels = listOf("Screenshot"),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("12.00", "42.00")),
            qualityScore = 88,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            hasContent = true,
            selectedProfile = OcrPreprocessingProfile.SCREENSHOT
        )

        val prompt = service.generateCompactPrompt(listOf(screenshotResult), "帮我识别图片")

        assertTrue(prompt.contains("用户发了1张图片"))
        assertTrue(prompt.contains("-12.00 餐饮"))
        assertTrue(prompt.contains("-42.00 餐饮"))
    }

    @Test
    fun generateCompactPrompt_keepsDenseNonReceiptScreenshotText() {
        val service = ImageProcessingService(mockk<AppLogLogger>(relaxed = true))
        val screenshotResult = ImageProcessingService.ImageAnalysisResult(
            rawText = "2026年4月7日（周二，今天）\n-12.00 餐饮（早餐：咖啡+三明治）——瑞幸（微信）\n-42.00 餐饮（午餐：麻辣香锅外卖）——美团外卖\n-199.00 购物（得物：运动鞋定金）——信用卡",
            text = "2026年4月7日（周二，今天）\n-12.00 餐饮（早餐：咖啡+三明治）——瑞幸（微信）\n-42.00 餐饮（午餐：麻辣香锅外卖）——美团外卖\n-199.00 购物（得物：运动鞋定金）——信用卡",
            keyLines = listOf(
                "2026年4月7日（周二，今天）",
                "-12.00 餐饮（早餐：咖啡+三明治）——瑞幸（微信）",
                "-42.00 餐饮（午餐：麻辣香锅外卖）——美团外卖",
                "-199.00 购物（得物：运动鞋定金）——信用卡"
            ),
            labels = emptyList(),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("12.00", "42.00", "199.00")),
            qualityScore = 76,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            hasContent = true,
            selectedProfile = OcrPreprocessingProfile.SCREENSHOT
        )

        val prompt = service.generateCompactPrompt(listOf(screenshotResult), "")

        assertTrue(prompt.contains("2026年4月7日（周二，今天）"))
        assertTrue(prompt.contains("-199.00 购物"))
    }
}
