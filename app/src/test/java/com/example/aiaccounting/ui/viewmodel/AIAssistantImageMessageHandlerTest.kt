package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerPersonality
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.ImageProcessingService
import com.example.aiaccounting.data.service.image.OcrAgreementLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AIAssistantImageMessageHandlerTest {

    @Test
    fun buildImagePrompt_returnsErrorWhenNoHighConfidenceResultExists() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, aiUsageRepository)

        val uri = mockk<Uri>()
        val context = mockk<Context>()
        val butler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )

        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(uri), context, timeoutMs = 8000)
        } returns listOf(
            ImageProcessingService.ImageAnalysisResult(
                rawText = "a\n1\n?",
                text = "",
                keyLines = emptyList(),
                labels = emptyList(),
                receiptSignals = ImageProcessingService.ReceiptSignals(),
                qualityScore = 18,
                confidence = ImageProcessingService.OcrConfidence.LOW,
                hasContent = true
            )
        )

        val result = handler.buildImagePrompt(
            message = "帮我记账",
            imageUris = listOf(uri),
            context = context,
            currentButler = butler,
            isNativeImageSupported = false
        )

        assertTrue(result is ImagePromptResult.Error)
        assertTrue((result as ImagePromptResult.Error).message.contains("置信度"))
    }

    @Test
    fun buildImagePrompt_usesOnlyHighConfidenceResultsInPrompt() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, aiUsageRepository)

        val firstUri = mockk<Uri>()
        val secondUri = mockk<Uri>()
        val context = mockk<Context>()
        val butler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )
        val results = listOf(
            ImageProcessingService.ImageAnalysisResult(
                rawText = "霸王茶姬\n总计 ￥45.00\n微信支付",
                text = "霸王茶姬\n总计 ￥45.00\n微信支付",
                keyLines = listOf("霸王茶姬", "总计 ￥45.00", "微信支付"),
                labels = listOf("Receipt"),
                receiptSignals = ImageProcessingService.ReceiptSignals(
                    amounts = listOf("45.00"),
                    paymentMethods = listOf("微信支付"),
                    merchants = listOf("霸王茶姬")
                ),
                qualityScore = 88,
                confidence = ImageProcessingService.OcrConfidence.HIGH,
                hasContent = true,
                agreementLevel = OcrAgreementLevel.STRONG
            ),
            ImageProcessingService.ImageAnalysisResult(
                rawText = "a\n1\n?",
                text = "",
                keyLines = emptyList(),
                labels = emptyList(),
                receiptSignals = ImageProcessingService.ReceiptSignals(),
                qualityScore = 12,
                confidence = ImageProcessingService.OcrConfidence.LOW,
                hasContent = true
            )
        )

        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(firstUri, secondUri), context, timeoutMs = 8000)
        } returns results
        every {
            imageProcessingService.generateCompactPrompt(any(), eq("帮我记账"))
        } returns "高置信度提示词"

        val result = handler.buildImagePrompt(
            message = "帮我记账",
            imageUris = listOf(firstUri, secondUri),
            context = context,
            currentButler = butler,
            isNativeImageSupported = false
        )

        assertTrue(result is ImagePromptResult.Success)
        assertTrue((result as ImagePromptResult.Success).prompt.contains("高置信度提示词"))
    }

    @Test
    fun processImageMessage_returnsConfigErrorBeforeImageProcessing() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, aiUsageRepository)

        val result = handler.processImageMessage(
            message = "帮我记账",
            imageUris = listOf(mockk()),
            context = mockk(),
            currentButler = Butler(
                id = "cat",
                name = "小财娘",
                title = "可爱管家",
                avatarResId = 0,
                description = "",
                systemPrompt = "你是助手",
                personality = ButlerPersonality.CUTE,
                specialties = emptyList()
            ),
            config = AIConfig(provider = AIProvider.CUSTOM, apiKey = "", apiUrl = "https://example.com", isEnabled = true),
            isNetworkAvailable = true
        )

        assertTrue(result is ImageMessageProcessingResult.Error)
        assertTrue((result as ImageMessageProcessingResult.Error).message.contains("API 密钥"))
    }

    @Test
    fun processImageMessage_shortCircuitsWhenNetworkUnavailable() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, aiUsageRepository)

        val result = handler.processImageMessage(
            message = "帮我记账",
            imageUris = listOf(mockk()),
            context = mockk(),
            currentButler = Butler(
                id = "cat",
                name = "小财娘",
                title = "可爱管家",
                avatarResId = 0,
                description = "",
                systemPrompt = "你是助手",
                personality = ButlerPersonality.CUTE,
                specialties = emptyList()
            ),
            config = AIConfig(provider = AIProvider.CUSTOM, apiKey = "key", apiUrl = "https://example.com", isEnabled = true),
            isNetworkAvailable = false
        )

        assertTrue(result is ImageMessageProcessingResult.Error)
        assertTrue((result as ImageMessageProcessingResult.Error).message.contains("网络不可用"))
        coVerify(exactly = 0) { imageProcessingService.analyzeMultipleImages(any(), any(), any()) }
        verify(exactly = 0) { aiService.isImageSupported(any()) }
    }
}
