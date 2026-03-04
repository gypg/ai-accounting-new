package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.*
import com.example.aiaccounting.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
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
    private val budgetRepository: BudgetRepository
) {
    
    sealed class AIOperationResult {
        data class Success(val message: String) : AIOperationResult()
        data class Error(val error: String) : AIOperationResult()
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
        val account = accountRepository.getAccountById(accountId)
            ?: return AIOperationResult.Error("账户不存在，请先创建账户")
        
        // 检查分类是否存在
        val category = categoryRepository.getCategoryById(categoryId)
            ?: return AIOperationResult.Error("分类不存在")
        
        // 先更新账户余额
        val newBalance = when (op.type) {
            TransactionType.INCOME -> account.balance + op.amount
            TransactionType.EXPENSE -> account.balance - op.amount
            TransactionType.TRANSFER -> account.balance // 转账需要特殊处理
        }
        val updatedAccount = account.copy(balance = newBalance)
        accountRepository.updateAccount(updatedAccount)
        
        // 插入交易记录
        val transaction = Transaction(
            amount = op.amount,
            type = op.type,
            categoryId = categoryId,
            accountId = accountId,
            date = op.date,
            note = op.note ?: ""
        )
        transactionRepository.insertTransaction(transaction)
        
        return AIOperationResult.Success("已添加${if (op.type == TransactionType.INCOME) "收入" else "支出"}记录: ${op.amount}元")
    }
    
    /**
     * 更新交易记录并重新计算账户余额
     */
    private suspend fun updateTransaction(op: AIOperation.UpdateTransaction): AIOperationResult {
        val oldTransaction = transactionRepository.getTransactionById(op.id) 
            ?: return AIOperationResult.Error("未找到该交易记录")
        
        // 恢复旧账户余额
        val oldAccount = accountRepository.getAccountById(oldTransaction.accountId)
        if (oldAccount != null) {
            val restoredBalance = when (oldTransaction.type) {
                TransactionType.INCOME -> oldAccount.balance - oldTransaction.amount
                TransactionType.EXPENSE -> oldAccount.balance + oldTransaction.amount
                TransactionType.TRANSFER -> oldAccount.balance
            }
            accountRepository.updateAccount(oldAccount.copy(balance = restoredBalance))
        }
        
        // 更新交易
        val updated = oldTransaction.copy(
            amount = op.amount ?: oldTransaction.amount,
            type = op.type ?: oldTransaction.type,
            categoryId = op.categoryId ?: oldTransaction.categoryId,
            accountId = op.accountId ?: oldTransaction.accountId,
            note = op.note ?: oldTransaction.note
        )
        transactionRepository.updateTransaction(updated)
        
        // 更新新账户余额
        val newAccount = accountRepository.getAccountById(updated.accountId)
        if (newAccount != null) {
            val newBalance = when (updated.type) {
                TransactionType.INCOME -> newAccount.balance + updated.amount
                TransactionType.EXPENSE -> newAccount.balance - updated.amount
                TransactionType.TRANSFER -> newAccount.balance
            }
            accountRepository.updateAccount(newAccount.copy(balance = newBalance))
        }
        
        return AIOperationResult.Success("已更新交易记录")
    }
    
    /**
     * 删除交易记录并恢复账户余额
     */
    private suspend fun deleteTransaction(op: AIOperation.DeleteTransaction): AIOperationResult {
        val transaction = transactionRepository.getTransactionById(op.id) 
            ?: return AIOperationResult.Error("未找到该交易记录")
        
        // 恢复账户余额
        val account = accountRepository.getAccountById(transaction.accountId)
        if (account != null) {
            val restoredBalance = when (transaction.type) {
                TransactionType.INCOME -> account.balance - transaction.amount
                TransactionType.EXPENSE -> account.balance + transaction.amount
                TransactionType.TRANSFER -> account.balance
            }
            accountRepository.updateAccount(account.copy(balance = restoredBalance))
        }
        
        transactionRepository.deleteTransaction(transaction)
        return AIOperationResult.Success("已删除交易记录")
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
        accountRepository.insertAccount(account)
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
        return AIOperationResult.Success("已更新账户: ${updated.name}")
    }
    
    private suspend fun deleteAccount(op: AIOperation.DeleteAccount): AIOperationResult {
        val account = accountRepository.getAccountById(op.id) ?: return AIOperationResult.Error("未找到该账户")
        accountRepository.deleteAccount(account)
        return AIOperationResult.Success("已删除账户: ${account.name}")
    }
    
    /**
     * 添加分类 - 使用智能图标和颜色
     */
    private suspend fun addCategory(op: AIOperation.AddCategory): AIOperationResult {
        // 根据分类名称智能选择图标和颜色
        val (smartIcon, smartColor) = getSmartIconAndColor(op.name, op.type)
        
        val category = Category(
            name = op.name,
            type = op.type,
            color = op.color.takeIf { it != "#2196F3" } ?: smartColor,
            icon = op.icon.takeIf { it != "📁" } ?: smartIcon
        )
        categoryRepository.insertCategory(category)
        return AIOperationResult.Success("已添加分类: ${op.name}")
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
        return AIOperationResult.Success("已更新分类: ${updated.name}")
    }
    
    private suspend fun deleteCategory(op: AIOperation.DeleteCategory): AIOperationResult {
        val category = categoryRepository.getCategoryById(op.id) ?: return AIOperationResult.Error("未找到该分类")
        categoryRepository.deleteCategory(category)
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
 * AI操作指令密封类
 */
sealed class AIOperation {
    // 交易操作
    data class AddTransaction(
        val amount: Double,
        val type: TransactionType,
        val categoryId: Long? = null,
        val accountId: Long? = null,
        val date: Long = System.currentTimeMillis(),
        val note: String? = null,
        val description: String? = null
    ) : AIOperation()
    
    data class UpdateTransaction(
        val id: Long,
        val amount: Double? = null,
        val type: TransactionType? = null,
        val categoryId: Long? = null,
        val accountId: Long? = null,
        val note: String? = null,
        val description: String? = null
    ) : AIOperation()
    
    data class DeleteTransaction(val id: Long) : AIOperation()
    
    // 账户操作
    data class AddAccount(
        val name: String,
        val type: AccountType,
        val balance: Double = 0.0,
        val color: String = "#2196F3",
        val icon: String = "💰",
        val isDefault: Boolean = false
    ) : AIOperation()
    
    data class UpdateAccount(
        val id: Long,
        val name: String? = null,
        val balance: Double? = null,
        val color: String? = null,
        val icon: String? = null
    ) : AIOperation()
    
    data class DeleteAccount(val id: Long) : AIOperation()
    
    // 分类操作
    data class AddCategory(
        val name: String,
        val type: TransactionType,
        val color: String = "#2196F3",
        val icon: String = "📁"
    ) : AIOperation()
    
    data class UpdateCategory(
        val id: Long,
        val name: String? = null,
        val color: String? = null,
        val icon: String? = null
    ) : AIOperation()
    
    data class DeleteCategory(val id: Long) : AIOperation()
    
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
