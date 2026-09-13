package com.example.aiaccounting.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.aiaccounting.R
import com.example.aiaccounting.ai.ButlerPromptEngine
import com.example.aiaccounting.data.local.entity.CustomButlerEntity
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerManager
import com.example.aiaccounting.data.model.ButlerPersonality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管家仓库
 * 管理当前选中的管家和管家偏好设置
 * 支持内置管家 + 自定义管家
 */
@Singleton
class ButlerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customButlerRepository: CustomButlerRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 当前选中的管家ID Flow
    private val _currentButlerId = MutableStateFlow(getSelectedButlerId())
    val currentButlerId: Flow<String> = _currentButlerId.asStateFlow()

    /**
     * 获取当前选中的管家（支持内置管家 + 自定义管家）
     */
    suspend fun getCurrentButler(): Butler {
        val butlerId = getSelectedButlerId()
        ButlerManager.getButlerById(butlerId)?.let { return it }

        val custom = customButlerRepository.getById(butlerId)
        if (custom != null) {
            return custom.toRuntimeButler()
        }

        return ButlerManager.getDefaultButler()
    }

    /**
     * 获取当前选中的自定义管家（仅自定义管家返回非 null）
     */
    suspend fun getCurrentCustomButler(): CustomButlerEntity? {
        val butlerId = getSelectedButlerId()
        // 先检查是否为内置管家
        if (ButlerManager.getButlerById(butlerId) != null) return null
        // 查询自定义管家
        return customButlerRepository.getById(butlerId)
    }

    /**
     * 当前选中的是否为自定义管家
     */
    fun isCustomButlerSelected(): Boolean {
        val butlerId = getSelectedButlerId()
        return ButlerManager.getButlerById(butlerId) == null
    }

    /**
     * 获取当前管家的系统提示词（支持内置 + 自定义）
     */
    suspend fun getCurrentButlerSystemPrompt(): String {
        val butlerId = getSelectedButlerId()
        // 优先查内置管家
        val builtIn = ButlerManager.getButlerById(butlerId)
        if (builtIn != null) return builtIn.systemPrompt
        // 查自定义管家，用 PromptEngine 生成
        val custom = customButlerRepository.getById(butlerId)
        if (custom != null) {
            // 如果已有手动设置的 prompt 且非空，直接用
            if (custom.systemPrompt.isNotBlank()) return custom.systemPrompt
            // 否则用引擎生成
            return withContext(Dispatchers.Default) {
                ButlerPromptEngine.generate(custom)
            }
        }
        // 兜底：返回默认管家
        return ButlerManager.getDefaultButler().systemPrompt
    }

    /**
     * 获取选中的管家ID
     */
    fun getSelectedButlerId(): String {
        return prefs.getString(KEY_SELECTED_BUTLER, ButlerManager.BUTLER_TAOTAO)
            ?: ButlerManager.BUTLER_TAOTAO
    }

    /**
     * 设置选中的管家
     */
    fun setSelectedButler(butlerId: String) {
        prefs.edit().putString(KEY_SELECTED_BUTLER, butlerId).apply()
        _currentButlerId.value = butlerId
    }

    /**
     * 获取所有可用管家列表
     */
    fun getAllButlers(): List<Butler> {
        return ButlerManager.getAllButlers()
    }

    /**
     * 根据 id 获取管家（支持内置管家 + 自定义管家）。
     */
    suspend fun getButlerByIdSuspend(id: String): Butler {
        ButlerManager.getButlerById(id)?.let { return it }

        val custom = customButlerRepository.getById(id)
        if (custom != null) {
            return custom.toRuntimeButler()
        }

        return ButlerManager.getDefaultButler()
    }

    /**
     * 根据ID获取管家（仅内置管家；保留旧 API 以兼容原有调用点）
     */
    fun getButlerById(id: String): Butler? {
        return ButlerManager.getButlerById(id)
    }

    private suspend fun CustomButlerEntity.toRuntimeButler(): Butler {
        val generatedPrompt = if (systemPrompt.isNotBlank()) {
            systemPrompt
        } else {
            withContext(Dispatchers.Default) {
                ButlerPromptEngine.generate(this@toRuntimeButler)
            }
        }

        return Butler(
            id = id,
            name = name,
            title = title.ifBlank { "自定义管家" },
            avatarResId = R.drawable.ic_butler_xiaocainiang,
            description = description.ifBlank { "用户创建的自定义管家" },
            systemPrompt = generatedPrompt,
            personality = ButlerPersonality.PROFESSIONAL,
            specialties = listOf("记账管理", "账户管理", "预算管理", "财务分析")
        )
    }

    companion object {
        private const val PREFS_NAME = "butler_preferences"
        private const val KEY_SELECTED_BUTLER = "selected_butler_id"
    }
}
