package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository

internal data class AccountResolutionResult(
    val account: Account?,
    val creationError: String? = null,
    val requestedLabel: String? = null
)

internal data class CategoryResolutionResult(
    val category: Category?,
    val creationError: String? = null,
    val terminalCreationError: String? = null,
    val requestedLabel: String? = null
)

internal object AITransactionEntityResolver {

    suspend fun resolveAccount(
        accountRepository: AccountRepository,
        aiOperationExecutor: AIOperationExecutor,
        accountId: Long? = null,
        accountName: String = "",
        fallbackAccountName: String = "默认账户",
        fallbackAccountType: AccountType = AccountType.CASH,
        enableFuzzyNameMatch: Boolean = false,
        enableTypeMatch: Boolean = false,
        excludeArchived: Boolean = false,
        allowExplicitFallbackCreation: Boolean = false,
        traceContext: AITraceContext = AITraceContext(sourceType = "AI_LOCAL")
    ): AccountResolutionResult {
        var accounts = loadAccounts(accountRepository, excludeArchived)
        val normalizedAccountName = accountName.trim()
        val typeMatchSource = normalizedAccountName.uppercase()
        val requestedLabel = when {
            accountId != null && accountId > 0 -> if (normalizedAccountName.isNotBlank()) "$normalizedAccountName(ID=$accountId)" else "ID=$accountId"
            normalizedAccountName.isNotBlank() -> normalizedAccountName
            else -> fallbackAccountName
        }

        var account = when {
            accountId != null && accountId > 0 -> accounts.find { it.id == accountId }
            normalizedAccountName.isNotBlank() -> {
                accounts.find { it.name == normalizedAccountName }
                    ?: if (enableFuzzyNameMatch) {
                        accounts.find {
                            it.name.contains(normalizedAccountName) || normalizedAccountName.contains(it.name)
                        }
                    } else {
                        null
                    }
                    ?: if (enableTypeMatch) {
                        accounts.find { it.type.name == typeMatchSource }
                    } else {
                        null
                    }
            }
            else -> accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
        }

        if (account == null && (accountId != null && accountId > 0 || normalizedAccountName.isNotBlank()) && !allowExplicitFallbackCreation) {
            return AccountResolutionResult(
                account = null,
                creationError = "未找到指定账户",
                requestedLabel = requestedLabel
            )
        }

        if (account == null) {
            when (
                val result = aiOperationExecutor.executeOperation(
                    AIOperation.AddAccount(
                        name = fallbackAccountName,
                        type = fallbackAccountType,
                        balance = 0.0,
                        traceContext = traceContext
                    )
                )
            ) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    accounts = loadAccounts(accountRepository, excludeArchived)
                    account = accounts.find { it.name == fallbackAccountName }
                        ?: accounts.firstOrNull { it.isDefault }
                        ?: accounts.firstOrNull()
                }
                is AIOperationExecutor.AIOperationResult.Error -> {
                    return AccountResolutionResult(
                        account = null,
                        creationError = result.error,
                        requestedLabel = requestedLabel
                    )
                }
            }
        }

        return AccountResolutionResult(account = account, requestedLabel = requestedLabel)
    }

    suspend fun resolveCategory(
        categoryRepository: CategoryRepository,
        aiOperationExecutor: AIOperationExecutor,
        transactionType: TransactionType,
        categoryId: Long? = null,
        categoryName: String = "",
        fallbackCategoryName: String = defaultCategoryName(transactionType),
        emergencyCategoryName: String? = null,
        allowAnyTypeFallback: Boolean = true,
        allowExplicitFallbackCreation: Boolean = false,
        traceContext: AITraceContext = AITraceContext(sourceType = "AI_LOCAL")
    ): CategoryResolutionResult {
        var categories = categoryRepository.getAllCategoriesList()
        val normalizedCategoryName = categoryName.trim()
        val requestedLabel = when {
            categoryId != null && categoryId > 0 -> if (normalizedCategoryName.isNotBlank()) "$normalizedCategoryName(ID=$categoryId)" else "ID=$categoryId"
            normalizedCategoryName.isNotBlank() -> normalizedCategoryName
            else -> fallbackCategoryName
        }

        var category = when {
            categoryId != null && categoryId > 0 -> categories.find { it.id == categoryId }
            normalizedCategoryName.isNotBlank() -> {
                categories.find { it.name == normalizedCategoryName }
                    ?: categories.find {
                        it.name.contains(normalizedCategoryName) || normalizedCategoryName.contains(it.name)
                    }
            }
            else -> categories.firstOrNull { it.type == transactionType }
        }

        var creationError: String? = null
        if (category == null && (categoryId != null && categoryId > 0 || normalizedCategoryName.isNotBlank()) && !allowExplicitFallbackCreation) {
            return CategoryResolutionResult(
                category = null,
                creationError = "未找到指定分类",
                terminalCreationError = null,
                requestedLabel = requestedLabel
            )
        }
        if (category == null) {
            when (
                val result = aiOperationExecutor.executeOperation(
                    AIOperation.AddCategory(name = fallbackCategoryName, type = transactionType, traceContext = traceContext)
                )
            ) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    categories = categoryRepository.getAllCategoriesList()
                    category = categories.find { it.name == fallbackCategoryName }
                        ?: categories.firstOrNull { it.type == transactionType }
                }
                is AIOperationExecutor.AIOperationResult.Error -> {
                    creationError = result.error
                    category = categories.firstOrNull { it.type == transactionType }
                }
            }
        }

        if (category == null && allowAnyTypeFallback) {
            category = categories.firstOrNull { it.type == transactionType } ?: categories.firstOrNull()
        }

        if (category == null && emergencyCategoryName != null) {
            when (
                val result = aiOperationExecutor.executeOperation(
                    AIOperation.AddCategory(name = emergencyCategoryName, type = transactionType, traceContext = traceContext)
                )
            ) {
                is AIOperationExecutor.AIOperationResult.Success -> {
                    categories = categoryRepository.getAllCategoriesList()
                    category = categories.firstOrNull { it.type == transactionType }
                }
                is AIOperationExecutor.AIOperationResult.Error -> {
                    return CategoryResolutionResult(
                        category = null,
                        creationError = creationError,
                        terminalCreationError = result.error,
                        requestedLabel = requestedLabel
                    )
                }
            }
        }

        if (category == null && allowAnyTypeFallback) {
            category = categories.firstOrNull { it.type == transactionType } ?: categories.firstOrNull()
        }

        return CategoryResolutionResult(
            category = category,
            creationError = creationError,
            terminalCreationError = null,
            requestedLabel = requestedLabel
        )
    }

    private suspend fun loadAccounts(
        accountRepository: AccountRepository,
        excludeArchived: Boolean
    ): List<Account> {
        return accountRepository.getAllAccountsList().let { accounts ->
            if (excludeArchived) accounts.filterNot { it.isArchived } else accounts
        }
    }

    private fun defaultCategoryName(transactionType: TransactionType): String {
        return if (transactionType == TransactionType.INCOME) "其他收入" else "其他支出"
    }
}
