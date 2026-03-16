package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.local.entity.CustomButlerEntity
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerManager
import com.example.aiaccounting.data.repository.ButlerRepository
import com.example.aiaccounting.data.repository.CustomButlerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * 管家市场中的统一列表项（内置管家 or 自定义管家）
 */
sealed class ButlerListItem {
    data class BuiltIn(val butler: Butler) : ButlerListItem()
    data class Custom(val entity: CustomButlerEntity) : ButlerListItem()

    val id: String
        get() = when (this) {
            is BuiltIn -> butler.id
            is Custom -> entity.id
        }

    val name: String
        get() = when (this) {
            is BuiltIn -> butler.name
            is Custom -> entity.name
        }

    val title: String
        get() = when (this) {
            is BuiltIn -> butler.title
            is Custom -> entity.title
        }

    val description: String
        get() = when (this) {
            is BuiltIn -> butler.description
            is Custom -> entity.description
        }

    val isBuiltIn: Boolean
        get() = this is BuiltIn
}

data class ButlerMarketUiState(
    val isLoading: Boolean = true,
    val selectedButlerId: String = "",
    val showDeleteConfirm: String? = null // butler id to confirm delete
)

/**
 * 管家编辑器的 UI 状态
 */
data class ButlerEditorState(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val description: String = "",
    val avatarType: String = "RESOURCE",
    val avatarValue: String = "",
    val userCallName: String = "主人",
    val butlerSelfName: String = "我",
    val communicationStyle: Int = 50,
    val emotionIntensity: Int = 50,
    val professionalism: Int = 50,
    val humor: Int = 50,
    val proactivity: Int = 50,
    val createdAt: Long = 0L,
    val isNewButler: Boolean = true,
    val isSaving: Boolean = false,
    val isEditorReady: Boolean = false
)

sealed class ButlerMarketEvent {
    data class ShowSnackbar(val message: String) : ButlerMarketEvent()
    data class NavigateToEditor(val butlerId: String?) : ButlerMarketEvent()
    object NavigateBack : ButlerMarketEvent()
    data class ShareJson(val json: String) : ButlerMarketEvent()
}

@HiltViewModel
class ButlerMarketViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customButlerRepository: CustomButlerRepository,
    private val butlerRepository: ButlerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ButlerMarketUiState())
    val uiState: StateFlow<ButlerMarketUiState> = _uiState.asStateFlow()

    private val _editorState = MutableStateFlow(ButlerEditorState())
    val editorState: StateFlow<ButlerEditorState> = _editorState.asStateFlow()

    private val _events = MutableSharedFlow<ButlerMarketEvent>()
    val events = _events.asSharedFlow()

    /** 内置管家 + 自定义管家混合列表 */
    val butlerList: StateFlow<List<ButlerListItem>> = combine(
        MutableStateFlow(ButlerManager.getAllButlers()),
        customButlerRepository.observeAll()
    ) { builtInList, customList ->
        val builtInItems = builtInList.map { ButlerListItem.BuiltIn(it) }
        val customItems = customList.map { ButlerListItem.Custom(it) }
        builtInItems + customItems
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        _uiState.update { it.copy(selectedButlerId = butlerRepository.getSelectedButlerId(), isLoading = false) }
    }

    // ─── 市场列表操作 ───

    fun selectButler(id: String) {
        butlerRepository.setSelectedButler(id)
        _uiState.update { it.copy(selectedButlerId = id) }
        viewModelScope.launch {
            _events.emit(ButlerMarketEvent.ShowSnackbar("已切换管家"))
        }
    }

    fun requestDelete(id: String) {
        _uiState.update { it.copy(showDeleteConfirm = id) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(showDeleteConfirm = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.showDeleteConfirm ?: return
        viewModelScope.launch {
            customButlerRepository.softDelete(id, System.currentTimeMillis())
            // 如果删除的正是当前选中的管家，回退到默认
            if (_uiState.value.selectedButlerId == id) {
                selectButler(ButlerManager.BUTLER_TAOTAO)
            }
            _uiState.update { it.copy(showDeleteConfirm = null) }
            _events.emit(ButlerMarketEvent.ShowSnackbar("已删除"))
        }
    }

    fun duplicateButler(id: String) {
        viewModelScope.launch {
            try {
                customButlerRepository.duplicate(id)
                _events.emit(ButlerMarketEvent.ShowSnackbar("已复制"))
            } catch (e: Exception) {
                _events.emit(ButlerMarketEvent.ShowSnackbar("复制失败: ${e.message}"))
            }
        }
    }

    fun exportButler(id: String) {
        viewModelScope.launch {
            try {
                val json = customButlerRepository.exportToJson(id)
                _events.emit(ButlerMarketEvent.ShareJson(json))
            } catch (e: Exception) {
                _events.emit(ButlerMarketEvent.ShowSnackbar("导出失败: ${e.message}"))
            }
        }
    }

    fun importButlerFromJson(json: String) {
        viewModelScope.launch {
            try {
                val avatarDir = File(context.filesDir, "butler_avatars")
                customButlerRepository.importFromJson(
                    json = json,
                    importMode = CustomButlerRepository.ImportMode.NEW_ID,
                    avatarStorageDir = avatarDir
                )
                _events.emit(ButlerMarketEvent.ShowSnackbar("导入成功"))
            } catch (e: Exception) {
                _events.emit(ButlerMarketEvent.ShowSnackbar("导入失败: ${e.message}"))
            }
        }
    }

    // ─── 编辑器操作 ───

    fun initNewEditor() {
        _editorState.value = ButlerEditorState(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            isNewButler = true,
            isEditorReady = true
        )
    }

    fun initEditEditor(butlerId: String) {
        // Fail-safe: mark as edit mode immediately to avoid accidental save creating a blank-id entity.
        _editorState.update {
            it.copy(
                id = butlerId,
                isNewButler = false,
                isEditorReady = false
            )
        }

        viewModelScope.launch {
            val entity = customButlerRepository.getById(butlerId)
            if (entity == null) {
                _events.emit(ButlerMarketEvent.ShowSnackbar("未找到该管家，可能已被删除"))
                _events.emit(ButlerMarketEvent.NavigateBack)
                return@launch
            }

            _editorState.value = ButlerEditorState(
                id = entity.id,
                name = entity.name,
                title = entity.title,
                description = entity.description,
                avatarType = entity.avatarType,
                avatarValue = entity.avatarValue,
                userCallName = entity.userCallName,
                butlerSelfName = entity.butlerSelfName,
                communicationStyle = entity.communicationStyle,
                emotionIntensity = entity.emotionIntensity,
                professionalism = entity.professionalism,
                humor = entity.humor,
                proactivity = entity.proactivity,
                createdAt = entity.createdAt,
                isNewButler = false,
                isSaving = false,
                isEditorReady = true
            )
        }
    }

    fun updateEditorField(updater: (ButlerEditorState) -> ButlerEditorState) {
        _editorState.update(updater)
    }

    /**
     * 将用户从相册选的图片拷贝到 app 私有存储
     */
    fun saveAvatarFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val avatarDir = File(context.filesDir, "butler_avatars")
                if (!avatarDir.exists()) avatarDir.mkdirs()
                val fileName = "butler_${_editorState.value.id}_${System.currentTimeMillis()}.jpg"
                val destFile = File(avatarDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                _editorState.update {
                    it.copy(
                        avatarType = CustomButlerRepository.AVATAR_TYPE_LOCAL_PATH,
                        avatarValue = destFile.absolutePath
                    )
                }
            } catch (e: Exception) {
                _events.emit(ButlerMarketEvent.ShowSnackbar("头像保存失败: ${e.message}"))
            }
        }
    }

    fun saveButler() {
        val state = _editorState.value
        if (!state.isEditorReady) {
            viewModelScope.launch { _events.emit(ButlerMarketEvent.ShowSnackbar("正在加载管家信息，请稍后")) }
            return
        }
        if (state.id.isBlank()) {
            viewModelScope.launch { _events.emit(ButlerMarketEvent.ShowSnackbar("管家 ID 无效，请返回重试")) }
            return
        }
        if (state.name.isBlank()) {
            viewModelScope.launch { _events.emit(ButlerMarketEvent.ShowSnackbar("请输入管家名称")) }
            return
        }
        _editorState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val entity = CustomButlerEntity(
                    id = state.id,
                    name = state.name,
                    title = state.title.ifBlank { "自定义管家" },
                    description = state.description.ifBlank { "用户创建的自定义管家" },
                    avatarType = state.avatarType,
                    avatarValue = state.avatarValue,
                    userCallName = state.userCallName.ifBlank { "主人" },
                    butlerSelfName = state.butlerSelfName.ifBlank { "我" },
                    communicationStyle = state.communicationStyle,
                    emotionIntensity = state.emotionIntensity,
                    professionalism = state.professionalism,
                    humor = state.humor,
                    proactivity = state.proactivity,
                    featureFlagsJson = "{}",
                    priorityJson = "[]",
                    systemPrompt = "", // Prompt engine 将在后续 Step 生成
                    promptVersion = 1,
                    createdAt = if (state.isNewButler) now else state.createdAt,
                    updatedAt = now,
                    isDeleted = false
                )
                customButlerRepository.upsert(entity)
                _events.emit(ButlerMarketEvent.ShowSnackbar(if (state.isNewButler) "创建成功" else "保存成功"))
                _events.emit(ButlerMarketEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(ButlerMarketEvent.ShowSnackbar("保存失败: ${e.message}"))
            } finally {
                _editorState.update { it.copy(isSaving = false) }
            }
        }
    }
}
