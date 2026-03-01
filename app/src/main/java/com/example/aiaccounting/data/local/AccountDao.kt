package com.example.aiaccounting.data.local

import androidx.room.*
import com.example.aiaccounting.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<Account>>
    
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?
    
    @Query("SELECT * FROM accounts WHERE isAsset = :isAsset")
    fun getAccountsByType(isAsset: Boolean): Flow<List<Account>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long
    
    @Update
    suspend fun updateAccount(account: Account)
    
    @Delete
    suspend fun deleteAccount(account: Account)
    
    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amount: Double)
    
    @Query("SELECT SUM(balance) FROM accounts WHERE isAsset = 1")
    fun getTotalAssets(): Flow<Double?>
    
    @Query("SELECT SUM(balance) FROM accounts WHERE isAsset = 0")
    fun getTotalLiabilities(): Flow<Double?>
}
