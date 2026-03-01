package com.example.aiaccounting.data.remote.dto

data class AccountDto(
    val id: Long?,
    val name: String,
    val type: String,
    val balance: Double,
    val icon: String?,
    val color: String?,
    val isAsset: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

data class CategoryDto(
    val id: Long?,
    val name: String,
    val parentId: Long?,
    val type: String,
    val icon: String?,
    val color: String?,
    val sortOrder: Int?,
    val createdAt: String?
)

data class TransactionDto(
    val id: Long?,
    val accountId: Long,
    val categoryId: Long?,
    val type: String,
    val amount: Double,
    val date: String,
    val remark: String?,
    val toAccountId: Long?,
    val excludeFromTotal: Boolean?,
    val createdAt: String?,
    val updatedAt: String?
)

data class MonthlyReportDto(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val categoryBreakdown: List<CategoryBreakdownDto>
)

data class CategoryBreakdownDto(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Double
)

data class ExportResponseDto(
    val fileUrl: String,
    val fileName: String
)
