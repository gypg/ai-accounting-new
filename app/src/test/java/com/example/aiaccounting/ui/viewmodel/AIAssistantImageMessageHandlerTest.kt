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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AIAssistantImageMessageHandlerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun buildImagePrompt_fallsBackToAnyContentWhenNoHighConfidenceResultExists() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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
        val lowConfidenceResult = ImageProcessingService.ImageAnalysisResult(
            rawText = "a\n1\n?",
            text = "模糊票据文本",
            keyLines = emptyList(),
            labels = emptyList(),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 18,
            confidence = ImageProcessingService.OcrConfidence.LOW,
            hasContent = true
        )

        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(uri), context, timeoutMs = 15000)
        } returns listOf(lowConfidenceResult)
        every {
            imageProcessingService.generateCompactPrompt(listOf(lowConfidenceResult), eq("帮我记账"))
        } returns "低置信度也继续送云端"

        val result = handler.buildImagePrompt(
            message = "帮我记账",
            imageUris = listOf(uri),
            context = context,
            currentButler = butler,
            isNativeImageSupported = false
        )

        assertTrue(result is ImagePromptResult.Success)
        assertEquals("低置信度也继续送云端", (result as ImagePromptResult.Success).prompt)
    }

    @Test
    fun buildImagePrompt_usesOnlyHighConfidenceResultsInPrompt() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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
            imageProcessingService.analyzeMultipleImages(listOf(firstUri, secondUri), context, timeoutMs = 15000)
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

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(result is ImagePromptResult.Success)
        assertTrue((result as ImagePromptResult.Success).prompt.contains("高置信度提示词"))
    }

    @Test
    fun processImageMessage_returnsConfigErrorBeforeImageProcessing() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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

    @Test
    fun processImageMessage_usesNativeImagePathWhenModelSupportsNativeImage() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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
        val config = AIConfig(provider = AIProvider.CUSTOM, apiKey = "key", apiUrl = "https://example.com", isEnabled = true)

        every { aiService.isImageSupported(config) } returns true
        coEvery {
            aiService.analyzeImageAndRecord(
                imageUri = firstUri,
                config = config,
                context = context,
                promptContext = any()
            )
        } returns com.example.aiaccounting.data.service.ImageAnalysisResult(
            success = true,
            message = "第一张识别成功",
            actions = null
        )
        coEvery {
            aiService.analyzeImageAndRecord(
                imageUri = secondUri,
                config = config,
                context = context,
                promptContext = any()
            )
        } returns com.example.aiaccounting.data.service.ImageAnalysisResult(
            success = true,
            message = "第二张识别成功",
            actions = null
        )

        val result = handler.processImageMessage(
            message = "帮我看下这两张票据",
            imageUris = listOf(firstUri, secondUri),
            context = context,
            currentButler = butler,
            config = config,
            isNetworkAvailable = true
        )

        assertTrue(result is ImageMessageProcessingResult.TextReply)
        assertEquals("第一张识别成功\n\n第二张识别成功", (result as ImageMessageProcessingResult.TextReply).message)
        coVerify(exactly = 0) { imageProcessingService.analyzeMultipleImages(any(), any(), any()) }
        coVerify(exactly = 2) { aiService.analyzeImageAndRecord(any(), config, context, any()) }
    }

    @Test
    fun processImageMessage_nativePath_returnsExecutableEnvelope_whenActionsPresent() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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
        val config = AIConfig(provider = AIProvider.CUSTOM, apiKey = "key", apiUrl = "https://example.com", isEnabled = true)

        every { aiService.isImageSupported(config) } returns true
        coEvery {
            aiService.analyzeImageAndRecord(
                imageUri = uri,
                config = config,
                context = context,
                promptContext = any()
            )
        } returns com.example.aiaccounting.data.service.ImageAnalysisResult(
            success = true,
            message = "已识别到账单",
            actions = listOf(
                com.example.aiaccounting.data.service.ImageAction(
                    action = "add_transaction",
                    amount = 56.0,
                    type = "expense",
                    category = "餐饮",
                    account = "微信",
                    note = "午餐"
                )
            )
        )

        val result = handler.processImageMessage(
            message = "帮我记账",
            imageUris = listOf(uri),
            context = context,
            currentButler = butler,
            config = config,
            isNetworkAvailable = true
        )

        assertTrue(result is ImageMessageProcessingResult.ExecuteEnvelope)
        val envelope = (result as ImageMessageProcessingResult.ExecuteEnvelope).envelope
        assertEquals(1, envelope.actions.size)
        assertTrue(envelope.actions.first() is AIAssistantTypedAction.AddTransaction)
        assertEquals("已识别到账单", envelope.reply)
    }

    @Test
    fun processImageMessage_nonNativePath_returnsBookkeepingPromptForRemoteExecution() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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
        val config = AIConfig(provider = AIProvider.CUSTOM, apiKey = "key", apiUrl = "https://example.com", isEnabled = true)

        every { aiService.isImageSupported(config) } returns false
        val analysis = ImageProcessingService.ImageAnalysisResult(
            rawText = "瑞幸 12.00 微信支付",
            text = "瑞幸 12.00 微信支付",
            keyLines = listOf("瑞幸", "12.00", "微信支付"),
            labels = listOf("Receipt"),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("12.00")),
            qualityScore = 80,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            hasContent = true
        )
        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(uri), context, timeoutMs = 15000)
        } returns listOf(analysis)
        every {
            imageProcessingService.generateCompactPrompt(listOf(analysis), "帮我记账")
        } returns "这是给远端记账执行的prompt"

        val result = handler.processImageMessage(
            message = "帮我记账",
            imageUris = listOf(uri),
            context = context,
            currentButler = butler,
            config = config,
            isNetworkAvailable = true
        )

        assertTrue(result is ImageMessageProcessingResult.RemoteBookkeepingPrompt)
        assertEquals("这是给远端记账执行的prompt", (result as ImageMessageProcessingResult.RemoteBookkeepingPrompt).prompt)
        coVerify(exactly = 0) { aiService.chat(any(), any()) }
    }

    @Test
    fun buildImagePrompt_whenLowConfidenceButHasRawText_stillBuildsPrompt() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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

        val lowConfidenceResult = ImageProcessingService.ImageAnalysisResult(
            rawText = "a\n1\n?",
            text = "",
            keyLines = emptyList(),
            labels = emptyList(),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 18,
            confidence = ImageProcessingService.OcrConfidence.LOW,
            hasContent = true
        )

        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(uri), context, timeoutMs = 15000)
        } returns listOf(lowConfidenceResult)
        every {
            imageProcessingService.generateCompactPrompt(listOf(lowConfidenceResult), "帮我记账")
        } returns "低置信文字也送云端"

        val result = handler.buildImagePrompt(
            message = "帮我记账",
            imageUris = listOf(uri),
            context = context,
            currentButler = butler,
            isNativeImageSupported = false
        )

        assertTrue(result is ImagePromptResult.Success)
        assertEquals("低置信文字也送云端", (result as ImagePromptResult.Success).prompt)
    }

    @Test
    fun buildImagePrompt_whenOnlyLabelsExist_stillBuildsPrompt() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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

        val labelsOnlyResult = ImageProcessingService.ImageAnalysisResult(
            rawText = "",
            text = "",
            keyLines = emptyList(),
            labels = listOf("Receipt"),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 8,
            confidence = ImageProcessingService.OcrConfidence.LOW,
            hasContent = true
        )

        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(uri), context, timeoutMs = 15000)
        } returns listOf(labelsOnlyResult)
        every {
            imageProcessingService.generateCompactPrompt(listOf(labelsOnlyResult), "帮我记账")
        } returns "标签也允许继续送云端"

        val result = handler.buildImagePrompt(
            message = "帮我记账",
            imageUris = listOf(uri),
            context = context,
            currentButler = butler,
            isNativeImageSupported = false
        )

        assertTrue(result is ImagePromptResult.Success)
        assertEquals("标签也允许继续送云端", (result as ImagePromptResult.Success).prompt)
    }

    @Test
    fun buildImagePrompt_whenBatchAnalysisReturnsAllEmpty_retriesPerImageAndSucceedsIfAnyImageHasContent() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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
        val recoveredResult = ImageProcessingService.ImageAnalysisResult(
            rawText = "瑞幸\n总计 12.00\n微信支付",
            text = "瑞幸\n总计 12.00\n微信支付",
            keyLines = listOf("瑞幸", "总计 12.00", "微信支付"),
            labels = listOf("Receipt"),
            receiptSignals = ImageProcessingService.ReceiptSignals(
                amounts = listOf("12.00"),
                paymentMethods = listOf("微信支付"),
                merchants = listOf("瑞幸")
            ),
            qualityScore = 82,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            hasContent = true,
            agreementLevel = OcrAgreementLevel.STRONG
        )

        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(firstUri, secondUri), context, timeoutMs = 15000)
        } returns listOf(emptyResult, emptyResult)
        coEvery {
            imageProcessingService.analyzeImage(firstUri, context, 10000)
        } returns recoveredResult
        coEvery {
            imageProcessingService.analyzeImage(secondUri, context, 10000)
        } returns emptyResult
        every {
            imageProcessingService.generateCompactPrompt(listOf(recoveredResult), "帮我记账")
        } returns "恢复成功提示词"

        val result = handler.buildImagePrompt(
            message = "帮我记账",
            imageUris = listOf(firstUri, secondUri),
            context = context,
            currentButler = butler,
            isNativeImageSupported = false
        )

        assertTrue(result is ImagePromptResult.Success)
        assertEquals("恢复成功提示词", (result as ImagePromptResult.Success).prompt)
        coVerify(exactly = 1) { imageProcessingService.analyzeImage(firstUri, context, 10000) }
        coVerify(exactly = 1) { imageProcessingService.analyzeImage(secondUri, context, 10000) }
    }

    @Test
    fun buildImagePrompt_whenBatchAndPerImageFallbackAllEmpty_returnsNoTextError() = runTest {
        val aiService = mockk<AIService>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>()
        val handler = AIAssistantImageMessageHandler(aiService, imageProcessingService, mockk<AIUsageRepository>(relaxed = true))

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

        coEvery {
            imageProcessingService.analyzeMultipleImages(listOf(firstUri, secondUri), context, timeoutMs = 15000)
        } returns listOf(emptyResult, emptyResult)
        coEvery {
            imageProcessingService.analyzeImage(firstUri, context, 10000)
        } returns emptyResult
        coEvery {
            imageProcessingService.analyzeImage(secondUri, context, 10000)
        } returns emptyResult

        val result = handler.buildImagePrompt(
            message = "帮我记账",
            imageUris = listOf(firstUri, secondUri),
            context = context,
            currentButler = butler,
            isNativeImageSupported = false
        )

        assertTrue(result is ImagePromptResult.Error)
        assertTrue((result as ImagePromptResult.Error).message.contains("没有在图片里识别到文字或标签"))
    }
}
