package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.AccountDao
import com.example.aiaccounting.data.local.entity.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Account data
 */
@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Get all accounts as Flow
     */
    fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts()
    }

    /**
     * Get all accounts grouped by type
     */
    fun getAllAccountsGrouped(): Flow<List<List<Account>>> {
        return accountDao.getAllAccounts().map { accounts ->
            val grouped = accounts.groupBy { it.type }
            grouped.values.map { accountList ->
                accountList.sortedByDescending { it.isDefault }
            }.toList()
        }
    }

    /**
     * Get all accounts as list
     */
    suspend fun getAllAccountsList(): List<Account> {
        return accountDao.getAllAccounts().first()
    }

    /**
     * Get all accounts synchronously (for AI operations)
     */
    suspend fun getAllAccountsSync(): List<Account> {
        return getAllAccountsList()
    }

    /**
     * Get account by ID
     */
    suspend fun getAccountById(accountId: Long): Account? {
        return accountDao.getAccountById(accountId)
    }

    /**
     * Find account by name (case-insensitive, partial match)
     */
    suspend fun findAccountByName(name: String): Account? {
        val accounts = getAllAccountsList()
        val lowerName = name.lowercase()
        return accounts.find { account ->
            account.name.lowercase() == lowerName ||
            account.name.lowercase().contains(lowerName) ||
            lowerName.contains(account.name.lowercase())
        }
    }

    /**
     * Insert new account
     */
    suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account)
    }

    /**
     * Update account
     */
    suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    /**
     * Delete account and all related transactions
     */
    suspend fun deleteAccount(account: Account) {
        // 先删除该账户下的所有交易记录
        transactionRepository.deleteTransactionsByAccount(account.id)
        // 再删除账户
        accountDao.deleteAccount(account)
    }

    /**
     * Delete account by ID and all related transactions
     */
    suspend fun deleteAccountById(accountId: Long) {
        // 先删除该账户下的所有交易记录
        transactionRepository.deleteTransactionsByAccount(accountId)
        // 再删除账户
        accountDao.deleteAccountById(accountId)
    }

    /**
     * Archive account
     */
    suspend fun archiveAccount(accountId: Long) {
        accountDao.archiveAccount(accountId)
    }

    /**
     * Update account balance
     */
    suspend fun updateBalance(accountId: Long, amount: Double) {
        accountDao.updateBalance(accountId, amount)
    }

    /**
     * Set default account
     */
    suspend fun setDefaultAccount(accountId: Long) {
        accountDao.setDefaultAccount(accountId)
    }

    /**
     * Get default account
     */
    suspend fun getDefaultAccount(): Account? {
        return accountDao.getDefaultAccount()
    }

    /**
     * Get total balance
     */
    suspend fun getTotalBalance(): Double {
        return accountDao.getTotalBalance() ?: 0.0
    }

    /**
     * Get total assets (positive balance accounts)
     */
    fun getTotalAssets(): Flow<Double> {
        return accountDao.getTotalAssets()
    }

    /**
     * Get total liabilities (negative balance accounts)
     */
    fun getTotalLiabilities(): Flow<Double> {
        return accountDao.getTotalLiabilities()
    }

    /**
     * Get account count
     */
    suspend fun getAccountCount(): Int {
        return accountDao.getAccountCount()
    }

    /**
     * Check if account exists
     */
    suspend fun accountExists(name: String): Boolean {
        return accountDao.accountExists(name)
    }

    /**
     * Update account balance
     */
    suspend fun updateAccountBalance(accountId: Long, newBalance: Double) {
        accountDao.updateAccountBalance(accountId, newBalance)
    }

    /**
     * Adjust account balance
     */
    suspend fun adjustAccountBalance(accountId: Long, amount: Double) {
        accountDao.adjustAccountBalance(accountId, amount)
    }
}
