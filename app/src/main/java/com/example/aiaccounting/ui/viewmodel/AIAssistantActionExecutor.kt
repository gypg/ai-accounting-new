package com.example.aiaccounting.ui.viewmodel

import android.util.Log
import com.example.aiaccounting.BuildConfig
import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.ai.AILocalProcessor
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import org.json.JSONObject
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

  /**
   * 执行 AI 返回的动作
   */
  suspend fun executeAIActions(response: String): String {
    return try {
      val jsonStr = extractAndFixJson(response)
      if (BuildConfig.DEBUG) {
        Log.d("AIAssistantActionExecutor", "解析后的JSON长度: ${jsonStr.length}")
      }

      val json = JSONObject(jsonStr)
      val results = mutableListOf<String>()

      if (json.has("actions")) {
        val actions = json.getJSONArray("actions")
        Log.d("AIAssistantActionExecutor", "发现 ${actions.length()} 个操作")
        for (i in 0 until actions.length()) {
          val actionObj = actions.getJSONObject(i)
          normalizeActionType(actionObj)
          results.add(executeSingleAction(actionObj))
        }
      } else {
        normalizeActionType(json)
        results.add(executeSingleAction(json))
      }

      val aiReply = if (json.has("reply")) json.getString("reply") else null
      if (aiReply != null) {
        listOf(aiReply, results.joinToString("\n").trim())
          .filter { it.isNotBlank() }
          .joinToString("\n\n")
      } else {
        generateFriendlyResponse(results)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e("AIAssistantActionExecutor", "执行AI操作失败", e)
      "执行操作时出错: ${e.message}\n\n请尝试简化您的指令，或分多次发送。"
    }
  }

  /**
   * 归一化动作类型字段
   */
  private fun normalizeActionType(actionJson: JSONObject) {
    if (!actionJson.has("action") && actionJson.has("type")) {
      val actionType = actionJson.getString("type")
      when (actionType) {
        "add_transaction", "create_account", "query" -> actionJson.put("action", actionType)
        "query_accounts" -> {
          actionJson.put("action", "query")
          actionJson.put("target", "accounts")
        }
        "query_categories" -> {
          actionJson.put("action", "query")
          actionJson.put("target", "categories")
        }
        "query_transactions" -> {
          actionJson.put("action", "query")
          actionJson.put("target", "transactions")
        }
        "create_category" -> actionJson.put("action", "create_category")
      }
    }
  }

  /**
   * 执行单个动作
   */
  private suspend fun executeSingleAction(actionJson: JSONObject): String {
    val action = actionJson.optString("action", "")

    return when (action) {
      "create_account" -> executeCreateAccount(actionJson)
      "add_transaction" -> executeAddTransaction(actionJson)
      "query" -> {
        val target = actionJson.optString("target", "")
        aiLocalProcessor.handleQueryCommand(target)
      }
      "create_category" -> executeCreateCategory(actionJson)
      else -> "未知的操作类型：$action"
    }
  }

  private suspend fun executeCreateAccount(actionJson: JSONObject): String {
    val name = actionJson.optString("name", "")
    val rawTypeStr = actionJson.optString("type", "")
    val accountTypeStr = actionJson.optString(
      "accountType",
      when (rawTypeStr) {
        "create_account", "add_transaction", "query" -> "OTHER"
        else -> rawTypeStr.ifBlank { "OTHER" }
      }
    )
    val balance = actionJson.optDouble("balance", 0.0)

    if (name.isBlank()) {
      return "创建账户失败：账户名称不能为空"
    }

    val accountType = parseAccountType(accountTypeStr)
    val operation = AIOperation.AddAccount(name = name, type = accountType, balance = balance)

    return when (val result = aiOperationExecutor.executeOperation(operation)) {
      is AIOperationExecutor.AIOperationResult.Success -> "✅ 已创建账户：$name，余额：¥$balance"
      is AIOperationExecutor.AIOperationResult.Error -> "❌ 创建账户失败：${result.error}"
    }
  }

  private suspend fun executeAddTransaction(actionJson: JSONObject): String {
    val amount = actionJson.optDouble("amount", 0.0)
    val typeStr = actionJson.optString("type", "expense")
    val transactionTypeStr = actionJson.optString("transactionType", "")

    val safeTypeStr = when (typeStr) {
      "add_transaction", "create_account", "query", "create_category" -> ""
      else -> typeStr
    }

    val categoryName = actionJson.optString("category", "")
    val categoryIdStr = actionJson.optString("categoryId", "")
    val categoryIdLong = actionJson.optLong("categoryId", -1)

    val accountName = actionJson.optString("account", "")
    val accountIdStr = actionJson.optString("accountId", "")

    val note = actionJson.optString("note", "")
    val dateTimestamp = actionJson.optLong("date", System.currentTimeMillis())

    if (amount <= 0) {
      return "记账失败：金额必须大于0"
    }

    val effectiveTypeStr = transactionTypeStr.ifBlank { safeTypeStr.ifBlank { "expense" } }
    val transactionType = when (effectiveTypeStr.uppercase()) {
      "INCOME", "收入" -> TransactionType.INCOME
      "EXPENSE", "支出" -> TransactionType.EXPENSE
      "TRANSFER", "转账" -> TransactionType.TRANSFER
      else -> TransactionType.EXPENSE
    }

    val effectiveCategoryName = when {
      categoryName.isNotBlank() -> categoryName
      categoryIdStr.isNotBlank() && categoryIdLong == -1L -> categoryIdStr
      else -> ""
    }

    val effectiveAccountName = when {
      accountName.isNotBlank() -> accountName
      accountIdStr.isNotBlank() -> accountIdStr
      else -> ""
    }

    if (BuildConfig.DEBUG) {
      Log.d("AIAssistantActionExecutor", "解析交易字段已提取")
    }

    aiLocalProcessor.ensureBasicCategoriesExist()

    var accounts = accountRepository.getAllAccountsList()
    var account = if (effectiveAccountName.isNotBlank()) {
      accounts.find { it.name == effectiveAccountName }
        ?: accounts.find { it.name.contains(effectiveAccountName) || effectiveAccountName.contains(it.name) }
        ?: accounts.find { it.type.name == effectiveAccountName.uppercase() }
    } else {
      accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
    }

    if (account == null) {
      val accountType = parseAccountType(effectiveAccountName)
      val newAccountName = effectiveAccountName.ifBlank { "默认账户" }
      val createAccountOp = AIOperation.AddAccount(name = newAccountName, type = accountType, balance = 0.0)
      when (val result = aiOperationExecutor.executeOperation(createAccountOp)) {
        is AIOperationExecutor.AIOperationResult.Success -> {
          accounts = accountRepository.getAllAccountsList()
          account = accounts.find { it.name == newAccountName }
        }
        is AIOperationExecutor.AIOperationResult.Error -> {
          return "记账失败：创建账户失败 - ${result.error}"
        }
      }
    }

    if (account == null) {
      return "记账失败：无法创建或找到账户"
    }

    var categories = categoryRepository.getAllCategoriesList()
    var category = if (effectiveCategoryName.isNotBlank()) {
      categories.find { it.name == effectiveCategoryName }
        ?: categories.find { it.name.contains(effectiveCategoryName) || effectiveCategoryName.contains(it.name) }
    } else {
      categories.firstOrNull { it.type == transactionType }
    }

    if (category == null) {
      val categoryNameToCreate = effectiveCategoryName.ifBlank {
        if (transactionType == TransactionType.INCOME) "其他收入" else "其他支出"
      }
      val createCategoryOp = AIOperation.AddCategory(name = categoryNameToCreate, type = transactionType)
      when (aiOperationExecutor.executeOperation(createCategoryOp)) {
        is AIOperationExecutor.AIOperationResult.Success -> {
          categories = categoryRepository.getAllCategoriesList()
          category = categories.find { it.name == categoryNameToCreate }
        }
        is AIOperationExecutor.AIOperationResult.Error -> {
          category = categories.firstOrNull { it.type == transactionType }
        }
      }
    }

    if (category == null) {
      category = categories.firstOrNull { it.type == transactionType } ?: categories.firstOrNull()
    }

    if (category == null) {
      return "记账失败：无法创建或找到分类"
    }

    val operation = AIOperation.AddTransaction(
      amount = amount,
      type = transactionType,
      accountId = account.id,
      categoryId = category.id,
      date = dateTimestamp,
      note = note.ifBlank { "AI记账" }
    )

    return when (val result = aiOperationExecutor.executeOperation(operation)) {
      is AIOperationExecutor.AIOperationResult.Success ->
        "✅ 已记账：${category.name} ${if (transactionType == TransactionType.INCOME) "收入" else "支出"} ¥$amount"
      is AIOperationExecutor.AIOperationResult.Error ->
        "❌ 记账失败：${result.error}"
    }
  }

  private suspend fun executeCreateCategory(actionJson: JSONObject): String {
    val name = actionJson.optString("name", "").ifBlank {
      actionJson.optString("categoryName", "")
    }

    val rawTypeStr = actionJson.optString("type", "")
    val categoryTypeStr = actionJson.optString(
      "categoryType",
      actionJson.optString(
        "transactionType",
        when (rawTypeStr) {
          "create_category", "add_transaction", "create_account", "query" -> ""
          else -> rawTypeStr
        }
      )
    ).uppercase()

    val txnType = when (categoryTypeStr) {
      "INCOME", "收入" -> TransactionType.INCOME
      "EXPENSE", "支出" -> TransactionType.EXPENSE
      else -> TransactionType.EXPENSE
    }

    val parentId = when {
      actionJson.has("parentId") -> actionJson.optLong("parentId").takeIf { it > 0 }
      actionJson.has("parentCategoryId") -> actionJson.optLong("parentCategoryId").takeIf { it > 0 }
      else -> null
    }

    if (name.isBlank()) {
      return "创建分类失败：分类名称不能为空"
    }

    val operation = AIOperation.AddCategory(name = name, type = txnType, parentId = parentId)

    return when (val result = aiOperationExecutor.executeOperation(operation)) {
      is AIOperationExecutor.AIOperationResult.Success -> "✅ 已创建分类：$name"
      is AIOperationExecutor.AIOperationResult.Error -> "❌ 创建分类失败：${result.error}"
    }
  }

  /**
   * 从响应中提取并修复JSON
   */
  private fun extractAndFixJson(response: String): String {
    var jsonStr = response

    val codeBlockRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```")
    val match = codeBlockRegex.find(response)
    if (match != null) {
      jsonStr = match.groupValues[1].trim()
    }

    val jsonStart = jsonStr.indexOf("{")
    if (jsonStart == -1) {
      throw IllegalArgumentException("未找到JSON对象")
    }

    var jsonEnd = jsonStr.lastIndexOf("}")
    if (jsonEnd == -1 || jsonEnd <= jsonStart) {
      jsonStr = fixUnclosedJson(jsonStr.substring(jsonStart))
    } else {
      jsonStr = jsonStr.substring(jsonStart, jsonEnd + 1)
    }

    return jsonStr
  }

  /**
   * 修复未闭合的JSON
   */
  private fun fixUnclosedJson(json: String): String {
    var fixed = json.trim()

    val openBrackets = fixed.count { it == '[' }
    val closeBrackets = fixed.count { it == ']' }

    if (fixed.contains("\"actions\"") && openBrackets > closeBrackets) {
      val lastObjEnd = fixed.lastIndexOf("}")
      if (lastObjEnd > 0) {
        fixed = fixed.substring(0, lastObjEnd + 1)
        fixed += "\n ]\n}"
      }
    }

    val finalOpenBraces = fixed.count { it == '{' }
    val finalCloseBraces = fixed.count { it == '}' }
    if (finalOpenBraces > finalCloseBraces) {
      fixed += "}".repeat(finalOpenBraces - finalCloseBraces)
    }

    if (BuildConfig.DEBUG) {
      Log.d("AIAssistantActionExecutor", "修复后的JSON长度: ${fixed.length}")
    }
    return fixed
  }

  /**
   * 生成友好的响应
   */
  private suspend fun generateFriendlyResponse(results: List<String>): String {
    if (results.isEmpty()) return "操作已完成"
    if (results.size == 1) return results.first()

    return buildString {
      append("已完成以下操作：\n")
      results.forEachIndexed { index, result ->
        append("${index + 1}. $result\n")
      }
    }
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
