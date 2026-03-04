package com.example.aiaccounting.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.service.AIVoiceRecognitionService
import com.example.aiaccounting.ui.viewmodel.VoiceInputViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 语音输入按钮组件
 * 支持两种模式：
 * 1. 系统语音识别（需要安装语音输入法）
 * 2. AI语音识别（需要配置AI API）
 */
@Composable
fun VoiceInputButton(
    onVoiceResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceInputViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val recognitionState by viewModel.recognitionState.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()

    var isPressed by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var recognizedText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    // 录音时长计时器
    LaunchedEffect(isRecording) {
        if (isRecording) {
            var seconds = 0
            while (isRecording) {
                delay(1000)
                seconds++
                viewModel.updateRecordingDuration(seconds)
                // 最大录音时长60秒
                if (seconds >= 60) {
                    viewModel.stopRecording { result ->
                        result?.let { onVoiceResult(it) }
                    }
                    isRecording = false
                    showDialog = false
                }
            }
        }
    }

    // 录音权限申请launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，开始录音
            startRecording(viewModel) { success, error ->
                if (success) {
                    isRecording = true
                    showDialog = true
                } else {
                    errorMessage = error ?: "录音启动失败"
                    showDialog = true
                }
            }
        } else {
            showPermissionDialog = true
        }
    }

    Box(modifier = modifier) {
        // 语音按钮
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(if (isPressed || isRecording) 0.9f else 1f)
                .background(
                    color = when {
                        isRecording -> MaterialTheme.colorScheme.error
                        isPressed -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.primary
                    },
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // 检查权限
                            val permissionStatus = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )
                            when (permissionStatus) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    // 已有权限
                                    isPressed = true
                                    startRecording(viewModel) { success, error ->
                                        if (success) {
                                            isRecording = true
                                            showDialog = true
                                        } else {
                                            errorMessage = error ?: "录音启动失败"
                                            showDialog = true
                                        }
                                    }
                                }
                                else -> {
                                    // 申请权限
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            tryAwaitRelease()
                            // 松开时停止录音
                            if (isRecording) {
                                isRecording = false
                                isPressed = false
                                viewModel.stopRecording { result ->
                                    result?.let {
                                        recognizedText = it
                                        onVoiceResult(it)
                                    }
                                    showDialog = false
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "停止录音" else "语音记账",
                tint = when {
                    isRecording -> MaterialTheme.colorScheme.onError
                    isPressed -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onPrimary
                },
                modifier = Modifier.size(24.dp)
            )
        }

        // 录音中对话框
        if (showDialog) {
            VoiceRecognitionDialog(
                isRecording = isRecording,
                recordingDuration = recordingDuration,
                recognizedText = recognizedText,
                errorMessage = errorMessage,
                onDismiss = {
                    if (isRecording) {
                        isRecording = false
                        viewModel.cancelRecording()
                    }
                    showDialog = false
                    errorMessage = ""
                    recognizedText = ""
                },
                onStopRecording = {
                    isRecording = false
                    viewModel.stopRecording { result ->
                        result?.let {
                            recognizedText = it
                            onVoiceResult(it)
                        }
                        showDialog = false
                    }
                }
            )
        }

        // 权限提示对话框
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("需要录音权限") },
                text = { Text("语音记账功能需要录音权限才能使用，请在设置中开启权限。") },
                confirmButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

/**
 * 开始录音
 */
private fun startRecording(
    viewModel: VoiceInputViewModel,
    onResult: (Boolean, String?) -> Unit
) {
    viewModel.startRecording { success, error ->
        onResult(success, error)
    }
}

/**
 * 语音识别对话框
 */
@Composable
private fun VoiceRecognitionDialog(
    isRecording: Boolean,
    recordingDuration: Int,
    recognizedText: String,
    errorMessage: String,
    onDismiss: () -> Unit,
    onStopRecording: () -> Unit
) {
    val hasError = errorMessage.isNotEmpty()
    val hasResult = recognizedText.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    hasError -> "识别失败"
                    hasResult -> "识别成功"
                    isRecording -> "正在录音..."
                    else -> "语音识别"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRecording && !hasError) {
                    // 显示录音动画
                    VoiceWaveAnimation(
                        isActive = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 录音时长
                    Text(
                        text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "松开手指完成录音",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 识别文本
                if (hasResult) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "\"$recognizedText\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 错误信息
                if (hasError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (hasError) {
                TextButton(onClick = onDismiss) {
                    Text("确定")
                }
            } else if (isRecording) {
                Button(
                    onClick = onStopRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("完成")
                }
            }
        },
        dismissButton = if (isRecording || hasResult) {
            {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        } else null
    )
}

/**
 * 语音波形动画
 */
@Composable
private fun VoiceWaveAnimation(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 5

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val delay = index * 100
            val infiniteTransition = rememberInfiniteTransition(label = "wave_$index")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = delay
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_animation_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(scale)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
