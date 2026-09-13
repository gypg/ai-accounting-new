package com.example.aiaccounting.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.aiaccounting.ui.viewmodel.ButlerEditorState
import com.example.aiaccounting.ui.viewmodel.ButlerMarketEvent
import com.example.aiaccounting.ui.viewmodel.ButlerMarketViewModel
import java.io.File

/**
 * 管家编辑器页面 — 新建 / 编辑自定义管家
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButlerEditorScreen(
    butlerId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ButlerMarketViewModel = hiltViewModel()
) {
    val editorState by viewModel.editorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 初始化编辑器
    LaunchedEffect(butlerId) {
        if (butlerId != null) {
            viewModel.initEditEditor(butlerId)
        } else {
            viewModel.initNewEditor()
        }
    }

    // 相册图片选择器
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.saveAvatarFromUri(it) }
    }

    // 收集事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ButlerMarketEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
                is ButlerMarketEvent.NavigateBack ->
                    onNavigateBack()
                else -> {} // 其他事件不处理
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editorState.isNewButler) "创建管家" else "编辑管家",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveButler() },
                        enabled = editorState.isEditorReady && !editorState.isSaving
                    ) {
                        if (editorState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── 头像选择 ───
            AvatarEditor(
                editorState = editorState,
                onPickImage = { photoPickerLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── 基本信息 ───
            SectionLabel("基本信息")

            OutlinedTextField(
                value = editorState.name,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(name = v) } },
                label = { Text("管家名称 *") },
                placeholder = { Text("例如：小助手") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = editorState.isEditorReady
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = editorState.title,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(title = v) } },
                label = { Text("称号") },
                placeholder = { Text("例如：温柔管家") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = editorState.isEditorReady
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = editorState.description,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(description = v) } },
                label = { Text("简介") },
                placeholder = { Text("一句话描述管家的特点") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                enabled = editorState.isEditorReady
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── 交互称呼 ───
            SectionLabel("交互称呼")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editorState.userCallName,
                    onValueChange = { v -> viewModel.updateEditorField { it.copy(userCallName = v) } },
                    label = { Text("称呼用户") },
                    placeholder = { Text("主人") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = editorState.isEditorReady
                )
                OutlinedTextField(
                    value = editorState.butlerSelfName,
                    onValueChange = { v -> viewModel.updateEditorField { it.copy(butlerSelfName = v) } },
                    label = { Text("自称") },
                    placeholder = { Text("我") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = editorState.isEditorReady
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── 性格滑杆 ───
            SectionLabel("性格调节")

            PersonalitySlider(
                label = "沟通风格",
                leftLabel = "简洁直接",
                rightLabel = "细腻温柔",
                value = editorState.communicationStyle,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(communicationStyle = v) } }
            )
            PersonalitySlider(
                label = "情感浓度",
                leftLabel = "理性克制",
                rightLabel = "热情奔放",
                value = editorState.emotionIntensity,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(emotionIntensity = v) } }
            )
            PersonalitySlider(
                label = "专业程度",
                leftLabel = "轻松日常",
                rightLabel = "严谨专业",
                value = editorState.professionalism,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(professionalism = v) } }
            )
            PersonalitySlider(
                label = "幽默感",
                leftLabel = "正经稳重",
                rightLabel = "风趣幽默",
                value = editorState.humor,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(humor = v) } }
            )
            PersonalitySlider(
                label = "主动性",
                leftLabel = "被动响应",
                rightLabel = "主动关怀",
                value = editorState.proactivity,
                onValueChange = { v -> viewModel.updateEditorField { it.copy(proactivity = v) } }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── 子组件 ───

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
private fun AvatarEditor(
    editorState: ButlerEditorState,
    onPickImage: () -> Unit
) {
    val hasAvatar = editorState.avatarType == "LOCAL_PATH" && editorState.avatarValue.isNotBlank()

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onPickImage),
        contentAlignment = Alignment.Center
    ) {
        if (hasAvatar) {
            val file = File(editorState.avatarValue)
            if (file.exists()) {
                AsyncImage(
                    model = file,
                    contentDescription = "管家头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 遮罩 + 相机图标
        if (!hasAvatar) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = "选择头像",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            // 半透明编辑指示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "更换头像",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }

    Text(
        text = "点击选择头像",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun PersonalitySlider(
    label: String,
    leftLabel: String,
    rightLabel: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$value",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = leftLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = rightLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
