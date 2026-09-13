package com.example.aiaccounting.data.repository

import com.example.aiaccounting.data.local.dao.ChatMemoryDao
import com.example.aiaccounting.data.local.dao.ChatMessageDao
import com.example.aiaccounting.data.local.dao.ChatSessionDao
import com.example.aiaccounting.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 对话会话仓库
 * 管理多对话记录和AI记忆
 */
@Singleton
class ChatSessionRepository @Inject constructor(
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao,
    private val memoryDao: ChatMemoryDao
) {
    /**
     * 获取所有对话列表
     */
    fun getAllSessions(): Flow<List<ChatSession>> = sessionDao.getAllSessions()
    
    /**
     * 获取当前激活的对话
     */
    suspend fun getActiveSession(): ChatSession? = sessionDao.getActiveSession()
    
    /**
     * 创建新对话
     */
    suspend fun createSession(title: String = "新对话"): ChatSession {
        // 清除当前激活的对话
        sessionDao.clearActiveSession()
        
        // 创建新对话
        val session = ChatSession(
            title = title,
            isActive = true
        )
        sessionDao.insertSession(session)
        return session
    }
    
    /**
     * 切换对话
     */
    suspend fun switchSession(sessionId: String) {
        sessionDao.clearActiveSession()
        sessionDao.setActiveSession(sessionId)
    }
    
    /**
     * 删除对话
     */
    suspend fun deleteSession(sessionId: String) {
        // 删除对话及其所有相关数据
        sessionDao.deleteSessionById(sessionId)
        messageDao.deleteMessagesBySession(sessionId)
        memoryDao.deleteMemoriesBySession(sessionId)
    }
    
    /**
     * 更新对话标题
     */
    suspend fun updateSessionTitle(sessionId: String, title: String) {
        val session = sessionDao.getSessionById(sessionId)
        session?.let {
            val updated = it.copy(title = title, updatedAt = System.currentTimeMillis())
            sessionDao.updateSession(updated)
        }
    }
    
    /**
     * 添加消息到当前对话
     */
    suspend fun addMessage(sessionId: String, role: MessageRole, content: String, imageUris: List<String>? = null) {
        val message = ChatMessageEntity(
            sessionId = sessionId,
            role = role,
            content = content,
            imageUris = imageUris?.joinToString(",")
        )
        messageDao.insertMessage(message)
        sessionDao.incrementMessageCount(sessionId)
    }
    
    /**
     * 获取对话的消息流
     */
    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> = 
        messageDao.getMessagesBySession(sessionId)
    
    /**
     * 获取最近N条消息（用于AI上下文）
     */
    suspend fun getRecentMessages(sessionId: String, limit: Int = 5): List<ChatMessageEntity> {
        return messageDao.getRecentMessages(sessionId, limit).reversed()
    }
    
    /**
     * 搜索消息
     */
    suspend fun searchMessages(sessionId: String, keyword: String): List<ChatMessageEntity> {
        return messageDao.searchMessages(sessionId, keyword)
    }
    
    // ==================== 记忆管理 ====================
    
    /**
     * 添加记忆
     */
    suspend fun addMemory(
        sessionId: String,
        category: MemoryCategory,
        content: String,
        importance: Int = 5
    ) {
        val memory = ChatMemory(
            sessionId = sessionId,
            category = category,
            content = content,
            importance = importance
        )
        memoryDao.insertMemory(memory)
    }
    
    /**
     * 获取对话的所有记忆
     */
    fun getMemories(sessionId: String): Flow<List<ChatMemory>> = 
        memoryDao.getMemoriesBySession(sessionId)
    
    /**
     * 获取指定分类的记忆
     */
    suspend fun getMemoriesByCategory(sessionId: String, category: MemoryCategory): List<ChatMemory> {
        return memoryDao.getMemoriesByCategory(sessionId, category)
    }
    
    /**
     * 搜索记忆
     */
    suspend fun searchMemories(sessionId: String, keyword: String): List<ChatMemory> {
        return memoryDao.searchMemories(sessionId, keyword)
    }
    
    /**
     * 获取最近访问的记忆
     */
    suspend fun getRecentMemories(sessionId: String, limit: Int = 10): List<ChatMemory> {
        return memoryDao.getRecentMemories(sessionId, limit)
    }
    
    /**
     * 更新记忆访问时间
     */
    suspend fun touchMemory(memoryId: String) {
        memoryDao.updateLastAccessed(memoryId)
    }
    
    /**
     * 删除记忆
     */
    suspend fun deleteMemory(memory: ChatMemory) {
        memoryDao.deleteMemory(memory)
    }
    
    /**
     * 智能检索记忆
     * 根据关键词先粗略匹配，再精确排序
     */
    suspend fun smartSearchMemories(sessionId: String, query: String): List<ChatMemory> {
        // 1. 先进行粗略搜索
        val candidates = memoryDao.searchMemories(sessionId, query)
        
        // 2. 按相关性和重要性排序
        return candidates.sortedWith(compareByDescending<ChatMemory> { memory ->
            // 计算相关性得分
            var score = memory.importance * 10
            
            // 如果内容包含完整关键词，加分
            if (memory.content.contains(query, ignoreCase = true)) {
                score += 50
            }
            
            // 根据分类优先级加分
            score += when (memory.category) {
                MemoryCategory.USER_INFO -> 30
                MemoryCategory.ACCOUNT_INFO -> 25
                MemoryCategory.BUDGET_PLAN -> 20
                MemoryCategory.SPENDING_HABIT -> 15
                MemoryCategory.IMPORTANT_EVENT -> 10
                else -> 5
            }
            
            score
        }.thenByDescending { it.lastAccessedAt })
    }
    
    /**
     * 构建AI提示词（包含记忆和上下文）
     */
    suspend fun buildAIPrompt(sessionId: String, userMessage: String): String {
        val sb = StringBuilder()
        
        // 1. AI人设
        sb.appendLine("你是小财娘，活泼可爱的管家婆AI助手！")
        sb.appendLine()
        
        // 2. 检索相关记忆
        val relevantMemories = smartSearchMemories(sessionId, userMessage).take(5)
        if (relevantMemories.isNotEmpty()) {
            sb.appendLine("【重要记忆】")
            relevantMemories.forEach { memory ->
                sb.appendLine("- ${memory.category}: ${memory.content}")
                // 更新访问时间
                touchMemory(memory.id)
            }
            sb.appendLine()
        }
        
        // 3. 最近5条对话上下文
        val recentMessages = getRecentMessages(sessionId, 5)
        if (recentMessages.isNotEmpty()) {
            sb.appendLine("【最近对话】")
            recentMessages.forEach { msg ->
                val role = when (msg.role) {
                    MessageRole.USER -> "用户"
                    MessageRole.ASSISTANT -> "小财娘"
                    MessageRole.SYSTEM -> "系统"
                }
                sb.appendLine("$role: ${msg.content}")
            }
            sb.appendLine()
        }
        
        // 4. 用户当前消息
        sb.appendLine("【当前消息】")
        sb.appendLine("用户: $userMessage")
        sb.appendLine()
        
        // 5. 提示
        sb.appendLine("请根据以上信息回复用户，保持活泼可爱的语气~")
        
        return sb.toString()
    }
    
    /**
     * 从AI回复中提取并保存记忆
     */
    suspend fun extractAndSaveMemories(sessionId: String, aiResponse: String) {
        // 简单的记忆提取逻辑（可以后续优化）
        // 提取关键信息如：用户偏好、重要事件等
        
        // 示例：如果回复中包含"记住"等关键词
        if (aiResponse.contains("记住") || aiResponse.contains("知道了")) {
            // 提取关键句子作为记忆
            val sentences = aiResponse.split("。", "！", "？")
            sentences.forEach { sentence ->
                if (sentence.length > 10 && sentence.length < 200) {
                    // 判断记忆类型
                    val category = when {
                        sentence.contains("预算") || sentence.contains("计划") -> MemoryCategory.BUDGET_PLAN
                        sentence.contains("喜欢") || sentence.contains("偏好") -> MemoryCategory.PREFERENCE
                        sentence.contains("账户") || sentence.contains("钱包") -> MemoryCategory.ACCOUNT_INFO
                        sentence.contains("习惯") || sentence.contains("经常") -> MemoryCategory.SPENDING_HABIT
                        else -> MemoryCategory.TOPIC_CONTEXT
                    }
                    
                    addMemory(sessionId, category, sentence.trim(), importance = 7)
                }
            }
        }
    }
}
