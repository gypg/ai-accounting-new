package com.example.aiaccounting.data.exporter

import android.content.Context
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import io.mockk.every
import io.mockk.mockk
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ExcelExporterTest {

    @Test
    fun `exportTransactions writes workbook with expected headers and rows`() {
        val baseDir = Files.createTempDirectory("excel_export_transactions").toFile()
        try {
            val context = mockContext(baseDir)
            val exporter = ExcelExporter(context)
            val timestamp = 1711111111000L
            val transactions = listOf(
                Transaction(
                    id = 1L,
                    accountId = 10L,
                    categoryId = 100L,
                    type = TransactionType.INCOME,
                    amount = 1234.56,
                    date = timestamp,
                    note = "工资",
                    tags = "收入,固定"
                ),
                Transaction(
                    id = 2L,
                    accountId = 20L,
                    categoryId = 200L,
                    type = TransactionType.EXPENSE,
                    amount = 88.8,
                    date = timestamp + 1000,
                    note = "午餐",
                    tags = "餐饮"
                )
            )

            val result = exporter.exportTransactions(
                transactions = transactions,
                fileName = "transactions_test.xlsx"
            )

            assertTrue(result.isSuccess)
            val file = result.getOrNull()
            assertNotNull(file)
            assertTrue(file!!.exists())

            XSSFWorkbook(file.inputStream()).use { workbook ->
                val sheet = workbook.getSheet("交易记录")
                assertNotNull(sheet)

                val headerRow = sheet.getRow(0)
                val headers = listOf("ID", "日期", "类型", "金额", "账户ID", "分类ID", "备注", "标签")
                headers.forEachIndexed { index, expected ->
                    assertEquals(expected, headerRow.getCell(index).stringCellValue)
                }

                val firstRow = sheet.getRow(1)
                assertEquals(1.0, firstRow.getCell(0).numericCellValue, 0.0)
                assertTrue(firstRow.getCell(1).stringCellValue.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
                assertEquals("INCOME", firstRow.getCell(2).stringCellValue)
                assertEquals(1234.56, firstRow.getCell(3).numericCellValue, 0.001)
                assertEquals(10.0, firstRow.getCell(4).numericCellValue, 0.0)
                assertEquals(100.0, firstRow.getCell(5).numericCellValue, 0.0)
                assertEquals("工资", firstRow.getCell(6).stringCellValue)
                assertEquals("收入,固定", firstRow.getCell(7).stringCellValue)

                val secondRow = sheet.getRow(2)
                assertEquals("EXPENSE", secondRow.getCell(2).stringCellValue)
                assertEquals(88.8, secondRow.getCell(3).numericCellValue, 0.001)
                assertEquals("午餐", secondRow.getCell(6).stringCellValue)
                assertEquals("餐饮", secondRow.getCell(7).stringCellValue)
            }
        } finally {
            baseDir.deleteRecursively()
        }
    }

    @Test
    fun `exportTransactions with empty list keeps header row only`() {
        val baseDir = Files.createTempDirectory("excel_export_empty").toFile()
        try {
            val context = mockContext(baseDir)
            val exporter = ExcelExporter(context)

            val result = exporter.exportTransactions(
                transactions = emptyList(),
                fileName = "transactions_empty.xlsx"
            )

            assertTrue(result.isSuccess)
            val file = result.getOrNull()
            assertNotNull(file)
            assertTrue(file!!.exists())

            XSSFWorkbook(file.inputStream()).use { workbook ->
                val sheet = workbook.getSheet("交易记录")
                assertNotNull(sheet)
                assertEquals(0, sheet.lastRowNum)
                assertEquals("ID", sheet.getRow(0).getCell(0).stringCellValue)
                assertEquals("标签", sheet.getRow(0).getCell(7).stringCellValue)
            }
        } finally {
            baseDir.deleteRecursively()
        }
    }

    @Test
    fun `exportMonthlySummary writes summary title merged region and totals`() {
        val baseDir = Files.createTempDirectory("excel_export_summary").toFile()
        try {
            val context = mockContext(baseDir)
            val exporter = ExcelExporter(context)
            val transactions = listOf(
                Transaction(
                    id = 1L,
                    accountId = 1L,
                    categoryId = 1L,
                    type = TransactionType.INCOME,
                    amount = 5000.0,
                    date = 1711111111000L,
                    note = "工资"
                ),
                Transaction(
                    id = 2L,
                    accountId = 1L,
                    categoryId = 2L,
                    type = TransactionType.EXPENSE,
                    amount = 1200.0,
                    date = 1711112222000L,
                    note = "房租"
                )
            )

            val result = exporter.exportMonthlySummary(
                year = 2026,
                month = 3,
                transactions = transactions,
                totalIncome = 5000.0,
                totalExpense = 1200.0,
                fileName = "monthly_summary_test.xlsx"
            )

            assertTrue(result.isSuccess)
            val file = result.getOrNull()
            assertNotNull(file)
            assertTrue(file!!.exists())

            XSSFWorkbook(file.inputStream()).use { workbook ->
                val sheet = workbook.getSheet("月度汇总")
                assertNotNull(sheet)
                assertEquals("2026年3月 财务汇总", sheet.getRow(0).getCell(0).stringCellValue)
                assertEquals(1, sheet.numMergedRegions)
                assertEquals(0, sheet.getMergedRegion(0).firstRow)
                assertEquals(0, sheet.getMergedRegion(0).lastRow)
                assertEquals(0, sheet.getMergedRegion(0).firstColumn)
                assertEquals(3, sheet.getMergedRegion(0).lastColumn)

                val summaryHeaderRow = sheet.getRow(2)
                assertEquals("收入", summaryHeaderRow.getCell(0).stringCellValue)
                assertEquals("支出", summaryHeaderRow.getCell(1).stringCellValue)
                assertEquals("结余", summaryHeaderRow.getCell(2).stringCellValue)

                val summaryRow = sheet.getRow(3)
                assertEquals(5000.0, summaryRow.getCell(0).numericCellValue, 0.001)
                assertEquals(1200.0, summaryRow.getCell(1).numericCellValue, 0.001)
                assertEquals(3800.0, summaryRow.getCell(2).numericCellValue, 0.001)

                val transactionHeaderRow = sheet.getRow(6)
                assertEquals("日期", transactionHeaderRow.getCell(0).stringCellValue)
                assertEquals("类型", transactionHeaderRow.getCell(1).stringCellValue)
                assertEquals("金额", transactionHeaderRow.getCell(2).stringCellValue)
                assertEquals("备注", transactionHeaderRow.getCell(3).stringCellValue)

                val firstTransactionRow = sheet.getRow(7)
                assertTrue(firstTransactionRow.getCell(0).stringCellValue.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
                assertEquals("INCOME", firstTransactionRow.getCell(1).stringCellValue)
                assertEquals(5000.0, firstTransactionRow.getCell(2).numericCellValue, 0.001)
                assertEquals("工资", firstTransactionRow.getCell(3).stringCellValue)
            }
        } finally {
            baseDir.deleteRecursively()
        }
    }

    private fun mockContext(baseDir: File): Context {
        return mockk {
            every { getExternalFilesDir(null) } returns baseDir
        }
    }
}
