package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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
    fun reason_returnsIdentityPersonaReply_forKnownButler() = runTest {
        every { identityConfirmationDetector.detectIdentityQuery(any(), any()) } returns
            IdentityConfirmationDetector.IdentityQueryResult(
                isIdentityQuery = true,
                queryType = IdentityConfirmationDetector.IdentityQueryType.DIRECT_IDENTITY_ASK,
                confidence = 0.95f
            )
        every { identityConfirmationDetector.hasMixedIntent(any(), any()) } returns false
        every {
            identityConfirmationDetector.generateIdentityResponse(
                currentButlerId = "taotao",
                queryResult = any(),
                activeButlerName = any()
            )
        } returns "我是桃桃～"

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "你是谁"),
            currentButlerId = "taotao"
        )

        assertEquals(AIReasoningEngine.UserIntent.IDENTITY_CONFIRMATION, result.intent)
        assertEquals(
            "我是桃桃～",
            (result.actions.single() as AIReasoningEngine.AIAction.GenerateResponse).responseContent
        )
    }

    @Test
    fun reason_returnsGeneralConversation_forGreeting() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "你好"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.GenerateResponse)
        assertTrue(
            (result.actions.single() as AIReasoningEngine.AIAction.GenerateResponse)
                .responseContent.isNotBlank()
        )
    }

    @Test
    fun reason_returnsGeneralConversation_forTaskQuestion() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "你的任务是什么"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION, result.intent)
        val response = (result.actions.single() as AIReasoningEngine.AIAction.GenerateResponse).responseContent
        assertTrue(response.isNotBlank())
        assertTrue(!response.contains("抱歉，我不太理解您的意思"))
    }

    @Test
    fun reason_returnsSpecificReply_forCapabilityVariant_withoutGenericFallbackPhrase() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "你都能干嘛"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION, result.intent)
        val response = (result.actions.single() as AIReasoningEngine.AIAction.GenerateResponse).responseContent
        assertTrue(!response.contains("我在听。你可以直接跟我聊天，也可以让我帮你记账、查账、看分析；按你现在想说的继续就好。"))
        assertTrue(response.contains("记账") || response.contains("查账") || response.contains("助手"))
    }

    @Test
    fun reason_returnsSpecificReply_forModelVariant_withoutGenericFallbackPhrase() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "你用的是啥模型"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION, result.intent)
        val response = (result.actions.single() as AIReasoningEngine.AIAction.GenerateResponse).responseContent
        assertTrue(!response.contains("我在听。你可以直接跟我聊天，也可以让我帮你记账、查账、看分析；按你现在想说的继续就好。"))
        assertTrue(response.contains("模型") || response.contains("助手"))
    }

    @Test
    fun reason_returnsSpecificReply_forGreetingVariant_withoutGenericFallbackPhrase() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "哈喽"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION, result.intent)
        val response = (result.actions.single() as AIReasoningEngine.AIAction.GenerateResponse).responseContent
        assertTrue(!response.contains("我在听。你可以直接跟我聊天，也可以让我帮你记账、查账、看分析；按你现在想说的继续就好。"))
        assertTrue(response.contains("你好") || response.contains("在这儿") || response.contains("助手"))
    }


    @Test
    fun reason_returnsGeneralConversation_whenMessageContainsReminderWordButNoAccountingIntent() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "记得明天提醒我开会"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.GenerateResponse)
    }


    @Test
    fun reason_returnsGeneralConversation_whenReminderMentionsBookkeepingLater() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "提醒我晚上记账 25 元"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.GENERAL_CONVERSATION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.GenerateResponse)
    }

    @Test
    fun reason_returnsRecordTransaction_forStructuredLedgerTextWithoutExplicitBookkeepingKeywords() = runTest {
        stubNonIdentityAndNonModification()

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(
                userMessage = "📅 2026年3月31日（周二）\n-12.00 餐饮（咖啡+三明治）——瑞幸（微信）\n-42.00 餐饮（午餐：麻辣香锅外卖）——美团外卖\n+5000.00 工资（4月预发部分）——银行代发"
            ),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
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



    @Test
    fun reason_returnsTransferRecordTransaction_whenMessageDescribesTransfer() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returns listOf(
            Account(id = 1, name = "微信", type = AccountType.WECHAT),
            Account(id = 2, name = "支付宝", type = AccountType.ALIPAY)
        )

        val result = engine.reason(
            context = AIReasoningEngine.ReasoningContext(userMessage = "从微信转账100到支付宝 今天"),
            currentButlerId = "xiaocainiang"
        )

        assertEquals(AIReasoningEngine.UserIntent.RECORD_TRANSACTION, result.intent)
        assertTrue(result.actions.single() is AIReasoningEngine.AIAction.RecordTransaction)
        val action = result.actions.single() as AIReasoningEngine.AIAction.RecordTransaction
        assertEquals(TransactionType.TRANSFER, action.type)
        assertEquals("微信", action.accountHint)
        assertEquals("支付宝", action.targetAccountHint)
    }

    @Test
    fun executeActions_autoCreatesExplicitMissingAccount_insteadOfUsingDefaultAccount() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            listOf(Account(id = 1, name = "默认账户", type = AccountType.CASH, isDefault = true)),
            listOf(
                Account(id = 1, name = "默认账户", type = AccountType.CASH, isDefault = true),
                Account(id = 21, name = "旅行卡", type = AccountType.BANK)
            )
        )
        coEvery { categoryRepository.getAllCategoriesList() } returns listOf(
            com.example.aiaccounting.data.local.entity.Category(id = 2, name = "餐饮", type = TransactionType.EXPENSE)
        )
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddAccount> { it.name == "旅行卡" })
        } returns AIOperationExecutor.AIOperationResult.Success("created")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddTransaction> { it.accountId == 21L })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")

        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.RecordTransaction(
                    amount = 18.0,
                    type = TransactionType.EXPENSE,
                    categoryHint = "餐饮",
                    accountHint = "旅行卡",
                    note = "早餐",
                    date = System.currentTimeMillis()
                )
            )
        )

        assertTrue(reply.contains("旅行卡"))
        assertTrue(reply.contains("自动创建账户") || reply.contains("已自动创建账户"))
        coVerify(exactly = 1) {
            aiOperationExecutor.executeOperation(match<AIOperation.AddAccount> { it.name == "旅行卡" })
        }
    }

    @Test
    fun executeActions_transfer_autoCreatesMissingTargetAccount_andCompletesTransfer() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            listOf(Account(id = 1, name = "微信", type = AccountType.WECHAT)),
            listOf(
                Account(id = 1, name = "微信", type = AccountType.WECHAT),
                Account(id = 2, name = "旅行卡", type = AccountType.BANK)
            )
        )
        coEvery { categoryRepository.getAllCategoriesList() } returns listOf(
            Category(id = 3, name = "转账", type = TransactionType.TRANSFER)
        )
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddAccount> { it.name == "旅行卡" })
        } returns AIOperationExecutor.AIOperationResult.Success("created")
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> {
                    it.type == TransactionType.TRANSFER &&
                        it.accountId == 1L &&
                        it.transferAccountId == 2L
                }
            )
        } returns AIOperationExecutor.AIOperationResult.Success("ok")

        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.RecordTransaction(
                    amount = 100.0,
                    type = TransactionType.TRANSFER,
                    categoryHint = "转账",
                    accountHint = "微信",
                    targetAccountHint = "旅行卡",
                    note = "转账",
                    date = System.currentTimeMillis()
                )
            )
        )

        assertTrue(reply.contains("已记录转账") || reply.contains("已转账"))
        assertTrue(reply.contains("微信"))
        assertTrue(reply.contains("旅行卡"))
        assertTrue(reply.contains("自动创建目标账户") || reply.contains("已自动创建目标账户"))
        coVerify(exactly = 1) {
            aiOperationExecutor.executeOperation(match<AIOperation.AddAccount> { it.name == "旅行卡" })
        }
    }

    @Test
    fun executeActions_keepsSingleLocalTraceContext_forAutoCreatedEntitiesAndTransaction() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            emptyList(),
            listOf(Account(id = 21, name = "旅行卡", type = AccountType.BANK))
        )
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(
            emptyList(),
            listOf(Category(id = 35, name = "夜宵", type = TransactionType.EXPENSE))
        )

        val executedOperations = mutableListOf<AIOperation>()
        coEvery { aiOperationExecutor.executeOperation(capture(executedOperations)) } answers {
            when (firstArg<AIOperation>()) {
                is AIOperation.AddAccount -> AIOperationExecutor.AIOperationResult.Success("created")
                is AIOperation.AddCategory -> AIOperationExecutor.AIOperationResult.Success("created")
                is AIOperation.AddTransaction -> AIOperationExecutor.AIOperationResult.Success("ok")
                else -> AIOperationExecutor.AIOperationResult.Success("ok")
            }
        }

        engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.RecordTransaction(
                    amount = 18.0,
                    type = TransactionType.EXPENSE,
                    categoryHint = "夜宵",
                    accountHint = "旅行卡",
                    note = "早餐",
                    date = System.currentTimeMillis()
                )
            )
        )

        val addAccount = executedOperations.filterIsInstance<AIOperation.AddAccount>().single()
        val addCategory = executedOperations.filterIsInstance<AIOperation.AddCategory>().single()
        val addTransaction = executedOperations.filterIsInstance<AIOperation.AddTransaction>().single()

        assertEquals("AI_LOCAL", addAccount.traceContext.sourceType)
        assertEquals("AI_LOCAL", addCategory.traceContext.sourceType)
        assertEquals("AI_LOCAL", addTransaction.traceContext.sourceType)
        assertEquals(
            1,
            setOf(
                addAccount.traceContext.traceId,
                addCategory.traceContext.traceId,
                addTransaction.traceContext.traceId
            ).size
        )
    }

    @Test
    fun executeActions_returnsIndexedBatchResults_andContinuesWhenSingleActionFails() = runTest {
        stubNonIdentityAndNonModification()
        coEvery { accountRepository.getAllAccountsList() } returns listOf(
            Account(id = 1, name = "微信", type = AccountType.WECHAT),
            Account(id = 2, name = "支付宝", type = AccountType.ALIPAY)
        )
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(
            listOf(Category(id = 3, name = "餐饮", type = TransactionType.EXPENSE)),
            listOf(Category(id = 3, name = "餐饮", type = TransactionType.EXPENSE))
        )
        coEvery {
            aiInformationSystem.executeQuery(
                match<AIInformationSystem.QueryRequest> {
                    it.queryType == AIInformationSystem.QueryType.ACCOUNT_INFO
                }
            )
        } returns AIInformationSystem.QueryResult(
            success = true,
            data = null,
            summary = "账户信息",
            details = "账户总览"
        )
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "夜宵" })
        } throws IllegalStateException("category db locked")

        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                ),
                AIReasoningEngine.AIAction.RecordTransaction(
                    amount = 26.0,
                    type = TransactionType.EXPENSE,
                    categoryHint = "夜宵",
                    accountHint = "微信",
                    note = "加班餐",
                    date = System.currentTimeMillis()
                )
            )
        )

        assertTrue(reply.contains("已完成以下操作："))
        assertTrue(reply.contains("1. 账户总览"))
        assertTrue(reply.contains("2. ❌ "))
        assertTrue(reply.contains("请稍后重试"))
        assertTrue(!reply.contains("category db locked"))
    }

    @Test
    fun executeActions_returnsSingleResultWithoutBatchPrefix_whenOnlyOneAction() = runTest {
        stubNonIdentityAndNonModification()
        coEvery {
            aiInformationSystem.executeQuery(
                match<AIInformationSystem.QueryRequest> {
                    it.queryType == AIInformationSystem.QueryType.ACCOUNT_INFO
                }
            )
        } returns AIInformationSystem.QueryResult(
            success = true,
            data = null,
            summary = "账户信息",
            details = "账户总览"
        )

        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                )
            )
        )

        assertEquals("账户总览", reply)
        assertTrue(!reply.contains("已完成以下操作："))
        assertTrue(!reply.contains("\n"))
    }

    @Test
    fun executeActions_returnsClarificationQuestionOnly_whenClarificationActionExists() = runTest {
        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                ),
                AIReasoningEngine.AIAction.RecordTransaction(
                    amount = 12.0,
                    type = TransactionType.EXPENSE,
                    categoryHint = "餐饮",
                    accountHint = "微信",
                    note = "午饭",
                    date = System.currentTimeMillis()
                ),
                AIReasoningEngine.AIAction.RequestClarification("这笔是收入还是支出？")
            )
        )

        assertEquals("这笔是收入还是支出？", reply)
        coVerify(exactly = 0) { aiInformationSystem.executeQuery(any()) }
        coVerify(exactly = 0) { aiOperationExecutor.executeOperation(any()) }
    }

    @Test(expected = CancellationException::class)
    fun executeActions_rethrowsCancellationException_withoutSwallowingIt() = runTest {
        stubNonIdentityAndNonModification()
        coEvery {
            aiInformationSystem.executeQuery(
                match<AIInformationSystem.QueryRequest> {
                    it.queryType == AIInformationSystem.QueryType.ACCOUNT_INFO
                }
            )
        } throws CancellationException("cancelled")

        engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                )
            )
        )
    }

    @Test
    fun executeActions_continuesBatch_whenFirstActionFails_andSecondActionSucceeds() = runTest {
        stubNonIdentityAndNonModification()
        coEvery {
            aiInformationSystem.executeQuery(
                match<AIInformationSystem.QueryRequest> {
                    it.queryType == AIInformationSystem.QueryType.ACCOUNT_INFO
                }
            )
        } throws IllegalStateException("query transport failed")
        coEvery {
            aiInformationSystem.executeQuery(
                match<AIInformationSystem.QueryRequest> {
                    it.queryType == AIInformationSystem.QueryType.CATEGORY_INFO
                }
            )
        } returns AIInformationSystem.QueryResult(
            success = true,
            data = null,
            summary = "分类信息",
            details = "分类总览"
        )

        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                ),
                AIReasoningEngine.AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.CATEGORY_INFO
                )
            )
        )

        assertTrue(reply.contains("已完成以下操作："))
        assertTrue(reply.contains("1. ❌ 执行失败：请稍后重试"))
        assertTrue(reply.contains("2. 分类总览"))
        assertTrue(!reply.contains("query transport failed"))
    }

    @Test
    fun executeActions_sanitizesQueryFailureMessage_whenQueryResultIsUnsuccessful() = runTest {
        stubNonIdentityAndNonModification()
        coEvery {
            aiInformationSystem.executeQuery(
                match<AIInformationSystem.QueryRequest> {
                    it.queryType == AIInformationSystem.QueryType.ACCOUNT_INFO
                }
            )
        } returns AIInformationSystem.QueryResult(
            success = false,
            data = null,
            summary = "失败",
            details = "",
            errorMessage = "internal query stack"
        )

        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.QueryInformation(
                    queryType = AIInformationSystem.QueryType.ACCOUNT_INFO
                )
            )
        )

        assertTrue(reply.contains("查询失败，请稍后重试"))
        assertTrue(!reply.contains("internal query stack"))
    }

    @Test
    fun executeActions_sanitizesAnalyzeFailureMessage_whenAnalyzeResultIsUnsuccessful() = runTest {
        stubNonIdentityAndNonModification()
        coEvery {
            aiInformationSystem.executeQuery(
                match<AIInformationSystem.QueryRequest> {
                    it.queryType == AIInformationSystem.QueryType.EXPENSE_ANALYSIS
                }
            )
        } returns AIInformationSystem.QueryResult(
            success = false,
            data = null,
            summary = "失败",
            details = "",
            errorMessage = "internal analyze trace"
        )

        val reply = engine.executeActions(
            listOf(
                AIReasoningEngine.AIAction.AnalyzeData(
                    analysisType = AIReasoningEngine.AnalysisType.EXPENSE_STRUCTURE,
                    scope = AIReasoningEngine.AnalysisScope.THIS_MONTH
                )
            )
        )

        assertTrue(reply.contains("分析失败，请稍后重试"))
        assertTrue(!reply.contains("internal analyze trace"))
    }

    private suspend fun stubNonIdentityAndNonModification() {
        coEvery { transactionRepository.getRecentTransactionsList(any()) } returns emptyList()
        every { identityConfirmationDetector.detectIdentityQuery(any(), any()) } returns
            IdentityConfirmationDetector.IdentityQueryResult(
                isIdentityQuery = false,
                queryType = IdentityConfirmationDetector.IdentityQueryType.NONE,
                confidence = 0f
            )
        every { identityConfirmationDetector.hasMixedIntent(any(), any()) } returns false
        every {
            identityConfirmationDetector.generateIdentityResponse(any(), any(), any())
        } returns ""
        coEvery { transactionModificationHandler.detectModificationIntent(any()) } returns
            TransactionModificationHandler.ModificationRequest(
                intent = TransactionModificationHandler.ModificationIntent.UNKNOWN,
                originalMessage = "",
                targetTransaction = null,
                confidence = 0.3f
            )
    }
}
