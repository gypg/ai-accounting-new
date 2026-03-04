package com.example.aiaccounting.data.service

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 语音识别服务
 * 用于语音记账功能
 */
class SpeechRecognitionService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var useIntentMode: Boolean = false
    
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()
    
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null

    /**
     * 检查设备是否支持语音识别
     * 使用Intent方式检测，兼容性更好
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        // 方法1：检查是否有应用可以处理语音识别Intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        if (activities.isNotEmpty()) {
            return true
        }
        
        // 方法2：检查系统语音识别服务
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * 开始语音识别
     */
    fun startListening(onResult: (String) -> Unit) {
        onResultCallback = onResult
        _recognizedText.value = ""
        
        // 优先尝试使用Intent方式（兼容性更好）
        if (tryStartIntentRecognition()) {
            return
        }
        
        // 如果Intent方式不可用，使用内置SpeechRecognizer
        startInternalRecognizer()
    }

    /**
     * 使用Intent方式启动语音识别（兼容性更好）
     */
    private fun tryStartIntentRecognition(): Boolean {
        return try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出记账内容，例如：今天吃饭花了50元")
            }
            
            // 检查是否有应用可以处理这个Intent
            val activities = context.packageManager.queryIntentActivities(intent, 0)
            if (activities.isNotEmpty()) {
                useIntentMode = true
                _recognitionState.value = RecognitionState.Listening
                
                // 启动语音识别Activity
                if (context is Activity) {
                    context.startActivityForResult(intent, SPEECH_REQUEST_CODE)
                }
                true
            } else {
                false
            }
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 使用内置SpeechRecognizer启动语音识别
     */
    private fun startInternalRecognizer() {
        useIntentMode = false
        
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        _recognitionState.value = RecognitionState.Listening
        speechRecognizer?.startListening(intent)
    }

    /**
     * 处理Activity结果（用于Intent方式）
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _recognizedText.value = text
                    _recognitionState.value = RecognitionState.Success(text)
                    onResultCallback?.invoke(text)
                } else {
                    _recognitionState.value = RecognitionState.Error("未能识别语音")
                }
            } else {
                _recognitionState.value = RecognitionState.Error("语音识别取消或失败")
            }
        }
    }

    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (!useIntentMode) {
            speechRecognizer?.stopListening()
        }
        // Intent模式下不需要手动停止
    }

    /**
     * 取消语音识别
     */
    fun cancel() {
        if (!useIntentMode) {
            speechRecognizer?.cancel()
        }
        _recognitionState.value = RecognitionState.Idle
    }

    /**
     * 释放资源
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _recognitionState.value = RecognitionState.Ready
            }

            override fun onBeginningOfSpeech() {
                _recognitionState.value = RecognitionState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 可以在这里更新音量指示器
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _recognitionState.value = RecognitionState.Processing
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "音频录制失败"
                    SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    SpeechRecognizer.ERROR_NETWORK -> "网络错误，请检查网络连接"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    SpeechRecognizer.ERROR_NO_MATCH -> "未能识别语音，请重试"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                    SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音输入超时，请重试"
                    else -> "未知错误"
                }
                _recognitionState.value = RecognitionState.Error(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _recognizedText.value = text
                    _recognitionState.value = RecognitionState.Success(text)
                    onResultCallback?.invoke(text)
                } else {
                    _recognitionState.value = RecognitionState.Error("未能识别语音")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    companion object {
        const val SPEECH_REQUEST_CODE = 1001
    }

    /**
     * 语音识别状态
     */
    sealed class RecognitionState {
        object Idle : RecognitionState()
        object Ready : RecognitionState()
        object Listening : RecognitionState()
        object Processing : RecognitionState()
        data class Success(val text: String) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
    }
}
