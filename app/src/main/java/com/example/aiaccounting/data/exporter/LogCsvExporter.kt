package com.example.aiaccounting.data.exporter

import android.content.Context
import com.example.aiaccounting.data.local.entity.AppLogEntry
import com.example.aiaccounting.data.local.entity.AIOperationTrace
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogCsvExporter(
    private val context: Context
) {

    companion object {
        private const val CSV_SEPARATOR = ","
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    }

    fun exportLogs(
        logs: List<AppLogEntry>,
        fileName: String = "logs_${System.currentTimeMillis()}.csv"
    ): Result<File> {
        return try {
            val exportsDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportsDir.exists()) {
                exportsDir.mkdirs()
            }

            val file = File(exportsDir, fileName)
            FileWriter(file).use { writer ->
                writer.write("\uFEFF")
                writer.write(
                    listOf(
                        "时间",
                        "级别",
                        "来源",
                        "分类",
                        "消息",
                        "详情",
                        "traceId",
                        "实体类型",
                        "实体ID"
                    ).joinToString(CSV_SEPARATOR)
                )
                writer.write("\n")

                logs.forEach { log ->
                    val row = listOf(
                        dateFormat.format(Date(log.timestamp)),
                        escapeCsv(log.level),
                        escapeCsv(log.source),
                        escapeCsv(log.category),
                        escapeCsv(log.message),
                        escapeCsv(log.details.orEmpty()),
                        escapeCsv(log.traceId.orEmpty()),
                        escapeCsv(log.entityType.orEmpty()),
                        escapeCsv(log.entityId.orEmpty())
                    )
                    writer.write(row.joinToString(CSV_SEPARATOR))
                    writer.write("\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportTraces(
        traces: List<AIOperationTrace>,
        fileName: String = "logs_${System.currentTimeMillis()}.csv"
    ): Result<File> {
        return try {
            val exportsDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportsDir.exists()) {
                exportsDir.mkdirs()
            }

            val file = File(exportsDir, fileName)
            FileWriter(file).use { writer ->
                writer.write("\uFEFF")
                writer.write(
                    listOf(
                        "时间",
                        "级别",
                        "来源",
                        "动作",
                        "实体类型",
                        "实体ID",
                        "关联交易ID",
                        "摘要",
                        "详情",
                        "错误信息",
                        "traceId"
                    ).joinToString(CSV_SEPARATOR)
                )
                writer.write("\n")

                traces.forEach { trace ->
                    val level = if (trace.success) "INFO" else "ERROR"
                    val row = listOf(
                        dateFormat.format(Date(trace.timestamp)),
                        level,
                        escapeCsv(trace.sourceType),
                        escapeCsv(trace.actionType),
                        escapeCsv(trace.entityType),
                        escapeCsv(trace.entityId.orEmpty()),
                        trace.relatedTransactionId?.toString().orEmpty(),
                        escapeCsv(trace.summary),
                        escapeCsv(trace.details.orEmpty()),
                        escapeCsv(trace.errorMessage.orEmpty()),
                        escapeCsv(trace.traceId)
                    )
                    writer.write(row.joinToString(CSV_SEPARATOR))
                    writer.write("\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(CSV_SEPARATOR) || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
