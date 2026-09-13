package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.logging.AppLogLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogCleanupCoordinator @Inject constructor(
    private val appStateManager: AppStateManager,
    private val appLogLogger: AppLogLogger,
    private val appLogRepository: AppLogRepository,
    private val aiPermissionLogRepository: AIPermissionLogRepository,
    private val aiOperationTraceRepository: AIOperationTraceRepository
) {
    suspend fun runIfDue(now: Long = System.currentTimeMillis()) {
        val prefs = appStateManager.getLogAutoClearPreferences()
        if (!prefs.enabled) return

        val intervalMillis = prefs.intervalHours * 60L * 60L * 1000L
        if (prefs.lastRunTimestamp > 0 && now - prefs.lastRunTimestamp < intervalMillis) {
            return
        }

        val cutoff = now - intervalMillis
        var allSucceeded = true

        allSucceeded = clearLogsSafely(
            repositoryName = "platform_logs",
            cutoff = cutoff,
            clearAction = { appLogRepository.clearOldLogs(cutoff) }
        ) && allSucceeded
        allSucceeded = clearLogsSafely(
            repositoryName = "ai_permission_logs",
            cutoff = cutoff,
            clearAction = { aiPermissionLogRepository.clearOldLogs(cutoff) }
        ) && allSucceeded
        allSucceeded = clearLogsSafely(
            repositoryName = "ai_operation_traces",
            cutoff = cutoff,
            clearAction = { aiOperationTraceRepository.clearOldTraces(cutoff) }
        ) && allSucceeded

        if (allSucceeded) {
            appStateManager.setLogAutoClearLastRun(now)
        }
    }

    private suspend fun clearLogsSafely(
        repositoryName: String,
        cutoff: Long,
        clearAction: suspend () -> Unit
    ): Boolean {
        return try {
            clearAction()
            true
        } catch (throwable: Throwable) {
            if (throwable is kotlinx.coroutines.CancellationException) throw throwable
            appLogLogger.error(
                source = "APP",
                category = "log_cleanup",
                message = "日志自动清理失败",
                details = "repository=$repositoryName,cutoff=$cutoff,error=${throwable.message}"
            )
            false
        }
    }
}
