package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.AIPermissionLogDao
import com.example.aiaccounting.data.local.entity.AIPermissionLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI权限日志Repository
 */
@Singleton
class AIPermissionLogRepository @Inject constructor(
    private val logDao: AIPermissionLogDao
) {
    
    suspend fun insertLog(log: AIPermissionLog) {
        logDao.insertLog(log)
    }
    
    fun getRecentLogs(limit: Int): Flow<List<AIPermissionLog>> {
        return logDao.getRecentLogs(limit)
    }
    
    fun getPendingInterventions(): Flow<List<AIPermissionLog>> {
        return logDao.getPendingInterventions()
    }
    
    suspend fun updateHumanIntervention(
        logId: String, 
        approved: Boolean, 
        reason: String
    ) {
        logDao.updateHumanIntervention(
            logId = logId,
            approved = approved,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun getLogById(logId: String): AIPermissionLog? {
        return logDao.getLogById(logId)
    }
    
    suspend fun clearOldLogs(beforeTimestamp: Long) {
        logDao.deleteLogsBefore(beforeTimestamp)
    }
}
