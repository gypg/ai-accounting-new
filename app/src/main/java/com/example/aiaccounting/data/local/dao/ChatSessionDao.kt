package com.example.aiaccounting.data.local.dao

import androidx.room.*
import com.example.aiaccounting.data.local.entity.ChatMemory
import com.example.aiaccounting.data.local.entity.ChatMessageEntity
import com.example.aiaccounting.data.local.entity.ChatSession
import com.example.aiaccounting.data.local.entity.MemoryCategory
import kotlinx.coroutines.flow.Flow

/**
 * 对话会话 DAO
 */
@Dao
interface ChatSessionDao {
    
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>
    
    @Query("SELECT * FROM chat_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): ChatSession?
    
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): ChatSession?
    
    @Insert
    suspend fun insertSession(session: ChatSession)
    
    @Update
    suspend fun updateSession(session: ChatSession)
    
    @Delete
    suspend fun deleteSession(session: ChatSession)
    
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
    
    @Query("UPDATE chat_sessions SET isActive = 0")
    suspend fun clearActiveSession()
    
    @Query("UPDATE chat_sessions SET isActive = 1 WHERE id = :sessionId")
    suspend fun setActiveSession(sessionId: String)
    
    @Query("UPDATE chat_sessions SET messageCount = messageCount + 1, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun incrementMessageCount(sessionId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun getSessionCount(): Int
}

/**
 * 对话消息 DAO
 */
@Dao
interface ChatMessageDao {
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: String): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: String, limit: Int): List<ChatMessageEntity>
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND content LIKE '%' || :keyword || '%'")
    suspend fun searchMessages(sessionId: String, keyword: String): List<ChatMessageEntity>
    
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: String)
    
    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int
}

/**
 * 对话记忆 DAO
 */
@Dao
interface ChatMemoryDao {
    
    @Query("SELECT * FROM chat_memories WHERE sessionId = :sessionId ORDER BY importance DESC, lastAccessedAt DESC")
    fun getMemoriesBySession(sessionId: String): Flow<List<ChatMemory>>
    
    @Query("SELECT * FROM chat_memories WHERE sessionId = :sessionId AND category = :category ORDER BY importance DESC")
    suspend fun getMemoriesByCategory(sessionId: String, category: MemoryCategory): List<ChatMemory>
    
    @Query("SELECT * FROM chat_memories WHERE sessionId = :sessionId AND content LIKE '%' || :keyword || '%' ORDER BY importance DESC")
    suspend fun searchMemories(sessionId: String, keyword: String): List<ChatMemory>
    
    @Query("SELECT * FROM chat_memories WHERE sessionId = :sessionId ORDER BY lastAccessedAt DESC LIMIT :limit")
    suspend fun getRecentMemories(sessionId: String, limit: Int): List<ChatMemory>
    
    @Insert
    suspend fun insertMemory(memory: ChatMemory)
    
    @Update
    suspend fun updateMemory(memory: ChatMemory)
    
    @Delete
    suspend fun deleteMemory(memory: ChatMemory)
    
    @Query("DELETE FROM chat_memories WHERE sessionId = :sessionId")
    suspend fun deleteMemoriesBySession(sessionId: String)
    
    @Query("UPDATE chat_memories SET lastAccessedAt = :timestamp WHERE id = :memoryId")
    suspend fun updateLastAccessed(memoryId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM chat_memories WHERE sessionId = :sessionId AND category IN (:categories) ORDER BY importance DESC LIMIT :limit")
    suspend fun getMemoriesByCategories(sessionId: String, categories: List<MemoryCategory>, limit: Int): List<ChatMemory>
}
