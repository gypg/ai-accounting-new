package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aiaccounting.data.model.AIModel
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.ui.viewmodel.AISettingsViewModel
import com.example.aiaccounting.ui.viewmodel.InviteBindResult
import com.example.aiaccounting.utils.ModelIdCategorizer.categorizeModelId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedGatewayBaseUrl by viewModel.gatewayBaseUrl.collectAsState()
    val inviteApiBaseUrl by viewModel.inviteApiBaseUrl.collectAsState()
    val inviteRpm by viewModel.inviteRpm.collectAsState()
    val inviteCodeMasked by viewModel.inviteCodeMasked.collectAsState()
    val preferredNetworkRoute by viewModel.preferredNetworkRoute.collectAsState()
    val gatewayBaseUrlState = remember(savedGatewayBaseUrl) { mutableStateOf(savedGatewayBaseUrl) }
    var gatewayBaseUrl by gatewayBaseUrlState
    val isInviteBound = uiState.inviteBound
    var showApiKey by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showImportKeyDialog by remember { mutableStateOf(false) }
    var importApiKeyInput by remember { mutableStateOf("") }

    var inviteCode by remember { mutableStateOf("") }

    // 显示保存成功提示
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaveSuccess()
        }
    }

    // 网络状态提示
    LaunchedEffect(Unit) {
        viewModel.refreshNetworkStatus()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNetworkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 导入 API Key + 应用预设
    if (showImportKeyDialog) {
        AlertDialog(
            onDismissRequest = { showImportKeyDialog = false },
            title = { Text("导入 API Key") },
            text = {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importApiKeyInput,
                        onValueChange = { importApiKeyInput = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = importApiKeyInput.trim().replace("\n", "").replace("\r", "").replace(" ", "")
                        viewModel.applyBuiltinPresetWithApiKey(cleaned)
                        importApiKeyInput = ""
                        showImportKeyDialog = false
                    },
                    enabled = importApiKeyInput.isNotBlank()
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportKeyDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI助手设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                uiState.networkHealthWarning?.let { warning ->
                    NetworkHealthWarningCard(warning = warning)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 内置默认模型开关（如果内置配置可用）
                if (uiState.isBuiltinAvailable) {
                    BuiltinConfigCard(
                        isEnabled = uiState.useBuiltinConfig,
                        onEnabledChange = { viewModel.toggleBuiltinConfig(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 邀请码绑定（推荐：给朋友用，不暴露主 Key）
                if (!uiState.useBuiltinConfig && !isInviteBound) {
                    InviteCodeBindCard(
                        inviteCode = inviteCode,
                        gatewayBaseUrl = gatewayBaseUrl,
                        isBinding = uiState.isBindingInvite,
                        bindResult = uiState.inviteBindResult,
                        onInviteCodeChange = { inviteCode = it },
                        onGatewayBaseUrlChange = {
                            gatewayBaseUrl = it
                            viewModel.setGatewayBaseUrl(it)
                        },
                        onBind = {
                            viewModel.bindInviteCode(
                                inviteCode = inviteCode,
                                gatewayBaseUrl = gatewayBaseUrl
                            )
                        },
                        onDismissResult = { viewModel.clearInviteBindResult() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (!uiState.useBuiltinConfig && isInviteBound) {
                    InviteBoundStatusCard(
                        inviteCodeMasked = inviteCodeMasked,
                        gatewayBaseUrl = savedGatewayBaseUrl,
                        apiBaseUrl = inviteApiBaseUrl,
                        rpmText = inviteRpm.takeIf { rpm -> rpm > 0 }?.let { rpm -> "$rpm rpm" }
                            ?: (uiState.inviteBindResult as? InviteBindResult.Success)?.let { success -> "${success.rpm} rpm" }.orEmpty(),
                        onClear = { viewModel.clearInviteBinding() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 一键预设（允许编辑切换，但减少朋友配置负担）
                if (!uiState.useBuiltinConfig && !isInviteBound) {
                    Button(
                        onClick = { showImportKeyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isNetworkAvailable
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("一键使用默认模型")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 启用AI助手开关
                EnableAICard(
                    isEnabled = uiState.config.isEnabled,
                    onEnabledChange = { viewModel.updateEnabled(it) }
                )

                if (!uiState.useBuiltinConfig && isInviteBound) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "已通过邀请码绑定，为避免泄露站点/密钥，已隐藏详细 API 配置。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI提供商选择（仅在不使用内置配置时显示）
                if (!uiState.useBuiltinConfig && !isInviteBound) {
                    ProviderCard(
                        selectedProvider = uiState.config.provider,
                        onProviderSelected = { viewModel.updateProvider(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isInviteBound) {
                    Spacer(modifier = Modifier.height(16.dp))
                    InviteModelSelectorCard(
                        isAuto = uiState.inviteModelMode == com.example.aiaccounting.data.repository.AIConfigRepository.ModelSelectionMode.AUTO,
                        selectedModelId = uiState.config.model,
                        remoteModels = uiState.remoteModels,
                        isFetchingModels = uiState.isFetchingModels,
                        onToggleAuto = { isAuto ->
                            val mode = if (isAuto) {
                                com.example.aiaccounting.data.repository.AIConfigRepository.ModelSelectionMode.AUTO
                            } else {
                                com.example.aiaccounting.data.repository.AIConfigRepository.ModelSelectionMode.FIXED
                            }
                            viewModel.updateInviteModelMode(mode)
                        },
                        onFetchModels = { viewModel.fetchRemoteModels() },
                        onModelSelected = { viewModel.updateInviteModel(it) },
                        useBuiltinConfig = uiState.useBuiltinConfig,
                        onTestConnection = { modelId: String, callback: (ModelTestResult) -> Unit ->
                            viewModel.testModelConnection(modelId, callback)
                        }
                    )
                }

                // API配置（仅在不使用默认模型且非邀请码绑定时显示）
                if (!uiState.useBuiltinConfig && !isInviteBound) {
                    Spacer(modifier = Modifier.height(16.dp))

                    APIConfigCard(
                        apiKey = uiState.config.apiKey,
                        apiUrl = uiState.config.apiUrl,
                        model = uiState.config.model,
                        provider = uiState.config.provider,
                        remoteModels = uiState.remoteModels,
                        isFetchingModels = uiState.isFetchingModels,
                        showApiKey = showApiKey,
                        isAuto = uiState.userModelMode == com.example.aiaccounting.data.repository.AIConfigRepository.ModelSelectionMode.AUTO,
                        onToggleAuto = { isAuto ->
                            val mode = if (isAuto) {
                                com.example.aiaccounting.data.repository.AIConfigRepository.ModelSelectionMode.AUTO
                            } else {
                                com.example.aiaccounting.data.repository.AIConfigRepository.ModelSelectionMode.FIXED
                            }
                            viewModel.updateUserModelMode(mode)
                        },
                        onShowApiKeyChange = { showApiKey = it },
                        onApiKeyChange = { viewModel.updateApiKey(it) },
                        onApiUrlChange = { viewModel.updateApiUrl(it) },
                        onModelChange = { viewModel.updateModel(it) },
                        onFetchModels = { viewModel.fetchRemoteModels() },
                        onTestModelConnection = { modelId, callback ->
                            viewModel.testModelConnection(modelId, callback)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 测试连接按钮
                    Button(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTesting && uiState.config.apiKey.isNotBlank() && uiState.isNetworkAvailable
                    ) {
                        if (uiState.isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.NetworkCheck, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试连接")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.runNetworkSpeedTest() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTestingNetworkSpeed && uiState.isNetworkAvailable &&
                            (uiState.config.apiUrl.isNotBlank() || savedGatewayBaseUrl.isNotBlank())
                    ) {
                        if (uiState.isTestingNetworkSpeed) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Speed, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("网络测速")
                    }

                    preferredNetworkRoute?.let { route ->
                        Spacer(modifier = Modifier.height(8.dp))
                        PreferredRouteCard(
                            label = route.label,
                            latencyMs = route.latencyMs
                        )
                    }

                    uiState.recommendedModelSummary?.takeIf {
                        uiState.userModelMode == AIConfigRepository.ModelSelectionMode.AUTO ||
                            uiState.inviteModelMode == AIConfigRepository.ModelSelectionMode.AUTO
                    }?.let { recommendation ->
                        Spacer(modifier = Modifier.height(8.dp))
                        RecommendedModelCard(
                            modelId = recommendation.modelId,
                            reason = recommendation.reason,
                            latencyMs = recommendation.latencyMs
                        )
                    }

                    uiState.networkSpeedTestResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkSpeedTestResultCard(
                            result = result,
                            onDismiss = { viewModel.clearNetworkSpeedTestResult() }
                        )
                    }

                    // 测试结果
                    uiState.testResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        TestResultCard(
                            result = result,
                            onDismiss = { viewModel.clearTestResult() }
                        )
                    }
                }

                // 保存按钮（仅在不使用内置配置且非邀请码绑定时显示）
                if (!uiState.useBuiltinConfig && !isInviteBound) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.saveConfig() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存设置")
                    }

                    // 保存成功提示
                    if (uiState.saveSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "设置已保存",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 使用统计卡片
                UsageStatsCard(
                    stats = uiState.usageStats,
                    onResetClick = { showResetConfirmDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 说明文字
                InfoCard()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 重置确认对话框
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("重置统计") },
            text = { Text("确定要重置所有使用统计数据吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetUsageStats()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PreferredRouteCard(
    label: String,
    latencyMs: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "智能路由建议：$label",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "基于最近测速推荐，延迟 ${latencyMs}ms",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun RecommendedModelCard(
    modelId: String,
    reason: String,
    latencyMs: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "模型推荐：$modelId",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = latencyMs?.let { "$reason，最近平均延迟 ${it}ms" } ?: reason,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

// NetworkStatusCard, BuiltinConfigCard, EnableAICard, InfoCard 已移至 AISettingsCards.kt
// InviteModelSelectorCard, CategorizedModelSelector, ModelListItem 已移至 AISettingsModelSelector.kt
// ProviderCard, ProviderItem, APIConfigCard, TestResultCard 已移至 AISettingsProviderAndAPI.kt
// InviteCodeBindCard, InviteBoundStatusCard 已移至 AISettingsInvite.kt
// UsageStatsCard, StatItem, StatDetailRow 已移至 AISettingsUsage.kt
// CustomModelManager, SelectedModelItem, ModelManageDialog, AddModelDialog, CustomModelListItem, CustomModelInfo, categorizeModel 已移至 AISettingsCustomModel.kt
