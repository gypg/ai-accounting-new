package com.example.aiaccounting.logging

import com.example.aiaccounting.data.local.entity.AppLogEntry
import com.example.aiaccounting.data.repository.AppLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogLogger @Inject constructor(
    private val repository: AppLogRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun debug(source: String, category: String, message: String, details: String? = null, traceId: String? = null, entityType: String? = null, entityId: String? = null) {
        enqueue("DEBUG", source, category, message, details, traceId, entityType, entityId)
    }

    fun info(source: String, category: String, message: String, details: String? = null, traceId: String? = null, entityType: String? = null, entityId: String? = null) {
        enqueue("INFO", source, category, message, details, traceId, entityType, entityId)
    }

    fun warning(source: String, category: String, message: String, details: String? = null, traceId: String? = null, entityType: String? = null, entityId: String? = null) {
        enqueue("WARNING", source, category, message, details, traceId, entityType, entityId)
    }

    fun error(source: String, category: String, message: String, details: String? = null, traceId: String? = null, entityType: String? = null, entityId: String? = null) {
        enqueue("ERROR", source, category, message, details, traceId, entityType, entityId)
    }

    fun errorBlocking(source: String, category: String, message: String, details: String? = null, traceId: String? = null, entityType: String? = null, entityId: String? = null) {
        persistBlocking(
            buildEntry(
                level = "ERROR",
                source = source,
                category = category,
                message = message,
                details = details,
                traceId = traceId,
                entityType = entityType,
                entityId = entityId
            )
        )
    }

    private fun enqueue(level: String, source: String, category: String, message: String, details: String?, traceId: String?, entityType: String?, entityId: String?) {
        val entry = buildEntry(level, source, category, message, details, traceId, entityType, entityId)
        scope.launch {
            runCatching {
                repository.insertLog(entry)
            }
        }
    }

    private fun persistBlocking(entry: AppLogEntry) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                repository.insertLog(entry)
            }
        }
    }

    private fun buildEntry(level: String, source: String, category: String, message: String, details: String?, traceId: String?, entityType: String?, entityId: String?): AppLogEntry {
        return AppLogEntry(
            id = UUID.randomUUID().toString(),
            level = level,
            source = source,
            category = category,
            message = message,
            details = details,
            traceId = traceId,
            entityType = entityType,
            entityId = entityId
        )
    }
}
