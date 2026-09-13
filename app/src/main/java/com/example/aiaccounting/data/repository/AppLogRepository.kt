package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.AppLogEntryDao
import com.example.aiaccounting.data.local.entity.AppLogEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogRepository @Inject constructor(
    private val appLogEntryDao: AppLogEntryDao
) {
    suspend fun insertLog(entry: AppLogEntry) {
        appLogEntryDao.insertLog(entry)
    }

    fun getRecentLogs(limit: Int): Flow<List<AppLogEntry>> {
        return appLogEntryDao.getRecentLogs(limit)
    }

    suspend fun clearLogs() {
        appLogEntryDao.deleteAllLogs()
    }

    suspend fun clearOldLogs(beforeTimestamp: Long) {
        appLogEntryDao.deleteLogsBefore(beforeTimestamp)
    }
}
