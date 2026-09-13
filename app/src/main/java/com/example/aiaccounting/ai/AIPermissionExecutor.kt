package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.repository.TransactionRepository
import com.example.aiaccounting.security.AIPermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI权限执行器
 * 将AI权限管理系统与具体业务操作集成
 * 实现自动权限判断和执行
 */
@Singleton
class AIPermissionExecutor @Inject constructor(
    private val aiPermissionManager: AIPermissionManager,
    private val transactionRepository: TransactionRepository,
    private val transactionModificationHandler: TransactionModificationHandler
) {

    /**
     * 执行交易修改（带权限检查）
     * 
     * 流程：
     * 1. AI智能判断权限
     * 2. 如权限通过，自动执行修改
     * 3. 记录审计日志
     * 4. 返回执行结果
     */
    suspend fun executeTransactionModification(
        confirmation: TransactionModificationHandler.ModificationConfirmation,
        butlerId: String
    ): ExecutionResult {
        
        val transaction = confirmation.transaction
        val newCategoryId = confirmation.newValues["categoryId"] as? Long ?: transaction.categoryId
        
        // 1. AI权限判断
        val permissionResult = aiPermissionManager.checkPermission(
            operationType = AIPermissionManager.OperationType.UPDATE_TRANSACTION,
            operationDetails = mapOf(
                "transactionId" to transaction.id,
                "amount" to transaction.amount,
                "oldCategoryId" to transaction.categoryId,
                "newCategoryId" to newCategoryId
            )
        )
        
        // 2. 根据权限结果执行
        return when {
            permissionResult.granted && !permissionResult.requiresHumanIntervention -> {
                // AI自动授权，直接执行
                executeModificationInternal(confirmation, butlerId)
            }
            
            permissionResult.requiresHumanIntervention -> {
                // 需要人工干预
                ExecutionResult.NeedsHumanIntervention(
                    reason = permissionResult.interventionReason ?: "需要人工确认",
                    logId = permissionResult.toString() // 实际应该返回日志ID
                )
            }
            
            else -> {
                // 权限被拒绝
                ExecutionResult.PermissionDenied(
                    reason = permissionResult.reason
                )
            }
        }
    }
    
    /**
     * 执行交易创建（带权限检查）
     */
    suspend fun executeTransactionCreation(
        amount: Double,
        type: com.example.aiaccounting.data.local.entity.TransactionType,
        categoryId: Long,
        accountId: Long,
        note: String,
        butlerId: String
    ): ExecutionResult {
        
        // 1. AI权限判断
        val permissionResult = aiPermissionManager.checkPermission(
            operationType = AIPermissionManager.OperationType.CREATE_TRANSACTION,
            operationDetails = mapOf(
                "amount" to amount,
                "type" to type.name,
                "categoryId" to categoryId
            )
        )
        
        // 2. 根据权限结果执行
        return when {
            permissionResult.granted && !permissionResult.requiresHumanIntervention -> {
                // 执行创建
                try {
                    val transaction = Transaction(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note,
                        date = System.currentTimeMillis()
                    )
                    
                    val id = transactionRepository.insertTransaction(transaction)
                    
                    ExecutionResult.Success(
                        message = generateSuccessMessage(butlerId, "CREATE"),
                        data = id
                    )
                } catch (e: Exception) {
                    ExecutionResult.Error(
                        message = "创建失败: ${e.message}"
                    )
                }
            }
            
            permissionResult.requiresHumanIntervention -> {
                ExecutionResult.NeedsHumanIntervention(
                    reason = permissionResult.interventionReason ?: "需要人工确认"
                )
            }
            
            else -> {
                ExecutionResult.PermissionDenied(
                    reason = permissionResult.reason
                )
            }
        }
    }
    
    /**
     * 执行交易删除（带权限检查）
     */
    suspend fun executeTransactionDeletion(
        transaction: Transaction,
        butlerId: String
    ): ExecutionResult {
        
        // 1. AI权限判断
        val permissionResult = aiPermissionManager.checkPermission(
            operationType = AIPermissionManager.OperationType.DELETE_TRANSACTION,
            operationDetails = mapOf(
                "transactionId" to transaction.id,
                "amount" to transaction.amount
            )
        )
        
        // 2. 根据权限结果执行
        return when {
            permissionResult.granted && !permissionResult.requiresHumanIntervention -> {
                try {
                    transactionRepository.deleteTransaction(transaction)
                    ExecutionResult.Success(
                        message = generateSuccessMessage(butlerId, "DELETE")
                    )
                } catch (e: Exception) {
                    ExecutionResult.Error(
                        message = "删除失败: ${e.message}"
                    )
                }
            }
            
            permissionResult.requiresHumanIntervention -> {
                ExecutionResult.NeedsHumanIntervention(
                    reason = permissionResult.interventionReason ?: "删除操作需要人工确认"
                )
            }
            
            else -> {
                ExecutionResult.PermissionDenied(
                    reason = permissionResult.reason
                )
            }
        }
    }
    
    /**
     * 内部执行修改
     */
    private suspend fun executeModificationInternal(
        confirmation: TransactionModificationHandler.ModificationConfirmation,
        butlerId: String
    ): ExecutionResult {
        return try {
            val transaction = confirmation.transaction
            
            // 构建更新后的交易
            val updatedTransaction = transaction.copy(
                amount = confirmation.newValues["amount"] as? Double ?: transaction.amount,
                type = confirmation.newValues["type"] as? com.example.aiaccounting.data.local.entity.TransactionType ?: transaction.type,
                note = confirmation.newValues["note"] as? String ?: transaction.note,
                categoryId = confirmation.newValues["categoryId"] as? Long ?: transaction.categoryId,
                updatedAt = System.currentTimeMillis()
            )
            
            // 执行更新
            transactionRepository.updateTransaction(updatedTransaction)
            
            ExecutionResult.Success(
                message = transactionModificationHandler.generatePersonalitySuccessMessage(
                    butlerId,
                    TransactionModificationHandler.ModificationResult(
                        success = true,
                        message = "修改成功",
                        updatedTransaction = updatedTransaction
                    )
                ),
                data = updatedTransaction
            )
        } catch (e: Exception) {
            ExecutionResult.Error(
                message = "修改失败: ${e.message}"
            )
        }
    }
    
    /**
     * 生成成功消息
     */
    private fun generateSuccessMessage(butlerId: String, operation: String): String {
        return when (butlerId) {
            "xiaocainiang" -> when (operation) {
                "CREATE" -> "主人～已经帮您记好账啦！🌸"
                "DELETE" -> "主人～已经帮您删除啦！💕"
                else -> "主人～操作完成啦！✨"
            }
            "taotao" -> when (operation) {
                "CREATE" -> "记好啦～✨ 桃桃已经帮您记录好啦！(◕‿◕✿)"
                "DELETE" -> "删掉啦～🌸 已经帮您删除啦！"
                else -> "完成啦～✨"
            }
            "guchen" -> when (operation) {
                "CREATE" -> "（叹气）...记好了...哈啊...别吵我睡觉..."
                "DELETE" -> "（懒洋洋地）...删了...我继续睡了..."
                else -> "（翻个身）...好了..."
            }
            "suqian" -> when (operation) {
                "CREATE" -> "（平静地）...已记录。"
                "DELETE" -> "（平静地）...已删除。"
                else -> "（平静地）...完成。"
            }
            "yishuihan" -> when (operation) {
                "CREATE" -> "（微笑）已经为您记录好了，请查看。"
                "DELETE" -> "（微笑）已经为您删除好了。"
                else -> "（微笑）操作已完成。"
            }
            else -> "操作成功"
        }
    }
    
    /**
     * 执行结果
     */
    sealed class ExecutionResult {
        data class Success(
            val message: String,
            val data: Any? = null
        ) : ExecutionResult()
        
        data class Error(
            val message: String
        ) : ExecutionResult()
        
        data class PermissionDenied(
            val reason: String
        ) : ExecutionResult()
        
        data class NeedsHumanIntervention(
            val reason: String,
            val logId: String? = null
        ) : ExecutionResult()
    }
}
