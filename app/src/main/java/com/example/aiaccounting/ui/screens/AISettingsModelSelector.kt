package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.data.model.AIModel
import com.example.aiaccounting.data.repository.AIConfigRepository
import com.example.aiaccounting.data.service.RemoteModel
import com.example.aiaccounting.utils.ModelIdCategorizer.categorizeModelId

/**
 * AISettingsScreen 模型选择组件
 *
 * 包含以下组件：
 * - InviteModelSelectorCard: 邀请码模式下的模型选择卡片
 * - CategorizedModelSelector: 分类模型选择器（支持搜索和分类过滤）
 * - ModelListItem: 模型列表项
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InviteModelSelectorCard(
    isAuto: Boolean,
    selectedModelId: String,
    remoteModels: List<RemoteModel>,
    isFetchingModels: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    onFetchModels: () -> Unit,
    onModelSelected: (String) -> Unit,
    useBuiltinConfig: Boolean
) {
    val modelsToShow = remember(remoteModels) {
        remoteModels.map {
            AIModel(
                id = it.id,
                displayName = if (it.name.isBlank()) it.id else it.name,
                description = it.description,
                category = categorizeModelId(it.id)
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "模型设置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isAuto) "自动切换可用模型" else "手动选择固定模型",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isAuto,
                    onCheckedChange = { onToggleAuto(it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onFetchModels,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFetchingModels
            ) {
                if (isFetchingModels) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("获取中...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("获取模型列表")
                }
            }

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

                // 邀请码模式也支持分类+搜索，和自定义模式保持一致
                CategorizedModelSelector(
                    models = modelsToShow,
                    selectedModelId = selectedModelId,
                    onModelSelected = onModelSelected
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                val modeText = if (useBuiltinConfig) {
                    "当前偏好：Auto（默认模型已开启，当前对话将优先使用本地AI）"
                } else {
                    "当前偏好：Auto（邀请码云端模型优先，推荐 openai/gpt-oss-120b；若不可用将自动切换）"
                }
                Text(
                    text = modeText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategorizedModelSelector(
    models: List<AIModel>,
    selectedModelId: String,
    onModelSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }

    val selectedModel = models.find { it.id == selectedModelId }

    val preferredCategoryOrder = remember {
        listOf(
            "OpenAI",
            "Claude",
            "Gemini",
            "DeepSeek",
            "通义千问",
            "Llama",
            "Mistral",
            "ChatGLM",
            "Yi",
            "其他"
        )
    }

    val categories = remember(models) {
        val raw = models.map { it.category }.distinct()
        val ordered = preferredCategoryOrder.filter { it in raw }
        val remaining = (raw - preferredCategoryOrder.toSet()).sorted()
        listOf("全部") + ordered + remaining
    }

    val filteredModels = remember(models, searchQuery, selectedCategory) {
        val rawQuery = searchQuery.trim()
        val tokens = rawQuery
            .lowercase()
            .replace(Regex("[-_./]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        models.filter { model ->
            val searchable = (model.displayName + " " + model.id + " " + model.description)
                .lowercase()
                .replace(Regex("[-_./]"), " ")

            val matchesSearch = tokens.isEmpty() || tokens.all { token ->
                searchable.contains(token)
            }
            val matchesCategory = selectedCategory == "全部" || model.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

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
                        text = "${selectedModel.category} · ${selectedModel.description}",
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                searchQuery = ""
                selectedCategory = "全部"
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索模型") },
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "找到 ${filteredModels.size} 个结果",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(filteredModels, key = { it.id }) { aiModel ->
                            ModelListItem(
                                model = aiModel,
                                isSelected = aiModel.id == selectedModelId,
                                onClick = {
                                    onModelSelected(aiModel.id)
                                    showDialog = false
                                    searchQuery = ""
                                    selectedCategory = "全部"
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
                                    Text(
                                        text = "未找到匹配模型",
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
                TextButton(onClick = {
                    showDialog = false
                    searchQuery = ""
                    selectedCategory = "全部"
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
internal fun ModelListItem(
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
