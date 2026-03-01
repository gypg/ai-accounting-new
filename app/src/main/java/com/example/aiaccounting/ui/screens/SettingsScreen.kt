package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "设置") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 安全设置
            SecuritySettingsSection(
                isBiometricEnabled = uiState.isBiometricEnabled,
                onBiometricToggle = { viewModel.toggleBiometric(it) },
                onChangePin = { viewModel.showChangePinDialog() }
            )

            // AI配置
            AISettingsSection(
                isAIConfigured = uiState.isAIConfigured,
                aiModel = uiState.aiModel,
                onAIConfig = { viewModel.showAIConfigDialog() }
            )

            // 数据备份
            BackupSettingsSection(
                onBackup = { viewModel.backupData() },
                onRestore = { viewModel.showRestoreDialog() },
                onExport = { viewModel.exportData() }
            )

            // 关于
            AboutSection(
                version = uiState.appVersion,
                onAbout = { viewModel.showAboutDialog() }
            )
        }
    }

    // 修改PIN对话框
    if (uiState.showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { viewModel.hideChangePinDialog() },
            onConfirm = { oldPin, newPin ->
                viewModel.changePin(oldPin, newPin)
            }
        )
    }

    // AI配置对话框
    if (uiState.showAIConfigDialog) {
        AISettingsDialog(
            currentConfig = uiState.aiConfiguration,
            onDismiss = { viewModel.hideAIConfigDialog() },
            onSave = { config ->
                viewModel.saveAIConfiguration(config)
            }
        )
    }

    // 恢复数据对话框
    if (uiState.showRestoreDialog) {
        RestoreDataDialog(
            onDismiss = { viewModel.hideRestoreDialog() },
            onRestore = { viewModel.restoreData() }
        )
    }

    // 关于对话框
    if (uiState.showAboutDialog) {
        AboutDialog(
            onDismiss = { viewModel.hideAboutDialog() }
        )
    }
}

@Composable
fun SecuritySettingsSection(
    isBiometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onChangePin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "安全设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 生物识别开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "生物识别",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "使用指纹或面部识别快速解锁",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = onBiometricToggle
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // 修改PIN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangePin() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "修改PIN码",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "更改应用解锁密码",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.rotate(180f)
                )
            }
        }
    }
}

@Composable
fun AISettingsSection(
    isAIConfigured: Boolean,
    aiModel: String,
    onAIConfig: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "AI配置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAIConfig() },
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
                            text = "AI服务配置",
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isAIConfigured) "已配置 - $aiModel" else "未配置",
                            fontSize = 12.sp,
                            color = if (isAIConfigured) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.rotate(180f)
                )
            }
        }
    }
}

@Composable
fun BackupSettingsSection(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "数据管理",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 备份数据
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackup() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "备份数据",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "创建数据备份文件",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.rotate(180f)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // 恢复数据
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRestore() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "恢复数据",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "从备份文件恢复数据",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.rotate(180f)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // 导出Excel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExport() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "导出Excel",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "导出账单为Excel文件",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.rotate(180f)
                )
            }
        }
    }
}

@Composable
fun AboutSection(
    version: String,
    onAbout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "关于",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAbout() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "关于应用",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "版本 $version",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.rotate(180f)
                )
            }
        }
    }
}

// 对话框组件
@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改PIN码") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { 
                        if (it.length <= 6) oldPin = it
                        error = null
                    },
                    label = { Text("当前PIN码") },
                    singleLine = true,
                    isError = error != null
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { 
                        if (it.length <= 6) newPin = it
                        error = null
                    },
                    label = { Text("新PIN码") },
                    singleLine = true,
                    isError = error != null
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { 
                        if (it.length <= 6) confirmPin = it
                        error = null
                    },
                    label = { Text("确认新PIN码") },
                    singleLine = true,
                    isError = error != null
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        oldPin.isBlank() || newPin.isBlank() || confirmPin.isBlank() -> {
                            error = "请填写所有字段"
                        }
                        newPin != confirmPin -> {
                            error = "两次输入的新PIN码不一致"
                        }
                        newPin.length < 4 -> {
                            error = "PIN码至少需要4位"
                        }
                        else -> {
                            onConfirm(oldPin, newPin)
                        }
                    }
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AISettingsDialog(
    currentConfig: AIConfiguration?,
    onDismiss: () -> Unit,
    onSave: (AIConfiguration) -> Unit
) {
    var apiKey by remember { mutableStateOf(currentConfig?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(currentConfig?.baseUrl ?: "https://api.openai.com/v1/") }
    var model by remember { mutableStateOf(currentConfig?.model ?: "gpt-3.5-turbo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI配置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("API地址") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(AIConfiguration(apiKey, baseUrl, model))
                },
                enabled = apiKey.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun RestoreDataDialog(
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复数据") },
        text = { Text("恢复数据将覆盖当前所有数据，确定要继续吗？") },
        confirmButton = {
            TextButton(
                onClick = onRestore,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("恢复")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于AI记账") },
        text = {
            Column {
                Text("AI记账是一款智能的个人财务管理应用。")
                Spacer(modifier = Modifier.height(8.dp))
                Text("主要功能：")
                Text("• 智能记账助手")
                Text("• 数据安全加密")
                Text("• 多维度统计分析")
                Text("• Excel导出")
                Spacer(modifier = Modifier.height(8.dp))
                Text("版本：1.0.0")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

// 数据类
data class AIConfiguration(
    val apiKey: String,
    val baseUrl: String,
    val model: String
)
