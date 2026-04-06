package com.example.aiaccounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.aiaccounting.data.local.entity.AppLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLogEntryDao {

    @Insert
    suspend fun insertLog(entry: AppLogEntry)

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AppLogEntry>>

    @Query("DELETE FROM app_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM app_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteLogsBefore(beforeTimestamp: Long)
}
