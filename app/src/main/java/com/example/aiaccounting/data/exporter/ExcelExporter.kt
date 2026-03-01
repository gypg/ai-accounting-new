package com.example.aiaccounting.data.exporter

import android.content.Context
import com.example.aiaccounting.data.local.entity.Transaction
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Excel Exporter for exporting transactions to Excel files
 */
class ExcelExporter(private val context: Context) {

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    }

    /**
     * Export transactions to Excel file
     */
    fun exportTransactions(
        transactions: List<Transaction>,
        fileName: String = "transactions_${System.currentTimeMillis()}.xlsx",
        password: String? = null
    ): Result<File> {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("交易记录")

            // Create styles
            val headerStyle = createHeaderStyle(workbook)
            val dateStyle = createDateStyle(workbook)
            val amountStyle = createAmountStyle(workbook)
            val incomeStyle = createIncomeStyle(workbook)
            val expenseStyle = createExpenseStyle(workbook)

            // Create header row
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("ID", "日期", "类型", "金额", "账户ID", "分类ID", "备注", "标签")
            
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // Fill data
            transactions.forEachIndexed { index, transaction ->
                val row = sheet.createRow(index + 1)

                // ID
                row.createCell(0).setCellValue(transaction.id)

                // Date
                val dateCell = row.createCell(1)
                dateCell.setCellValue(dateFormat.format(Date(transaction.date)))
                dateCell.cellStyle = dateStyle

                // Type
                row.createCell(2).setCellValue(transaction.type.name)

                // Amount
                val amountCell = row.createCell(3)
                amountCell.setCellValue(transaction.amount)
                amountCell.cellStyle = when (transaction.type) {
                    com.example.aiaccounting.data.local.entity.TransactionType.INCOME -> incomeStyle
                    com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE -> expenseStyle
                    else -> amountStyle
                }

                // Account ID
                row.createCell(4).setCellValue(transaction.accountId)

                // Category ID
                row.createCell(5).setCellValue(transaction.categoryId)

                // Note
                row.createCell(6).setCellValue(transaction.note)

                // Tags
                row.createCell(7).setCellValue(transaction.tags)
            }

            // Auto-size columns
            for (i in headers.indices) {
                sheet.autoSizeColumn(i)
            }

            // Save file
            val downloadsDir = File(context.getExternalFilesDir(null), "exports")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            
            if (password != null && password.isNotEmpty()) {
                // Note: Password protection requires additional POI libraries
                // For now, we'll save without password
                workbook.write(fileOutputStream)
            } else {
                workbook.write(fileOutputStream)
            }
            
            fileOutputStream.close()
            workbook.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export monthly summary to Excel
     */
    fun exportMonthlySummary(
        year: Int,
        month: Int,
        transactions: List<Transaction>,
        totalIncome: Double,
        totalExpense: Double,
        fileName: String = "monthly_summary_${year}_${month}.xlsx"
    ): Result<File> {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("月度汇总")

            // Create styles
            val headerStyle = createHeaderStyle(workbook)
            val titleStyle = createTitleStyle(workbook)
            val amountStyle = createAmountStyle(workbook)
            val incomeStyle = createIncomeStyle(workbook)
            val expenseStyle = createExpenseStyle(workbook)

            var rowIndex = 0

            // Title
            val titleRow = sheet.createRow(rowIndex++)
            val titleCell = titleRow.createCell(0)
            titleCell.setCellValue("${year}年${month}月 财务汇总")
            titleCell.cellStyle = titleStyle
            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 3))

            // Summary
            rowIndex++
            val summaryHeaderRow = sheet.createRow(rowIndex++)
            val summaryHeaders = arrayOf("收入", "支出", "结余")
            summaryHeaders.forEachIndexed { index, header ->
                val cell = summaryHeaderRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            val summaryRow = sheet.createRow(rowIndex++)
            val incomeCell = summaryRow.createCell(0)
            incomeCell.setCellValue(totalIncome)
            incomeCell.cellStyle = incomeStyle

            val expenseCell = summaryRow.createCell(1)
            expenseCell.setCellValue(totalExpense)
            expenseCell.cellStyle = expenseStyle

            val balanceCell = summaryRow.createCell(2)
            balanceCell.setCellValue(totalIncome - totalExpense)
            balanceCell.cellStyle = if (totalIncome >= totalExpense) incomeStyle else expenseStyle

            // Transactions
            rowIndex += 2
            val transHeaderRow = sheet.createRow(rowIndex++)
            val transHeaders = arrayOf("日期", "类型", "金额", "备注")
            transHeaders.forEachIndexed { index, header ->
                val cell = transHeaderRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            transactions.forEach { transaction ->
                val row = sheet.createRow(rowIndex++)

                val dateCell = row.createCell(0)
                dateCell.setCellValue(dateFormat.format(Date(transaction.date)))
                dateCell.cellStyle = createDateStyle(workbook)

                row.createCell(1).setCellValue(transaction.type.name)

                val amountCell = row.createCell(2)
                amountCell.setCellValue(transaction.amount)
                amountCell.cellStyle = when (transaction.type) {
                    com.example.aiaccounting.data.local.entity.TransactionType.INCOME -> incomeStyle
                    com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE -> expenseStyle
                    else -> amountStyle
                }

                row.createCell(3).setCellValue(transaction.note)
            }

            // Auto-size columns
            for (i in 0..3) {
                sheet.autoSizeColumn(i)
            }

            // Save file
            val downloadsDir = File(context.getExternalFilesDir(null), "exports")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            workbook.write(fileOutputStream)
            fileOutputStream.close()
            workbook.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create header style
     */
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        font.bold = true
        font.color = IndexedColors.WHITE.index
        
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.BLUE_GREY.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderTop(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        
        return style
    }

    /**
     * Create title style
     */
    private fun createTitleStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        font.bold = true
        font.fontHeightInPoints = 16
        
        style.setFont(font)
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        
        return style
    }

    /**
     * Create date style
     */
    private fun createDateStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.dataFormat = workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss")
        return style
    }

    /**
     * Create amount style
     */
    private fun createAmountStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
        style.alignment = HorizontalAlignment.RIGHT
        return style
    }

    /**
     * Create income style
     */
    private fun createIncomeStyle(workbook: Workbook): CellStyle {
        val style = createAmountStyle(workbook)
        val font = workbook.createFont()
        font.color = IndexedColors.GREEN.index
        style.setFont(font)
        return style
    }

    /**
     * Create expense style
     */
    private fun createExpenseStyle(workbook: Workbook): CellStyle {
        val style = createAmountStyle(workbook)
        val font = workbook.createFont()
        font.color = IndexedColors.RED.index
        style.setFont(font)
        return style
    }
}