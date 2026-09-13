package com.example.aiaccounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 对话会话实体
 * 每个对话完全隔离，独立存储
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val isActive: Boolean = false,  // 当前是否激活
    val summary: String? = null,    // 对话摘要（用于记忆检索）
    val tags: String = ""           // 标签，逗号分隔
)

/**
 * 对话记忆实体
 * 存储AI需要记住的关键信息
 */
@Entity(tableName = "chat_memories")
data class ChatMemory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,          // 所属对话ID
    val category: MemoryCategory,   // 记忆分类
    val content: String,            // 记忆内容
    val importance: Int = 5,        // 重要程度 1-10
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)

/**
 * 记忆分类
 */
enum class MemoryCategory {
    USER_INFO,      // 用户信息（名字、身份等）
    PREFERENCE,     // 偏好设置
    ACCOUNT_INFO,   // 账户信息
    BUDGET_PLAN,    // 预算计划
    SPENDING_HABIT, // 消费习惯
    IMPORTANT_EVENT,// 重要事件
    TOPIC_CONTEXT,  // 话题上下文
    OTHER           // 其他
}

/**
 * 对话消息实体（扩展，支持会话ID）
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,          // 所属对话ID
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUris: String? = null   // 图片URI，JSON数组
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
