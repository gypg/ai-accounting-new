package com.example.aiaccounting.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.service.AIVoiceRecognitionService
import com.example.aiaccounting.data.service.SpeechRecognitionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 语音输入ViewModel
 * 支持系统语音识别和AI语音识别两种模式
 */
@HiltViewModel
class VoiceInputViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speechRecognitionService: SpeechRecognitionService,
    private val aiVoiceRecognitionService: AIVoiceRecognitionService,
    private val aiConfigRepository: AIConfigRepository
) : ViewModel() {

    // 系统语音识别状态
    val systemRecognitionState: StateFlow<SpeechRecognitionService.RecognitionState> =
        speechRecognitionService.recognitionState

    val systemRecognizedText: StateFlow<String> =
        speechRecognitionService.recognizedText

    // AI语音识别状态
    val aiRecognitionState: StateFlow<AIVoiceRecognitionService.RecognitionState> =
        aiVoiceRecognitionService.recognitionState

    val aiRecognizedText: StateFlow<String> =
        aiVoiceRecognitionService.recognizedText

    val recordingDuration: StateFlow<Int> =
        aiVoiceRecognitionService.recordingDuration

    // 当前使用的识别模式
    private val _currentMode = MutableStateFlow(RecognitionMode.AI)
    val currentMode: StateFlow<RecognitionMode> = _currentMode.asStateFlow()

    // 合并的识别状态（供UI使用）
    val recognitionState: StateFlow<RecognitionState> = combine(
        aiRecognitionState,
        systemRecognitionState
    ) { aiState, systemState ->
        when (_currentMode.value) {
            RecognitionMode.AI -> mapAIState(aiState)
            RecognitionMode.SYSTEM -> mapSystemState(systemState)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), RecognitionState.Idle)

    // AI配置
    private val aiConfig: StateFlow<AIConfig> = aiConfigRepository.getAIConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AIConfig())

    /**
     * 开始录音
     * 优先使用AI语音识别，如果不满足条件则使用系统语音识别
     */
    fun startRecording(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val config = aiConfig.value

        // 检查是否可以使用AI语音识别
        if (aiVoiceRecognitionService.isAIRecognitionAvailable(config)) {
            _currentMode.value = RecognitionMode.AI
            val success = aiVoiceRecognitionService.startRecording(context)
            if (success) {
                onResult(true, null)
            } else {
                onResult(false, "录音启动失败")
            }
        } else {
            // 回退到系统语音识别
            _currentMode.value = RecognitionMode.SYSTEM
            if (speechRecognitionService.isSpeechRecognitionAvailable()) {
                speechRecognitionService.startListening { text ->
                    onResult(true, null)
                }
            } else {
                // 给出更友好的提示，引导用户配置AI
                onResult(false, "语音功能需要配置AI API\n\n请在设置中配置AI助手，或使用支持语音的输入法（搜狗/百度/讯飞）")
            }
        }
    }

    /**
     * 停止录音并识别
     */
    fun stopRecording(onResult: (String?) -> Unit = {}) {
        when (_currentMode.value) {
            RecognitionMode.AI -> {
                viewModelScope.launch {
                    val result = aiVoiceRecognitionService.stopRecordingAndRecognize(aiConfig.value)
                    onResult(result)
                }
            }
            RecognitionMode.SYSTEM -> {
                speechRecognitionService.stopListening()
                onResult(systemRecognizedText.value)
            }
        }
    }

    /**
     * 取消录音
     */
    fun cancelRecording() {
        when (_currentMode.value) {
            RecognitionMode.AI -> aiVoiceRecognitionService.cancelRecording()
            RecognitionMode.SYSTEM -> speechRecognitionService.cancel()
        }
    }

    /**
     * 更新录音时长
     */
    fun updateRecordingDuration(seconds: Int) {
        aiVoiceRecognitionService.updateRecordingDuration(seconds)
    }

    /**
     * 检查是否支持语音识别
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return aiVoiceRecognitionService.isAIRecognitionAvailable(aiConfig.value) ||
                speechRecognitionService.isSpeechRecognitionAvailable()
    }

    /**
     * 获取当前AI配置
     */
    fun getAIConfig(): AIConfig = aiConfig.value

    override fun onCleared() {
        super.onCleared()
        speechRecognitionService.destroy()
    }

    /**
     * 映射AI语音识别状态到统一状态
     */
    private fun mapAIState(state: AIVoiceRecognitionService.RecognitionState): RecognitionState {
        return when (state) {
            is AIVoiceRecognitionService.RecognitionState.Idle -> RecognitionState.Idle
            is AIVoiceRecognitionService.RecognitionState.Recording -> RecognitionState.Recording
            is AIVoiceRecognitionService.RecognitionState.Processing -> RecognitionState.Processing
            is AIVoiceRecognitionService.RecognitionState.Success -> RecognitionState.Success(state.text)
            is AIVoiceRecognitionService.RecognitionState.Error -> RecognitionState.Error(state.message)
        }
    }

    /**
     * 映射系统语音识别状态到统一状态
     */
    private fun mapSystemState(state: SpeechRecognitionService.RecognitionState): RecognitionState {
        return when (state) {
            is SpeechRecognitionService.RecognitionState.Idle -> RecognitionState.Idle
            is SpeechRecognitionService.RecognitionState.Ready -> RecognitionState.Idle
            is SpeechRecognitionService.RecognitionState.Listening -> RecognitionState.Recording
            is SpeechRecognitionService.RecognitionState.Processing -> RecognitionState.Processing
            is SpeechRecognitionService.RecognitionState.Success -> RecognitionState.Success(state.text)
            is SpeechRecognitionService.RecognitionState.Error -> RecognitionState.Error(state.message)
        }
    }

    /**
     * 语音识别模式
     */
    enum class RecognitionMode {
        AI,      // AI语音识别
        SYSTEM   // 系统语音识别
    }

    /**
     * 统一的语音识别状态
     */
    sealed class RecognitionState {
        object Idle : RecognitionState()
        object Recording : RecognitionState()
        object Processing : RecognitionState()
        data class Success(val text: String) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
    }
}
