package com.example.aiaccounting.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.aiaccounting.data.local.entity.YearlyWealthAnalysis
import kotlinx.coroutines.flow.Flow

@Dao
interface YearlyWealthAnalysisDao {

    @Query("SELECT * FROM yearly_wealth_analysis WHERE year = :year LIMIT 1")
    fun observeByYear(year: Int): Flow<YearlyWealthAnalysis?>

    @Query("SELECT * FROM yearly_wealth_analysis WHERE year = :year LIMIT 1")
    suspend fun getByYear(year: Int): YearlyWealthAnalysis?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: YearlyWealthAnalysis)
}
