package com.example.aiaccounting.data.exporter

import android.content.Context
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV Exporter for exporting transactions to CSV files
 * Compatible with Android (no Apache POI dependency)
 */
class CsvExporter(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private const val DATE_FORMAT_SHORT = "yyyy-MM-dd"
        private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        private val dateFormatShort = SimpleDateFormat(DATE_FORMAT_SHORT, Locale.getDefault())
        private const val CSV_SEPARATOR = ","
    }

    /**
     * Export transactions to CSV file with full details
     */
    fun exportTransactions(
        transactions: List<Transaction>,
        fileName: String = "transactions_${System.currentTimeMillis()}.csv"
    ): Result<File> {
        return try {
            // Create exports directory
            val downloadsDir = File(context.getExternalFilesDir(null), "exports")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            val writer = FileWriter(file)

            // Write BOM for Excel UTF-8 compatibility
            writer.write("\uFEFF")

            // Write header with more details
            val headers = arrayOf(
                "序号", "交易日期", "交易类型", "金额", 
                "账户名称", "分类名称", "备注", "标签"
            )
            writer.write(headers.joinToString(CSV_SEPARATOR))
            writer.write("\n")

            // Get account and category names
            val accounts = runBlocking { accountRepository.getAllAccountsList() }
            val categories = runBlocking { categoryRepository.getAllCategoriesList() }
            
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            // Write data
            transactions.sortedByDescending { it.date }.forEachIndexed { index, transaction ->
                val accountName = accountMap[transaction.accountId]?.name ?: "未知账户"
                val categoryName = categoryMap[transaction.categoryId]?.name ?: "未知分类"
                val typeText = when(transaction.type.name) {
                    "INCOME" -> "收入"
                    "EXPENSE" -> "支出"
                    "TRANSFER" -> "转账"
                    else -> transaction.type.name
                }
                
                val row = arrayOf(
                    (index + 1).toString(),
                    dateFormat.format(Date(transaction.date)),
                    typeText,
                    transaction.amount.toString(),
                    escapeCsv(accountName),
                    escapeCsv(categoryName),
                    escapeCsv(transaction.note),
                    escapeCsv(transaction.tags)
                )
                writer.write(row.joinToString(CSV_SEPARATOR))
                writer.write("\n")
            }

            writer.flush()
            writer.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export monthly summary to CSV with full details
     */
    fun exportMonthlySummary(
        year: Int,
        month: Int,
        transactions: List<Transaction>,
        totalIncome: Double,
        totalExpense: Double,
        fileName: String = "monthly_summary_${year}_${month}.csv"
    ): Result<File> {
        return try {
            // Create exports directory
            val downloadsDir = File(context.getExternalFilesDir(null), "exports")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            val writer = FileWriter(file)

            // Write BOM for Excel UTF-8 compatibility
            writer.write("\uFEFF")

            // Write title
            writer.write("${year}年${month}月 财务汇总报表\n\n")

            // Write summary
            writer.write("统计项目,金额\n")
            writer.write("总收入,${totalIncome}\n")
            writer.write("总支出,${totalExpense}\n")
            writer.write("结余,${totalIncome - totalExpense}\n")
            writer.write("交易笔数,${transactions.size}\n\n")

            // Get account and category names
            val accounts = runBlocking { accountRepository.getAllAccountsList() }
            val categories = runBlocking { categoryRepository.getAllCategoriesList() }
            
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            // Write transactions header with more details
            writer.write("交易明细\n")
            writer.write("序号,日期,类型,金额,账户,分类,备注\n")

            // Write transactions sorted by date
            transactions.sortedByDescending { it.date }.forEachIndexed { index, transaction ->
                val accountName = accountMap[transaction.accountId]?.name ?: "未知账户"
                val categoryName = categoryMap[transaction.categoryId]?.name ?: "未知分类"
                val typeText = when(transaction.type.name) {
                    "INCOME" -> "收入"
                    "EXPENSE" -> "支出"
                    "TRANSFER" -> "转账"
                    else -> transaction.type.name
                }
                
                val row = arrayOf(
                    (index + 1).toString(),
                    dateFormatShort.format(Date(transaction.date)),
                    typeText,
                    transaction.amount.toString(),
                    escapeCsv(accountName),
                    escapeCsv(categoryName),
                    escapeCsv(transaction.note)
                )
                writer.write(row.joinToString(CSV_SEPARATOR))
                writer.write("\n")
            }

            writer.flush()
            writer.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escape CSV special characters
     */
    private fun escapeCsv(value: String): String {
        return when {
            value.contains(CSV_SEPARATOR) || value.contains("\"") || value.contains("\n") -> {
                "\"${value.replace("\"", "\"\"")}\""
            }
            else -> value
        }
    }
}
