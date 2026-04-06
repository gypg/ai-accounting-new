package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.AIOperationTraceDao
import com.example.aiaccounting.data.local.entity.AIOperationTrace
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIOperationTraceRepository @Inject constructor(
    private val traceDao: AIOperationTraceDao
) {
    suspend fun insertTrace(trace: AIOperationTrace) {
        traceDao.insertTrace(trace)
    }

    fun getTracesByTraceId(traceId: String): Flow<List<AIOperationTrace>> {
        return traceDao.getTracesByTraceId(traceId)
    }

    fun getTracesByEntity(entityType: String, entityId: String): Flow<List<AIOperationTrace>> {
        return traceDao.getTracesByEntity(entityType, entityId)
    }

    fun getRecentTraces(limit: Int): Flow<List<AIOperationTrace>> {
        return traceDao.getRecentTraces(limit)
    }

    suspend fun clearOldTraces(beforeTimestamp: Long) {
        traceDao.deleteTracesBefore(beforeTimestamp)
    }
}
