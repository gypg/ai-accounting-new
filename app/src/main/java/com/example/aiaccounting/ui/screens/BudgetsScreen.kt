package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.aiaccounting.data.local.entity.BudgetProgress
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
    val totalBudget by viewModel.totalBudget.collectAsState()
    val alertBudgets by viewModel.alertBudgets.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // 显示消息
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // 可以在这里显示Snackbar
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "预算管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 月份选择器
            MonthSelector(
                year = currentYear,
                month = currentMonth,
                onYearMonthChanged = { year, month ->
                    viewModel.setYearMonth(year, month)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 总预算卡片
            totalBudget?.let { budget ->
                TotalBudgetCard(budgetProgress = budget)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 超支提醒
            if (alertBudgets.isNotEmpty()) {
                AlertCard(alertBudgets = alertBudgets)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 预算列表
            Text(
                text = "预算列表",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无预算设置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(budgets) { budgetProgress ->
                        BudgetItem(budgetProgress = budgetProgress)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // 添加预算对话框
    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { amount ->
                viewModel.createTotalBudget(amount)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MonthSelector(
    year: Int,
    month: Int,
    onYearMonthChanged: (Int, Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showPicker = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${year}年${month}月",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.CalendarToday, contentDescription = "选择月份")
        }
    }
}

@Composable
fun TotalBudgetCard(budgetProgress: BudgetProgress) {
    val percentage = (budgetProgress.percentage * 100).toInt()
    val color = when {
        budgetProgress.isOverBudget -> Color(0xFFF44336)
        percentage >= 80 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "月度总预算",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${NumberUtils.formatMoney(budgetProgress.spent)} / ${NumberUtils.formatMoney(budgetProgress.budget.amount)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = budgetProgress.percentage,
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "已使用 $percentage%",
                fontSize = 12.sp,
                color = color
            )
        }
    }
}

@Composable
fun AlertCard(alertBudgets: List<BudgetProgress>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "预算提醒",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            alertBudgets.forEach { budget ->
                val message = if (budget.isOverBudget) {
                    "${budget.budget.name}已超支${NumberUtils.formatMoney(budget.spent - budget.budget.amount)}"
                } else {
                    "${budget.budget.name}已使用${(budget.percentage * 100).toInt()}%"
                }
                Text(
                    text = "• $message",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun BudgetItem(budgetProgress: BudgetProgress) {
    val percentage = (budgetProgress.percentage * 100).toInt()
    val color = when {
        budgetProgress.isOverBudget -> Color(0xFFF44336)
        percentage >= 80 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
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
                Text(
                    text = budgetProgress.budget.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$percentage%",
                    fontSize = 14.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${NumberUtils.formatMoney(budgetProgress.spent)} / ${NumberUtils.formatMoney(budgetProgress.budget.amount)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = budgetProgress.percentage,
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置月度预算") },
        text = {
            Column {
                Text("请输入本月总预算金额")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("预算金额") },
                    prefix = { Text("¥") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > 0) {
                        onConfirm(amountValue)
                    }
                }
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
