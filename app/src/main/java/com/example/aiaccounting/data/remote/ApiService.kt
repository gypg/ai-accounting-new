package com.example.aiaccounting.data.remote

import com.example.aiaccounting.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/accounts")
    suspend fun getAccounts(): Response<List<AccountDto>>
    
    @GET("api/accounts/{id}")
    suspend fun getAccountById(@Path("id") id: Long): Response<AccountDto>
    
    @POST("api/accounts")
    suspend fun createAccount(@Body account: AccountDto): Response<AccountDto>
    
    @PUT("api/accounts/{id}")
    suspend fun updateAccount(@Path("id") id: Long, @Body account: AccountDto): Response<AccountDto>
    
    @DELETE("api/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: Long): Response<Unit>
    
    @GET("api/categories")
    suspend fun getCategories(): Response<List<CategoryDto>>
    
    @POST("api/categories")
    suspend fun createCategory(@Body category: CategoryDto): Response<CategoryDto>
    
    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("accountId") accountId: Long?,
        @Query("categoryId") categoryId: Long?
    ): Response<List<TransactionDto>>
    
    @POST("api/transactions")
    suspend fun createTransaction(@Body transaction: TransactionDto): Response<TransactionDto>
    
    @PUT("api/transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: Long, @Body transaction: TransactionDto): Response<TransactionDto>
    
    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<Unit>
    
    @GET("api/reports/monthly")
    suspend fun getMonthlyReport(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<MonthlyReportDto>
    
    @GET("api/reports/export")
    suspend fun exportExcel(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ExportResponseDto>
}
