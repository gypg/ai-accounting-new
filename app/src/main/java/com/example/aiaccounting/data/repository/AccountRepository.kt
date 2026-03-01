package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.AccountDao
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Account data
 */
@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {

    /**
     * Get all accounts as Flow
     */
    fun getAllAccounts(): Flow<List<List<Account>>> {
        return accountDao.getAllAccounts().map { accounts ->
            accounts.groupBy { it.type }
                .mapValues { entry -> entry.value.sortedByDescending { it.isDefault } }
                .values
                .toList()
        }
    }

    /**
     * Get all accounts as list
     */
    suspend fun getAllAccountsList(): List<Account> {
        return accountDao.getAllAccounts().first()
    }

    /**
     * Get account by ID
     */
    suspend fun getAccountById(accountId: Long): Account? {
        return accountDao.getAccountById(accountId)
    }

    /**
     * Get default account
     */
    suspend fun getDefaultAccount(): Account? {
        return accountDao.getDefaultAccount()
    }

    /**
     * Get accounts by type
     */
    fun getAccountsByType(type: AccountType): Flow<List<Account>> {
        return accountDao.getAccountsByType(type)
    }

    /**
     * Insert new account
     */
    suspend fun insertAccount(account: Account): Long {
        // If this is set as default, remove default from other accounts
        if (account.isDefault) {
            val existingAccounts = getAllAccountsList()
            existingAccounts.forEach { acc ->
                if (acc.isDefault && acc.id != account.id) {
                    accountDao.updateAccount(acc.copy(isDefault = false))
                }
            }
        }
        return accountDao.insertAccount(account)
    }

    /**
     * Update account
     */
    suspend fun updateAccount(account: Account) {
        // If this is set as default, remove default from other accounts
        if (account.isDefault) {
            val existingAccounts = getAllAccountsList()
            existingAccounts.forEach { acc ->
                if (acc.isDefault && acc.id != account.id) {
                    accountDao.updateAccount(acc.copy(isDefault = false))
                }
            }
        }
        accountDao.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Delete account
     */
    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account)
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
     * Get total total balance across all accounts
     */
    suspend fun getTotalBalance(): Double {
        return accountDao.getTotalBalance() ?: 0.0
    }

    /**
     * Search accounts
     */
    fun searchAccounts(query: String): Flow<List<Account>> {
        return accountDao.searchAccounts(query)
    }

    /**
     * Create default cash account if none exists
     */
    suspend fun createDefaultCashAccountIfNeeded() {
        val accounts = getAllAccountsList()
        if (accounts.isEmpty()) {
            val defaultAccount = Account(
                name = "现金",
                type = AccountType.CASH,
                balance = 0.0,
                icon = "💵",
                color = "#4CAF50",
                isDefault = true
            )
            insertAccount(defaultAccount)
        }
    }
}
