package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
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
    fun reason_doesNotMergeClarificationMessageTwice_whenMessageAlreadyContainsOriginalRequest() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(
                userMessage = "帮我记一笔午饭 支出 今天 25 元",
                conversationHistory = listOf(
                    "帮我记一笔午饭 支出 今天",
                    "请问这笔交易的金额是多少呢？"
                )
            ),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RecordTransaction)
        val action = result.actions.single() as AIReasoningEngine.AIAction.RecordTransaction
        assertEquals(25.0, action.amount, 0.001)
    }

    @Test
    fun reason_returnsRequestClarification_forExplicitRecordRequestWithoutAccount_whenMultipleAccountsExist() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returns listOf(
            Account(id = 1, name = "微信", type = AccountType.WECHAT),
            Account(id = 2, name = "支付宝", type = AccountType.ALIPAY)
        )

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "帮我记一笔午饭 25 元 支出 今天"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RequestClarification)
        assertEquals(
            "这笔记到哪个账户呢？比如现金、微信、支付宝或银行卡。",
            (result.actions.single() as AIReasoningEngine.AIAction.RequestClarification).question
        )
    }

    @Test
    fun reason_returnsRecordTransaction_whenExplicitAccountIsProvided_evenWithMultipleAccounts() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returns listOf(
            Account(id = 1, name = "微信", type = AccountType.WECHAT),
            Account(id = 2, name = "支付宝", type = AccountType.ALIPAY)
        )

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "帮我记一笔午饭 25 元 支出 微信 今天"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RecordTransaction)
        val action = result.actions.single() as AIReasoningEngine.AIAction.RecordTransaction
        assertEquals("微信", action.accountHint)
    }

    @Test
    fun reason_returnsRecordTransaction_whenOnlyOneActiveAccountExists() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returns listOf(
            Account(id = 1, name = "默认账户", type = AccountType.BANK)
        )

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "帮我记一笔午饭 25 元 支出 今天"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RecordTransaction)
    }

    @Test
    fun reason_returnsRequestClarification_forExplicitRecordRequestWithoutCategory_whenDateAndTypeArePresent() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { categoryRepository.getAllCategoriesList() } returns listOf(
            com.example.aiaccounting.data.local.entity.Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE),
            com.example.aiaccounting.data.local.entity.Category(id = 2, name = "交通", type = TransactionType.EXPENSE)
        )

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "帮我记一笔 25 元 支出 今天"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RequestClarification)
        assertEquals(
            "这笔记到哪个分类呢？比如餐饮、交通或购物。",
            (result.actions.single() as AIReasoningEngine.AIAction.RequestClarification).question
        )
    }

    @Test
    fun reason_returnsRequestClarification_forExplicitRecordRequestWithoutDate_whenAmountTypeAndCategoryArePresent() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "帮我记一笔午饭 25 元 支出"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RequestClarification)
        assertEquals(
            "这笔是哪天发生的呢？比如今天、昨天或 3 月 15 日。",
            (result.actions.single() as AIReasoningEngine.AIAction.RequestClarification).question
        )
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
