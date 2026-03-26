package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.AILocalProcessor
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

class AIAssistantActionExecutorTest {

    private val aiOperationExecutor = mockk<AIOperationExecutor>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val aiLocalProcessor = mockk<AILocalProcessor>(relaxed = true)

    private val executor = AIAssistantActionExecutor(
        aiOperationExecutor = aiOperationExecutor,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        aiLocalProcessor = aiLocalProcessor
    )

    @Test
    fun executeAIActions_reportsEachActionResult_whenBatchHasMixedOutcome() = runTest {
        coEvery { aiLocalProcessor.ensureBasicCategoriesExist() } returns Unit
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            listOf(Account(id = 1, name = "微信", type = AccountType.WECHAT)),
            listOf(Account(id = 1, name = "微信", type = AccountType.WECHAT)),
            listOf(Account(id = 1, name = "微信", type = AccountType.WECHAT))
        )
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(
            listOf(Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE)),
            listOf(Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE)),
            listOf(Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE))
        )
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> { it.amount == 25.0 }
            )
        } returnsMany listOf(
            AIOperationExecutor.AIOperationResult.Success("ok"),
            AIOperationExecutor.AIOperationResult.Success("ok")
        )
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> { it.amount == 0.0 }
            )
        } returns AIOperationExecutor.AIOperationResult.Error("金额非法")

        val result = executor.executeAIActions(
            """
            {
              "actions": [
                {"action":"add_transaction","amount":25,"type":"expense","category":"餐饮","account":"微信","note":"午饭"},
                {"action":"add_transaction","amount":0,"type":"expense","category":"餐饮","account":"微信","note":"错误样本"}
              ]
            }
            """.trimIndent()
        )

        assertTrue(result.contains("1."))
        assertTrue(result.contains("2."))
        assertTrue(result.contains("午饭") || result.contains("已记账"))
        assertTrue(result.contains("金额必须大于0") || result.contains("失败"))
    }

    @Test
    fun executeAIActions_createsDefaultAccount_whenAccountListIsEmpty() = runTest {
        coEvery { aiLocalProcessor.ensureBasicCategoriesExist() } returns Unit
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            emptyList(),
            listOf(Account(id = 9, name = "默认账户", type = AccountType.CASH))
        )
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(
            listOf(Category(id = 2, name = "其他支出", type = TransactionType.EXPENSE)),
            listOf(Category(id = 2, name = "其他支出", type = TransactionType.EXPENSE)),
            listOf(Category(id = 2, name = "其他支出", type = TransactionType.EXPENSE))
        )
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddAccount> { it.name == "默认账户" })
        } returnsMany listOf(
            AIOperationExecutor.AIOperationResult.Success("created"),
            AIOperationExecutor.AIOperationResult.Success("created")
        )
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddTransaction> { it.accountId == 9L })
        } returns AIOperationExecutor.AIOperationResult.Success("ok")

        val result = executor.executeAIActions(
            """
            {"action":"add_transaction","amount":18,"type":"expense","category":"其他支出","note":"早餐"}
            """.trimIndent()
        )

        assertTrue(result.contains("已记账"))
    }


    @Test
    fun executeAIActions_prefersExistingIds_overCreatingNamedPlaceholders() = runTest {
        coEvery { aiLocalProcessor.ensureBasicCategoriesExist() } returns Unit
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            listOf(Account(id = 12, name = "日常卡", type = AccountType.BANK)),
            listOf(Account(id = 12, name = "日常卡", type = AccountType.BANK))
        )
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(
            listOf(Category(id = 34, name = "餐饮", type = TransactionType.EXPENSE)),
            listOf(Category(id = 34, name = "餐饮", type = TransactionType.EXPENSE))
        )
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> {
                    it.accountId == 12L &&
                        it.categoryId == 34L &&
                        it.amount == 26.0
                }
            )
        } returns AIOperationExecutor.AIOperationResult.Success("ok")

        val result = executor.executeAIActions(
            """
            {"action":"add_transaction","amount":26,"type":"expense","accountId":12,"categoryId":34,"note":"工作餐"}
            """.trimIndent()
        )

        assertTrue(result.contains("账户: 日常卡"))
        assertTrue(result.contains("分类: 餐饮"))
    }


    @Test
    fun executeAIActions_executesActionsWrappedInsidePayloadObject() = runTest {
        coEvery { aiLocalProcessor.handleQueryCommand("accounts") } returns "账户列表：微信"
        coEvery { aiLocalProcessor.handleQueryCommand("categories") } returns "分类列表：餐饮"

        val result = executor.executeAIActions(
            """
            {"payload":{"actions":[{"action":"query","target":"accounts"},{"action":"query","target":"categories"}]}}
            """.trimIndent()
        )

        assertTrue(result.contains("账户列表：微信"))
        assertTrue(result.contains("分类列表：餐饮"))
    }

    @Test
    fun executeAIActions_executesActionWrappedInsideResultObject() = runTest {
        coEvery { aiLocalProcessor.handleQueryCommand("accounts") } returns "账户列表：微信"

        val result = executor.executeAIActions(
            """
            {"result":{"action":"query","target":"accounts"}}
            """.trimIndent()
        )

        assertTrue(result.contains("账户列表：微信"))
    }



    @Test
    fun executeAIActions_formatsBatchResults_withoutDoubleNumbering() = runTest {
        coEvery { aiLocalProcessor.handleQueryCommand("accounts") } returns "账户列表：微信"
        coEvery { aiLocalProcessor.handleQueryCommand("categories") } returns "分类列表：餐饮"

        val result = executor.executeAIActions(
            """
            {
              "actions": [
                {"action":"query","target":"accounts"},
                {"action":"query","target":"categories"}
              ]
            }
            """.trimIndent()
        )

        assertTrue(result.contains("已完成以下操作："))
        assertTrue(result.contains("1. 账户列表：微信"))
        assertTrue(result.contains("2. 分类列表：餐饮"))
        assertTrue(!result.contains("1. 1."))
        assertTrue(!result.contains("2. 2."))
    }

    @Test
    fun executeAIActions_reportsRequestedAccount_whenAccountResolutionFails() = runTest {
        coEvery { aiLocalProcessor.ensureBasicCategoriesExist() } returns Unit
        coEvery { accountRepository.getAllAccountsList() } returns emptyList()
        coEvery { categoryRepository.getAllCategoriesList() } returns listOf(
            Category(id = 2, name = "餐饮", type = TransactionType.EXPENSE)
        )
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddAccount> { it.name == "旅行卡" })
        } returns AIOperationExecutor.AIOperationResult.Error("数据库不可写")

        val result = executor.executeAIActions(
            """
            {"action":"add_transaction","amount":18,"type":"expense","account":"旅行卡","category":"餐饮","note":"早餐"}
            """.trimIndent()
        )

        assertTrue(result.contains("旅行卡"))
        assertTrue(result.contains("未找到指定账户") || result.contains("无法创建或找到账户"))
    }

    @Test
    fun executeAIActions_reportsRequestedCategory_whenCategoryResolutionFails() = runTest {
        coEvery { aiLocalProcessor.ensureBasicCategoriesExist() } returns Unit
        coEvery { accountRepository.getAllAccountsList() } returns listOf(
            Account(id = 9, name = "默认账户", type = AccountType.CASH, isDefault = true)
        )
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(emptyList(), emptyList())
        coEvery {
            aiOperationExecutor.executeOperation(match<AIOperation.AddCategory> { it.name == "夜宵" })
        } returns AIOperationExecutor.AIOperationResult.Error("分类表锁定")

        val result = executor.executeAIActions(
            """
            {"action":"add_transaction","amount":22,"type":"expense","category":"夜宵","note":"加班餐"}
            """.trimIndent()
        )

        assertTrue(result.contains("夜宵"))
        assertTrue(result.contains("未找到指定分类") || result.contains("无法创建或找到分类"))
    }

    @Test
    fun executeAIActions_executesQueryTypeAlias_fromRawTypePayload() = runTest {
        coEvery { aiLocalProcessor.handleQueryCommand("transactions") } returns "最近交易：1笔"

        val result = executor.executeAIActions(
            """
            {"type":"query_transactions"}
            """.trimIndent()
        )

        assertTrue(result.contains("最近交易：1笔"))
    }


    @Test
    fun executeAIActions_prefersTypedEntityReferences_whenRawNamesConflict() = runTest {
        coEvery { aiLocalProcessor.ensureBasicCategoriesExist() } returns Unit
        coEvery { accountRepository.getAllAccountsList() } returnsMany listOf(
            listOf(
                Account(id = 12, name = "日常卡", type = AccountType.BANK),
                Account(id = 99, name = "微信", type = AccountType.WECHAT)
            ),
            listOf(
                Account(id = 12, name = "日常卡", type = AccountType.BANK),
                Account(id = 99, name = "微信", type = AccountType.WECHAT)
            )
        )
        coEvery { categoryRepository.getAllCategoriesList() } returnsMany listOf(
            listOf(
                Category(id = 34, name = "餐饮", type = TransactionType.EXPENSE),
                Category(id = 88, name = "交通", type = TransactionType.EXPENSE)
            ),
            listOf(
                Category(id = 34, name = "餐饮", type = TransactionType.EXPENSE),
                Category(id = 88, name = "交通", type = TransactionType.EXPENSE)
            )
        )
        coEvery {
            aiOperationExecutor.executeOperation(
                match<AIOperation.AddTransaction> {
                    it.accountId == 12L &&
                        it.categoryId == 34L &&
                        it.amount == 26.0
                }
            )
        } returns AIOperationExecutor.AIOperationResult.Success("ok")

        val result = executor.executeAIActions(
            """
            {
              "action":"add_transaction",
              "amount":26,
              "transactionType":"expense",
              "account":"微信",
              "category":"交通",
              "accountRef":{"id":12,"name":"日常卡","kind":"account"},
              "categoryRef":{"id":34,"name":"餐饮","kind":"category"},
              "note":"工作餐"
            }
            """.trimIndent()
        )

        assertTrue(result.contains("账户: 日常卡"))
        assertTrue(result.contains("分类: 餐饮"))
    }

    @Test
    fun executeAIActions_rejectsTransferAction_withExplicitMessage() = runTest {
        val result = executor.executeAIActions(
            """
            {
              "action":"add_transaction",
              "amount":100,
              "transactionType":"transfer",
              "accountRef":{"id":1,"name":"微信","kind":"account"},
              "transferAccountRef":{"id":2,"name":"支付宝","kind":"account"},
              "note":"转账"
            }
            """.trimIndent()
        )

        assertTrue(result.contains("暂不支持"))
        assertTrue(result.contains("转账"))
    }

    @Test
    fun executeAIActions_returnsFriendlyUnknownAction_forUnsupportedTypedAction() = runTest {
        val result = executor.executeAIActions(
            AIAssistantActionEnvelope(actions = listOf(AIAssistantTypedAction.Unknown("delete_everything")))
        )

        assertTrue(result.contains("未知的操作类型"))
        assertTrue(result.contains("delete_everything"))
    }
}
