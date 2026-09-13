package com.example.aiaccounting.data.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.di.VoiceOkHttpClient
import com.example.aiaccounting.utils.OpenAiUrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI语音识别服务
 * 使用AI API进行语音识别，不依赖系统语音识别服务
 */
@Singleton
class AIVoiceRecognitionService @Inject constructor(
    private val aiService: AIService,
    @VoiceOkHttpClient private val client: OkHttpClient
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    /**
     * 开始录音
     */
    fun startRecording(context: Context): Boolean {
        return try {
            // 创建临时音频文件
            audioFile = File(context.cacheDir, "voice_recording_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioChannels(1)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            _recognitionState.value = RecognitionState.Recording
            _recognizedText.value = ""
            _recordingDuration.value = 0

            true
        } catch (e: Exception) {
            _recognitionState.value = RecognitionState.Error("录音启动失败: ${e.message}")
            false
        }
    }

    /**
     * 停止录音并进行识别
     */
    suspend fun stopRecordingAndRecognize(config: AIConfig): String? = withContext(Dispatchers.IO) {
        try {
            _recognitionState.value = RecognitionState.Processing

            // 停止录音
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // 检查音频文件
            val file = audioFile
            if (file == null || !file.exists() || file.length() == 0L) {
                _recognitionState.value = RecognitionState.Error("录音文件为空")
                return@withContext null
            }

            // 使用AI进行语音识别
            val result = recognizeWithAI(file, config)

            // 清理文件
            file.delete()
            audioFile = null

            result
        } catch (e: Exception) {
            _recognitionState.value = RecognitionState.Error("识别失败: ${e.message}")
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            null
        }
    }

    /**
     * 取消录音
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // 忽略
        }
        mediaRecorder = null
        audioFile?.delete()
        audioFile = null
        _recognitionState.value = RecognitionState.Idle
        _recognizedText.value = ""
    }

    /**
     * 使用AI进行语音识别
     * 支持多种方式：Whisper API 或 多模态模型
     */
    private suspend fun recognizeWithAI(audioFile: File, config: AIConfig): String? {
        if (!config.isEnabled || config.apiKey.isBlank()) {
            _recognitionState.value = RecognitionState.Error("请先配置AI API密钥")
            return null
        }

        return try {
            // 尝试使用Whisper API（如果API支持）
            val result = tryWhisperAPI(audioFile, config)
                ?: tryMultimodalAI(audioFile, config)

            if (result != null) {
                _recognizedText.value = result
                _recognitionState.value = RecognitionState.Success(result)
            }

            result
        } catch (e: Exception) {
            _recognitionState.value = RecognitionState.Error("AI识别失败: ${e.message}")
            null
        }
    }

    /**
     * 尝试使用Whisper API进行识别
     */
    private fun tryWhisperAPI(audioFile: File, config: AIConfig): String? {
        return try {
            // 检查是否是OpenAI兼容的API
            val whisperUrl = OpenAiUrlUtils.whisperTranscriptions(config.apiUrl)

            // 读取音频文件
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.DEFAULT)

            // 构建请求
            val requestBody = JSONObject().apply {
                put("model", "whisper-1")
                put("file", base64Audio)
                put("language", "zh")
                put("response_format", "json")
            }

            val request = Request.Builder()
                .url(whisperUrl)
                .header("Authorization", "Bearer ${config.apiKey.trim()}")
                .header("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Whisper API不可用，返回null让上层尝试其他方式
                    return null
                }

                val responseBody = response.body?.string() ?: return null
                val json = JSONObject(responseBody)
                json.optString("text", "")
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 尝试使用多模态AI进行识别
     * 将音频转换为文本描述后发送给AI
     */
    private suspend fun tryMultimodalAI(audioFile: File, config: AIConfig): String? {
        // 由于大多数AI模型不直接支持音频输入
        // 这里使用一个变通方案：提示用户当前使用文本输入
        // 实际生产环境应该集成专门的语音识别SDK

        _recognitionState.value = RecognitionState.Error(
            "当前AI模型不支持直接语音识别。\n\n" +
            "建议方案：\n" +
            "1. 使用支持语音的输入法（如搜狗、讯飞）\n" +
            "2. 直接在输入框中输入文字\n" +
            "3. 配置支持Whisper API的OpenAI密钥"
        )

        return null
    }

    /**
     * 检查是否支持AI语音识别
     */
    fun isAIRecognitionAvailable(config: AIConfig): Boolean {
        return config.isEnabled && config.apiKey.isNotBlank()
    }

    /**
     * 更新录音时长
     */
    fun updateRecordingDuration(seconds: Int) {
        _recordingDuration.value = seconds
    }

    /**
     * 语音识别状态
     */
    sealed class RecognitionState {
        object Idle : RecognitionState()
        object Recording : RecognitionState()
        object Processing : RecognitionState()
        data class Success(val text: String) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
    }
}
