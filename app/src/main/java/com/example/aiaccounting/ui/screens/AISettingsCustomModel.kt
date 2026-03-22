package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.aiaccounting.data.service.RemoteModel
import com.example.aiaccounting.utils.ModelIdCategorizer.categorizeModelId

/**
 * AISettingsScreen 自定义模型管理组件
 *
 * 包含以下组件：
 * - CustomModelManager: 自定义模型管理器（Cherry Studio风格）
 * - SelectedModelItem: 已选择模型项
 * - ModelManageDialog: 模型管理弹窗
 * - AddModelDialog: 添加模型弹窗
 * - CustomModelListItem: 自定义模型列表项
 * - CustomModelInfo: 自定义模型信息数据类
 */

/**
 * 自定义模型信息数据类
 */
internal data class CustomModelInfo(
    val id: String,
    val name: String,
    val group: String,
    val isRemote: Boolean,
    val category: String
)

/**
 * 自定义模型管理器 - Cherry Studio风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomModelManager(
    apiKey: String,
    remoteModels: List<RemoteModel>,
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
                group = it.description,
                isRemote = true,
                category = categorizeModelId(it.id)
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

    // 分类列表
    val categories = remember(models) {
        val raw = models.map { it.category }.distinct()
        val ordered = preferredCategoryOrder.filter { it in raw }
        val remaining = (raw - preferredCategoryOrder.toSet()).sorted()
        listOf("全部") + ordered + remaining
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
