package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.AIReasoningEngine
import com.example.aiaccounting.ai.TransactionModificationHandler
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.ChatSession
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerPersonality
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIConversationRepository
import com.example.aiaccounting.data.repository.AIUsageRepository
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.ButlerRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.ChatSessionRepository
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.ImageAction
import com.example.aiaccounting.data.service.ImageAnalysisResult
import com.example.aiaccounting.data.service.ImageProcessingService
import com.example.aiaccounting.logging.AppLogLogger
import com.example.aiaccounting.utils.NetworkUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.cancel

@OptIn(ExperimentalCoroutinesApi::class)
class AIAssistantViewModelImageFlowTest {

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
    fun sendMessageWithImages_whenNativeImageFails_fallsBackToOcrAndStillReturnsErrorWithoutRemoteChatPrompt() = runTest {
        val conversationRepository = mockk<AIConversationRepository>(relaxed = true)
        val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val aiService = mockk<AIService>(relaxed = true)
        val aiConfigRepository = mockk<AIConfigRepository>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val networkUtils = mockk<NetworkUtils>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val chatSessionRepository = mockk<ChatSessionRepository>(relaxed = true)
        val butlerRepository = mockk<ButlerRepository>(relaxed = true)
        val aiReasoningEngine = mockk<AIReasoningEngine>(relaxed = true)
        val transactionModificationHandler = mockk<TransactionModificationHandler>(relaxed = true)
        val actionExecutor = mockk<AIAssistantActionExecutor>(relaxed = true)
        val appLogLogger = mockk<AppLogLogger>(relaxed = true)

        val sessionId = "session-native-fallback"
        val defaultButler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )
        val emptyAnalysis = ImageProcessingService.ImageAnalysisResult(
            rawText = "",
            text = "",
            keyLines = emptyList(),
            labels = emptyList(),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 0,
            confidence = ImageProcessingService.OcrConfidence.NONE,
            hasContent = false
        )
        val savedMessages = mutableListOf<String>()

        every { aiConfigRepository.getAIConfig() } returns flowOf(
            AIConfig(provider = AIProvider.CUSTOM, apiKey = "key", apiUrl = "https://example.com", model = "qwen-vl", isEnabled = true)
        )
        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { networkUtils.isNetworkAvailable() } returns true
        every { butlerRepository.currentButlerId } returns flowOf(defaultButler.id)
        coEvery { butlerRepository.getButlerByIdSuspend(defaultButler.id) } returns defaultButler
        coEvery { butlerRepository.getCurrentButler() } returns defaultButler
        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = sessionId, title = "新对话", isActive = true)))
        coEvery { chatSessionRepository.addMessage(any(), any(), any(), any()) } answers {
            savedMessages += thirdArg<String>()
            Unit
        }
        every { aiService.isImageSupported(any()) } returns true
        coEvery { aiService.analyzeImageAndRecord(any(), any(), any(), any(), any()) } returns ImageAnalysisResult(
            success = false,
            message = "图片不清晰",
            actions = null
        )
        coEvery { imageProcessingService.analyzeMultipleImages(any(), any(), any()) } returns listOf(emptyAnalysis)
        coEvery { imageProcessingService.analyzeImage(any(), any(), any()) } returns emptyAnalysis
        every { imageProcessingService.generateCompactPrompt(any(), any()) } returns ""

        val viewModel = AIAssistantViewModel(
            conversationRepository = conversationRepository,
            aiOperationExecutor = aiOperationExecutor,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            aiService = aiService,
            aiConfigRepository = aiConfigRepository,
            aiUsageRepository = aiUsageRepository,
            networkUtils = networkUtils,
            imageProcessingService = imageProcessingService,
            chatSessionRepository = chatSessionRepository,
            butlerRepository = butlerRepository,
            aiReasoningEngine = aiReasoningEngine,
            transactionModificationHandler = transactionModificationHandler,
            actionExecutor = actionExecutor,
            appLogLogger = appLogLogger
        )
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            viewModel.sendMessageWithImages(
                message = "",
                imageUris = listOf(mockk<Uri>()),
                context = mockk<Context>()
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(atMost = 1) { aiService.analyzeImageAndRecord(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { aiService.sendChatStream(any(), any()) }
            assertTrue(savedMessages.size >= 2)
            assertTrue(savedMessages.last().contains("没有在图片里识别到文字或标签") || savedMessages.last().contains("没有在图片里识别到有效内容") || savedMessages.last().contains("图片不清晰"))
        } finally {
            viewModel.viewModelScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun sendMessageWithImages_logsTraceForStartAndFinish() = runTest {
        val conversationRepository = mockk<AIConversationRepository>(relaxed = true)
        val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val aiService = mockk<AIService>(relaxed = true)
        val aiConfigRepository = mockk<AIConfigRepository>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val networkUtils = mockk<NetworkUtils>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val chatSessionRepository = mockk<ChatSessionRepository>(relaxed = true)
        val butlerRepository = mockk<ButlerRepository>(relaxed = true)
        val aiReasoningEngine = mockk<AIReasoningEngine>(relaxed = true)
        val transactionModificationHandler = mockk<TransactionModificationHandler>(relaxed = true)
        val actionExecutor = mockk<AIAssistantActionExecutor>(relaxed = true)
        val appLogLogger = mockk<AppLogLogger>(relaxed = true)

        val sessionId = "session-log"
        val defaultButler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )
        val emptyAnalysis = ImageProcessingService.ImageAnalysisResult(
            rawText = "",
            text = "",
            keyLines = emptyList(),
            labels = emptyList(),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 0,
            confidence = ImageProcessingService.OcrConfidence.NONE,
            hasContent = false
        )

        every { aiConfigRepository.getAIConfig() } returns flowOf(
            AIConfig(provider = AIProvider.CUSTOM, apiKey = "key", apiUrl = "https://example.com", model = "qwen-plus", isEnabled = true)
        )
        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { networkUtils.isNetworkAvailable() } returns true
        every { butlerRepository.currentButlerId } returns flowOf(defaultButler.id)
        coEvery { butlerRepository.getButlerByIdSuspend(defaultButler.id) } returns defaultButler
        coEvery { butlerRepository.getCurrentButler() } returns defaultButler
        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = sessionId, title = "新对话", isActive = true)))
        coEvery { chatSessionRepository.addMessage(any(), any(), any(), any()) } returns Unit
        every { aiService.isImageSupported(any()) } returns false
        coEvery { imageProcessingService.analyzeMultipleImages(any(), any(), any(), any()) } returns listOf(emptyAnalysis)
        coEvery { imageProcessingService.analyzeImage(any(), any(), any(), any(), any()) } returns emptyAnalysis
        every { imageProcessingService.generateCompactPrompt(any(), any()) } returns ""

        val viewModel = AIAssistantViewModel(
            conversationRepository = conversationRepository,
            aiOperationExecutor = aiOperationExecutor,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            aiService = aiService,
            aiConfigRepository = aiConfigRepository,
            aiUsageRepository = aiUsageRepository,
            networkUtils = networkUtils,
            imageProcessingService = imageProcessingService,
            chatSessionRepository = chatSessionRepository,
            butlerRepository = butlerRepository,
            aiReasoningEngine = aiReasoningEngine,
            transactionModificationHandler = transactionModificationHandler,
            actionExecutor = actionExecutor,
            appLogLogger = appLogLogger
        )
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            viewModel.sendMessageWithImages(
                message = "帮我记账",
                imageUris = listOf(mockk<Uri>()),
                context = mockk<Context>()
            )
            testDispatcher.scheduler.advanceUntilIdle()

            verify(atLeast = 1) {
                appLogLogger.info(
                    source = "AI",
                    category = "image_send_start",
                    message = any(),
                    details = any(),
                    traceId = any(),
                    entityType = any(),
                    entityId = any()
                )
            }
            verify(atLeast = 1) {
                appLogLogger.info(
                    source = "AI",
                    category = "image_send_finish",
                    message = any(),
                    details = any(),
                    traceId = any(),
                    entityType = any(),
                    entityId = any()
                )
            }
        } finally {
            viewModel.viewModelScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun sendMessageWithImages_whenNativeImageReturnsActions_executesQueryBeforeExecution() = runTest {
        val conversationRepository = mockk<AIConversationRepository>(relaxed = true)
        val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val aiService = mockk<AIService>(relaxed = true)
        val aiConfigRepository = mockk<AIConfigRepository>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val networkUtils = mockk<NetworkUtils>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val chatSessionRepository = mockk<ChatSessionRepository>(relaxed = true)
        val butlerRepository = mockk<ButlerRepository>(relaxed = true)
        val aiReasoningEngine = mockk<AIReasoningEngine>(relaxed = true)
        val transactionModificationHandler = mockk<TransactionModificationHandler>(relaxed = true)
        val actionExecutor = mockk<AIAssistantActionExecutor>(relaxed = true)
        val appLogLogger = mockk<AppLogLogger>(relaxed = true)

        val sessionId = "session-1"
        val defaultButler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )

        every { aiConfigRepository.getAIConfig() } returns flowOf(
            AIConfig(
                provider = AIProvider.CUSTOM,
                apiKey = "key",
                apiUrl = "https://example.com",
                model = "qwen-vl",
                isEnabled = true
            )
        )
        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { networkUtils.isNetworkAvailable() } returns true
        every { butlerRepository.currentButlerId } returns flowOf(defaultButler.id)
        coEvery { butlerRepository.getButlerByIdSuspend(defaultButler.id) } returns defaultButler
        coEvery { butlerRepository.getCurrentButler() } returns defaultButler

        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = sessionId, title = "新对话", isActive = true)))
        coEvery { chatSessionRepository.addMessage(any(), any(), any(), any()) } returns Unit

        every { aiService.isImageSupported(any()) } returns true
        coEvery {
            aiService.analyzeImageAndRecord(any(), any(), any(), any())
        } returns ImageAnalysisResult(
            success = true,
            message = "识别到账单",
            actions = listOf(
                ImageAction(
                    action = "add_transaction",
                    amount = 56.0,
                    type = "expense",
                    category = "餐饮",
                    account = "微信",
                    note = "午餐"
                )
            )
        )

        coEvery { actionExecutor.executeQueryBeforeExecution(any()) } returns "记账完成：成功1笔"

        val viewModel = AIAssistantViewModel(
            conversationRepository = conversationRepository,
            aiOperationExecutor = aiOperationExecutor,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            aiService = aiService,
            aiConfigRepository = aiConfigRepository,
            aiUsageRepository = aiUsageRepository,
            networkUtils = networkUtils,
            imageProcessingService = imageProcessingService,
            chatSessionRepository = chatSessionRepository,
            butlerRepository = butlerRepository,
            aiReasoningEngine = aiReasoningEngine,
            transactionModificationHandler = transactionModificationHandler,
            actionExecutor = actionExecutor,
            appLogLogger = appLogLogger
        )
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            viewModel.sendMessageWithImages(
                message = "帮我记账",
                imageUris = listOf(mockk<Uri>()),
                context = mockk<Context>()
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { actionExecutor.executeQueryBeforeExecution(any()) }
            verify(exactly = 0) { aiService.sendChatStream(any(), any()) }
        } finally {
            viewModel.viewModelScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun sendMessageWithImages_whenOcrPathHasNoContent_returnsImageErrorWithoutRemoteExecution() = runTest {
        val conversationRepository = mockk<AIConversationRepository>(relaxed = true)
        val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val aiService = mockk<AIService>(relaxed = true)
        val aiConfigRepository = mockk<AIConfigRepository>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val networkUtils = mockk<NetworkUtils>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val chatSessionRepository = mockk<ChatSessionRepository>(relaxed = true)
        val butlerRepository = mockk<ButlerRepository>(relaxed = true)
        val aiReasoningEngine = mockk<AIReasoningEngine>(relaxed = true)
        val transactionModificationHandler = mockk<TransactionModificationHandler>(relaxed = true)
        val actionExecutor = mockk<AIAssistantActionExecutor>(relaxed = true)
        val appLogLogger = mockk<AppLogLogger>(relaxed = true)

        val sessionId = "session-ocr-empty"
        val defaultButler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )
        val savedMessages = mutableListOf<String>()
        val emptyAnalysis = ImageProcessingService.ImageAnalysisResult(
            rawText = "",
            text = "",
            keyLines = emptyList(),
            labels = emptyList(),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 0,
            confidence = ImageProcessingService.OcrConfidence.NONE,
            hasContent = false
        )

        every { aiConfigRepository.getAIConfig() } returns flowOf(
            AIConfig(
                provider = AIProvider.CUSTOM,
                apiKey = "key",
                apiUrl = "https://example.com",
                model = "qwen-plus",
                isEnabled = true
            )
        )
        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { networkUtils.isNetworkAvailable() } returns true
        every { butlerRepository.currentButlerId } returns flowOf(defaultButler.id)
        coEvery { butlerRepository.getButlerByIdSuspend(defaultButler.id) } returns defaultButler
        coEvery { butlerRepository.getCurrentButler() } returns defaultButler
        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = sessionId, title = "新对话", isActive = true)))
        coEvery { chatSessionRepository.addMessage(any(), any(), any(), any()) } answers {
            savedMessages += thirdArg<String>()
            Unit
        }
        every { aiService.isImageSupported(any()) } returns false
        coEvery { imageProcessingService.analyzeMultipleImages(any(), any(), any()) } returns listOf(emptyAnalysis)
        coEvery { imageProcessingService.analyzeImage(any(), any(), any()) } returns emptyAnalysis
        every { imageProcessingService.generateCompactPrompt(any(), any()) } returns ""

        val viewModel = AIAssistantViewModel(
            conversationRepository = conversationRepository,
            aiOperationExecutor = aiOperationExecutor,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            aiService = aiService,
            aiConfigRepository = aiConfigRepository,
            aiUsageRepository = aiUsageRepository,
            networkUtils = networkUtils,
            imageProcessingService = imageProcessingService,
            chatSessionRepository = chatSessionRepository,
            butlerRepository = butlerRepository,
            aiReasoningEngine = aiReasoningEngine,
            transactionModificationHandler = transactionModificationHandler,
            actionExecutor = actionExecutor,
            appLogLogger = appLogLogger
        )
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            viewModel.sendMessageWithImages(
                message = "帮我记账",
                imageUris = listOf(mockk<Uri>()),
                context = mockk<Context>()
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { aiService.sendChatStream(any(), any()) }
            coVerify(exactly = 0) { actionExecutor.executeAIActions(any<com.example.aiaccounting.ui.viewmodel.AIAssistantActionEnvelope>()) }
            coVerify(exactly = 0) { actionExecutor.executeQueryBeforeExecution(any()) }
            assertTrue(savedMessages.last().contains("没有在图片里识别到文字或标签"))
        } finally {
            viewModel.viewModelScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun sendMessageWithImages_whenOcrPath_returnsRemotePrompt_usesBookkeepingRemoteExecutionPath() = runTest {
        val conversationRepository = mockk<AIConversationRepository>(relaxed = true)
        val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val aiService = mockk<AIService>(relaxed = true)
        val aiConfigRepository = mockk<AIConfigRepository>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val networkUtils = mockk<NetworkUtils>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val chatSessionRepository = mockk<ChatSessionRepository>(relaxed = true)
        val butlerRepository = mockk<ButlerRepository>(relaxed = true)
        val aiReasoningEngine = mockk<AIReasoningEngine>(relaxed = true)
        val transactionModificationHandler = mockk<TransactionModificationHandler>(relaxed = true)
        val actionExecutor = mockk<AIAssistantActionExecutor>(relaxed = true)
        val appLogLogger = mockk<AppLogLogger>(relaxed = true)

        val sessionId = "session-2"
        val defaultButler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )

        every { aiConfigRepository.getAIConfig() } returns flowOf(
            AIConfig(
                provider = AIProvider.CUSTOM,
                apiKey = "key",
                apiUrl = "https://example.com",
                model = "qwen-plus",
                isEnabled = true
            )
        )
        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { networkUtils.isNetworkAvailable() } returns true
        every { butlerRepository.currentButlerId } returns flowOf(defaultButler.id)
        coEvery { butlerRepository.getButlerByIdSuspend(defaultButler.id) } returns defaultButler
        coEvery { butlerRepository.getCurrentButler() } returns defaultButler

        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = sessionId, title = "新对话", isActive = true)))
        coEvery { chatSessionRepository.addMessage(any(), any(), any(), any()) } returns Unit

        every { aiService.isImageSupported(any()) } returns false
        val analysis = ImageProcessingService.ImageAnalysisResult(
            rawText = "瑞幸 12.00 微信支付",
            text = "瑞幸 12.00 微信支付",
            keyLines = listOf("瑞幸", "12.00", "微信支付"),
            labels = listOf("Receipt"),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("12.00")),
            qualityScore = 85,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            hasContent = true
        )
        coEvery { imageProcessingService.analyzeMultipleImages(any(), any(), any()) } returns listOf(analysis)
        every { imageProcessingService.generateCompactPrompt(any(), any()) } returns "OCR记账prompt"

        coEvery { accountRepository.getAllAccountsList() } returns listOf(
            Account(id = 1L, name = "微信", type = AccountType.WECHAT, balance = 100.0)
        )
        coEvery { categoryRepository.getAllCategoriesList() } returns listOf(
            Category(id = 10L, name = "餐饮", type = TransactionType.EXPENSE)
        )
        coEvery { aiUsageRepository.recordCall(any()) } returns Unit
        coEvery {
            aiService.sendChatStream(any(), any())
        } returns flowOf("{\"actions\":[{\"action\":\"query_accounts\"}],\"reply\":\"已整理\"}")

        coEvery { actionExecutor.executeAIActions(any<com.example.aiaccounting.ui.viewmodel.AIAssistantActionEnvelope>()) } returns "执行完成"

        val viewModel = AIAssistantViewModel(
            conversationRepository = conversationRepository,
            aiOperationExecutor = aiOperationExecutor,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            aiService = aiService,
            aiConfigRepository = aiConfigRepository,
            aiUsageRepository = aiUsageRepository,
            networkUtils = networkUtils,
            imageProcessingService = imageProcessingService,
            chatSessionRepository = chatSessionRepository,
            butlerRepository = butlerRepository,
            aiReasoningEngine = aiReasoningEngine,
            transactionModificationHandler = transactionModificationHandler,
            actionExecutor = actionExecutor,
            appLogLogger = appLogLogger
        )
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            viewModel.sendMessageWithImages(
                message = "帮我记账",
                imageUris = listOf(mockk<Uri>()),
                context = mockk<Context>()
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { aiService.sendChatStream(any(), any()) }
            coVerify(exactly = 1) { actionExecutor.executeAIActions(any<com.example.aiaccounting.ui.viewmodel.AIAssistantActionEnvelope>()) }
            coVerify(exactly = 0) { actionExecutor.executeQueryBeforeExecution(any()) }
        } finally {
            viewModel.viewModelScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun sendMessageWithImages_whenOcrPathHasLongReceipt_preservesLaterLinesInPrompt() = runTest {
        val conversationRepository = mockk<AIConversationRepository>(relaxed = true)
        val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val aiService = mockk<AIService>(relaxed = true)
        val aiConfigRepository = mockk<AIConfigRepository>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val networkUtils = mockk<NetworkUtils>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val chatSessionRepository = mockk<ChatSessionRepository>(relaxed = true)
        val butlerRepository = mockk<ButlerRepository>(relaxed = true)
        val aiReasoningEngine = mockk<AIReasoningEngine>(relaxed = true)
        val transactionModificationHandler = mockk<TransactionModificationHandler>(relaxed = true)
        val actionExecutor = mockk<AIAssistantActionExecutor>(relaxed = true)
        val appLogLogger = mockk<AppLogLogger>(relaxed = true)

        val sessionId = "session-3"
        val defaultButler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )

        every { aiConfigRepository.getAIConfig() } returns flowOf(
            AIConfig(
                provider = AIProvider.CUSTOM,
                apiKey = "key",
                apiUrl = "https://example.com",
                model = "qwen-plus",
                isEnabled = true
            )
        )
        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { networkUtils.isNetworkAvailable() } returns true
        every { butlerRepository.currentButlerId } returns flowOf(defaultButler.id)
        coEvery { butlerRepository.getButlerByIdSuspend(defaultButler.id) } returns defaultButler
        coEvery { butlerRepository.getCurrentButler() } returns defaultButler
        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = sessionId, title = "新对话", isActive = true)))
        coEvery { chatSessionRepository.addMessage(any(), any(), any(), any()) } returns Unit
        every { aiService.isImageSupported(any()) } returns false

        val analysis = ImageProcessingService.ImageAnalysisResult(
            rawText = "long receipt",
            text = "第一笔\n第二笔\n第三笔\n第四笔\n第五笔\n第六笔\n第七笔",
            keyLines = listOf("第一笔", "第二笔", "第三笔", "第四笔", "第五笔", "第六笔", "第七笔"),
            labels = listOf("Receipt"),
            receiptSignals = ImageProcessingService.ReceiptSignals(amounts = listOf("6.00", "38.00", "9.90", "25.00", "12.00", "188.00", "5000.00")),
            qualityScore = 90,
            confidence = ImageProcessingService.OcrConfidence.HIGH,
            hasContent = true
        )
        coEvery { imageProcessingService.analyzeMultipleImages(any(), any(), any()) } returns listOf(analysis)
        every { imageProcessingService.generateCompactPrompt(any(), any()) } answers {
            val results = firstArg<List<ImageProcessingService.ImageAnalysisResult>>()
            results.first().text
        }

        coEvery { accountRepository.getAllAccountsList() } returns emptyList()
        coEvery { categoryRepository.getAllCategoriesList() } returns emptyList()
        coEvery { aiUsageRepository.recordCall(any()) } returns Unit
        coEvery { aiService.sendChatStream(any(), any()) } returns flowOf("{\"actions\":[],\"reply\":\"已整理\"}")
        coEvery { actionExecutor.executeAIActions(any<com.example.aiaccounting.ui.viewmodel.AIAssistantActionEnvelope>()) } returns "执行完成"

        val viewModel = AIAssistantViewModel(
            conversationRepository = conversationRepository,
            aiOperationExecutor = aiOperationExecutor,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            aiService = aiService,
            aiConfigRepository = aiConfigRepository,
            aiUsageRepository = aiUsageRepository,
            networkUtils = networkUtils,
            imageProcessingService = imageProcessingService,
            chatSessionRepository = chatSessionRepository,
            butlerRepository = butlerRepository,
            aiReasoningEngine = aiReasoningEngine,
            transactionModificationHandler = transactionModificationHandler,
            actionExecutor = actionExecutor,
            appLogLogger = appLogLogger
        )
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            viewModel.sendMessageWithImages(
                message = "帮我记账",
                imageUris = listOf(mockk<Uri>()),
                context = mockk<Context>()
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(atLeast = 1) {
                aiService.sendChatStream(withArg { messages ->
                    val joined = messages.joinToString("\n") { it.content }
                    assert(joined.contains("第五笔"))
                    assert(joined.contains("第七笔"))
                }, any())
            }
        } finally {
            viewModel.viewModelScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun sendMessageWithImages_whenUserAsksImageContent_usesImageUnderstandingInsteadOfBookkeepingClarification() = runTest {
        val conversationRepository = mockk<AIConversationRepository>(relaxed = true)
        val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val aiService = mockk<AIService>(relaxed = true)
        val aiConfigRepository = mockk<AIConfigRepository>(relaxed = true)
        val aiUsageRepository = mockk<AIUsageRepository>(relaxed = true)
        val networkUtils = mockk<NetworkUtils>(relaxed = true)
        val imageProcessingService = mockk<ImageProcessingService>(relaxed = true)
        val chatSessionRepository = mockk<ChatSessionRepository>(relaxed = true)
        val butlerRepository = mockk<ButlerRepository>(relaxed = true)
        val aiReasoningEngine = mockk<AIReasoningEngine>(relaxed = true)
        val transactionModificationHandler = mockk<TransactionModificationHandler>(relaxed = true)
        val actionExecutor = mockk<AIAssistantActionExecutor>(relaxed = true)
        val appLogLogger = mockk<AppLogLogger>(relaxed = true)

        val sessionId = "session-image-understanding"
        val defaultButler = Butler(
            id = "cat",
            name = "小财娘",
            title = "可爱管家",
            avatarResId = 0,
            description = "",
            systemPrompt = "你是助手",
            personality = ButlerPersonality.CUTE,
            specialties = emptyList()
        )

        every { aiConfigRepository.getAIConfig() } returns flowOf(
            AIConfig(
                provider = AIProvider.CUSTOM,
                apiKey = "key",
                apiUrl = "https://example.com",
                model = "qwen-plus",
                isEnabled = true
            )
        )
        every { aiConfigRepository.getUseBuiltin() } returns flowOf(false)
        every { networkUtils.isNetworkAvailable() } returns true
        every { butlerRepository.currentButlerId } returns flowOf(defaultButler.id)
        coEvery { butlerRepository.getButlerByIdSuspend(defaultButler.id) } returns defaultButler
        coEvery { butlerRepository.getCurrentButler() } returns defaultButler
        val conversationMessages = mutableListOf<String>()
        every { chatSessionRepository.getAllSessions() } returns flowOf(listOf(ChatSession(id = sessionId, title = "新对话", isActive = true)))
        coEvery { chatSessionRepository.addMessage(any(), any(), any(), any()) } answers {
            conversationMessages += thirdArg<String>()
            Unit
        }
        every { aiService.isImageSupported(any()) } returns false
        val weakAnalysis = ImageProcessingService.ImageAnalysisResult(
            rawText = "claude-haiku-4-5-20251001 64GF 1750.52 MB",
            text = "claude-haiku-4-5-20251001 64GF 1750.52 MB",
            keyLines = listOf("claude-haiku-4-5-20251001", "64GF", "1750.52 MB"),
            labels = listOf("Screenshot"),
            receiptSignals = ImageProcessingService.ReceiptSignals(),
            qualityScore = 46,
            confidence = ImageProcessingService.OcrConfidence.MEDIUM,
            hasContent = true
        )
        coEvery { imageProcessingService.analyzeMultipleImages(any(), any(), any()) } returns listOf(weakAnalysis)
        coEvery { imageProcessingService.analyzeImage(any(), any(), any()) } returns weakAnalysis
        every { imageProcessingService.generateCompactPrompt(any(), any()) } returns "图片OCR摘要：claude-haiku-4-5-20251001 64GF 1750.52 MB"

        val viewModel = AIAssistantViewModel(
            conversationRepository = conversationRepository,
            aiOperationExecutor = aiOperationExecutor,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            aiService = aiService,
            aiConfigRepository = aiConfigRepository,
            aiUsageRepository = aiUsageRepository,
            networkUtils = networkUtils,
            imageProcessingService = imageProcessingService,
            chatSessionRepository = chatSessionRepository,
            butlerRepository = butlerRepository,
            aiReasoningEngine = aiReasoningEngine,
            transactionModificationHandler = transactionModificationHandler,
            actionExecutor = actionExecutor,
            appLogLogger = appLogLogger
        )
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            coEvery { aiUsageRepository.recordCall(any()) } returns Unit
            coEvery { aiService.sendChatStream(any(), any()) } returns flowOf("这是一张包含模型名称与价格信息的截图。")

            viewModel.sendMessageWithImages(
                message = "图片的内容是什么？",
                imageUris = listOf(mockk<Uri>()),
                context = mockk<Context>()
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { aiService.sendChatStream(any(), any()) }
            val lastAssistantMessage = conversationMessages.last()
            assertTrue(!lastAssistantMessage.contains("未收到可执行的记账指令"))
            assertTrue(!lastAssistantMessage.contains("暂时没法直接记账"))
        } finally {
            viewModel.viewModelScope.cancel()
            testDispatcher.scheduler.advanceUntilIdle()
        }
    }
}
