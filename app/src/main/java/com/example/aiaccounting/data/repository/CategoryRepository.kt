package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.CategoryDao
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Category data
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Get all categories
     */
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    /**
     * Get all categories as list
     */
    suspend fun getAllCategoriesList(): List<Category> {
        return categoryDao.getAllCategories().first()
    }

    /**
     * Get all categories synchronously (for AI operations)
     */
    suspend fun getAllCategoriesSync(): List<Category> {
        return getAllCategoriesList()
    }

    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: Long): Category? {
        return categoryDao.getCategoryById(categoryId)
    }

    /**
     * Get categories by type (income/expense)
     */
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type)
    }

    /**
     * Get parent categories
     */
    fun getParentCategories(): Flow<List<Category>> {
        return categoryDao.getParentCategories()
    }

    /**
     * Get sub-categories
     */
    fun getSubCategories(parentId: Long): Flow<List<Category>> {
        return categoryDao.getSubCategories(parentId)
    }

    /**
     * Insert new category
     */
    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    /**
     * Update category
     */
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Delete category and all related transactions
     */
    suspend fun deleteCategory(category: Category) {
        // 先删除该分类下的所有交易记录
        transactionRepository.deleteTransactionsByCategory(category.id)
        // 再删除分类
        categoryDao.deleteCategory(category)
    }

    /**
     * Delete category by ID and all related transactions
     */
    suspend fun deleteCategoryById(categoryId: Long) {
        // 先删除该分类下的所有交易记录
        transactionRepository.deleteTransactionsByCategory(categoryId)
        // 再删除分类
        categoryDao.deleteCategoryById(categoryId)
    }

    /**
     * Search categories
     */
    fun searchCategories(query: String): Flow<List<Category>> {
        return categoryDao.searchCategories(query)
    }

    /**
     * Get income categories
     */
    fun getIncomeCategories(): Flow<List<Category>> {
        return getCategoriesByType(TransactionType.INCOME)
    }

    /**
     * Get expense categories
     */
    fun getExpenseCategories(): Flow<List<Category>> {
        return getCategoriesByType(TransactionType.EXPENSE)
    }

    /**
     * Get default categories
     */
    suspend fun getDefaultCategories(): List<Category> {
        return categoryDao.getDefaultCategories()
    }
}