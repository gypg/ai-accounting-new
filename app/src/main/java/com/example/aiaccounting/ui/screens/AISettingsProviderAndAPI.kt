package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.data.model.AIModel
import com.example.aiaccounting.data.model.AIProvider
import com.example.aiaccounting.data.service.RemoteModel
import com.example.aiaccounting.ui.viewmodel.NetworkSpeedTestUiResult
import com.example.aiaccounting.utils.ModelIdCategorizer.categorizeModelId

/**
 * AISettingsScreen 提供商与API配置组件
 *
 * 包含以下组件：
 * - ProviderCard: AI提供商选择卡片
 * - ProviderItem: 提供商列表项
 * - APIConfigCard: API配置卡片
 * - TestResultCard: 测试结果卡片
 */

@Composable
internal fun ProviderCard(
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
internal fun ProviderItem(
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
internal fun APIConfigCard(
    apiKey: String,
    apiUrl: String,
    model: String,
    provider: AIProvider,
    remoteModels: List<RemoteModel>,
    isFetchingModels: Boolean,
    showApiKey: Boolean,
    isAuto: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    onShowApiKeyChange: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onFetchModels: () -> Unit,
    onTestModelConnection: ((String, (ModelTestResult) -> Unit) -> Unit)? = null
) {
    // 使用远程模型列表（如果有）或默认模型列表
    val modelsToShow = if (remoteModels.isNotEmpty()) {
        remoteModels.map {
            AIModel(
                id = it.id,
                displayName = if (it.name.isBlank()) it.id else it.name,
                description = it.description,
                category = categorizeModelId(it.id)
            )
        }
    } else {
        provider.models
    }

    val fixedSelectedModelId = if (isAuto) "" else model

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

            // 模型模式（普通用户）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "模型模式",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isAuto) "自动切换可用模型" else "固定使用指定模型",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isAuto,
                    onCheckedChange = onToggleAuto
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isAuto) {
                Text(
                    text = "当前偏好：Auto（自动选择第一个可用模型，无需手动指定）",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 自定义提供商使用新的模型管理方式
            if (provider == AIProvider.CUSTOM) {
                if (!isAuto) {
                    CustomModelManager(
                        apiKey = apiKey,
                        remoteModels = remoteModels,
                        isFetchingModels = isFetchingModels,
                        selectedModelId = model,
                        onModelSelected = onModelChange,
                        onFetchModels = onFetchModels
                    )
                }
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

                if (!isAuto) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 模型选择（分类 + 搜索）
                    CategorizedModelSelector(
                        models = modelsToShow,
                        selectedModelId = fixedSelectedModelId,
                        onModelSelected = onModelChange,
                        onTestConnection = onTestModelConnection
                    )
                }
            }
        }
    }
}

@Composable
internal fun NetworkSpeedTestResultCard(
    result: NetworkSpeedTestUiResult,
    onDismiss: () -> Unit
) {
    when (result) {
        is NetworkSpeedTestUiResult.Success -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "最快节点：${result.fastestLabel}（${result.latencyMs} ms）",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    result.measuredTargets.forEach { target ->
                        Text(
                            text = if (target.latencyMs != null) {
                                "• ${target.label}: ${target.latencyMs} ms（${target.message}）"
                            } else {
                                "• ${target.label}: ${target.message}"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        is NetworkSpeedTestUiResult.Error -> {
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TestResultCard(
    result: com.example.aiaccounting.ui.viewmodel.TestResult,
    onDismiss: () -> Unit
) {
    when (result) {
        is com.example.aiaccounting.ui.viewmodel.TestResult.Success -> {
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "连接成功！AI助手可以正常使用。",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        is com.example.aiaccounting.ui.viewmodel.TestResult.Error -> {
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
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = result.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
