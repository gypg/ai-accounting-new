package com.example.aiaccounting.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "ai_conversations")
data class AIConversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,
    val isUser: Boolean,
    val timestamp: Date = Date(),
    val relatedTransactionId: Long? = null
)
