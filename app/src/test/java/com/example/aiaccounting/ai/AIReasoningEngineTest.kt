package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIReasoningEngineTest {

    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val aiInformationSystem = mockk<AIInformationSystem>(relaxed = true)
    private val identityConfirmationDetector = mockk<IdentityConfirmationDetector>()
    private val transactionModificationHandler = mockk<TransactionModificationHandler>()
    private val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
    private val messageParser = AIMessageParser()
    private val categoryInferrer = AICategoryInferrer()
    private val keywordMatcher = AIKeywordMatcher()

    private val engine = AIReasoningEngine(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        aiInformationSystem = aiInformationSystem,
        identityConfirmationDetector = identityConfirmationDetector,
        transactionModificationHandler = transactionModificationHandler,
        aiOperationExecutor = aiOperationExecutor,
        messageParser = messageParser,
        categoryInferrer = categoryInferrer,
        keywordMatcher = keywordMatcher
    )

    @Test
    fun reason_returnsRequestClarification_forIncompleteTransactionMessageWithoutAmount() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "帮我记一笔午饭"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RequestClarification)
        assertEquals(
            "请问这笔交易的金额是多少呢？",
            (result.actions.single() as AIReasoningEngine.AIAction.RequestClarification).question
        )
    }

    @Test
    fun reason_treatsAmountOnlyFollowUpAsRecordTransaction_whenConversationHistoryShowsPreviousClarification() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(
                userMessage = "25元",
                conversationHistory = listOf(
                    "帮我记一笔午饭",
                    "请问这笔交易的金额是多少呢？"
                )
            ),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RecordTransaction)
        val action = result.actions.single() as AIReasoningEngine.AIAction.RecordTransaction
        assertEquals(25.0, action.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, action.type)
        assertEquals("午餐", action.categoryHint)
    }

    private suspend fun stubNonIdentityAndNonModification() {
        coEvery { transactionRepository.getRecentTransactionsList(any()) } returns emptyList()
        coEvery { identityConfirmationDetector.detectIdentityQuery(any()) } returns
            IdentityConfirmationDetector.IdentityQueryResult(
                isIdentityQuery = false,
                queryType = IdentityConfirmationDetector.IdentityQueryType.NONE,
                confidence = 0f
            )
        coEvery { transactionModificationHandler.detectModificationIntent(any()) } returns
            TransactionModificationHandler.ModificationRequest(
                intent = TransactionModificationHandler.ModificationIntent.UNKNOWN,
                originalMessage = "",
                targetTransaction = null,
                confidence = 0.3f
            )
    }
}
