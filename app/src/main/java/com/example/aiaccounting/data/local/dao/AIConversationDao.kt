package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.AIConversation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AIConversation entity
 */
@Dao
interface AIConversationDao {

    @Query("SELECT * FROM ai_conversations ORDER BY timestamp ASC")
    fun getAllConversations(): Flow<List<AIConversation>>

    @Query("SELECT * FROM ai_conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: Long): AIConversation?

    @Query("""
        SELECT * FROM ai_conversations 
        WHERE timestamp >= :startTime 
        ORDER BY timestamp ASC
    """)
    fun getConversationsAfter(startTime: Long): Flow<List<AIConversation>>

    @Query("""
        SELECT * FROM ai_conversations 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp ASC
    """)
    fun getConversationsByDateRange(startTime: Long, endTime: Long): Flow<List<AIConversation>>

    @Query("""
        SELECT * FROM ai_conversations 
        WHERE role = :role 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getConversationsByRole(role: com.example.aiaccounting.data.local.entity.ConversationRole, limit: Int = 50): Flow<List<AIConversation>>

    @Query("""
        SELECT * FROM ai_conversations 
        WHERE transactionId = :transactionId
        ORDER BY timestamp ASC
    """)
    fun getConversationsByTransaction(transactionId: Long): Flow<List<AIConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AIConversation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<AIConversation>)

    @Update
    suspend fun updateConversation(conversation: AIConversation)

    @Delete
    suspend fun deleteConversation(conversation: AIConversation)

    @Query("DELETE FROM ai_conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: Long)

    @Query("DELETE FROM ai_conversations WHERE timestamp < :beforeTime")
    suspend fun deleteConversationsBefore(beforeTime: Long): Int

    @Query("DELETE FROM ai_conversations")
    suspend fun clearAllConversations(): Int

    @Query("SELECT COUNT(*) FROM ai_conversations")
    suspend fun getConversationCount(): Int

    @Query("SELECT * FROM ai_conversations WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedConversations(): Flow<List<AIConversation>>

    @Query("UPDATE ai_conversations SET isBookmarked = :isBookmarked WHERE id = :conversationId")
    suspend fun updateBookmarkStatus(conversationId: Long, isBookmarked: Boolean)

    @Query("""
        SELECT * FROM ai_conversations 
        WHERE content LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun searchConversations(query: String, limit: Int = 20): Flow<List<AIConversation>>
}