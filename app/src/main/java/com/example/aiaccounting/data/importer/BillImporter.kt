package com.example.aiaccounting.data.importer

import android.content.Context
import android.net.Uri
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 账单导入器 - 支持支付宝、微信、CSV格式
 */
@Singleton
class BillImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository
) {

    /**
     * 从URI导入账单
     */
    suspend fun importFromUri(uri: Uri, type: ImportType): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext ImportResult.Error("无法打开文件")

                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()
                reader.close()

                when (type) {
                    ImportType.ALIPAY -> parseAlipayBill(lines)
                    ImportType.WECHAT -> parseWechatBill(lines)
                    ImportType.CSV -> parseCsvBill(lines)
                }
            } catch (e: Exception) {
                ImportResult.Error("导入失败: ${e.message}")
            }
        }
    }

    /**
     * 解析支付宝账单
     */
    private suspend fun parseAlipayBill(lines: List<String>): ImportResult {
        var importedCount = 0
        var skippedCount = 0

        // 找到表头行
        var headerIndex = -1
        for (i in lines.indices) {
            if (lines[i].contains("交易时间") && lines[i].contains("交易类型")) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) {
            return ImportResult.Error("无法识别支付宝账单格式")
        }

        // 解析每一行
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.contains("统计时间")) continue

            try {
                val parts = line.split(",")
                if (parts.size < 10) continue

                val transaction = parseAlipayLine(parts)
                if (transaction != null) {
                    transactionRepository.insertTransaction(transaction)
                    importedCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                skippedCount++
            }
        }

        return ImportResult.Success(importedCount, skippedCount)
    }

    /**
     * 解析支付宝单行数据
     */
    private fun parseAlipayLine(parts: List<String>): Transaction? {
        try {
            // 支付宝CSV格式：交易时间,交易分类,交易对方,对方账号,商品说明,收/支,金额,收/付款方式,交易状态,交易订单号,商家订单号,备注
            val dateStr = parts[0].trim() // 2024-01-15 12:30:45
            val category = parts[1].trim()
            val counterparty = parts[2].trim()
            val description = parts[4].trim()
            val typeStr = parts[5].trim() // 收入/支出/不计收支
            val amountStr = parts[6].trim().replace("¥", "").replace(",", "")
            val status = parts[8].trim()

            // 跳过未完成的交易
            if (status != "交易成功" && status != "已存入零钱") return null

            // 跳过不计收支的
            if (typeStr == "不计收支") return null

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(dateStr)?.time ?: return null

            val amount = amountStr.toDoubleOrNull() ?: return null
            val type = if (typeStr == "收入") TransactionType.INCOME else TransactionType.EXPENSE

            return Transaction(
                amount = amount,
                type = type,
                categoryId = mapAlipayCategory(category),
                accountId = 1, // 默认账户
                date = date,
                note = "$counterparty - $description"
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 解析微信账单
     */
    private suspend fun parseWechatBill(lines: List<String>): ImportResult {
        var importedCount = 0
        var skippedCount = 0

        // 找到表头行
        var headerIndex = -1
        for (i in lines.indices) {
            if (lines[i].contains("交易时间") && lines[i].contains("交易类型")) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) {
            return ImportResult.Error("无法识别微信账单格式")
        }

        // 解析每一行
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.contains("统计时间")) continue

            try {
                val parts = line.split(",")
                if (parts.size < 8) continue

                val transaction = parseWechatLine(parts)
                if (transaction != null) {
                    transactionRepository.insertTransaction(transaction)
                    importedCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                skippedCount++
            }
        }

        return ImportResult.Success(importedCount, skippedCount)
    }

    /**
     * 解析微信单行数据
     */
    private fun parseWechatLine(parts: List<String>): Transaction? {
        try {
            // 微信CSV格式：交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注
            val dateStr = parts[0].trim()
            val typeStr = parts[1].trim()
            val counterparty = parts[2].trim()
            val description = parts[3].trim()
            val incomeExpense = parts[4].trim() // 收入/支出
            val amountStr = parts[5].trim().replace("¥", "").replace(",", "")
            val status = parts[7].trim()

            // 跳过未完成的交易
            if (status != "支付成功" && status != "已存入零钱" && status != "已转账") return null

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateFormat.parse(dateStr)?.time ?: return null

            val amount = amountStr.toDoubleOrNull() ?: return null
            val type = if (incomeExpense == "收入") TransactionType.INCOME else TransactionType.EXPENSE

            return Transaction(
                amount = amount,
                type = type,
                categoryId = mapWechatCategory(typeStr),
                accountId = 1, // 默认账户
                date = date,
                note = "$counterparty - $description"
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 解析通用CSV账单
     */
    private suspend fun parseCsvBill(lines: List<String>): ImportResult {
        var importedCount = 0
        var skippedCount = 0

        if (lines.isEmpty()) {
            return ImportResult.Error("CSV文件为空")
        }

        // 解析表头
        val headerLine = lines[0]
        val headers = headerLine.split(",").map { it.trim().lowercase() }

        // 查找列索引
        val dateIndex = headers.indexOfFirst { it.contains("date") || it.contains("时间") || it.contains("日期") }
        val amountIndex = headers.indexOfFirst { it.contains("amount") || it.contains("金额") }
        val typeIndex = headers.indexOfFirst { it.contains("type") || it.contains("类型") || it.contains("收支") }
        val categoryIndex = headers.indexOfFirst { it.contains("category") || it.contains("分类") }
        val noteIndex = headers.indexOfFirst { it.contains("note") || it.contains("备注") || it.contains("说明") }

        if (dateIndex == -1 || amountIndex == -1) {
            return ImportResult.Error("CSV格式不正确，缺少必要的列（日期、金额）")
        }

        // 解析每一行
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            try {
                val parts = line.split(",")
                if (parts.size <= maxOf(dateIndex, amountIndex)) continue

                val dateStr = parts[dateIndex].trim()
                val amountStr = parts[amountIndex].trim().replace("¥", "").replace(",", "")
                val typeStr = if (typeIndex != -1) parts[typeIndex].trim() else "支出"
                val category = if (categoryIndex != -1) parts[categoryIndex].trim() else "其他"
                val note = if (noteIndex != -1) parts[noteIndex].trim() else ""

                val date = parseDate(dateStr) ?: continue
                val amount = amountStr.toDoubleOrNull() ?: continue
                val type = if (typeStr.contains("收入") || typeStr.contains("income")) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }

                val transaction = Transaction(
                    amount = amount,
                    type = type,
                    categoryId = mapCategory(category),
                    accountId = 1,
                    date = date,
                    note = note
                )

                transactionRepository.insertTransaction(transaction)
                importedCount++
            } catch (e: Exception) {
                skippedCount++
            }
        }

        return ImportResult.Success(importedCount, skippedCount)
    }

    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                return sdf.parse(dateStr)?.time
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * 映射支付宝分类到本地分类ID
     */
    private fun mapAlipayCategory(category: String): Long {
        return when {
            category.contains("餐饮") -> 6
            category.contains("交通") -> 7
            category.contains("购物") -> 8
            category.contains("娱乐") -> 9
            category.contains("医疗") -> 10
            category.contains("教育") -> 11
            category.contains("住房") -> 12
            category.contains("通讯") -> 13
            category.contains("工资") -> 1
            category.contains("奖金") -> 2
            category.contains("投资") -> 3
            else -> 14 // 其他支出
        }
    }

    /**
     * 映射微信分类到本地分类ID
     */
    private fun mapWechatCategory(category: String): Long {
        return when {
            category.contains("商户消费") -> 6
            category.contains("转账") -> 14
            category.contains("红包") -> 9
            category.contains("充值") -> 13
            category.contains("提现") -> 14
            category.contains("退款") -> 1
            else -> 14
        }
    }

    /**
     * 映射通用分类到本地分类ID
     */
    private fun mapCategory(category: String): Long {
        val lower = category.lowercase()
        return when {
            lower.contains("food") || lower.contains("餐饮") || lower.contains("餐") -> 6
            lower.contains("transport") || lower.contains("交通") -> 7
            lower.contains("shopping") || lower.contains("购物") -> 8
            lower.contains("entertainment") || lower.contains("娱乐") -> 9
            lower.contains("medical") || lower.contains("医疗") || lower.contains("医药") -> 10
            lower.contains("education") || lower.contains("教育") -> 11
            lower.contains("housing") || lower.contains("住房") || lower.contains("房租") -> 12
            lower.contains("phone") || lower.contains("通讯") || lower.contains("话费") -> 13
            lower.contains("salary") || lower.contains("工资") -> 1
            lower.contains("bonus") || lower.contains("奖金") -> 2
            lower.contains("investment") || lower.contains("投资") -> 3
            lower.contains("income") || lower.contains("收入") -> 1
            else -> 14
        }
    }



    // ============ 预览方法 ============

    /**
     * 预览支付宝账单（不保存到数据库）
     */
    fun previewAlipayBill(content: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val lines = content.lines()

        // 找到表头行
        var headerIndex = -1
        for (i in lines.indices) {
            if (lines[i].contains("交易时间") && lines[i].contains("交易类型")) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) return transactions

        // 解析每一行
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.contains("统计时间")) continue

            try {
                val parts = line.split(",")
                if (parts.size < 10) continue

                val transaction = parseAlipayLine(parts)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            } catch (e: Exception) {
                // 跳过解析失败的行
            }
        }

        return transactions
    }

    /**
     * 预览微信账单（不保存到数据库）
     */
    fun previewWechatBill(content: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val lines = content.lines()

        // 找到表头行
        var headerIndex = -1
        for (i in lines.indices) {
            if (lines[i].contains("交易时间") && lines[i].contains("交易类型")) {
                headerIndex = i
                break
            }
        }

        if (headerIndex == -1) return transactions

        // 解析每一行
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.contains("统计时间")) continue

            try {
                val parts = line.split(",")
                if (parts.size < 8) continue

                val transaction = parseWechatLine(parts)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            } catch (e: Exception) {
                // 跳过解析失败的行
            }
        }

        return transactions
    }

    /**
     * 预览CSV账单（不保存到数据库）
     */
    fun previewCsvBill(content: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val lines = content.lines()

        if (lines.isEmpty()) return transactions

        // 解析表头
        val headerLine = lines[0]
        val headers = headerLine.split(",").map { it.trim().lowercase() }

        // 查找列索引
        val dateIndex = headers.indexOfFirst { it.contains("date") || it.contains("时间") || it.contains("日期") }
        val amountIndex = headers.indexOfFirst { it.contains("amount") || it.contains("金额") }
        val typeIndex = headers.indexOfFirst { it.contains("type") || it.contains("类型") || it.contains("收支") }
        val categoryIndex = headers.indexOfFirst { it.contains("category") || it.contains("分类") }
        val noteIndex = headers.indexOfFirst { it.contains("note") || it.contains("备注") || it.contains("说明") }

        if (dateIndex == -1 || amountIndex == -1) return transactions

        // 解析每一行
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            try {
                val parts = line.split(",")
                if (parts.size <= maxOf(dateIndex, amountIndex)) continue

                val dateStr = parts[dateIndex].trim()
                val amountStr = parts[amountIndex].trim().replace("¥", "").replace(",", "")
                val typeStr = if (typeIndex != -1) parts[typeIndex].trim() else "支出"
                val category = if (categoryIndex != -1) parts[categoryIndex].trim() else "其他"
                val note = if (noteIndex != -1) parts[noteIndex].trim() else ""

                val date = parseDate(dateStr) ?: continue
                val amount = amountStr.toDoubleOrNull() ?: continue
                val type = if (typeStr.contains("收入") || typeStr.contains("income")) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }

                val transaction = Transaction(
                    amount = amount,
                    type = type,
                    categoryId = mapCategory(category),
                    accountId = 1,
                    date = date,
                    note = note
                )

                transactions.add(transaction)
            } catch (e: Exception) {
                // 跳过解析失败的行
            }
        }

        return transactions
    }
}
