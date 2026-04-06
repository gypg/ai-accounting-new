package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["level"]),
        Index(value = ["source"]),
        Index(value = ["category"]),
        Index(value = ["traceId"]),
        Index(value = ["entityType", "entityId"])
    ]
)
data class AppLogEntry(
    @PrimaryKey
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val source: String,
    val category: String,
    val message: String,
    val details: String? = null,
    val traceId: String? = null,
    val entityType: String? = null,
    val entityId: String? = null,
    val sessionId: String? = null
)
