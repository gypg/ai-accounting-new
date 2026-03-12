package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.aiaccounting.data.model.AIModel
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.repository.AIUsageStats
import com.example.aiaccounting.ui.viewmodel.AISettingsViewModel
import com.example.aiaccounting.ui.viewmodel.TestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AISettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showApiKey by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // 显示保存成功提示
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaveSuccess()
        }
    }

    // 网络状态提示
    if (!uiState.isNetworkAvailable) {
        LaunchedEffect(Unit) {
            viewModel.refreshNetworkStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI助手设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                // 网络状态提示
                if (!uiState.isNetworkAvailable) {
                    NetworkStatusCard()
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

                // 启用AI助手开关
                EnableAICard(
                    isEnabled = uiState.config.isEnabled,
                    onEnabledChange = { viewModel.updateEnabled(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // AI提供商选择（仅在不使用内置配置时显示）
                if (!uiState.useBuiltinConfig) {
                    ProviderCard(
                        selectedProvider = uiState.config.provider,
                        onProviderSelected = { viewModel.updateProvider(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // API配置（仅在不使用内置配置时显示）
                if (!uiState.useBuiltinConfig) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    APIConfigCard(
                        apiKey = uiState.config.apiKey,
                        apiUrl = uiState.config.apiUrl,
                        model = uiState.config.model,
                        provider = uiState.config.provider,
                        remoteModels = uiState.remoteModels,
                        isFetchingModels = uiState.isFetchingModels,
                        showApiKey = showApiKey,
                        onShowApiKeyChange = { showApiKey = it },
                        onApiKeyChange = { viewModel.updateApiKey(it) },
                        onApiUrlChange = { viewModel.updateApiUrl(it) },
                        onModelChange = { viewModel.updateModel(it) },
                        onFetchModels = { viewModel.fetchRemoteModels() }
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

                    // 测试结果
                    uiState.testResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        TestResultCard(
                            result = result,
                            onDismiss = { viewModel.clearTestResult() }
                        )
                    }
                }

                // 保存按钮（仅在不使用内置配置时显示）
                if (!uiState.useBuiltinConfig) {
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
private fun NetworkStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "网络不可用",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "当前处于离线模式，AI助手将使用本地解析",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 内置默认模型配置卡片
 */
@Composable
private fun BuiltinConfigCard(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "使用默认模型",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "使用内置配置，无需手动设置",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@Composable
private fun EnableAICard(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "启用AI助手",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "使用云端AI进行智能分析和对话",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@Composable
private fun ProviderCard(
    selectedProvider: AIProvider,
    onProviderSelected: (AIProvider) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "选择AI提供商",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            AIProvider.values().forEach { provider ->
                ProviderItem(
                    provider = provider,
                    isSelected = selectedProvider == provider,
                    onClick = { onProviderSelected(provider) }
                )
                if (provider != AIProvider.values().last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProviderItem(
    provider: AIProvider,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = provider.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = provider.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun APIConfigCard(
    apiKey: String,
    apiUrl: String,
    model: String,
    provider: AIProvider,
    remoteModels: List<com.example.aiaccounting.data.service.RemoteModel>,
    isFetchingModels: Boolean,
    showApiKey: Boolean,
    onShowApiKeyChange: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onFetchModels: () -> Unit
) {
    // 使用远程模型列表（如果有）或默认模型列表
    val modelsToShow = if (remoteModels.isNotEmpty()) {
        remoteModels.map { AIModel(it.id, it.name, it.description) }
    } else {
        provider.models
    }
    
    val selectedModel = modelsToShow.find { it.id == model } ?: modelsToShow.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "API配置",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // API密钥
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API密钥") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { onShowApiKeyChange(!showApiKey) }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showApiKey) "隐藏" else "显示"
                        )
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // API地址
            OutlinedTextField(
                value = apiUrl,
                onValueChange = onApiUrlChange,
                label = { Text("API地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 自定义提供商使用新的模型管理方式
            if (provider == AIProvider.CUSTOM) {
                CustomModelManager(
                    apiKey = apiKey,
                    apiUrl = apiUrl,
                    remoteModels = remoteModels,
                    isFetchingModels = isFetchingModels,
                    selectedModelId = model,
                    onModelSelected = onModelChange,
                    onFetchModels = onFetchModels
                )
            } else {
                // 获取模型列表按钮
                OutlinedButton(
                    onClick = onFetchModels,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFetchingModels && apiKey.isNotBlank()
                ) {
                    if (isFetchingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isFetchingModels) "获取中..." else "获取模型列表")
                }

                // 显示获取到的模型数量
                if (remoteModels.isNotEmpty()) {
                    Text(
                        text = "已获取 ${remoteModels.size} 个模型",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 模型选择（带搜索功能）
                ModelSelectorWithSearch(
                    models = modelsToShow,
                    selectedModelId = model,
                    onModelSelected = onModelChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelectorWithSearch(
    models: List<AIModel>,
    selectedModelId: String,
    onModelSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val selectedModel = models.find { it.id == selectedModelId }
    
    // 筛选模型
    val filteredModels = remember(searchQuery, models) {
        if (searchQuery.isBlank()) {
            models
        } else {
            models.filter { model ->
                model.displayName.contains(searchQuery, ignoreCase = true) ||
                model.id.contains(searchQuery, ignoreCase = true) ||
                model.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 模型选择按钮
    OutlinedCard(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "选择模型",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedModel?.displayName ?: selectedModelId.ifEmpty { "默认模型" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (selectedModel != null) {
                    Text(
                        text = selectedModel.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "选择模型"
            )
        }
    }

    // 模型选择对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                searchQuery = ""
            },
            title = { 
                Column {
                    Text("选择模型")
                    Text(
                        text = "共 ${models.size} 个模型",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 搜索框
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索模型") },
                        placeholder = { Text("输入模型名称筛选...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 显示筛选结果数量
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "找到 ${filteredModels.size} 个结果",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // 模型列表
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(filteredModels, key = { it.id }) { aiModel ->
                            ModelListItem(
                                model = aiModel,
                                isSelected = aiModel.id == selectedModelId,
                                onClick = {
                                    onModelSelected(aiModel.id)
                                    showDialog = false
                                    searchQuery = ""
                                }
                            )
                        }
                        
                        if (filteredModels.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "未找到匹配的模型",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    showDialog = false
                    searchQuery = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ModelListItem(
    model: AIModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = model.description,
                fontSize = 12.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Text(
                text = model.id,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TestResultCard(
    result: TestResult,
    onDismiss: () -> Unit
) {
    val (backgroundColor, textColor, icon, message) = when (result) {
        is TestResult.Success -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle,
            "连接成功！AI助手可以正常使用。"
        )
        is TestResult.Error -> Quadruple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error,
            result.message
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = textColor
                )
            }
        }
    }
}

@Composable
private fun UsageStatsCard(
    stats: AIUsageStats,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "使用统计",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = onResetClick) {
                    Text("重置", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (stats.totalCalls > 0) {
                // 统计项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Call,
                        value = "${stats.totalCalls}",
                        label = "总调用"
                    )
                    StatItem(
                        icon = Icons.Default.CheckCircle,
                        value = "${stats.successRate.toInt()}%",
                        label = "成功率"
                    )
                    StatItem(
                        icon = Icons.Default.AttachMoney,
                        value = stats.formattedCostCNY(),
                        label = "预估费用"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 详细信息
                Column {
                    StatDetailRow(
                        label = "成功调用",
                        value = "${stats.successCalls} 次"
                    )
                    StatDetailRow(
                        label = "失败调用",
                        value = "${stats.failedCalls} 次"
                    )
                    StatDetailRow(
                        label = "总Token数",
                        value = "${stats.totalTokens}"
                    )
                    StatDetailRow(
                        label = "首次使用",
                        value = stats.firstUseTimeFormatted()
                    )
                    StatDetailRow(
                        label = "最后使用",
                        value = stats.lastCallTimeFormatted()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无使用记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "使用说明",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = """
                    1. 选择AI提供商（OpenAI、Claude、Gemini或自定义）
                    2. 输入对应的API密钥
                    3. 可选择性修改API地址和模型名称
                    4. 点击"测试连接"验证配置
                    5. 保存设置后即可使用AI功能
                    
                    注意：
                    • API密钥仅存储在本地，不会上传到服务器
                    • 无网络时会自动切换到本地AI模式
                    • 使用统计仅用于参考，实际费用以服务商账单为准
                """.trimIndent(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = 18.sp
            )
        }
    }
}

// 辅助数据类
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// ==================== 自定义模型管理组件 ====================

/**
 * 自定义模型管理器 - Cherry Studio风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomModelManager(
    apiKey: String,
    apiUrl: String,
    remoteModels: List<com.example.aiaccounting.data.service.RemoteModel>,
    isFetchingModels: Boolean,
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    onFetchModels: () -> Unit
) {
    var showManageDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    // 本地管理的自定义模型列表（已选择的模型）
    var customModels by remember { mutableStateOf<List<CustomModelInfo>>(emptyList()) }
    
    // 合并远程获取的模型和手动添加的模型
    val allModels = remember(remoteModels, customModels) {
        val remote = remoteModels.map { 
            CustomModelInfo(
                id = it.id,
                name = it.name,
                group = it.description ?: "其他",
                isRemote = true,
                category = categorizeModel(it.id)
            )
        }
        remote + customModels
    }
    
    // 当前已选择的模型
    val selectedModel = allModels.find { it.id == selectedModelId }

    Column {
        // 管理和添加按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 管理按钮
            OutlinedButton(
                onClick = { showManageDialog = true },
                modifier = Modifier.weight(1f),
                enabled = apiKey.isNotBlank() || allModels.isNotEmpty()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("管理")
            }
            
            // 添加按钮
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加")
            }
        }

        // 显示模型数量
        if (allModels.isNotEmpty()) {
            Text(
                text = "API共 ${remoteModels.size} 个模型，已选择 ${customModels.size + if (selectedModel?.isRemote == true) 1 else 0} 个",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 当前选择的模型显示（可点击打开管理）
        OutlinedCard(
            onClick = { showManageDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "当前使用模型",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedModel?.name ?: selectedModelId.ifEmpty { "未选择模型" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedModel != null) {
                        Text(
                            text = "${selectedModel.group} · ${selectedModel.category}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "选择模型"
                )
            }
        }
        
        // 已选择的模型列表（底部单独显示）
        if (customModels.isNotEmpty() || selectedModel != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "已选择的模型",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 已选择模型列表
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // 显示当前选中的远程模型（如果有）
                    if (selectedModel?.isRemote == true) {
                        SelectedModelItem(
                            model = selectedModel,
                            isActive = true,
                            onRemove = {
                                onModelSelected("")
                            }
                        )
                    }
                    
                    // 显示手动添加的自定义模型
                    customModels.forEach { model ->
                        SelectedModelItem(
                            model = model,
                            isActive = model.id == selectedModelId,
                            onClick = { onModelSelected(model.id) },
                            onRemove = {
                                customModels = customModels.filter { it.id != model.id }
                                if (model.id == selectedModelId) {
                                    onModelSelected("")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 模型管理弹窗
    if (showManageDialog) {
        ModelManageDialog(
            models = allModels,
            selectedModelId = selectedModelId,
            onModelSelected = { 
                onModelSelected(it)
                // 添加到已选择列表
                val model = allModels.find { m -> m.id == it }
                if (model != null && !model.isRemote && !customModels.any { cm -> cm.id == model.id }) {
                    customModels = customModels + model
                }
                showManageDialog = false
            },
            onDismiss = { showManageDialog = false },
            onFetchModels = onFetchModels,
            isFetching = isFetchingModels,
            onDeleteModel = { modelId ->
                customModels = customModels.filter { it.id != modelId }
                if (modelId == selectedModelId) {
                    onModelSelected("")
                }
            }
        )
    }

    // 添加模型弹窗
    if (showAddDialog) {
        AddModelDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { modelInfo ->
                customModels = customModels + modelInfo
                onModelSelected(modelInfo.id)
                showAddDialog = false
            }
        )
    }
}

/**
 * 已选择模型项（底部列表用）
 */
@Composable
private fun SelectedModelItem(
    model: CustomModelInfo,
    isActive: Boolean,
    onClick: () -> Unit = {},
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 激活指示器
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column {
                Text(
                    text = model.name,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = "${model.category} · ${if (model.isRemote) "API" else "自定义"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 删除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 模型管理弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelManageDialog(
    models: List<CustomModelInfo>,
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onFetchModels: () -> Unit,
    isFetching: Boolean,
    onDeleteModel: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    
    // 分类列表
    val categories = remember(models) {
        listOf("全部") + models.map { it.category }.distinct().sorted()
    }
    
    // 筛选后的模型
    val filteredModels = remember(models, searchQuery, selectedCategory) {
        models.filter { model ->
            val matchesSearch = searchQuery.isBlank() || 
                model.name.contains(searchQuery, ignoreCase = true) ||
                model.id.contains(searchQuery, ignoreCase = true) ||
                model.group.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "全部" || model.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("模型管理")
                Text(
                    text = "共 ${models.size} 个模型",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜索模型 ID 或名称") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 分类标签
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory),
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 0.dp
                ) {
                    categories.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            text = { Text(category, fontSize = 12.sp) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 刷新按钮
                TextButton(
                    onClick = onFetchModels,
                    enabled = !isFetching,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("从API获取", fontSize = 12.sp)
                }
                
                // 模型列表
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredModels, key = { it.id }) { model ->
                        CustomModelListItem(
                            model = model,
                            isSelected = model.id == selectedModelId,
                            onClick = { onModelSelected(model.id) },
                            onDelete = { onDeleteModel(model.id) }
                        )
                    }
                    
                    if (filteredModels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "未找到匹配的模型",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 添加模型弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddModelDialog(
    onDismiss: () -> Unit,
    onAdd: (CustomModelInfo) -> Unit
) {
    var modelId by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var endpointType by remember { mutableStateOf("OpenAI") }
    
    val endpointTypes = listOf("OpenAI", "Anthropic", "Gemini", "Azure", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 模型ID
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("模型 ID *") },
                    placeholder = { Text("例如 gpt-3.5-turbo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 模型名称
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    placeholder = { Text("例如 GPT-3.5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 分组名称
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("分组名称") },
                    placeholder = { Text("例如 ChatGPT") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 端点类型
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = endpointType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("端点类型 *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        endpointTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    endpointType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modelId.isNotBlank()) {
                        onAdd(
                            CustomModelInfo(
                                id = modelId,
                                name = modelName.ifBlank { modelId },
                                group = groupName.ifBlank { "自定义" },
                                isRemote = false,
                                category = endpointType
                            )
                        )
                    }
                },
                enabled = modelId.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 自定义模型列表项
 */
@Composable
private fun CustomModelListItem(
    model: CustomModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${model.group} · ${model.id}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            // 来源标签
            Text(
                text = if (model.isRemote) "📡 API获取" else "✏️ 手动添加",
                fontSize = 10.sp,
                color = if (model.isRemote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        Row {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!model.isRemote) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 自定义模型信息数据类
 */
private data class CustomModelInfo(
    val id: String,
    val name: String,
    val group: String,
    val isRemote: Boolean,
    val category: String
)

/**
 * 根据模型ID分类
 */
private fun categorizeModel(modelId: String): String {
    val lowerId = modelId.lowercase()
    return when {
        lowerId.contains("gpt") || lowerId.contains("openai") -> "OpenAI"
        lowerId.contains("claude") -> "Claude"
        lowerId.contains("gemini") || lowerId.contains("google") -> "Gemini"
        lowerId.contains("llama") || lowerId.contains("meta") -> "Llama"
        lowerId.contains("qwen") -> "通义千问"
        lowerId.contains("deepseek") -> "DeepSeek"
        lowerId.contains("glm") -> "ChatGLM"
        lowerId.contains("mistral") -> "Mistral"
        lowerId.contains("yi") -> "Yi"
        else -> "其他"
    }
}
