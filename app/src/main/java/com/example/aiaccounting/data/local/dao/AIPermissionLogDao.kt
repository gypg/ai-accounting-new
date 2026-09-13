package com.example.aiaccounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.aiaccounting.data.local.entity.AIPermissionLog
import kotlinx.coroutines.flow.Flow

/**
 * AI权限日志DAO
 */
@Dao
interface AIPermissionLogDao {
    
    @Insert
    suspend fun insertLog(log: AIPermissionLog)
    
    @Query("""
        SELECT * FROM ai_permission_logs 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentLogs(limit: Int): Flow<List<AIPermissionLog>>
    
    @Query("""
        SELECT * FROM ai_permission_logs 
        WHERE requiresHumanIntervention = 1 
        AND humanApproved IS NULL
        ORDER BY timestamp DESC
    """)
    fun getPendingInterventions(): Flow<List<AIPermissionLog>>
    
    @Query("""
        UPDATE ai_permission_logs 
        SET humanApproved = :approved, 
            humanApprovalReason = :reason,
            humanApprovalTime = :timestamp
        WHERE id = :logId
    """)
    suspend fun updateHumanIntervention(
        logId: String,
        approved: Boolean,
        reason: String,
        timestamp: Long
    )
    
    @Query("SELECT * FROM ai_permission_logs WHERE id = :logId")
    suspend fun getLogById(logId: String): AIPermissionLog?
    
    @Query("DELETE FROM ai_permission_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteLogsBefore(beforeTimestamp: Long)
    
    @Query("SELECT COUNT(*) FROM ai_permission_logs")
    suspend fun getLogCount(): Int
}
