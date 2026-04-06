package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.BudgetProgress
import com.example.aiaccounting.ui.components.AddTransactionMenu
import com.example.aiaccounting.ui.viewmodel.OverviewViewModel
import com.example.aiaccounting.utils.NumberUtils
import java.util.*

/**
 * 总览页面 - 模仿图1设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAI: () -> Unit,
    onNavigateToButlerMarket: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToTransactions: (Int, Int, Int?) -> Unit = { _, _, _ -> },
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCalendar: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToYearlyWealth: (Int) -> Unit = {},
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val transactions by viewModel.recentTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val yearlyTrendData by viewModel.yearlyTrendData.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()
    val yearlyBudgetProgress by viewModel.yearlyBudgetProgress.collectAsState()
    val totalBudgetProgress by viewModel.totalBudgetProgress.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.logOverviewScreenEnter(
            theme = "default",
            selectedYear = selectedYear,
            accountCount = accounts.size,
            categoryCount = categories.size,
            recentTransactionCount = transactions.size
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshCurrentYearMonth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val totalAssets by remember(accounts) { derivedStateOf { accounts.sumOf { it.balance } } }
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    // 底部菜单状态
    var showAddMenu by remember { mutableStateOf(false) }

    // 底部弹出菜单
    AddTransactionMenu(
        isVisible = showAddMenu,
        onDismiss = { showAddMenu = false },
        onAIAccounting = {
            viewModel.logOverviewEntrySelected(theme = "default", entry = "ai_accounting")
            onNavigateToAI()
        },
        onManualAccounting = {
            viewModel.logOverviewEntrySelected(theme = "default", entry = "manual_accounting")
            onNavigateToAddTransaction()
        }
    )

    Scaffold(
        floatingActionButton = {
            // AI+ 按钮 - 整合机器人和记账功能
            FloatingActionButton(
                onClick = {
                    viewModel.logOverviewAddMenuOpened(theme = "default")
                    showAddMenu = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "AI+",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // 总资产卡片
            TotalAssetsCard(
                totalAssets = totalAssets,
                onEyeClick = { viewModel.toggleBalanceVisibility() },
                isVisible = uiState.isBalanceVisible
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToButlerMarket),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI管家", fontWeight = FontWeight.Bold)
                            val butlerName by viewModel.currentButlerName.collectAsState()
                            val subtitle = if (butlerName.isBlank()) {
                                "选择 / 创建自定义管家"
                            } else {
                                "当前：$butlerName（点击切换/创建）"
                            }
                            Text(
                                subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 年度统计
            YearlyStatsCard(
                year = selectedYear,
                income = monthlyStats.totalIncome,
                expense = monthlyStats.totalExpense,
                balance = monthlyStats.totalIncome - monthlyStats.totalExpense,
                onPreviousYear = { viewModel.setSelectedYear(selectedYear - 1) },
                onNextYear = { viewModel.setSelectedYear(selectedYear + 1) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 月历入口
            CalendarOverviewCard(
                year = selectedYear,
                month = currentMonth,
                day = currentDay,
                onClick = { onNavigateToCalendar(selectedYear, currentMonth) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 月度概览和账户明细
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MonthlyOverviewCard(
                    year = selectedYear,
                    month = currentMonth,
                    income = monthlyStats.monthlyIncome,
                    expense = monthlyStats.monthlyExpense,
                    onClick = {
                        onNavigateToTransactions(selectedYear, currentMonth, null)
                    },
                    modifier = Modifier.weight(1f)
                )

                AccountsOverviewCard(
                    accounts = accounts.take(4),
                    onClick = onNavigateToAccounts,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BudgetOverviewCard(
                title = "${selectedYear}年度预算",
                budgetProgress = yearlyBudgetProgress,
                onClick = onNavigateToBudgets
            )

            Spacer(modifier = Modifier.height(12.dp))

            BudgetOverviewCard(
                title = "${selectedYear}年${currentMonth}月预算",
                budgetProgress = totalBudgetProgress,
                onClick = onNavigateToBudgets
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 当天和当周统计
            TodayWeekStatsCard(
                todayIncome = todayStats.income,
                todayExpense = todayStats.expense,
                weekIncome = weekStats.income,
                weekExpense = weekStats.expense
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 年度趋势图
            YearlyTrendCard(
                year = selectedYear,
                monthlyData = yearlyTrendData,
                onClick = { onNavigateToYearlyWealth(selectedYear) },
                onMonthClick = { month ->
                    onNavigateToTransactions(selectedYear, month, null)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TotalAssetsCard(
    totalAssets: Double,
    onEyeClick: () -> Unit,
    isVisible: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2196F3)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "总资产",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isVisible) NumberUtils.formatMoney(totalAssets) else "****",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 眼睛图标
            IconButton(
                onClick = onEyeClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isVisible) "隐藏" else "显示",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun YearlyStatsCard(
    year: Int,
    income: Double,
    expense: Double,
    balance: Double,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 年份标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousYear) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上一年")
                }
                Text(
                    text = "${year}年度",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onNextYear) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下一年")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 收入支出结余
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("收入", income, Color(0xFF4CAF50))
                StatColumn("支出", expense, Color(0xFFF44336))
                StatColumn("结余", balance, Color(0xFF2196F3))
            }
        }
    }
}

@Composable
fun StatColumn(label: String, amount: Double, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = NumberUtils.formatMoney(amount),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CalendarOverviewCard(
    year: Int,
    month: Int,
    day: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "月历",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${year}年${month}月 · 今天${day}日",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击查看真正月历页",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MonthlyOverviewCard(
    year: Int,
    month: Int,
    income: Double,
    expense: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "月度概览",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${year}年${month}月",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 收入标签
            Box(
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+${NumberUtils.formatMoney(income)}",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 支出标签
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "-${NumberUtils.formatMoney(expense)}",
                    fontSize = 12.sp,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun AccountsOverviewCard(
    accounts: List<com.example.aiaccounting.data.local.entity.Account>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "账户明细",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (accounts.isEmpty()) {
                Text(
                    text = "暂无账户，点击查看账户列表",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(account.color)),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = account.name,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = NumberUtils.formatMoney(account.balance),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetOverviewCard(
    title: String,
    budgetProgress: BudgetProgress?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (budgetProgress == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "该年度当前月份未设置预算",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击进入预算管理后即可查看该年份当月剩余额度",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Card
        }

        val percentage = (budgetProgress.percentage * 100).toInt()
        val progressColor = when {
            budgetProgress.isOverBudget -> Color(0xFFF44336)
            percentage >= 80 -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = NumberUtils.formatMoney(budgetProgress.budget.amount),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BudgetValuePill(
                    label = "已用",
                    value = NumberUtils.formatMoney(budgetProgress.spent),
                    containerColor = Color(0xFFFFEBEE),
                    contentColor = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                BudgetValuePill(
                    label = if (budgetProgress.isOverBudget) "超支" else "剩余",
                    value = NumberUtils.formatMoney(kotlin.math.abs(budgetProgress.remaining)),
                    containerColor = if (budgetProgress.isOverBudget) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                    contentColor = if (budgetProgress.isOverBudget) Color(0xFFFF9800) else Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { budgetProgress.percentage },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (budgetProgress.isOverBudget) {
                    "本月已超支 ${NumberUtils.formatMoney(kotlin.math.abs(budgetProgress.remaining))} · 已使用 $percentage%"
                } else {
                    "本月已使用 $percentage%"
                },
                fontSize = 12.sp,
                color = progressColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BudgetValuePill(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
fun TodayWeekStatsCard(
    todayIncome: Double,
    todayExpense: Double,
    weekIncome: Double,
    weekExpense: Double
) {
    // 计算本周日期范围（与ViewModel使用相同的逻辑）
    val today = Calendar.getInstance()
    val currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
    // 如果今天是周日，DAY_OF_WEEK=1，需要回退6天；其他情况回退到周一
    val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
    
    val monday = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_MONTH, -daysFromMonday)
    }
    
    val sunday = Calendar.getInstance().apply {
        timeInMillis = monday.timeInMillis
        add(Calendar.DAY_OF_MONTH, 6)
    }
    
    val dateFormat = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
    val weekRange = "${dateFormat.format(monday.time)} - ${dateFormat.format(sunday.time)}"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "收支概览",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 今天统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "今天",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "收入: +¥${String.format("%.2f", todayIncome)}",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "支出: -¥${String.format("%.2f", todayExpense)}",
                        fontSize = 14.sp,
                        color = Color(0xFFF44336)
                    )
                }

                // 本周统计
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "本周 ($weekRange)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "+¥${String.format("%.2f", weekIncome)}",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "-¥${String.format("%.2f", weekExpense)}",
                        fontSize = 14.sp,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun YearlyTrendCard(
    year: Int,
    monthlyData: List<com.example.aiaccounting.ui.components.charts.MonthlyData> = emptyList(),
    onClick: () -> Unit = {},
    onMonthClick: (Int) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("yearly_trend_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${year}年度趋势",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 使用真实数据的趋势图
            if (monthlyData.isNotEmpty()) {
                com.example.aiaccounting.ui.components.charts.TrendChart(
                    data = monthlyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    showIncome = true,
                    showExpense = true,
                    onDataPointClick = { selectedData ->
                        parseTrendMonth(selectedData.month)?.let(onMonthClick)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无数据",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 图例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItem("收入", Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem("支出", Color(0xFFF44336))
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem("结余", Color(0xFF2196F3))
            }
        }
    }
}

internal fun parseTrendMonth(label: String): Int? {
    val month = label.trim().removeSuffix("月").toIntOrNull() ?: return null
    return month.takeIf { it in 1..12 }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
