package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.Account
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Account entity
 */
@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY isDefault DESC, name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): Account?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?

    @Query("SELECT * FROM accounts WHERE type = :type AND isArchived = 0")
    fun getAccountsByType(type: com.example.aiaccounting.data.local.entity.AccountType): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: Long)

    @Query("UPDATE accounts SET isDefault = 0 WHERE isDefault = 1")
    suspend fun clearDefaultAccount()

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :accountId")
    suspend fun setDefaultAccount(accountId: Long)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :accountId")
    suspend fun archiveAccount(accountId: Long)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amount: Double)

    @Query("UPDATE accounts SET balance = :newBalance WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, newBalance: Double)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: Long, amount: Double)

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0")
    suspend fun getTotalBalance(): Double?

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0 AND balance > 0")
    fun getTotalAssets(): Flow<Double>

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0 AND balance < 0")
    fun getTotalLiabilities(): Flow<Double>

    @Query("SELECT COUNT(*) FROM accounts WHERE isArchived = 0")
    suspend fun getAccountCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM accounts WHERE name = :name AND isArchived = 0)")
    suspend fun accountExists(name: String): Boolean

    @Query("SELECT * FROM accounts WHERE name LIKE '%' || :query || '%' AND isArchived = 0")
    fun searchAccounts(query: String): Flow<List<Account>>
}
