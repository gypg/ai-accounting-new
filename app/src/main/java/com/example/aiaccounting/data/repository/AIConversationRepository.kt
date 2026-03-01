package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.AIConversationDao
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.ConversationRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for AI Conversation data
 */
@Singleton
class AIConversationRepository @Inject constructor(
    private val conversationDao: AIConversationDao
) {

    /**
     * Get all conversations
     */
    fun getAllConversations(): Flow<List<AIConversation>> {
        return conversationDao.getAllConversations()
    }

    /**
     * Get all conversations as list
     */
    suspend fun getAllConversationsList(): List<AIConversation> {
        return conversationDao.getAllConversations().first()
    }

    /**
     * Get conversation by ID
     */
    suspend fun getConversationById(conversationId: Long): AIConversation? {
        return conversationDao.getConversationById(conversationId)
    }

    /**
     * Get conversations after a timestamp
     */
    fun getConversationsAfter(startTime: Long): Flow<List<AIConversation>> {
        return conversationDao.getConversationsAfter(startTime)
    }

    /**
     * Get conversations by date range
     */
    fun getConversationsByDateRange(startTime: Long, endTime: Long): Flow<List<AIConversation>> {
        return conversationDao.getConversationsByDateRange(startTime, endTime)
    }

    /**
     * Get conversations by role
     */
    fun getConversationsByRole(role: ConversationRole, limit: Int = 50): Flow<List<AIConversation>> {
        return conversationDao.getConversationsByRole(role, limit)
    }

    /**
     * Get conversations by transaction
     */
    fun getConversationsByTransaction(transactionId: Long): Flow<List<AIConversation>> {
        return conversationDao.getConversationsByTransaction(transactionId)
    }

    /**
     * Insert new conversation
     */
    suspend fun insertConversation(conversation: AIConversation): Long {
        return conversationDao.insertConversation(conversation)
    }

    /**
     * Insert multiple conversations
     */
    suspend fun insertConversations(conversations: List<AIConversation>) {
        conversationDao.insertConversations(conversations)
    }

    /**
     * Update conversation
     */
    suspend fun updateConversation(conversation: AIConversation) {
        conversationDao.updateConversation(conversation)
    }

    /**
     * Delete conversation
     */
    suspend fun deleteConversation(conversation: AIConversation) {
        conversationDao.deleteConversation(conversation)
    }

    /**
     * Delete conversation by ID
     */
    suspend fun deleteConversationById(conversationId: Long) {
        conversationDao.deleteConversationById(conversationId)
    }

    /**
     * Delete conversations before a timestamp
     */
    suspend fun deleteConversationsBefore(beforeTime: Long): Int {
        return conversationDao.deleteConversationsBefore(beforeTime)
    }

    /**
     * Clear all conversations
     */
    suspend fun clearAllConversations(): Int {
        return conversationDao.clearAllConversations()
    }

    /**
     * Get conversation count
     */
    suspend fun getConversationCount(): Int {
        return conversationDao.getConversationCount()
    }

    /**
     * Get bookmarked conversations
     */
    fun getBookmarkedConversations(): Flow<List<AIConversation>> {
        return conversationDao.getBookmarkedConversations()
    }

    /**
     * Update bookmark status
     */
    suspend fun updateBookmarkStatus(conversationId: Long, isBookmarked: Boolean) {
        conversationDao.updateBookmarkStatus(conversationId, isBookmarked)
    }

    /**
     * Search conversations
     */
    fun searchConversations(query: String, limit: Int = 20): Flow<List<AIConversation>> {
        return conversationDao.searchConversations(query, limit)
    }

    /**
     * Get recent conversations for context
     */
    suspend fun getRecentConversationsForContext(limit: Int = 10): List<AIConversation> {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return conversationDao.getConversationsAfter(oneWeekAgo).first().takeLast(limit)
    }

    /**
     * Add user message
     */
    suspend fun addUserMessage(content: String, transactionId: Long? = null): Long {
        val conversation = AIConversation(
            role = ConversationRole.USER,
            content = content,
            transactionId = transactionId
        )
        return insertConversation(conversation)
    }

    /**
     * Add assistant message
     */
    suspend fun addAssistantMessage(content: String, transactionId: Long? = null): Long {
        val conversation = AIConversation(
            role = ConversationRole.ASSISTANT,
            content = content,
            transactionId = transactionId
        )
        return insertConversation(conversation)
    }

    /**
     * Clear old conversations (older than 30 days)
     */
    suspend fun clearOldConversations(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
        return deleteConversationsBefore(thirtyDaysAgo)
    }
}