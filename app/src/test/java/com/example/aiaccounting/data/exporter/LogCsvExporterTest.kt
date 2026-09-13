package com.example.aiaccounting.data.exporter

import android.content.Context
import com.example.aiaccounting.data.local.entity.AIOperationTrace
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LogCsvExporterTest {

    @Test
    fun `exportTraces writes csv with expected content`() {
        val baseDir = Files.createTempDirectory("log_csv_export").toFile()
        try {
            val context = mockContext(baseDir)
            val exporter = LogCsvExporter(context)
            val traces = listOf(
                AIOperationTrace(
                    id = "1",
                    traceId = "trace-1",
                    timestamp = 1_743_000_000_000L,
                    sourceType = "AI_REMOTE",
                    actionType = "ADD_TRANSACTION",
                    entityType = "transaction",
                    summary = "AI 添加交易",
                    details = "amount=25",
                    success = true
                )
            )

            val result = exporter.exportTraces(traces, "logs_test.csv")

            assertTrue(result.isSuccess)
            val file = result.getOrNull()!!
            assertTrue(file.exists())
            val content = file.readText(Charsets.UTF_8)
            assertTrue(content.contains("时间,级别,来源,动作"))
            assertTrue(content.contains("AI_REMOTE"))
            assertTrue(content.contains("ADD_TRANSACTION"))
            assertTrue(content.contains("AI 添加交易"))
        } finally {
            baseDir.deleteRecursively()
        }
    }

    private fun mockContext(baseDir: java.io.File): Context {
        return mockk {
            every { getExternalFilesDir(null) } returns baseDir
        }
    }
}
