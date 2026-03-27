package com.example.aiaccounting.ui.viewmodel

import android.util.Log
import com.example.aiaccounting.BuildConfig
import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.AILocalProcessor
import com.example.aiaccounting.ai.AITraceContext
import com.example.aiaccounting.ai.AITransactionEntityResolver
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 动作执行器
 * 负责解析和执行 AI 返回的 JSON 动作
 */
@Singleton
class AIAssistantActionExecutor @Inject constructor(
  private val aiOperationExecutor: AIOperationExecutor,
  private val accountRepository: AccountRepository,
  private val categoryRepository: CategoryRepository,
  private val aiLocalProcessor: AILocalProcessor
) {

  private val responseInterpreter = AIAssistantRemoteResponseInterpreter()

  /**
   * 执行 AI 返回的动作
   */
  suspend fun executeAIActions(response: String): String {
    return try {
      val envelope = responseInterpreter.parseActionEnvelopeFromResponse(response)
      executeAIActions(envelope)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logError("执行AI操作失败", e)
      "执行操作时出错: ${e.message}\n\n请尝试简化您的指令，或分多次发送。"
    }
  }

  internal suspend fun executeAIActions(envelope: AIAssistantActionEnvelope): String {
    return try {
      val results = envelope.actions.mapIndexed { index, action ->
        formatIndexedResult(index + 1, executeSingleAction(action))
      }

      val aiReply = envelope.reply.trim().removeDirtyNullWrappers()
      if (aiReply.isNotBlank()) {
        listOf(aiReply, results.joinToString("\n").trim())
          .filter { it.isNotBlank() }
          .joinToString("\n\n")
      } else {
        generateFriendlyResponse(results)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logError("执行AI操作失败", e)
      "执行操作时出错: ${e.message}\n\n请尝试简化您的指令，或分多次发送。"
    }
  }

  internal suspend fun executeQueryBeforeExecution(envelope: AIAssistantActionEnvelope): String {
    return try {
      val results = envelope.actions.mapIndexed { index, action ->
        formatIndexedResult(index + 1, executeQueryBeforeExecutionAction(action))
      }

      val aiReply = envelope.reply.trim().removeDirtyNullWrappers()
      if (aiReply.isNotBlank()) {
        listOf(aiReply, results.joinToString("\n").trim())
          .filter { it.isNotBlank() }
          .joinToString("\n\n")
      } else {
        generateFriendlyResponse(results)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logError("执行AI查询前置编排失败", e)
      "执行操作时出错: ${e.message}\n\n请尝试简化您的指令，或分多次发送。"
    }
  }

  /**
   * 执行单个动作
   */
  private suspend fun executeSingleAction(action: AIAssistantTypedAction): String {
    val traceContext = AITraceContext(sourceType = "AI_REMOTE")

    return when (action) {
      is AIAssistantTypedAction.CreateAccount -> executeCreateAccount(action, traceContext)
      is AIAssistantTypedAction.AddTransaction -> executeAddTransaction(action, traceContext)
      is AIAssistantTypedAction.Query -> aiLocalProcessor.handleQueryCommand(action.target)
      is AIAssistantTypedAction.CreateCategory -> executeCreateCategory(action, traceContext)
      is AIAssistantTypedAction.Unknown -> "未知的操作类型：${action.rawAction}"
    }
  }

  private suspend fun executeQueryBeforeExecutionAction(action: AIAssistantTypedAction): String {
    val traceContext = AITraceContext(sourceType = "AI_REMOTE")

    return when (action) {
      is AIAssistantTypedAction.AddTransaction -> executeAddTransactionWithQueryBeforeExecution(action, traceContext)
      else -> executeSingleAction(action)
    }
  }

  private suspend fun executeCreateAccount(action: AIAssistantTypedAction.CreateAccount, traceContext: AITraceContext): String {
    val name = action.name
    val accountTypeStr = action.accountTypeRaw
    val balance = action.balance

    if (name.isBlank()) {
      return "创建账户失败：账户名称不能为空"
    }

    val accountType = parseAccountType(accountTypeStr)
    val operation = AIOperation.AddAccount(name = name, type = accountType, balance = balance, traceContext = traceContext)

    return when (val result = aiOperationExecutor.executeOperation(operation)) {
      is AIOperationExecutor.AIOperationResult.Success -> "✅ 已创建账户：$name，余额：¥$balance"
      is AIOperationExecutor.AIOperationResult.Error -> "❌ 创建账户失败：${result.error}"
    }
  }

  private suspend fun executeAddTransaction(action: AIAssistantTypedAction.AddTransaction, traceContext: AITraceContext): String {
    val resolution = resolveAddTransactionExecution(action)
    return resolution.failureMessage ?: executeResolvedAddTransaction(
      amount = action.amount,
      transactionType = resolution.transactionType,
      account = resolution.account,
      category = resolution.category,
      note = action.note,
      dateTimestamp = action.dateTimestamp,
      traceContext = traceContext
    )
  }

  private suspend fun executeAddTransactionWithQueryBeforeExecution(
    action: AIAssistantTypedAction.AddTransaction,
    traceContext: AITraceContext
  ): String {
    val resolution = resolveAddTransactionExecution(action)
    val querySummary = buildQueryBeforeExecutionSummary(action, resolution)
    val executionResult = resolution.failureMessage ?: executeResolvedAddTransaction(
      amount = action.amount,
      transactionType = resolution.transactionType,
      account = resolution.account,
      category = resolution.category,
      note = action.note,
      dateTimestamp = action.dateTimestamp,
      traceContext = traceContext
    )
    return listOf(querySummary, executionResult)
      .filter { it.isNotBlank() }
      .joinToString("\n")
  }

  private suspend fun executeCreateCategory(action: AIAssistantTypedAction.CreateCategory, traceContext: AITraceContext): String {
    val name = action.name

    val categoryTypeStr = action.categoryTypeRaw.uppercase()

    val txnType = when (categoryTypeStr) {
      "INCOME", "收入" -> TransactionType.INCOME
      "EXPENSE", "支出" -> TransactionType.EXPENSE
      else -> TransactionType.EXPENSE
    }

    val parentId = action.parentId

    if (name.isBlank()) {
      return "创建分类失败：分类名称不能为空"
    }

    val operation = AIOperation.AddCategory(name = name, type = txnType, parentId = parentId, traceContext = traceContext)

    return when (val result = aiOperationExecutor.executeOperation(operation)) {
      is AIOperationExecutor.AIOperationResult.Success -> "✅ 已创建分类：$name"
      is AIOperationExecutor.AIOperationResult.Error -> "❌ 创建分类失败：${result.error}"
    }
  }

  private data class AddTransactionExecutionResolution(
    val transactionType: TransactionType,
    val account: com.example.aiaccounting.data.local.entity.Account? = null,
    val category: com.example.aiaccounting.data.local.entity.Category? = null,
    val failureMessage: String? = null,
    val accountQuerySummary: String = "",
    val categoryQuerySummary: String = ""
  )

  private suspend fun resolveAddTransactionExecution(
    action: AIAssistantTypedAction.AddTransaction
  ): AddTransactionExecutionResolution {
    val amount = action.amount
    val typeStr = action.transactionTypeRaw
    val transactionTypeStr = action.transactionTypeRaw

    val safeTypeStr = when (typeStr) {
      "add_transaction", "create_account", "query", "create_category" -> ""
      else -> typeStr
    }

    val categoryRef = action.categoryRef
    val accountRef = action.accountRef
    val transferAccountRef = action.transferAccountRef

    if (amount <= 0) {
      return AddTransactionExecutionResolution(
        transactionType = TransactionType.EXPENSE,
        failureMessage = "记账失败：金额必须大于0"
      )
    }

    val effectiveTypeStr = transactionTypeStr.ifBlank { safeTypeStr.ifBlank { "expense" } }
    val transactionType = when (effectiveTypeStr.uppercase()) {
      "INCOME", "收入" -> TransactionType.INCOME
      "EXPENSE", "支出" -> TransactionType.EXPENSE
      "TRANSFER", "转账" -> TransactionType.TRANSFER
      else -> TransactionType.EXPENSE
    }

    if (transactionType == TransactionType.TRANSFER) {
      val fromLabel = accountRef.name.ifBlank { accountRef.id?.let { "ID=$it" } ?: "源账户" }
      val toLabel = transferAccountRef?.name?.ifBlank { transferAccountRef.id?.let { "ID=$it" } ?: "目标账户" } ?: "目标账户"
      return AddTransactionExecutionResolution(
        transactionType = transactionType,
        failureMessage = "记账失败：暂不支持执行转账语义，请改成两步操作或先使用普通收支记账。来源账户：$fromLabel，目标账户：$toLabel"
      )
    }

    val effectiveCategoryName = when {
      categoryRef.name.isNotBlank() -> categoryRef.name
      categoryRef.rawIdText.isNotBlank() && categoryRef.id == null -> categoryRef.rawIdText
      else -> ""
    }

    val effectiveAccountName = when {
      accountRef.name.isNotBlank() -> accountRef.name
      accountRef.rawIdText.isNotBlank() && accountRef.id == null -> accountRef.rawIdText
      else -> ""
    }

    if (BuildConfig.DEBUG) {
      logDebug("解析交易字段已提取")
    }

    aiLocalProcessor.ensureBasicCategoriesExist()

    val accountResolution = AITransactionEntityResolver.resolveAccount(
      accountRepository = accountRepository,
      aiOperationExecutor = aiOperationExecutor,
      accountId = accountRef.id?.takeIf { it > 0 },
      accountName = effectiveAccountName,
      fallbackAccountName = effectiveAccountName.ifBlank { "默认账户" },
      fallbackAccountType = AccountType.CASH,
      enableFuzzyNameMatch = true,
      enableTypeMatch = true,
      excludeArchived = true,
      allowExplicitFallbackCreation = false
    )
    val account = accountResolution.account
      ?: accountResolution.creationError?.let {
        val requestedLabel = accountResolution.requestedLabel.orEmpty()
        return AddTransactionExecutionResolution(
          transactionType = transactionType,
          failureMessage = if (requestedLabel.isNotBlank()) {
            "记账失败：创建账户失败 - $requestedLabel：$it"
          } else {
            "记账失败：创建账户失败 - $it"
          },
          accountQuerySummary = buildAccountQuerySummary(accountRef, effectiveAccountName, accountResolution.account?.name)
        )
      }
      ?: return AddTransactionExecutionResolution(
        transactionType = transactionType,
        failureMessage = "记账失败：无法创建或找到账户",
        accountQuerySummary = buildAccountQuerySummary(accountRef, effectiveAccountName, null)
      )

    val categoryResolution = AITransactionEntityResolver.resolveCategory(
      categoryRepository = categoryRepository,
      aiOperationExecutor = aiOperationExecutor,
      transactionType = transactionType,
      categoryId = categoryRef.id?.takeIf { it > 0 },
      categoryName = effectiveCategoryName,
      fallbackCategoryName = effectiveCategoryName.ifBlank {
        if (transactionType == TransactionType.INCOME) "其他收入" else "其他支出"
      },
      emergencyCategoryName = null,
      allowAnyTypeFallback = true,
      allowExplicitFallbackCreation = false
    )
    val category = categoryResolution.category
      ?: categoryResolution.creationError?.let {
        val requestedLabel = categoryResolution.requestedLabel.orEmpty()
        return AddTransactionExecutionResolution(
          transactionType = transactionType,
          account = account,
          failureMessage = if (requestedLabel.isNotBlank()) {
            "记账失败：创建分类失败 - $requestedLabel：$it"
          } else {
            "记账失败：创建分类失败 - $it"
          },
          accountQuerySummary = buildAccountQuerySummary(accountRef, effectiveAccountName, account.name),
          categoryQuerySummary = buildCategoryQuerySummary(categoryRef, effectiveCategoryName, categoryResolution.category?.name)
        )
      }
      ?: return AddTransactionExecutionResolution(
        transactionType = transactionType,
        account = account,
        failureMessage = "记账失败：无法创建或找到分类",
        accountQuerySummary = buildAccountQuerySummary(accountRef, effectiveAccountName, account.name),
        categoryQuerySummary = buildCategoryQuerySummary(categoryRef, effectiveCategoryName, null)
      )

    return AddTransactionExecutionResolution(
      transactionType = transactionType,
      account = account,
      category = category,
      accountQuerySummary = buildAccountQuerySummary(accountRef, effectiveAccountName, account.name),
      categoryQuerySummary = buildCategoryQuerySummary(categoryRef, effectiveCategoryName, category.name)
    )
  }

  private suspend fun executeResolvedAddTransaction(
    amount: Double,
    transactionType: TransactionType,
    account: com.example.aiaccounting.data.local.entity.Account?,
    category: com.example.aiaccounting.data.local.entity.Category?,
    note: String,
    dateTimestamp: Long,
    traceContext: AITraceContext
  ): String {
    val resolvedAccount = account ?: return "记账失败：无法创建或找到账户"
    val resolvedCategory = category ?: return "记账失败：无法创建或找到分类"

    val operation = AIOperation.AddTransaction(
      amount = amount,
      type = transactionType,
      accountId = resolvedAccount.id,
      categoryId = resolvedCategory.id,
      date = dateTimestamp,
      note = note.ifBlank { "AI记账" },
      traceContext = traceContext
    )

    return when (val result = aiOperationExecutor.executeOperation(operation)) {
      is AIOperationExecutor.AIOperationResult.Success ->
        "✅ 已记账：${resolvedCategory.name} ${if (transactionType == TransactionType.INCOME) "收入" else "支出"} ¥$amount\n账户: ${resolvedAccount.name}\n分类: ${resolvedCategory.name}"
      is AIOperationExecutor.AIOperationResult.Error ->
        "❌ 记账失败：${result.error}"
    }
  }

  private fun buildQueryBeforeExecutionSummary(
    action: AIAssistantTypedAction.AddTransaction,
    resolution: AddTransactionExecutionResolution
  ): String {
    val amount = action.amount
    val direction = if (resolution.transactionType == TransactionType.INCOME) "收入" else "支出"
    return buildString {
      append("已先查询本地上下文，再执行记账：¥")
      append(amount)
      append(' ')
      append(direction)
      if (resolution.accountQuerySummary.isNotBlank()) {
        append("\n")
        append(resolution.accountQuerySummary)
      }
      if (resolution.categoryQuerySummary.isNotBlank()) {
        append("\n")
        append(resolution.categoryQuerySummary)
      }
    }
  }

  private fun buildAccountQuerySummary(
    accountRef: AIAssistantEntityReference,
    requestedName: String,
    resolvedName: String?
  ): String {
    val requested = requestedName.ifBlank {
      accountRef.id?.let { "ID=$it" }
        ?: accountRef.rawIdText.takeIf { it.isNotBlank() }
        ?: "默认账户"
    }
    val resolved = resolvedName ?: "未命中"
    return "账户查询：请求=$requested，命中=$resolved"
  }

  private fun buildCategoryQuerySummary(
    categoryRef: AIAssistantEntityReference,
    requestedName: String,
    resolvedName: String?
  ): String {
    val requested = requestedName.ifBlank {
      categoryRef.id?.let { "ID=$it" }
        ?: categoryRef.rawIdText.takeIf { it.isNotBlank() }
        ?: "自动分类"
    }
    val resolved = resolvedName ?: "未命中"
    return "分类查询：请求=$requested，命中=$resolved"
  }

  private fun logDebug(message: String) {
    runCatching {
      Log.d("AIAssistantActionExecutor", message)
    }
  }

  private fun logError(message: String, throwable: Throwable) {
    runCatching {
      Log.e("AIAssistantActionExecutor", message, throwable)
    }
  }

  private fun formatIndexedResult(index: Int, result: String): String {
    return "$index. $result"
  }

  /**
   * 生成友好的响应
   */
  private suspend fun generateFriendlyResponse(results: List<String>): String {
    if (results.isEmpty()) return "操作已完成"
    if (results.size == 1) return results.first()
    if (results.all { it.matches(Regex("^\\d+\\.\\s+.*")) }) {
      return buildString {
        append("已完成以下操作：\n")
        results.forEach { result ->
          append(result)
          append("\n")
        }
      }
    }

    return buildString {
      append("已完成以下操作：\n")
      results.forEachIndexed { index, result ->
        append("${index + 1}. $result\n")
      }
    }
  }

  private fun String.removeDirtyNullWrappers(): String {
    return trim()
      .replace(Regex("^(?:null\\s*)+"), "")
      .replace(Regex("(?:\\s*null)+$"), "")
      .trim()
  }

  private fun parseAccountType(typeStr: String): com.example.aiaccounting.data.local.entity.AccountType {
    val upper = typeStr.uppercase()
    return when {
      upper == "WECHAT" -> com.example.aiaccounting.data.local.entity.AccountType.WECHAT
      upper == "ALIPAY" -> com.example.aiaccounting.data.local.entity.AccountType.ALIPAY
      upper == "CASH" -> com.example.aiaccounting.data.local.entity.AccountType.CASH
      upper == "BANK" -> com.example.aiaccounting.data.local.entity.AccountType.BANK
      upper == "CREDIT_CARD" -> com.example.aiaccounting.data.local.entity.AccountType.CREDIT_CARD
      upper == "DEBIT_CARD" -> com.example.aiaccounting.data.local.entity.AccountType.DEBIT_CARD
      upper.contains("微信") -> com.example.aiaccounting.data.local.entity.AccountType.WECHAT
      upper.contains("支付宝") -> com.example.aiaccounting.data.local.entity.AccountType.ALIPAY
      upper.contains("现金") -> com.example.aiaccounting.data.local.entity.AccountType.CASH
      upper.contains("信用") -> com.example.aiaccounting.data.local.entity.AccountType.CREDIT_CARD
      upper.contains("借记") -> com.example.aiaccounting.data.local.entity.AccountType.DEBIT_CARD
      upper.contains("银行") -> com.example.aiaccounting.data.local.entity.AccountType.BANK
      else -> com.example.aiaccounting.data.local.entity.AccountType.OTHER
    }
  }
}
