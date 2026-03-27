package com.example.aiaccounting.ai

import android.util.Log
import com.example.aiaccounting.data.local.entity.*
import com.example.aiaccounting.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI操作执行器 - 执行AI解析后的操作指令
 * 除了密码相关操作外，AI可以执行所有功能
 */
@Singleton
class AIOperationExecutor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val aiOperationTraceRepository: AIOperationTraceRepository,
    private val appDatabase: com.example.aiaccounting.data.local.database.AppDatabase
) {
    
    sealed class AIOperationResult {
        data class Success(val message: String) : AIOperationResult()
        data class Error(val error: String) : AIOperationResult()
    }

    private suspend fun writeTrace(
        traceContext: AITraceContext,
        actionType: String,
        entityType: String,
        entityId: String? = null,
        relatedTransactionId: Long? = null,
        summary: String,
        details: String? = null,
        success: Boolean,
        errorMessage: String? = null
    ) {
        aiOperationTraceRepository.insertTrace(
            AIOperationTrace(
                id = UUID.randomUUID().toString(),
                traceId = traceContext.traceId,
                sourceType = traceContext.sourceType,
                actionType = actionType,
                entityType = entityType,
                entityId = entityId,
                relatedTransactionId = relatedTransactionId,
                summary = summary,
                details = details,
                success = success,
                errorMessage = errorMessage
            )
        )
    }

    /**
     * 执行AI操作
     */
    suspend fun executeOperation(operation: AIOperation): AIOperationResult {
        return withContext(Dispatchers.IO) {
            try {
                when (operation) {
                    is AIOperation.AddTransaction -> addTransaction(operation)
                    is AIOperation.UpdateTransaction -> updateTransaction(operation)
                    is AIOperation.DeleteTransaction -> deleteTransaction(operation)
                    is AIOperation.AddAccount -> addAccount(operation)
                    is AIOperation.UpdateAccount -> updateAccount(operation)
                    is AIOperation.DeleteAccount -> deleteAccount(operation)
                    is AIOperation.AddCategory -> addCategory(operation)
                    is AIOperation.UpdateCategory -> updateCategory(operation)
                    is AIOperation.DeleteCategory -> deleteCategory(operation)
                    is AIOperation.AddBudget -> addBudget(operation)
                    is AIOperation.UpdateBudget -> updateBudget(operation)
                    is AIOperation.DeleteBudget -> deleteBudget(operation)
                    is AIOperation.QueryData -> queryData(operation)
                    is AIOperation.ExportData -> exportData(operation)
                    is AIOperation.GenerateReport -> generateReport(operation)
                }
            } catch (e: Exception) {
                AIOperationResult.Error("执行操作失败: ${e.message}")
            }
        }
    }
    
    /**
     * 添加交易记录并更新账户余额
     * 修复：先插入交易记录，成功后再更新余额，确保数据一致性
     */
    private suspend fun addTransaction(op: AIOperation.AddTransaction): AIOperationResult {
        // 验证账户ID
        val accountId = op.accountId ?: 0
        if (accountId <= 0) {
            return AIOperationResult.Error("无效的账户ID，请先创建账户")
        }

        // 验证分类ID
        val categoryId = op.categoryId ?: 0
        if (categoryId <= 0) {
            return AIOperationResult.Error("无效的分类ID")
        }

        // 检查账户是否存在
        if (accountRepository.getAccountById(accountId) == null) {
            return AIOperationResult.Error("账户不存在，请先创建账户")
        }

        // 检查分类是否存在
        categoryRepository.getCategoryById(categoryId)
            ?: return AIOperationResult.Error("分类不存在")

        val transferTargetId = if (op.type == TransactionType.TRANSFER) {
            val targetId = op.transferAccountId ?: return AIOperationResult.Error("无效的目标账户ID")
            if (targetId == accountId) {
                return AIOperationResult.Error("转账失败：源账户和目标账户不能相同")
            }
            if (accountRepository.getAccountById(targetId) == null) {
                return AIOperationResult.Error("目标账户不存在")
            }
            targetId
        } else {
            null
        }

        // 先插入交易记录（关键修复：先插入交易，确保交易记录存在）
        val transaction = Transaction(
            amount = op.amount,
            type = op.type,
            categoryId = categoryId,
            accountId = accountId,
            transferAccountId = op.transferAccountId,
            date = op.date,
            note = op.note ?: "",
            aiSourceType = op.traceContext.sourceType,
            aiTraceId = op.traceContext.traceId
        )

        return try {
            Log.d("AIOperationExecutor", "开始插入交易记录: amount=${op.amount}, type=${op.type}, accountId=$accountId, categoryId=$categoryId")
            val transactionId = appDatabase.withTransaction {
                val insertedId = transactionRepository.insertTransaction(transaction)
                if (insertedId <= 0L) {
                    throw IllegalStateException("保存交易记录失败")
                }

                when (op.type) {
                    TransactionType.INCOME -> {
                        val currentSource = accountRepository.getAccountById(accountId)
                            ?: throw IllegalStateException("账户不存在")
                        val updatedSource = currentSource.copy(balance = currentSource.balance + op.amount)
                        accountRepository.updateAccount(updatedSource)
                    }
                    TransactionType.EXPENSE -> {
                        val currentSource = accountRepository.getAccountById(accountId)
                            ?: throw IllegalStateException("账户不存在")
                        val updatedSource = currentSource.copy(balance = currentSource.balance - op.amount)
                        accountRepository.updateAccount(updatedSource)
                    }
                    TransactionType.TRANSFER -> {
                        val resolvedTargetId = transferTargetId ?: throw IllegalStateException("目标账户不存在")
                        val currentSource = accountRepository.getAccountById(accountId)
                            ?: throw IllegalStateException("账户不存在")
                        val currentTarget = accountRepository.getAccountById(resolvedTargetId)
                            ?: throw IllegalStateException("目标账户不存在")
                        val updatedSource = currentSource.copy(balance = currentSource.balance - op.amount)
                        val updatedTarget = currentTarget.copy(balance = currentTarget.balance + op.amount)
                        accountRepository.updateAccount(updatedSource)
                        accountRepository.updateAccount(updatedTarget)
                    }
                }
                insertedId
            }
            Log.d("AIOperationExecutor", "交易记录插入结果: transactionId=$transactionId")

            writeTrace(
                traceContext = op.traceContext,
                actionType = "ADD_TRANSACTION",
                entityType = "transaction",
                entityId = transactionId.toString(),
                relatedTransactionId = transactionId,
                summary = "AI 添加交易",
                details = "amount=${op.amount}, type=${op.type}, accountId=$accountId, categoryId=$categoryId, transferAccountId=${op.transferAccountId}",
                success = true
            )

            val successMessage = when (op.type) {
                TransactionType.INCOME -> "已添加收入记录: ${op.amount}元"
                TransactionType.EXPENSE -> "已添加支出记录: ${op.amount}元"
                TransactionType.TRANSFER -> "已添加转账记录: ${op.amount}元"
            }
            AIOperationResult.Success(successMessage)
        } catch (e: Exception) {
            // 捕获异常，确保错误被正确处理
            Log.e("AIOperationExecutor", "保存交易记录异常: ${e.message}", e)
            writeTrace(
                traceContext = op.traceContext,
                actionType = "ADD_TRANSACTION",
                entityType = "transaction",
                summary = "AI 添加交易异常",
                details = "amount=${op.amount}, type=${op.type}, accountId=$accountId, categoryId=$categoryId, transferAccountId=${op.transferAccountId}",
                success = false,
                errorMessage = e.message
            )
            AIOperationResult.Error("保存交易记录失败: ${e.message}")
        }
    }
    
    /**
     * 更新交易记录并重新计算账户余额
     */
    private suspend fun updateTransaction(op: AIOperation.UpdateTransaction): AIOperationResult {
        val oldTransaction = transactionRepository.getTransactionById(op.id)
            ?: return AIOperationResult.Error("未找到该交易记录")

        val updated = oldTransaction.copy(
            amount = op.amount ?: oldTransaction.amount,
            type = op.type ?: oldTransaction.type,
            categoryId = op.categoryId ?: oldTransaction.categoryId,
            accountId = op.accountId ?: oldTransaction.accountId,
            transferAccountId = oldTransaction.transferAccountId,
            note = op.note ?: oldTransaction.note,
            aiSourceType = op.traceContext.sourceType,
            aiTraceId = op.traceContext.traceId
        )

        if (updated.type == TransactionType.TRANSFER && updated.transferAccountId == null) {
            return AIOperationResult.Error("转账失败：缺少目标账户")
        }
        if (updated.type == TransactionType.TRANSFER && updated.transferAccountId == updated.accountId) {
            return AIOperationResult.Error("转账失败：源账户和目标账户不能相同")
        }

        return try {
            appDatabase.withTransaction {
                // 恢复旧账户余额
                val oldAccount = accountRepository.getAccountById(oldTransaction.accountId)
                if (oldAccount != null) {
                    val restoredBalance = when (oldTransaction.type) {
                        TransactionType.INCOME -> oldAccount.balance - oldTransaction.amount
                        TransactionType.EXPENSE -> oldAccount.balance + oldTransaction.amount
                        TransactionType.TRANSFER -> oldAccount.balance + oldTransaction.amount
                    }
                    accountRepository.updateAccount(oldAccount.copy(balance = restoredBalance))
                }
                if (oldTransaction.type == TransactionType.TRANSFER && oldTransaction.transferAccountId != null) {
                    val oldTargetAccount = accountRepository.getAccountById(oldTransaction.transferAccountId)
                    if (oldTargetAccount != null) {
                        val restoredTargetBalance = oldTargetAccount.balance - oldTransaction.amount
                        accountRepository.updateAccount(oldTargetAccount.copy(balance = restoredTargetBalance))
                    }
                }

                // 更新交易
                transactionRepository.updateTransaction(updated)

                // 更新新账户余额
                val newAccount = accountRepository.getAccountById(updated.accountId)
                if (newAccount != null) {
                    val newBalance = when (updated.type) {
                        TransactionType.INCOME -> newAccount.balance + updated.amount
                        TransactionType.EXPENSE -> newAccount.balance - updated.amount
                        TransactionType.TRANSFER -> newAccount.balance - updated.amount
                    }
                    accountRepository.updateAccount(newAccount.copy(balance = newBalance))
                }
                if (updated.type == TransactionType.TRANSFER && updated.transferAccountId != null) {
                    val newTargetAccount = accountRepository.getAccountById(updated.transferAccountId)
                    if (newTargetAccount != null) {
                        val targetBalance = newTargetAccount.balance + updated.amount
                        accountRepository.updateAccount(newTargetAccount.copy(balance = targetBalance))
                    }
                }
            }

            writeTrace(
                traceContext = op.traceContext,
                actionType = "UPDATE_TRANSACTION",
                entityType = "transaction",
                entityId = updated.id.toString(),
                relatedTransactionId = updated.id,
                summary = "AI 更新交易",
                details = "amount=${updated.amount}, type=${updated.type}, accountId=${updated.accountId}, categoryId=${updated.categoryId}",
                success = true
            )
            AIOperationResult.Success("已更新交易记录")
        } catch (e: Exception) {
            Log.e("AIOperationExecutor", "更新交易记录异常: ${e.message}", e)
            writeTrace(
                traceContext = op.traceContext,
                actionType = "UPDATE_TRANSACTION",
                entityType = "transaction",
                entityId = updated.id.toString(),
                relatedTransactionId = updated.id,
                summary = "AI 更新交易异常",
                details = "amount=${updated.amount}, type=${updated.type}, accountId=${updated.accountId}, categoryId=${updated.categoryId}",
                success = false,
                errorMessage = e.message
            )
            AIOperationResult.Error("更新交易记录失败: ${e.message}")
        }
    }
    
    /**
     * 删除交易记录并恢复账户余额
     */
    private suspend fun deleteTransaction(op: AIOperation.DeleteTransaction): AIOperationResult {
        val transaction = transactionRepository.getTransactionById(op.id)
            ?: return AIOperationResult.Error("未找到该交易记录")

        return try {
            appDatabase.withTransaction {
                // 恢复账户余额
                val account = accountRepository.getAccountById(transaction.accountId)
                if (account != null) {
                    val restoredBalance = when (transaction.type) {
                        TransactionType.INCOME -> account.balance - transaction.amount
                        TransactionType.EXPENSE -> account.balance + transaction.amount
                        TransactionType.TRANSFER -> account.balance + transaction.amount
                    }
                    accountRepository.updateAccount(account.copy(balance = restoredBalance))
                }
                if (transaction.type == TransactionType.TRANSFER && transaction.transferAccountId != null) {
                    val targetAccount = accountRepository.getAccountById(transaction.transferAccountId)
                    if (targetAccount != null) {
                        val targetRestoredBalance = targetAccount.balance - transaction.amount
                        accountRepository.updateAccount(targetAccount.copy(balance = targetRestoredBalance))
                    }
                }

                transactionRepository.deleteTransaction(transaction)
            }

            writeTrace(
                traceContext = op.traceContext,
                actionType = "DELETE_TRANSACTION",
                entityType = "transaction",
                entityId = transaction.id.toString(),
                relatedTransactionId = transaction.id,
                summary = "AI 删除交易",
                details = "amount=${transaction.amount}, type=${transaction.type}, accountId=${transaction.accountId}, categoryId=${transaction.categoryId}",
                success = true
            )
            AIOperationResult.Success("已删除交易记录")
        } catch (e: Exception) {
            Log.e("AIOperationExecutor", "删除交易记录异常: ${e.message}", e)
            writeTrace(
                traceContext = op.traceContext,
                actionType = "DELETE_TRANSACTION",
                entityType = "transaction",
                entityId = transaction.id.toString(),
                relatedTransactionId = transaction.id,
                summary = "AI 删除交易异常",
                details = "amount=${transaction.amount}, type=${transaction.type}, accountId=${transaction.accountId}, categoryId=${transaction.categoryId}",
                success = false,
                errorMessage = e.message
            )
            AIOperationResult.Error("删除交易记录失败: ${e.message}")
        }
    }
    
    private suspend fun addAccount(op: AIOperation.AddAccount): AIOperationResult {
        // 检查是否已存在同名账户
        val existingAccount = accountRepository.findAccountByName(op.name)
        if (existingAccount != null) {
            // 如果账户已存在，更新余额
            val updated = existingAccount.copy(
                balance = existingAccount.balance + op.balance
            )
            accountRepository.updateAccount(updated)
            return AIOperationResult.Success("账户 ${op.name} 已存在，已更新余额为 ${updated.balance}")
        }
        
        val account = Account(
            name = op.name,
            type = op.type,
            balance = op.balance,
            color = op.color,
            icon = op.icon,
            isDefault = op.isDefault
        )
        val accountId = accountRepository.insertAccount(account)
        writeTrace(
            traceContext = op.traceContext,
            actionType = "ADD_ACCOUNT",
            entityType = "account",
            entityId = accountId.toString(),
            summary = "AI 创建账户",
            details = "name=${op.name}, type=${op.type}, balance=${op.balance}",
            success = true
        )
        return AIOperationResult.Success("已添加账户: ${op.name}")
    }
    
    private suspend fun updateAccount(op: AIOperation.UpdateAccount): AIOperationResult {
        val account = accountRepository.getAccountById(op.id) ?: return AIOperationResult.Error("未找到该账户")
        val updated = account.copy(
            name = op.name ?: account.name,
            balance = op.balance ?: account.balance,
            color = op.color ?: account.color,
            icon = op.icon ?: account.icon
        )
        accountRepository.updateAccount(updated)
        writeTrace(
            traceContext = op.traceContext,
            actionType = "UPDATE_ACCOUNT",
            entityType = "account",
            entityId = updated.id.toString(),
            summary = "AI 更新账户",
            details = "name=${updated.name}, balance=${updated.balance}",
            success = true
        )
        return AIOperationResult.Success("已更新账户: ${updated.name}")
    }
    
    private suspend fun deleteAccount(op: AIOperation.DeleteAccount): AIOperationResult {
        val account = accountRepository.getAccountById(op.id) ?: return AIOperationResult.Error("未找到该账户")
        accountRepository.deleteAccount(account)
        writeTrace(
            traceContext = op.traceContext,
            actionType = "DELETE_ACCOUNT",
            entityType = "account",
            entityId = account.id.toString(),
            summary = "AI 删除账户",
            details = "name=${account.name}, balance=${account.balance}",
            success = true
        )
        return AIOperationResult.Success("已删除账户: ${account.name}")
    }
    
    /**
     * 添加分类 - 使用智能图标和颜色，支持子分类自动推断
     */
    private suspend fun addCategory(op: AIOperation.AddCategory): AIOperationResult {
        // 根据分类名称智能选择图标和颜色
        val (smartIcon, smartColor) = getSmartIconAndColor(op.name, op.type)

        // 确定父分类ID：优先使用显式指定的，否则自动推断
        val parentId = op.parentId ?: inferParentCategory(op.name, op.type)

        val category = Category(
            name = op.name,
            type = op.type,
            color = op.color.takeIf { it != "#2196F3" } ?: smartColor,
            icon = op.icon.takeIf { it != "📁" } ?: smartIcon,
            parentId = parentId
        )
        val categoryId = categoryRepository.insertCategory(category)

        val parentInfo = if (parentId != null) {
            val parent = categoryRepository.getCategoryById(parentId)
            "（归属于「${parent?.name ?: "未知"}」）"
        } else ""

        writeTrace(
            traceContext = op.traceContext,
            actionType = "ADD_CATEGORY",
            entityType = "category",
            entityId = categoryId.toString(),
            summary = "AI 创建分类",
            details = "name=${op.name}, type=${op.type}, parentId=$parentId",
            success = true
        )

        return AIOperationResult.Success("已添加分类: ${op.name}$parentInfo")
    }

    /**
     * 根据分类名称自动推断应归属的父分类
     * 通过关键词匹配已有的顶级分类
     */
    private suspend fun inferParentCategory(name: String, type: TransactionType): Long? {
        val allCategories = categoryRepository.getAllCategoriesList()
        val topCategories = allCategories.filter { it.parentId == null && it.type == type }

        // 子分类名称 → 父分类关键词映射
        val subcategoryMapping = mapOf(
            // 餐饮子分类
            "火锅" to listOf("餐饮", "吃饭", "美食"),
            "烧烤" to listOf("餐饮", "吃饭", "美食"),
            "奶茶" to listOf("餐饮", "饮品", "美食"),
            "咖啡" to listOf("餐饮", "饮品", "美食"),
            "外卖" to listOf("餐饮", "吃饭", "美食"),
            "零食" to listOf("餐饮", "美食"),
            "水果" to listOf("餐饮", "美食"),
            "早餐" to listOf("餐饮", "吃饭"),
            "午餐" to listOf("餐饮", "吃饭"),
            "晚餐" to listOf("餐饮", "吃饭"),
            "夜宵" to listOf("餐饮", "吃饭"),
            // 交通子分类
            "打车" to listOf("交通", "出行"),
            "地铁" to listOf("交通", "出行"),
            "公交" to listOf("交通", "出行"),
            "加油" to listOf("交通", "出行", "汽车"),
            "停车" to listOf("交通", "出行", "汽车"),
            "高铁" to listOf("交通", "出行"),
            "机票" to listOf("交通", "出行", "旅行"),
            // 购物子分类
            "衣服" to listOf("购物", "服饰"),
            "鞋子" to listOf("购物", "服饰"),
            "化妆品" to listOf("购物", "美妆"),
            "护肤" to listOf("购物", "美妆"),
            "数码" to listOf("购物", "电子"),
            "家电" to listOf("购物", "家居"),
            "日用品" to listOf("购物", "家居"),
            // 娱乐子分类
            "电影" to listOf("娱乐", "休闲"),
            "游戏" to listOf("娱乐", "休闲"),
            "KTV" to listOf("娱乐", "休闲"),
            "健身" to listOf("娱乐", "运动"),
            "旅游" to listOf("娱乐", "旅行"),
            // 教育子分类
            "书籍" to listOf("教育", "学习"),
            "课程" to listOf("教育", "学习"),
            "培训" to listOf("教育", "学习"),
            // 医疗子分类
            "药品" to listOf("医疗", "健康"),
            "体检" to listOf("医疗", "健康"),
            "挂号" to listOf("医疗", "健康"),
            // 住房子分类
            "房租" to listOf("住房", "居住"),
            "水电" to listOf("住房", "居住"),
            "物业" to listOf("住房", "居住"),
            "维修" to listOf("住房", "居住")
        )

        // 1. 精确匹配映射表
        for ((keyword, parentKeywords) in subcategoryMapping) {
            if (name.contains(keyword)) {
                for (parentKeyword in parentKeywords) {
                    val match = topCategories.find { it.name.contains(parentKeyword) }
                    if (match != null) return match.id
                }
            }
        }

        // 2. 模糊语义推断：检查新分类名是否是某个已有分类的细化
        for (parent in topCategories) {
            val parentName = parent.name
            // 如果新分类名包含父分类名的一部分（如"中餐"包含"餐"）
            if (parentName.length >= 2 && name != parentName) {
                val parentChars = parentName.toList()
                val matchCount = parentChars.count { name.contains(it) }
                if (matchCount >= parentName.length / 2 && matchCount > 0) {
                    return parent.id
                }
            }
        }

        return null
    }
    
    /**
     * 根据分类名称智能获取图标和颜色
     */
    private fun getSmartIconAndColor(name: String, type: TransactionType): Pair<String, String> {
        val lowerName = name.lowercase()
        
        return when {
            // 餐饮类
            lowerName.contains("餐饮") || lowerName.contains("食物") || lowerName.contains("吃") || 
            lowerName.contains("饭") || lowerName.contains("菜") || lowerName.contains("肉") -> {
                "🍽️" to "#FF6B6B"
            }
            lowerName.contains("早餐") || lowerName.contains("午餐") || lowerName.contains("晚餐") -> {
                "🍚" to "#FF8C42"
            }
            lowerName.contains("水果") -> {
                "🍎" to "#FF6B9D"
            }
            lowerName.contains("零食") || lowerName.contains("小吃") -> {
                "🍿" to "#FFB347"
            }
            lowerName.contains("饮料") || lowerName.contains("奶茶") || lowerName.contains("咖啡") -> {
                "☕" to "#8B4513"
            }
            
            // 交通类
            lowerName.contains("交通") || lowerName.contains("公交") || lowerName.contains("地铁") -> {
                "🚌" to "#4ECDC4"
            }
            lowerName.contains("打车") || lowerName.contains("出租车") || lowerName.contains("滴滴") -> {
                "🚕" to "#FFD93D"
            }
            lowerName.contains("加油") || lowerName.contains("油费") -> {
                "⛽" to "#E74C3C"
            }
            lowerName.contains("停车") -> {
                "🅿️" to "#3498DB"
            }
            lowerName.contains("高铁") || lowerName.contains("火车") || lowerName.contains("动车") -> {
                "🚄" to "#9B59B6"
            }
            lowerName.contains("飞机") || lowerName.contains("机票") -> {
                "✈️" to "#1ABC9C"
            }
            
            // 购物类
            lowerName.contains("购物") || lowerName.contains("买东西") -> {
                "🛍️" to "#E91E63"
            }
            lowerName.contains("衣服") || lowerName.contains("服装") || lowerName.contains("鞋") -> {
                "👕" to "#9C27B0"
            }
            lowerName.contains("化妆品") || lowerName.contains("护肤") || lowerName.contains("美妆") -> {
                "💄" to "#F06292"
            }
            lowerName.contains("日用") || lowerName.contains("生活用品") -> {
                "🧴" to "#00BCD4"
            }
            lowerName.contains("电子") || lowerName.contains("数码") || lowerName.contains("手机") || lowerName.contains("电脑") -> {
                "📱" to "#3F51B5"
            }
            
            // 居住类
            lowerName.contains("房租") || lowerName.contains("租金") || lowerName.contains("房") -> {
                "🏠" to "#795548"
            }
            lowerName.contains("水电") || lowerName.contains("煤气") || lowerName.contains("物业费") -> {
                "💡" to "#FFC107"
            }
            lowerName.contains("装修") || lowerName.contains("家具") -> {
                "🛋️" to "#8D6E63"
            }
            
            // 娱乐类
            lowerName.contains("娱乐") || lowerName.contains("玩") || lowerName.contains("游戏") -> {
                "🎮" to "#9C27B0"
            }
            lowerName.contains("电影") || lowerName.contains("影院") -> {
                "🎬" to "#673AB7"
            }
            lowerName.contains("旅游") || lowerName.contains("旅行") || lowerName.contains("度假") -> {
                "✈️" to "#03A9F4"
            }
            lowerName.contains("ktv") || lowerName.contains("唱歌") || lowerName.contains("音乐") -> {
                "🎤" to "#FF5722"
            }
            
            // 医疗类
            lowerName.contains("医疗") || lowerName.contains("看病") || lowerName.contains("医院") || lowerName.contains("药") -> {
                "💊" to "#F44336"
            }
            lowerName.contains("体检") || lowerName.contains("检查") -> {
                "🏥" to "#E91E63"
            }
            
            // 学习类
            lowerName.contains("学习") || lowerName.contains("教育") || lowerName.contains("培训") || lowerName.contains("课程") -> {
                "📚" to "#2196F3"
            }
            lowerName.contains("书") || lowerName.contains("教材") -> {
                "📖" to "#4CAF50"
            }
            
            // 收入类
            lowerName.contains("工资") || lowerName.contains("薪水") || lowerName.contains("薪资") -> {
                "💰" to "#4CAF50"
            }
            lowerName.contains("奖金") || lowerName.contains("奖励") || lowerName.contains("红包") -> {
                "🎁" to "#FF9800"
            }
            lowerName.contains("投资") || lowerName.contains("理财") || lowerName.contains("股票") || lowerName.contains("基金") -> {
                "📈" to "#009688"
            }
            lowerName.contains("兼职") || lowerName.contains("副业") || lowerName.contains("外快") -> {
                "💼" to "#607D8B"
            }
            lowerName.contains("退款") || lowerName.contains("退货") || lowerName.contains("返现") -> {
                "💸" to "#8BC34A"
            }
            
            // 其他常用分类
            lowerName.contains("宠物") || lowerName.contains("猫") || lowerName.contains("狗") -> {
                "🐱" to "#FFAB91"
            }
            lowerName.contains("礼物") || lowerName.contains("送礼") || lowerName.contains("人情") -> {
                "🎀" to "#E91E63"
            }
            lowerName.contains("健身") || lowerName.contains("运动") || lowerName.contains(" gym") -> {
                "💪" to "#FF5722"
            }
            lowerName.contains("保险") -> {
                "🛡️" to "#607D8B"
            }
            lowerName.contains("通讯") || lowerName.contains("话费") || lowerName.contains("流量") || lowerName.contains("宽带") -> {
                "📞" to "#00BCD4"
            }
            lowerName.contains("会员") || lowerName.contains("订阅") || lowerName.contains("vip") -> {
                "👑" to "#FFD700"
            }
            
            // 默认
            else -> {
                if (type == TransactionType.INCOME) {
                    "💵" to "#4CAF50"
                } else {
                    "📦" to "#9E9E9E"
                }
            }
        }
    }
    
    private suspend fun updateCategory(op: AIOperation.UpdateCategory): AIOperationResult {
        val category = categoryRepository.getCategoryById(op.id) ?: return AIOperationResult.Error("未找到该分类")
        val updated = category.copy(
            name = op.name ?: category.name,
            color = op.color ?: category.color,
            icon = op.icon ?: category.icon
        )
        categoryRepository.updateCategory(updated)
        writeTrace(
            traceContext = op.traceContext,
            actionType = "UPDATE_CATEGORY",
            entityType = "category",
            entityId = updated.id.toString(),
            summary = "AI 更新分类",
            details = "name=${updated.name}, type=${updated.type}",
            success = true
        )
        return AIOperationResult.Success("已更新分类: ${updated.name}")
    }
    
    private suspend fun deleteCategory(op: AIOperation.DeleteCategory): AIOperationResult {
        val category = categoryRepository.getCategoryById(op.id) ?: return AIOperationResult.Error("未找到该分类")
        categoryRepository.deleteCategory(category)
        writeTrace(
            traceContext = op.traceContext,
            actionType = "DELETE_CATEGORY",
            entityType = "category",
            entityId = category.id.toString(),
            summary = "AI 删除分类",
            details = "name=${category.name}, type=${category.type}",
            success = true
        )
        return AIOperationResult.Success("已删除分类: ${category.name}")
    }
    
    private suspend fun addBudget(op: AIOperation.AddBudget): AIOperationResult {
        val calendar = java.util.Calendar.getInstance()
        val budget = Budget(
            name = op.categoryId?.let { "分类预算" } ?: "月度总预算",
            amount = op.amount,
            categoryId = op.categoryId,
            period = op.period,
            year = calendar.get(java.util.Calendar.YEAR),
            month = calendar.get(java.util.Calendar.MONTH) + 1
        )
        budgetRepository.insertBudget(budget)
        return AIOperationResult.Success("已添加预算: ${op.amount}元")
    }
    
    private suspend fun updateBudget(op: AIOperation.UpdateBudget): AIOperationResult {
        val budget = budgetRepository.getBudgetById(op.id) ?: return AIOperationResult.Error("未找到该预算")
        val updated = budget.copy(
            amount = op.amount ?: budget.amount,
            period = op.period ?: budget.period
        )
        budgetRepository.updateBudget(updated)
        return AIOperationResult.Success("已更新预算")
    }
    
    private suspend fun deleteBudget(op: AIOperation.DeleteBudget): AIOperationResult {
        val budget = budgetRepository.getBudgetById(op.id) ?: return AIOperationResult.Error("未找到该预算")
        budgetRepository.deleteBudget(budget)
        return AIOperationResult.Success("已删除预算")
    }
    
    private suspend fun queryData(op: AIOperation.QueryData): AIOperationResult {
        return when (op.queryType) {
            "transactions" -> {
                val transactions = transactionRepository.getRecentTransactionsSync(op.limit ?: 10)
                AIOperationResult.Success("最近${transactions.size}笔交易:\n${transactions.joinToString("\n") { "${it.date}: ${if (it.type == TransactionType.INCOME) "+" else "-"}${it.amount}" }}")
            }
            "accounts" -> {
                val accounts = accountRepository.getAllAccountsSync()
                AIOperationResult.Success("账户列表:\n${accounts.joinToString("\n") { "${it.name}: ${it.balance}元" }}")
            }
            "categories" -> {
                val categories = categoryRepository.getAllCategoriesSync()
                AIOperationResult.Success("分类列表:\n${categories.joinToString("\n") { it.name }}")
            }
            "balance" -> {
                val accounts = accountRepository.getAllAccountsSync()
                val totalBalance = accounts.sumOf { it.balance }
                AIOperationResult.Success("总资产: ${totalBalance}元")
            }
            else -> AIOperationResult.Error("未知的查询类型")
        }
    }
    
    private suspend fun exportData(op: AIOperation.ExportData): AIOperationResult {
        return AIOperationResult.Success("数据导出功能已触发，请前往导出页面查看")
    }
    
    private suspend fun generateReport(op: AIOperation.GenerateReport): AIOperationResult {
        return AIOperationResult.Success("报表生成功能已触发")
    }
}

/**
 * AI 留痕上下文
 */
data class AITraceContext(
    val sourceType: String = "AI_LOCAL",
    val traceId: String = UUID.randomUUID().toString()
)

/**
 * AI操作指令密封类
 */
sealed class AIOperation {
    // 交易操作
    data class AddTransaction(
        val amount: Double,
        val type: TransactionType,
        val categoryId: Long? = null,
        val accountId: Long? = null,
        val transferAccountId: Long? = null,
        val date: Long = System.currentTimeMillis(),
        val note: String? = null,
        val description: String? = null,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    data class UpdateTransaction(
        val id: Long,
        val amount: Double? = null,
        val type: TransactionType? = null,
        val categoryId: Long? = null,
        val accountId: Long? = null,
        val note: String? = null,
        val description: String? = null,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    data class DeleteTransaction(
        val id: Long,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    // 账户操作
    data class AddAccount(
        val name: String,
        val type: AccountType,
        val balance: Double = 0.0,
        val color: String = "#2196F3",
        val icon: String = "💰",
        val isDefault: Boolean = false,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    data class UpdateAccount(
        val id: Long,
        val name: String? = null,
        val balance: Double? = null,
        val color: String? = null,
        val icon: String? = null,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    data class DeleteAccount(
        val id: Long,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    // 分类操作
    data class AddCategory(
        val name: String,
        val type: TransactionType,
        val color: String = "#2196F3",
        val icon: String = "📁",
        val parentId: Long? = null,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    data class UpdateCategory(
        val id: Long,
        val name: String? = null,
        val color: String? = null,
        val icon: String? = null,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()

    data class DeleteCategory(
        val id: Long,
        val traceContext: AITraceContext = AITraceContext()
    ) : AIOperation()
    
    // 预算操作
    data class AddBudget(
        val categoryId: Long,
        val amount: Double,
        val period: BudgetPeriod = BudgetPeriod.MONTHLY,
        val startDate: Long = System.currentTimeMillis(),
        val endDate: Long? = null
    ) : AIOperation()
    
    data class UpdateBudget(
        val id: Long,
        val amount: Double? = null,
        val period: BudgetPeriod? = null
    ) : AIOperation()
    
    data class DeleteBudget(val id: Long) : AIOperation()
    
    // 查询操作
    data class QueryData(
        val queryType: String,
        val limit: Int? = null
    ) : AIOperation()
    
    // 导出操作
    data class ExportData(
        val format: String = "excel",
        val startDate: Long? = null,
        val endDate: Long? = null
    ) : AIOperation()
    
    // 报表操作
    data class GenerateReport(
        val reportType: String,
        val startDate: Long? = null,
        val endDate: Long? = null
    ) : AIOperation()
}
