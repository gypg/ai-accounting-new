package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

/**
 * Data Data Access Object for Category entity
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY type, `order`, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY `order`, name ASC")
    fun getCategoriesByType(type: com.example.aiaccounting.data.local.entity.TransactionType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY type, `order`, name ASC")
    fun getParentCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY `order`, name ASC")
    fun getSubCategories(parentId: Long): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%'")
    fun searchCategories(query: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<Category>
}