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

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :accountId")
    suspend fun archiveAccount(accountId: Long)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amount: Double)

    @Query("SELECT SUM(balance) FROM accounts WHERE isArchived = 0")
    suspend fun getTotalBalance(): Double?

    @Query("SELECT * FROM accounts WHERE name LIKE '%' || :query || '%' AND isArchived = 0")
    fun searchAccounts(query: String): Flow<List<Account>>
}