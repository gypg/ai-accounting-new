package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.components.NewYearHorseBackground
import com.example.aiaccounting.ui.theme.NewYearHorseThemeColors
import com.example.aiaccounting.ui.viewmodel.OverviewViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewYearHorseOverviewScreen(
    viewModel: OverviewViewModel = hiltViewModel(),
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToAI: () -> Unit = {},
    onNavigateToButlerMarket: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {}
) {
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val yearlyTrendData by viewModel.yearlyTrendData.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    // Safe theme colors with fallback defaults (prevent crash on first theme switch)
    // Check if theme colors are properly initialized by testing if primary color is not 0
    val safePrimary = if (NewYearHorseThemeColors.primary.value != 0UL) NewYearHorseThemeColors.primary else Color(0xFFD64040)
    val safeOnSurface = if (NewYearHorseThemeColors.onSurface.value != 0UL) NewYearHorseThemeColors.onSurface else Color(0xFF1E293B)
    val safeOnSurfaceVariant = if (NewYearHorseThemeColors.onSurfaceVariant.value != 0UL) NewYearHorseThemeColors.onSurfaceVariant else Color(0xFF475569)
    val safeSurface = if (NewYearHorseThemeColors.surface.value != 0UL) NewYearHorseThemeColors.surface else Color(0xFFFFFAF5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "上一年",
                                tint = safeOnSurface
                            )
                        }
                        Text(
                            text = "${currentYear}年度",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = safeOnSurface
                        )
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "下一年",
                                tint = safeOnSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToButlerMarket) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "管家市场",
                            tint = safeOnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAI,
                containerColor = safePrimary,
                contentColor = safeOnSurface
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI 助手")
            }
        }
    ) { padding ->
        NewYearHorseBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    val butlerName by viewModel.currentButlerName.collectAsState()

                    // 年度收支概览
                    NewYearHorseYearlySummaryCard(
                        totalIncome = monthlyStats.totalIncome,
                        totalExpense = monthlyStats.totalExpense,
                        balance = monthlyStats.totalIncome - monthlyStats.totalExpense,
                        butlerName = butlerName,
                        primaryColor = safePrimary,
                        onSurfaceColor = safeOnSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 快捷功能卡片
                    NewYearHorseQuickActionCards(
                        currentMonth = currentMonth,
                        monthlyIncome = monthlyStats.monthlyIncome,
                        monthlyExpense = monthlyStats.monthlyExpense,
                        yearlyExpense = monthlyStats.totalExpense,
                        accountCount = accounts.size,
                        onNavigateToTransactions = onNavigateToTransactions,
                        onNavigateToStatistics = onNavigateToStatistics,
                        primaryColor = safePrimary,
                        onSurfaceColor = safeOnSurface,
                        onSurfaceVariantColor = safeOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 本月支出趋势
                    NewYearHorseMonthlyTrendCard(yearlyTrendData = yearlyTrendData, primaryColor = safePrimary)

                    Spacer(modifier = Modifier.height(16.dp))

                    // 支出分类占比
                    NewYearHorseCategorySummaryCard(
                        recentTransactions = recentTransactions,
                        categories = categories,
                        primaryColor = safePrimary,
                        onSurfaceColor = safeOnSurface,
                        onSurfaceVariantColor = safeOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 最近交易
                    NewYearHorseRecentTransactionsCard(
                        transactions = recentTransactions,
                        onSurfaceColor = safeOnSurface,
                        onSurfaceVariantColor = safeOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(120.dp))
                }

                // 底部装饰
                SciFiBottomLine(modifier = Modifier.align(Alignment.BottomCenter), primaryColor = safePrimary)
            }
        }
    }
}

@Composable
fun NewYearHorseYearlySummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    balance: Double,
    butlerName: String,
    primaryColor: Color,
    onSurfaceColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NewYearHorseThemeColors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (butlerName.isBlank()) "AI管家" else "AI管家 · $butlerName",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NewYearHorseSummaryItem(
                    label = "收入",
                    amount = "¥${String.format("%.2f", totalIncome)}",
                    color = Color(0xFF4CAF50),
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )

                NewYearHorseSummaryItem(
                    label = "支出",
                    amount = "¥${String.format("%.2f", totalExpense)}",
                    color = Color(0xFFFF6B35),
                    icon = Icons.AutoMirrored.Filled.TrendingDown
                )

                NewYearHorseSummaryItem(
                    label = "结余",
                    amount = "¥${String.format("%.2f", balance)}",
                    color = primaryColor,
                    icon = Icons.Default.AccountBalance
                )
            }
        }
    }
}

@Composable
fun NewYearHorseSummaryItem(
    label: String,
    amount: String,
    color: Color,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = NewYearHorseThemeColors.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = amount, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NewYearHorseQuickActionCards(
    currentMonth: Int,
    monthlyIncome: Double,
    monthlyExpense: Double,
    yearlyExpense: Double,
    accountCount: Int,
    onNavigateToTransactions: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    primaryColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NewYearHorseActionCard(
            title = "日历",
            subtitle = "",
            icon = Icons.Default.CalendarMonth,
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val today = Calendar.getInstance()
                    val currentYear = today.get(Calendar.YEAR)
                    val currentMonthNum = today.get(Calendar.MONTH) + 1
                    val currentDay = today.get(Calendar.DAY_OF_MONTH)

                    Text(
                        text = "$currentYear",
                        color = Color(0xFF0D1B2E),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currentMonthNum}月${currentDay}日",
                        color = primaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val weekDays = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                    Text(
                        text = weekDays[today.get(Calendar.DAY_OF_WEEK) - 1],
                        color = onSurfaceVariantColor,
                        fontSize = 12.sp
                    )
                }
            },
            modifier = Modifier.weight(1f)
        )

        NewYearHorseActionCard(
            title = "本月收支",
            subtitle = "",
            icon = Icons.Default.AccountBalanceWallet,
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "收 ¥${if (monthlyIncome >= 10000) "${(monthlyIncome / 10000).toInt()}万" else monthlyIncome.toInt()}",
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "支 ¥${if (monthlyExpense >= 10000) "${(monthlyExpense / 10000).toInt()}万" else monthlyExpense.toInt()}",
                        color = Color(0xFFFF6B35),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            modifier = Modifier.weight(1f)
        )

        NewYearHorseActionCard(
            title = "账户",
            icon = Icons.Default.Receipt,
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${accountCount}个账户", color = onSurfaceVariantColor, fontSize = 11.sp)
                }
            },
            modifier = Modifier.weight(1f)
        )

        NewYearHorseActionCard(
            title = "趋势",
            icon = Icons.AutoMirrored.Filled.ShowChart,
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥${String.format("%.0f", yearlyExpense)}",
                        color = primaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NewYearHorseActionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    content: @Composable () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    primaryColor: Color = NewYearHorseThemeColors.primary,
    onSurfaceVariantColor: Color = NewYearHorseThemeColors.onSurfaceVariant
) {
    Card(
        modifier = modifier.clickable(onClick = onClick).height(130.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = primaryColor, modifier = Modifier.size(14.dp))
                Text(text = title, color = onSurfaceVariantColor, fontSize = 11.sp)
            }
            subtitle?.let {
                Text(text = it, color = Color(0xFF0D1B2E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
fun NewYearHorseMonthlyTrendCard(yearlyTrendData: List<com.example.aiaccounting.ui.components.charts.MonthlyData>, primaryColor: Color = NewYearHorseThemeColors.primary) {
    MonthlyTrendCardImpl(yearlyTrendData, primaryColor)
}

@Composable
private fun MonthlyTrendCardImpl(yearlyTrendData: List<com.example.aiaccounting.ui.components.charts.MonthlyData>, primaryColor: Color) {
    var selectedMonth by remember { mutableStateOf<com.example.aiaccounting.ui.components.charts.MonthlyData?>(null) }
    val displayData = yearlyTrendData.take(7)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "年度收支趋势", color = Color(0xFF0D1B2E), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF4CAF50), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("收入", color = Color(0xFF656D78), fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF2196F3), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("支出", color = Color(0xFF656D78), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (yearlyTrendData.isNotEmpty()) {
                val maxValue = yearlyTrendData.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    displayData.forEach { data ->
                        val incomeHeight = (data.income / maxValue).toFloat().coerceIn(0.05f, 1f)
                        val expenseHeight = (data.expense / maxValue).toFloat().coerceIn(0.05f, 1f)
                        val isSelected = selectedMonth?.month == data.month

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp).clickable { selectedMonth = if (isSelected) null else data }) {
                            Box(
                                modifier = Modifier.width(12.dp).height((incomeHeight * 40).dp).clip(RoundedCornerShape(2.dp)).background(if (isSelected) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.8f))
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier.width(12.dp).height((expenseHeight * 40).dp).clip(RoundedCornerShape(2.dp)).background(if (isSelected) Color(0xFF2196F3) else Color(0xFF2196F3).copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (yearlyTrendData.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    displayData.forEach { data ->
                        Text(text = data.month, color = Color(0xFF656D78), fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    }
                }
            }

            selectedMonth?.let { data ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = data.month, color = Color(0xFF0D1B2E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Row {
                            Text(text = "收: ¥${data.income.toInt()}", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "支: ¥${data.expense.toInt()}", color = Color(0xFF2196F3), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewYearHorseCategorySummaryCard(
    recentTransactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
    categories: List<com.example.aiaccounting.data.local.entity.Category>,
    primaryColor: Color = NewYearHorseThemeColors.primary,
    onSurfaceColor: Color = NewYearHorseThemeColors.onSurface,
    onSurfaceVariantColor: Color = NewYearHorseThemeColors.onSurfaceVariant
) {
    val categoryStats = rememberCategoryStats(recentTransactions, categories)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "支出分类占比", color = onSurfaceColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Icon(imageVector = Icons.Default.PieChart, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (categoryStats.isNotEmpty()) {
                categoryStats.take(4).forEachIndexed { index, stat ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stat.name, color = onSurfaceVariantColor, fontSize = 12.sp, modifier = Modifier.width(50.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { stat.percentage },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = stat.color,
                            trackColor = primaryColor.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${(stat.percentage * 100).toInt()}%", color = onSurfaceColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (index < categoryStats.size - 1 && index < 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                Text(text = "暂无支出数据", color = onSurfaceVariantColor, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun NewYearHorseRecentTransactionsCard(
    transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
    onSurfaceColor: Color = NewYearHorseThemeColors.onSurface,
    onSurfaceVariantColor: Color = NewYearHorseThemeColors.onSurfaceVariant
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NewYearHorseThemeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "最近交易", color = onSurfaceColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = "查看更多", color = NewYearHorseThemeColors.primary, fontSize = 12.sp, modifier = Modifier.clickable { })
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isNotEmpty()) {
                transactions.take(5).forEachIndexed { index, transaction ->
                    val isExpense = transaction.type == TransactionType.EXPENSE
                    val amountColor = if (isExpense) Color(0xFFFF6B35) else Color(0xFF4CAF50)
                    val amountPrefix = if (isExpense) "-" else "+"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isExpense) Color(0xFFFF6B35).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isExpense) Icons.Default.ShoppingCart else Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = amountColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = transaction.note.takeIf { it.isNotBlank() } ?: "未备注",
                                    color = Color(0xFF0D1B2E),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(text = dateFormat.format(Date(transaction.date)), color = Color(0xFF656D78), fontSize = 11.sp)
                            }
                        }
                        Text(text = "$amountPrefix¥${String.format("%.2f", transaction.amount)}", color = amountColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (index < transactions.size - 1 && index < 4) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                Text(text = "暂无交易记录", color = Color(0xFF656D78), fontSize = 14.sp)
            }
        }
    }
}
