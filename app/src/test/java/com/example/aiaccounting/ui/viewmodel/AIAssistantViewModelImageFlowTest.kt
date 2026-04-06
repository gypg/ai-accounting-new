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

            coVerify(exactly = 1) {
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
}
