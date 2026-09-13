package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.model.ButlerPersonaRegistry
import com.example.aiaccounting.data.repository.AIConversationRepository
import com.example.aiaccounting.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易修改处理器
 * 处理用户修改历史交易记录的请求
 * 自动回溯对话历史，定位交易记录，无需用户提供交易ID
 */
@Singleton
class TransactionModificationHandler @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val conversationRepository: AIConversationRepository,
    private val categoryRepository: com.example.aiaccounting.data.repository.CategoryRepository
) {

    /**
     * 修改意图类型
     */
    enum class ModificationIntent {
        MODIFY_LAST_TRANSACTION,      // 修改最近的交易
        MODIFY_SPECIFIC_TRANSACTION,  // 修改特定交易（通过描述）
        DELETE_LAST_TRANSACTION,      // 删除最近的交易
        DELETE_SPECIFIC_TRANSACTION,  // 删除特定交易
        UNKNOWN                       // 未知意图
    }

    /**
     * 修改请求
     */
    data class ModificationRequest(
        val intent: ModificationIntent,
        val originalMessage: String,
        val targetTransaction: Transaction? = null,
        val suggestedChanges: Map<String, Any> = emptyMap(),
        val confidence: Float = 0f
    )

    /**
     * 修改确认状态
     */
    data class ModificationConfirmation(
        val transaction: Transaction,
        val originalValues: Map<String, Any>,
        val newValues: Map<String, Any>,
        val confirmationMessage: String,
        val requiresConfirmation: Boolean = true
    )

    /**
     * 检测修改意图
     */
    suspend fun detectModificationIntent(message: String): ModificationRequest {
        val lowerMessage = message.lowercase()
        
        // 1. 检测修改最近交易的意图
        val modifyLastPatterns = listOf(
            "改一下", "修改", "改", "更正", "纠正", "改错", "改过来",
            "刚才记错了", "刚才错了", "刚才的", "上一条", "上一个",
            "刚才", "刚刚", "之前", "记错了", "错了", "不对",
            "改成", "改为", "改成是", "应该是", "其实是"
        )
        
        val isModifyLast = modifyLastPatterns.any { lowerMessage.contains(it) }
        
        // 2. 检测删除意图
        val deletePatterns = listOf(
            "删除", "删掉", "去掉", "取消", "撤销"
        )
        
        val isDelete = deletePatterns.any { lowerMessage.contains(it) }
        
        // 3. 提取金额信息（用于定位交易）
        val amount = extractAmount(lowerMessage)
        
        // 4. 提取修改内容
        val suggestedChanges = extractSuggestedChanges(lowerMessage)
        
        return when {
            isModifyLast && !isDelete -> {
                // 查找最近的交易记录
                val recentTransactions = getRecentTransactions(5)
                val targetTransaction = if (amount != null) {
                    // 如果有金额，找匹配金额的交易
                    recentTransactions.find { kotlin.math.abs(it.amount - amount) < 0.01 }
                } else {
                    // 否则找最近的一条
                    recentTransactions.firstOrNull()
                }
                
                ModificationRequest(
                    intent = ModificationIntent.MODIFY_LAST_TRANSACTION,
                    originalMessage = message,
                    targetTransaction = targetTransaction,
                    suggestedChanges = suggestedChanges,
                    confidence = if (targetTransaction != null) 0.85f else 0.60f
                )
            }
            
            isDelete && !isModifyLast -> {
                val recentTransactions = getRecentTransactions(5)
                val targetTransaction = if (amount != null) {
                    recentTransactions.find { kotlin.math.abs(it.amount - amount) < 0.01 }
                } else {
                    recentTransactions.firstOrNull()
                }
                
                ModificationRequest(
                    intent = ModificationIntent.DELETE_LAST_TRANSACTION,
                    originalMessage = message,
                    targetTransaction = targetTransaction,
                    confidence = if (targetTransaction != null) 0.85f else 0.60f
                )
            }
            
            else -> ModificationRequest(
                intent = ModificationIntent.UNKNOWN,
                originalMessage = message,
                confidence = 0.30f
            )
        }
    }

    /**
     * 生成修改确认信息
     */
    suspend fun generateModificationConfirmation(
        request: ModificationRequest
    ): ModificationConfirmation? {
        val transaction = request.targetTransaction ?: return null
        
        // 获取原始值
        val originalValues = mutableMapOf<String, Any>()
        originalValues["amount"] = transaction.amount
        originalValues["type"] = transaction.type
        originalValues["note"] = transaction.note
        originalValues["categoryId"] = transaction.categoryId
        
        // 获取原始分类名称
        val originalCategory = categoryRepository.getCategoryById(transaction.categoryId)
        val originalCategoryName = originalCategory?.name ?: "未分类"
        originalValues["categoryName"] = originalCategoryName
        
        // 构建新值
        val newValues = mutableMapOf<String, Any>()
        newValues.putAll(originalValues)
        
        // 应用建议的修改
        request.suggestedChanges.forEach { (key, value) ->
            when (key) {
                "categoryHint" -> {
                    // 将分类名称转换为分类ID
                    val categoryName = value as String
                    val category = findCategoryByName(categoryName, transaction.type)
                    if (category != null) {
                        newValues["categoryId"] = category.id
                        newValues["categoryName"] = category.name
                    }
                }
                else -> {
                    newValues[key] = value
                }
            }
        }
        
        // 生成确认消息
        val confirmationMessage = buildConfirmationMessage(
            transaction,
            originalValues,
            newValues,
            request.suggestedChanges
        )
        
        return ModificationConfirmation(
            transaction = transaction,
            originalValues = originalValues,
            newValues = newValues,
            confirmationMessage = confirmationMessage,
            requiresConfirmation = true
        )
    }
    
    /**
     * 根据分类名称查找分类
     */
    private suspend fun findCategoryByName(
        name: String, 
        type: TransactionType
    ): com.example.aiaccounting.data.local.entity.Category? {
        val categories = categoryRepository.getAllCategoriesList()
        return categories.find { 
            it.name.contains(name) || name.contains(it.name)
        } ?: categories.find { it.type == type }
    }

    /**
     * 执行交易修改
     */
    suspend fun executeModification(
        confirmation: ModificationConfirmation
    ): ModificationResult {
        return try {
            val transaction = confirmation.transaction
            
            // 构建更新后的交易
            val updatedTransaction = transaction.copy(
                amount = confirmation.newValues["amount"] as? Double ?: transaction.amount,
                type = confirmation.newValues["type"] as? TransactionType ?: transaction.type,
                note = confirmation.newValues["note"] as? String ?: transaction.note,
                categoryId = confirmation.newValues["categoryId"] as? Long ?: transaction.categoryId,
                updatedAt = System.currentTimeMillis()
            )
            
            // 执行更新
            transactionRepository.updateTransaction(updatedTransaction)
            
            ModificationResult(
                success = true,
                message = "已成功修改交易记录",
                updatedTransaction = updatedTransaction
            )
        } catch (e: Exception) {
            ModificationResult(
                success = false,
                message = "修改失败: ${e.message}",
                updatedTransaction = null
            )
        }
    }

    /**
     * 执行交易删除
     */
    suspend fun executeDelete(transaction: Transaction): ModificationResult {
        return try {
            transactionRepository.deleteTransaction(transaction)
            ModificationResult(
                success = true,
                message = "已成功删除交易记录",
                updatedTransaction = null
            )
        } catch (e: Exception) {
            ModificationResult(
                success = false,
                message = "删除失败: ${e.message}",
                updatedTransaction = null
            )
        }
    }

    /**
     * 获取最近的交易记录
     */
    private suspend fun getRecentTransactions(limit: Int): List<Transaction> {
        return transactionRepository.getRecentTransactionsList(limit)
    }

    /**
     * 提取金额
     */
    private fun extractAmount(message: String): Double? {
        val patterns = listOf(
            Regex("""(\d+\.?\d*)\s*[元块]"""),
            Regex("""(\d+\.?\d*)\s*块"""),
            Regex("""(\d+\.?\d*)""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].toDoubleOrNull()
            }
        }
        return null
    }

    /**
     * 提取建议的修改内容
     */
    private fun extractSuggestedChanges(message: String): Map<String, Any> {
        val changes = mutableMapOf<String, Any>()
        val lowerMessage = message.lowercase()
        
        // 检测分类修改
        val categoryChanges = mapOf(
            "工资" to "工资收入",
            "餐饮" to "餐饮",
            "交通" to "交通",
            "购物" to "购物",
            "娱乐" to "娱乐",
            "医疗" to "医疗",
            "住房" to "住房",
            "其他收入" to "其他收入"
        )
        
        for ((keyword, category) in categoryChanges) {
            if (lowerMessage.contains(keyword)) {
                // 这里需要获取分类ID，暂时用分类名称
                changes["categoryHint"] = category
                break
            }
        }
        
        // 检测类型修改
        when {
            lowerMessage.contains("收入") && !lowerMessage.contains("支出") -> {
                changes["type"] = TransactionType.INCOME
            }
            lowerMessage.contains("支出") || lowerMessage.contains("花了") -> {
                changes["type"] = TransactionType.EXPENSE
            }
        }
        
        // 检测备注修改
        val notePatterns = listOf(
            Regex("""备注[是为改成]*(.+?)[，。]?"""),
            Regex("""改成(.+?)[，。]?"""),
            Regex("""改为(.+?)[，。]?""")
        )
        
        for (pattern in notePatterns) {
            val match = pattern.find(message)
            if (match != null) {
                changes["note"] = match.groupValues[1].trim()
                break
            }
        }
        
        return changes
    }

    /**
     * 构建确认消息
     */
    private fun buildConfirmationMessage(
        transaction: Transaction,
        originalValues: Map<String, Any>,
        newValues: Map<String, Any>,
        suggestedChanges: Map<String, Any>
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(transaction.date))
        
        val sb = StringBuilder()
        sb.appendLine("【交易修改确认】")
        sb.appendLine()
        sb.appendLine("找到以下交易记录：")
        sb.appendLine("• 时间：$dateStr")
        sb.appendLine("• 金额：¥${String.format("%.2f", transaction.amount)}")
        sb.appendLine("• 类型：${if (transaction.type == TransactionType.INCOME) "收入" else "支出"}")
        sb.appendLine("• 分类：${originalValues["categoryName"] ?: "未分类"}")
        sb.appendLine("• 备注：${transaction.note}")
        sb.appendLine()
        
        // 显示修改内容
        if (suggestedChanges.isNotEmpty()) {
            sb.appendLine("建议修改为：")
            
            // 显示分类修改
            if (suggestedChanges.containsKey("categoryHint")) {
                val newCategoryName = newValues["categoryName"] as? String ?: suggestedChanges["categoryHint"]
                sb.appendLine("• 分类：${originalValues["categoryName"] ?: "未分类"} → $newCategoryName")
            }
            
            // 显示其他修改
            suggestedChanges.forEach { (key, value) ->
                if (key != "categoryHint") {
                    val displayKey = when (key) {
                        "type" -> "类型"
                        "note" -> "备注"
                        "amount" -> "金额"
                        else -> key
                    }
                    sb.appendLine("• $displayKey：$value")
                }
            }
            sb.appendLine()
        }
        
        sb.appendLine("是否确认修改？")
        sb.appendLine("回复\"确认\"执行修改，回复\"取消\"放弃修改。")
        
        return sb.toString()
    }

    /**
     * 生成人格化的确认回复
     */
    fun generatePersonalityConfirmationMessage(
        butlerId: String,
        confirmation: ModificationConfirmation
    ): String {
        return ButlerPersonaRegistry.buildModificationConfirmationReply(
            butlerId = butlerId,
            baseMessage = confirmation.confirmationMessage
        )
    }

    /**
     * 生成人格化的成功回复
     */
    fun generatePersonalitySuccessMessage(
        butlerId: String,
        result: ModificationResult
    ): String {
        return ButlerPersonaRegistry.buildModificationSuccessReply(
            butlerId = butlerId,
            fallbackMessage = result.message
        )
    }

    /**
     * 检查用户回复是否为确认
     */
    fun isConfirmation(message: String): Boolean {
        val confirmPatterns = listOf(
            "确认", "是的", "是", "对", "没错", "ok", "好", "行", "可以",
            "confirm", "yes", "yeah", "yep", "sure", "ok"
        )
        return confirmPatterns.any { message.lowercase().contains(it) }
    }

    /**
     * 检查用户回复是否为取消
     */
    fun isCancellation(message: String): Boolean {
        val cancelPatterns = listOf(
            "取消", "不", "不用", "算了", "不要", "否",
            "cancel", "no", "nope", "don't"
        )
        return cancelPatterns.any { message.lowercase().contains(it) }
    }

    /**
     * 修改结果
     */
    data class ModificationResult(
        val success: Boolean,
        val message: String,
        val updatedTransaction: Transaction?
    )
}
