package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.TransactionTemplate
import kotlinx.coroutines.flow.Flow

/**
 * 记账模板DAO
 */
@Dao
interface TransactionTemplateDao {

    @Query("SELECT * FROM transaction_templates WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getAllTemplates(): Flow<List<TransactionTemplate>>

    @Query("SELECT * FROM transaction_templates WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAllTemplatesList(): List<TransactionTemplate>

    @Query("SELECT * FROM transaction_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): TransactionTemplate?

    @Insert
    suspend fun insertTemplate(template: TransactionTemplate): Long

    @Update
    suspend fun updateTemplate(template: TransactionTemplate)

    @Delete
    suspend fun deleteTemplate(template: TransactionTemplate)

    @Query("DELETE FROM transaction_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)

    @Query("SELECT COUNT(*) FROM transaction_templates")
    suspend fun getTemplateCount(): Int
}
