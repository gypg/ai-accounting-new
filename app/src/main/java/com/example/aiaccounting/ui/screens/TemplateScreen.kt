package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.TransactionTemplate
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.viewmodel.TemplateViewModel
import com.example.aiaccounting.utils.NumberUtils

/**
 * 模板记账界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (Long) -> Unit,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<TransactionTemplate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("快记模板") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加模板")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (templates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无模板",
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右上角添加常用记账模板",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onClick = { onNavigateToAddTransaction(template.id) },
                            onEdit = { editingTemplate = template },
                            onDelete = { viewModel.deleteTemplate(template) }
                        )
                    }
                }
            }
        }
    }

    // 添加/编辑模板对话框
    if (showAddDialog || editingTemplate != null) {
        TemplateDialog(
            template = editingTemplate,
            onDismiss = {
                showAddDialog = false
                editingTemplate = null
            },
            onConfirm = { template ->
                val currentEditingTemplate = editingTemplate
                if (currentEditingTemplate != null) {
                    viewModel.updateTemplate(template.copy(id = currentEditingTemplate.id))
                } else {
                    viewModel.addTemplate(template)
                }
                showAddDialog = false
                editingTemplate = null
            }
        )
    }
}

@Composable
fun TemplateCard(
    template: TransactionTemplate,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Text(
                text = template.icon,
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = template.note,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // 金额
            Text(
                text = "${if (template.type == TransactionType.INCOME) "+" else "-"}${NumberUtils.formatMoney(template.amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (template.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            // 操作按钮
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
fun TemplateDialog(
    template: TransactionTemplate?,
    onDismiss: () -> Unit,
    onConfirm: (TransactionTemplate) -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var amount by remember { mutableStateOf(template?.amount?.toString() ?: "") }
    var type by remember { mutableStateOf(template?.type ?: TransactionType.EXPENSE) }
    var note by remember { mutableStateOf(template?.note ?: "") }
    var icon by remember { mutableStateOf(template?.icon ?: "💰") }

    val icons = listOf("💰", "🍔", "🚗", "🛒", "🎮", "💊", "📚", "🏠", "📱", "☕", "🍞", "🍱", "🚇", "🎁", "📈", "💼")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "添加模板" else "编辑模板") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模板名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("金额") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "支出",
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE }
                    )
                    FilterChip(
                        label = "收入",
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 图标选择
                Text(
                    text = "选择图标",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (icon == emoji) Color(0xFF2196F3) else Color(0xFFF5F5F5)
                                )
                                .clickable { icon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(
                        TransactionTemplate(
                            name = name,
                            amount = amountValue,
                            type = type,
                            categoryId = 1, // 默认分类
                            accountId = 1,  // 默认账户
                            note = note,
                            icon = icon
                        )
                    )
                },
                enabled = name.isNotBlank() && amount.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


