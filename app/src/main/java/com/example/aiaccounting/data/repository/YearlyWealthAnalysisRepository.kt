package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.YearlyWealthAnalysisDao
import com.example.aiaccounting.data.local.entity.YearlyWealthAnalysis
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YearlyWealthAnalysisRepository @Inject constructor(
    private val dao: YearlyWealthAnalysisDao
) {
    fun observeByYear(year: Int): Flow<YearlyWealthAnalysis?> = dao.observeByYear(year)

    suspend fun getByYear(year: Int): YearlyWealthAnalysis? = dao.getByYear(year)

    suspend fun upsert(record: YearlyWealthAnalysis) {
        dao.upsert(record.copy(updatedAt = System.currentTimeMillis()))
    }
}
