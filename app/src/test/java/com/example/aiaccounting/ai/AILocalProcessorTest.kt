package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AILocalProcessorTest {

    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)

    private val processor = AILocalProcessor(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        aiOperationExecutor = aiOperationExecutor
    )

    @Test
    fun processMessage_keepsOrdinaryMemoryChat_asGeneralConversation() = runTest {
        val result = processor.processMessage(
            message = "你记得我吗？",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("我在听") || result.contains("AI 管家助手"))
    }

    @Test
    fun processMessage_keepsReminderChat_asGeneralConversation() = runTest {
        val result = processor.processMessage(
            message = "记得明天提醒我开会",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("我在听") || result.contains("AI 管家助手"))
    }

    @Test
    fun processMessage_returnsButlerGreeting_forHello() = runTest {
        val result = processor.processMessage(
            message = "你好",
            isNetworkAvailable = true,
            isAIConfigured = true
        )

        assertTrue(result.contains("AI 管家助手"))
        assertTrue(result.contains("聊聊天") || result.contains("记账"))
    }

    @Test
    fun processMessage_returnsPersonaSpecificGreeting_forTaotao() = runTest {
        val result = processor.processMessage(
            message = "你好",
            isNetworkAvailable = true,
            isAIConfigured = true,
            currentButlerId = "taotao"
        )

        assertTrue(result.contains("桃桃"))
    }

    @Test
    fun processMessage_returnsButlerModelReply_forModelQuestion() = runTest {
        val result = processor.processMessage(
            message = "你的底层是什么模型",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("AI 管家") || result.contains("聊天") || result.contains("记账"))
        assertTrue(result.isNotBlank())
    }


    @Test
    fun processMessage_keepsNoteLikeChat_asGeneralConversation() = runTest {
        val result = processor.processMessage(
            message = "我想写日记，记一下今天的感受",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("我在听") || result.contains("AI 管家助手"))
    }

    @Test
    fun processMessage_recognizesConciseJiPlusAmountCommand_asTransaction() = runTest {
        coEvery { categoryRepository.getAllCategoriesList() } returns
            listOf(Category(id = 7, name = "其他支出", type = TransactionType.EXPENSE))
        coEvery { accountRepository.getAllAccountsList() } returns
            listOf(Account(id = 3, name = "默认账户", type = AccountType.CASH))
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> {
                    it.amount == 50.0 &&
                        it.type == TransactionType.EXPENSE &&
                        it.accountId == 3L &&
                        it.categoryId == 7L &&
                        it.note == "记50午饭"
                }
            )
        } returns AIOperationExecutor.AIOperationResult.Success("已记录这笔支出")

        val result = processor.processMessage(
            message = "记50午饭",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("已记录这笔支出"))
        assertTrue(result.contains("账户: 默认账户"))
        assertTrue(result.contains("分类: 其他支出"))
    }

    @Test
    fun processMessage_recognizesPolitePrefixedJiAmountCommand_asTransaction() = runTest {
        coEvery { categoryRepository.getAllCategoriesList() } returns
            listOf(Category(id = 7, name = "其他支出", type = TransactionType.EXPENSE))
        coEvery { accountRepository.getAllAccountsList() } returns
            listOf(Account(id = 3, name = "默认账户", type = AccountType.CASH))
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> {
                    it.amount == 50.0 &&
                        it.type == TransactionType.EXPENSE &&
                        it.accountId == 3L &&
                        it.categoryId == 7L &&
                        it.note == "帮我记50午饭"
                }
            )
        } returns AIOperationExecutor.AIOperationResult.Success("已记录这笔支出")

        val result = processor.processMessage(
            message = "帮我记50午饭",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("已记录这笔支出"))
        assertTrue(result.contains("账户: 默认账户"))
        assertTrue(result.contains("分类: 其他支出"))
    }

    @Test
    fun processMessage_keepsForgetSentence_withNumber_asGeneralConversation() = runTest {
        val result = processor.processMessage(
            message = "我怕忘记50这件事",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("我在听") || result.contains("AI 管家助手"))
    }

    @Test
    fun processMessage_createsDefaultAccountAndCategory_whenTransactionNeedsFallbackEntities() = runTest {
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(
            emptyList(),
            emptyList(),
            listOf(Category(id = 7, name = "其他支出", type = TransactionType.EXPENSE))
        )
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            emptyList(),
            listOf(Account(id = 3, name = "默认账户", type = AccountType.CASH))
        )
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "餐饮" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "交通" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "购物" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "娱乐" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "居住" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "医疗" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "其他支出" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "工资" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "奖金" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "投资" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "兼职" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "其他收入" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddAccount> { it.name == "默认账户" })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> {
                    it.accountId == 3L && it.categoryId == 7L && it.amount == 18.0
                }
            )
        } returns AIOperationExecutor.AIOperationResult.Success("已记录这笔支出")

        val result = processor.processMessage(
            message = "今天午饭花了18元",
            isNetworkAvailable = false,
            isAIConfigured = false
        )

        assertTrue(result.contains("已记录这笔支出"))
        assertTrue(result.contains("账户: 默认账户"))
        assertTrue(result.contains("分类: 其他支出"))
    }
}
