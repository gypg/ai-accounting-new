package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.BudgetViewModel
import com.example.aiaccounting.utils.NumberUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "预算管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加预算")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (budgets.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无预算",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右下角添加预算",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // 预算列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(budgets) { budget ->
                        BudgetCard(
                            budget = budget,
                            onEdit = { viewModel.editBudget(budget) },
                            onDelete = { viewModel.deleteBudget(budget.id) }
                        )
                    }
                }
            }
        }
    }

    // 添加预算对话框
    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { categoryId, amount, period ->
                viewModel.addBudget(categoryId, amount, period)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun BudgetCard(
    budget: com.example.aiaccounting.data.local.entity.Budget,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (budget.amount > 0) {
        (budget.spent / budget.amount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val progressColor = when {
        progress < 0.5f -> MaterialTheme.colorScheme.primary
        progress < 0.8f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budget.categoryName ?: "未分类",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${budget.period.displayName}预算",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 进度条
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已用: ${NumberUtils.formatMoney(budget.spent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = progressColor
                )
                Text(
                    text = "预算: ${NumberUtils.formatMoney(budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (progress >= 1f) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ 预算已超支",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, com.example.aiaccounting.data.local.entity.BudgetPeriod) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf(com.example.aiaccounting.data.local.entity.BudgetPeriod.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加预算") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = it
                        }
                    },
                    label = { Text("预算金额") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("预算周期", style = MaterialTheme.typography.labelMedium)
                Row {
                    com.example.aiaccounting.data.local.entity.BudgetPeriod.values().forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.displayName) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > 0) {
                        onConfirm(selectedCategory ?: 0, amountValue, selectedPeriod)
                    }
                },
                enabled = amount.isNotEmpty()
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
