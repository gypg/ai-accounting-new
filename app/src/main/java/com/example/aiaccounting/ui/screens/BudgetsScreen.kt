package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.aiaccounting.ui.components.FlowRow
import com.example.aiaccounting.ui.viewmodel.BudgetViewModel
import com.example.aiaccounting.utils.NumberUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val availableBudgetMonths by viewModel.availableBudgetMonths.collectAsState()
    val yearlyTotalBudget by viewModel.yearlyTotalBudget.collectAsState()
    val totalBudget by viewModel.totalBudget.collectAsState()
    val alertBudgets by viewModel.alertBudgets.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    var showAddMonthlyDialog by remember { mutableStateOf(false) }
    var showEditMonthlyDialog by remember { mutableStateOf(false) }
    var showDeleteMonthlyDialog by remember { mutableStateOf(false) }
    var showAddYearlyDialog by remember { mutableStateOf(false) }
    var showEditYearlyDialog by remember { mutableStateOf(false) }
    var showDeleteYearlyDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.logBudgetScreenEnter(
            year = currentYear,
            month = currentMonth,
            budgetCount = budgets.size,
            hasTotalBudget = totalBudget != null
        )
    }
    LaunchedEffect(currentYear, yearlyTotalBudget?.budget?.id, yearlyTotalBudget?.budget?.amount) {
        viewModel.logYearlyBudgetLoaded(
            year = currentYear,
            hasBudget = yearlyTotalBudget != null,
            amount = yearlyTotalBudget?.budget?.amount
        )
    }
    LaunchedEffect(currentYear, currentMonth, totalBudget?.budget?.id, totalBudget?.budget?.amount) {
        viewModel.logMonthlyBudgetLoaded(
            year = currentYear,
            month = currentMonth,
            hasBudget = totalBudget != null,
            amount = totalBudget?.budget?.amount
        )
    }

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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (yearlyTotalBudget == null) {
                    FloatingActionButton(
                        onClick = { showAddYearlyDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "添加年度预算")
                    }
                }
                if (totalBudget == null) {
                    FloatingActionButton(
                        onClick = { showAddMonthlyDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加月度预算")
                    }
                }
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
                onClick = { showMonthPicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            yearlyTotalBudget?.let { budget ->
                TotalBudgetCard(
                    title = "年度总预算",
                    budgetProgress = budget,
                    onEdit = {
                        viewModel.logBudgetEditRequested(
                            year = currentYear,
                            month = 0,
                            currentAmount = budget.budget.amount
                        )
                        showEditYearlyDialog = true
                    },
                    onDelete = {
                        viewModel.logBudgetDeleteRequested(
                            year = currentYear,
                            month = 0,
                            currentAmount = budget.budget.amount
                        )
                        showDeleteYearlyDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 总预算卡片
            totalBudget?.let { budget ->
                TotalBudgetCard(
                    title = "月度总预算",
                    budgetProgress = budget,
                    onEdit = {
                        viewModel.logBudgetEditRequested(
                            year = currentYear,
                            month = currentMonth,
                            currentAmount = budget.budget.amount
                        )
                        showEditMonthlyDialog = true
                    },
                    onDelete = {
                        viewModel.logBudgetDeleteRequested(
                            year = currentYear,
                            month = currentMonth,
                            currentAmount = budget.budget.amount
                        )
                        showDeleteMonthlyDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 超支提醒
            if (alertBudgets.isNotEmpty()) {
                AlertCard(alertBudgets = alertBudgets)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 预算列表
            Text(
                text = "预算列表（年度 + 月度）",
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
                    items(budgets, key = { it.budget.id }) { budgetProgress ->
                        BudgetItem(budgetProgress = budgetProgress)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showAddYearlyDialog) {
        AddBudgetDialog(
            title = "设置年度预算",
            initialAmount = null,
            onDismiss = { showAddYearlyDialog = false },
            onConfirm = { amount ->
                viewModel.createYearlyTotalBudget(amount)
                showAddYearlyDialog = false
            }
        )
    }

    if (showAddMonthlyDialog) {
        AddBudgetDialog(
            title = "设置月度预算",
            initialAmount = null,
            onDismiss = { showAddMonthlyDialog = false },
            onConfirm = { amount ->
                viewModel.createTotalBudget(amount)
                showAddMonthlyDialog = false
            }
        )
    }

    if (showEditYearlyDialog) {
        yearlyTotalBudget?.let { budget ->
            AddBudgetDialog(
                title = "修改年度预算",
                initialAmount = budget.budget.amount,
                onDismiss = { showEditYearlyDialog = false },
                onConfirm = { amount ->
                    viewModel.updateBudget(budget.budget.copy(amount = amount))
                    showEditYearlyDialog = false
                }
            )
        }
    }

    if (showEditMonthlyDialog) {
        totalBudget?.let { budget ->
            AddBudgetDialog(
                title = "修改月度预算",
                initialAmount = budget.budget.amount,
                onDismiss = { showEditMonthlyDialog = false },
                onConfirm = { amount ->
                    viewModel.updateBudget(budget.budget.copy(amount = amount))
                    showEditMonthlyDialog = false
                }
            )
        }
    }

    if (showDeleteYearlyDialog) {
        yearlyTotalBudget?.let { budget ->
            AlertDialog(
                onDismissRequest = { showDeleteYearlyDialog = false },
                title = { Text("删除年度预算") },
                text = { Text("确认删除${currentYear}年总预算吗？删除后可重新添加。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBudget(budget.budget)
                            showDeleteYearlyDialog = false
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteYearlyDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }

    if (showDeleteMonthlyDialog) {
        totalBudget?.let { budget ->
            AlertDialog(
                onDismissRequest = { showDeleteMonthlyDialog = false },
                title = { Text("删除月度预算") },
                text = { Text("确认删除${currentYear}年${currentMonth}月总预算吗？删除后可重新添加。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBudget(budget.budget)
                            showDeleteMonthlyDialog = false
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteMonthlyDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initialYear = currentYear,
            initialMonth = currentMonth,
            availableMonths = availableBudgetMonths,
            onDismiss = { showMonthPicker = false },
            onConfirm = { year, month ->
                viewModel.setYearMonth(year, month)
                showMonthPicker = false
            }
        )
    }
}

@Composable
fun MonthSelector(
    year: Int,
    month: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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
fun TotalBudgetCard(
    title: String,
    budgetProgress: BudgetProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEdit) {
                        Text("修改")
                    }
                    TextButton(onClick = onDelete) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

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
fun MonthPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    availableMonths: List<Pair<Int, Int>>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedYear by remember(initialYear) { mutableStateOf(initialYear.toString()) }
    var selectedMonth by remember(initialMonth) { mutableStateOf(initialMonth.toString()) }
    var displayYear by remember(initialYear) { mutableIntStateOf(initialYear) }
    val monthAvailability = remember(availableMonths, displayYear) {
        availableMonths.filter { it.first == displayYear }.map { it.second }.toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择年月") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "可视选择",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayYear -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一年")
                    }
                    Text(
                        text = "${displayYear}年",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { displayYear += 1 }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一年")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1..4, 5..8, 9..12).forEach { range ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            range.forEach { month ->
                                val selected = selectedYear == displayYear.toString() && selectedMonth == month.toString()
                                val available = monthAvailability.contains(month)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedYear = displayYear.toString()
                                        selectedMonth = month.toString()
                                    },
                                    label = { Text("${month}月") },
                                    leadingIcon = if (available) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "手动输入",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = selectedYear,
                    onValueChange = {
                        selectedYear = it.filter(Char::isDigit).take(4)
                        selectedYear.toIntOrNull()?.let { year -> displayYear = year }
                    },
                    label = { Text("年份") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = { selectedMonth = it.filter(Char::isDigit).take(2) },
                    label = { Text("月份") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val year = selectedYear.toIntOrNull()
                    val month = selectedMonth.toIntOrNull()
                    if (year != null && month != null && month in 1..12) {
                        onConfirm(year, month)
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

@Composable
fun AddBudgetDialog(
    title: String,
    initialAmount: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember(initialAmount) {
        mutableStateOf(initialAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
