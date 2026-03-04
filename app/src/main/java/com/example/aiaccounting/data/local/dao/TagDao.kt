package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.Tag
import com.example.aiaccounting.data.local.entity.TransactionTag
import kotlinx.coroutines.flow.Flow

/**
 * 标签数据访问对象
 */
@Dao
interface TagDao {
    // ==================== 标签CRUD ====================

    @Query("SELECT * FROM tags ORDER BY createdAt DESC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>)

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    // ==================== 交易标签关联 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionTag(transactionTag: TransactionTag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionTags(transactionTags: List<TransactionTag>)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteTransactionTags(transactionId: Long)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId AND tagId = :tagId")
    suspend fun deleteTransactionTag(transactionId: Long, tagId: Long)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN transaction_tags tt ON t.id = tt.tagId
        WHERE tt.transactionId = :transactionId
    """)
    fun getTagsForTransaction(transactionId: Long): Flow<List<Tag>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN transaction_tags tt ON t.id = tt.tagId
        WHERE tt.transactionId = :transactionId
    """)
    suspend fun getTagsForTransactionSync(transactionId: Long): List<Tag>

    @Query("SELECT COUNT(*) FROM transaction_tags WHERE tagId = :tagId")
    suspend fun getTransactionCountForTag(tagId: Long): Int

    // ==================== 统计 ====================

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int
}
