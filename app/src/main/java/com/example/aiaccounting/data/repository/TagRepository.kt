package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.TagDao
import com.example.aiaccounting.data.local.entity.Tag
import com.example.aiaccounting.data.local.entity.TransactionTag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {
    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    suspend fun getTagById(tagId: Long): Tag? = tagDao.getTagById(tagId)

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun insertTags(tags: List<Tag>) = tagDao.insertTags(tags)

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    suspend fun deleteTagById(tagId: Long) = tagDao.deleteTagById(tagId)

    fun getTagsForTransaction(transactionId: Long): Flow<List<Tag>> =
        tagDao.getTagsForTransaction(transactionId)

    suspend fun getTagsForTransactionSync(transactionId: Long): List<Tag> =
        tagDao.getTagsForTransactionSync(transactionId)

    suspend fun setTransactionTags(transactionId: Long, tagIds: List<Long>) {
        tagDao.deleteTransactionTags(transactionId)
        tagIds.forEach { tagId ->
            tagDao.insertTransactionTag(TransactionTag(transactionId, tagId))
        }
    }

    suspend fun getTransactionCountForTag(tagId: Long): Int =
        tagDao.getTransactionCountForTag(tagId)

    suspend fun getTagCount(): Int = tagDao.getTagCount()
}
