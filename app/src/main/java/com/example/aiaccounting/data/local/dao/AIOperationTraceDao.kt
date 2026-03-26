package com.example.aiaccounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.aiaccounting.data.local.entity.AIOperationTrace
import kotlinx.coroutines.flow.Flow

@Dao
interface AIOperationTraceDao {

    @Insert
    suspend fun insertTrace(trace: AIOperationTrace)

    @Query("SELECT * FROM ai_operation_traces WHERE traceId = :traceId ORDER BY timestamp ASC")
    fun getTracesByTraceId(traceId: String): Flow<List<AIOperationTrace>>

    @Query("SELECT * FROM ai_operation_traces WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    fun getTracesByEntity(entityType: String, entityId: String): Flow<List<AIOperationTrace>>

    @Query("SELECT * FROM ai_operation_traces ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTraces(limit: Int): Flow<List<AIOperationTrace>>
}
