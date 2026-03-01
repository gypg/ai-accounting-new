package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI Conversation entity for storing chat history
 */
@Entity(tableName = "ai_conversations")
data class AIConversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: ConversationRole,  // USER, ASSISTANT, SYSTEM
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val transactionId: Long? = null,  // If this message created a transaction
    val isBookmarked: Boolean = false
)

/**
 * Conversation role enum
 */
enum class ConversationRole {
    SYSTEM,
    USER,
    ASSISTANT
}