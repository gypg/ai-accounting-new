package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "yearly_wealth_analysis",
    indices = [Index(value = ["year"], unique = true)]
)
data class YearlyWealthAnalysis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val year: Int,
    val analysisText: String,
    val model: String,
    val traceId: String? = null,
    val snapshotJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
