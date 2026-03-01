package com.example.aiaccounting.data.local

import androidx.room.*
import com.example.aiaccounting.data.model.AIConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface AIConversationDao {
    @Query("SELECT * FROM ai_conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<AIConversation>>
    
    @Query("SELECT * FROM ai_conversations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConversations(limit: Int = 50): Flow<List<AIConversation>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AIConversation): Long
    
    @Delete
    suspend fun deleteConversation(conversation: AIConversation)
    
    @Query("DELETE FROM ai_conversations")
    suspend fun deleteAllConversations()
}
