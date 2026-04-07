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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors
import com.example.aiaccounting.ui.theme.LocalUiScale
import com.example.aiaccounting.ui.navigation.Screen
import com.example.aiaccounting.ui.viewmodel.OverviewViewModel
import com.example.aiaccounting.utils.NumberUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseOverviewScreen(
 viewModel: OverviewViewModel = hiltViewModel(),
 onNavigateToAddTransaction: () -> Unit = {},
 onNavigateToAI: () -> Unit = {},
 onNavigateToButlerMarket: () -> Unit = {},
 onNavigateToTransactions: (String) -> Unit = {},
 onNavigateToStatistics: () -> Unit = {},
 onNavigateToAccounts: () -> Unit = {},
 onNavigateToBudgets: () -> Unit = {},
 onNavigateToCalendar: (Int, Int) -> Unit = { _, _ -> },
 onNavigateToYearlyWealth: (Int) -> Unit = {}
) {
 val selectedYear by viewModel.selectedYear.collectAsState()
 val monthlyStats by viewModel.monthlyStats.collectAsState()
 val recentTransactions by viewModel.recentTransactions.collectAsState()
 val yearlyTrendData by viewModel.yearlyTrendData.collectAsState()
 val accounts by viewModel.accounts.collectAsState()
 val categories by viewModel.categories.collectAsState()
 val todayStats by viewModel.todayStats.collectAsState()
 val weekStats by viewModel.weekStats.collectAsState()
 val yearlyBudgetProgress by viewModel.yearlyBudgetProgress.collectAsState()
 val totalBudgetProgress by viewModel.totalBudgetProgress.collectAsState()
 val lifecycleOwner = LocalLifecycleOwner.current

 // UI scaling
 val uiScale = LocalUiScale.current
 val cardScale = uiScale.cardScale
 val fontScale = uiScale.fontScale

 var showAddMenu by remember { mutableStateOf(false) }
 val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

 LaunchedEffect(Unit) {
 viewModel.logOverviewScreenEnter(
 theme = "horse_2026",
 selectedYear = selectedYear,
 accountCount = accounts.size,
 categoryCount = categories.size,
 recentTransactionCount = recentTransactions.size
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

 AddTransactionMenu(
 onDismiss = { showAddMenu = false },
 isVisible = showAddMenu,
 onAIAccounting = {
 viewModel.logOverviewEntrySelected(theme = "horse_2026", entry = "ai_accounting")
 onNavigateToAI()
 },
 onManualAccounting = {
 viewModel.logOverviewEntrySelected(theme = "horse_2026", entry = "manual_accounting")
 onNavigateToAddTransaction()
 }
 )

 Scaffold(
 topBar = {
 TopAppBar(
 title = {
 Row(
 verticalAlignment = Alignment.CenterVertically,
 horizontalArrangement = Arrangement.Center,
 modifier = Modifier.fillMaxWidth()
 ) {
 IconButton(onClick = { viewModel.setSelectedYear(selectedYear - 1) }) {
 Icon(
 imageVector = Icons.Default.ChevronLeft,
 contentDescription = "上一年",
 tint = HorseTheme2026Colors.TextSecondary
 )
 }
 Text(
 text = "${selectedYear}年度",
 fontSize = (20 * fontScale).sp,
 fontWeight = FontWeight.Bold,
 color = HorseTheme2026Colors.TextPrimary
 )
 IconButton(onClick = { viewModel.setSelectedYear(selectedYear + 1) }) {
 Icon(
 imageVector = Icons.Default.ChevronRight,
 contentDescription = "下一年",
 tint = HorseTheme2026Colors.TextSecondary
 )
 }
 }
 },
 actions = {
 IconButton(onClick = onNavigateToButlerMarket) {
 Icon(
 imageVector = Icons.Default.SmartToy,
 contentDescription = "管家市场",
 tint = HorseTheme2026Colors.TextSecondary
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
 AIButton(onClick = {
 viewModel.logOverviewAddMenuOpened(theme = "horse_2026")
 showAddMenu = true
 })
 }
 ) { padding ->
 HorseBackground {
 Box(modifier = Modifier.fillMaxSize()) {
 Column(
 modifier = Modifier
 .fillMaxSize()
 .padding(padding)
 .verticalScroll(rememberScrollState())
 .padding(horizontal = 16.dp)
 ) {
 val butlerName by viewModel.currentButlerName.collectAsState()

 // 年度收支概览 - 真实数据
 YearlySummaryCard(
 totalIncome = monthlyStats.totalIncome,
 totalExpense = monthlyStats.totalExpense,
 balance = monthlyStats.totalIncome - monthlyStats.totalExpense,
 butlerName = butlerName,
 cardScale = cardScale,
 fontScale = fontScale
 )

 Spacer(modifier = Modifier.height(16.dp))

 // 快捷功能卡片 - 真实数据
 QuickActionCards(
 currentMonth = currentMonth,
 monthlyIncome = monthlyStats.monthlyIncome,
 monthlyExpense = monthlyStats.monthlyExpense,
 yearlyExpense = monthlyStats.totalExpense,
 accountCount = accounts.size,
 accountPreview = accounts.sortedByDescending { kotlin.math.abs(it.balance) }.take(3),
 onNavigateToTransactions = {
 onNavigateToTransactions(Screen.MonthlyTransactions.createRoute(selectedYear, currentMonth))
 },
 onNavigateToStatistics = onNavigateToStatistics,
 onNavigateToAccounts = onNavigateToAccounts,
 onNavigateToCalendar = onNavigateToCalendar,
 onNavigateToYearlyWealth = onNavigateToYearlyWealth,
 selectedYear = selectedYear,
 cardScale = cardScale,
 fontScale = fontScale
 )

 Spacer(modifier = Modifier.height(16.dp))

 BudgetOverviewCard(
 title = "${selectedYear}年度预算",
 budgetProgress = yearlyBudgetProgress,
 onClick = onNavigateToBudgets
 )

 Spacer(modifier = Modifier.height(12.dp))

 BudgetOverviewCard(
 title = "${selectedYear}年${Calendar.getInstance().get(Calendar.MONTH) + 1}月预算",
 budgetProgress = totalBudgetProgress,
 onClick = onNavigateToBudgets
 )

 Spacer(modifier = Modifier.height(16.dp))

 // 本月支出趋势（真实数据）
 MonthlyTrendCard(
 yearlyTrendData = yearlyTrendData,
 cardScale = cardScale,
 fontScale = fontScale
 )

 Spacer(modifier = Modifier.height(16.dp))

 // 支出分类占比（基于真实交易数据）
 CategorySummaryCard(
 recentTransactions = recentTransactions,
 categories = categories,
 cardScale = cardScale,
 fontScale = fontScale
 )

 Spacer(modifier = Modifier.height(16.dp))

 // 最近交易（真实数据）
 RecentTransactionsCard(
 transactions = recentTransactions,
 cardScale = cardScale,
 fontScale = fontScale
 )

 Spacer(modifier = Modifier.height(120.dp))
 }

 // 底部装饰
 BottomHorseDecoration(
 modifier = Modifier.align(Alignment.BottomCenter)
 )
 }
 }
 }
}

/**
 * 将十六进制颜色字符串转换为Compose Color
 */
private fun parseColor(colorString: String): Color {
 return try {
 val color = android.graphics.Color.parseColor(colorString)
 Color(color)
 } catch (e: Exception) {
 HorseTheme2026Colors.Gold
 }
}

@Composable
fun YearlySummaryCard(
 totalIncome: Double,
 totalExpense: Double,
 balance: Double,
 butlerName: String,
 cardScale: Float = 1f,
 fontScale: Float = 1f
) {
 Card(
 modifier = Modifier.fillMaxWidth(),
 shape = RoundedCornerShape(16.dp),
 colors = CardDefaults.cardColors(
 containerColor = HorseTheme2026Colors.CardBackground
 ),
 elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
 ) {
 Column(
 modifier = Modifier.padding(20.dp)
 ) {
 Text(
 text = if (butlerName.isBlank()) "AI管家" else "AI管家 · $butlerName",
 fontSize = (14 * fontScale).sp,
 fontWeight = FontWeight.SemiBold,
 color = HorseTheme2026Colors.TextSecondary,
 modifier = Modifier.padding(bottom = 12.dp)
 )

 Row(
 modifier = Modifier.fillMaxWidth(),
 horizontalArrangement = Arrangement.SpaceEvenly
 ) {
 // 收入
 SummaryItem(
 label = "收入",
 amount = "¥${String.format("%.2f", totalIncome)}",
 color = HorseTheme2026Colors.Income,
 icon = Icons.AutoMirrored.Filled.TrendingUp,
 cardScale = cardScale,
 fontScale = fontScale
 )

 // 支出 - 使用高对比度颜色
 SummaryItem(
 label = "支出",
 amount = "¥${String.format("%.2f", totalExpense)}",
 color = HorseTheme2026Colors.Expense,
 icon = Icons.AutoMirrored.Filled.TrendingDown,
 cardScale = cardScale,
 fontScale = fontScale
 )

 // 结余
 SummaryItem(
 label = "结余",
 amount = "¥${String.format("%.2f", balance)}",
 color = HorseTheme2026Colors.Gold,
 icon = Icons.Default.AccountBalance,
 cardScale = cardScale,
 fontScale = fontScale
 )
 }
 }
 }
}

@Composable
fun SummaryItem(
 label: String,
 amount: String,
 color: Color,
 icon: ImageVector,
 cardScale: Float = 1f,
 fontScale: Float = 1f
) {
 Column(
 horizontalAlignment = Alignment.CenterHorizontally
 ) {
 // 图标
 Box(
 modifier = Modifier
 .size((40 * cardScale).dp)
 .clip(CircleShape)
 .background(color.copy(alpha = 0.2f)),
 contentAlignment = Alignment.Center
 ) {
 Icon(
 imageVector = icon,
 contentDescription = label,
 tint = color,
 modifier = Modifier.size((24 * cardScale).dp)
 )
 }
 Spacer(modifier = Modifier.height(8.dp))
 Text(
 text = label,
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (12 * fontScale).sp
 )
 Spacer(modifier = Modifier.height(4.dp))
 Text(
 text = amount,
 color = color,
 fontSize = (18 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 }
}

@Composable
fun QuickActionCards(
 cardScale: Float = 1f,
 currentMonth: Int,
 monthlyIncome: Double,
 monthlyExpense: Double,
 yearlyExpense: Double,
 accountCount: Int,
 accountPreview: List<com.example.aiaccounting.data.local.entity.Account> = emptyList(),
 onNavigateToTransactions: () -> Unit,
 onNavigateToStatistics: () -> Unit,
 onNavigateToAccounts: () -> Unit = {},
 onNavigateToCalendar: (Int, Int) -> Unit,
 onNavigateToYearlyWealth: (Int) -> Unit,
 selectedYear: Int,
 fontScale: Float = 1f
) {
 val today = Calendar.getInstance()
 val dateLabel = "${today.get(Calendar.MONTH) + 1}月${today.get(Calendar.DAY_OF_MONTH)}日"
 val weekDays = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
 val incomeText = if (monthlyIncome >= 10000) "${String.format("%.1f", monthlyIncome / 10000)}万" else NumberUtils.formatMoney(monthlyIncome)
 val expenseText = if (monthlyExpense >= 10000) "${String.format("%.1f", monthlyExpense / 10000)}万" else NumberUtils.formatMoney(monthlyExpense)
 val trendText = if (yearlyExpense >= 10000) "${String.format("%.1f", yearlyExpense / 10000)}万" else NumberUtils.formatMoney(yearlyExpense)

 Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
 Row(
 modifier = Modifier.fillMaxWidth(),
 horizontalArrangement = Arrangement.spacedBy(12.dp)
 ) {
 ActionCard(
 title = "日历",
 subtitle = "${selectedYear}年${currentMonth}月",
 icon = Icons.Default.CalendarMonth,
 content = {
 Text(
 text = weekDays[today.get(Calendar.DAY_OF_WEEK) - 1],
 color = HorseTheme2026Colors.Gold,
 fontSize = (14 * fontScale).sp,
 fontWeight = FontWeight.SemiBold
 )
 Text(
 text = dateLabel,
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (11 * fontScale).sp
 )
 },
 onClick = { onNavigateToCalendar(selectedYear, currentMonth) },
 modifier = Modifier.weight(1f),
 cardScale = cardScale,
 fontScale = fontScale
 )

 ActionCard(
 title = "本月收支",
 subtitle = "${currentMonth}月",
 icon = Icons.Default.AccountBalanceWallet,
 content = {
 Column(modifier = Modifier.fillMaxWidth()) {
 Text("收 $incomeText", color = HorseTheme2026Colors.Income, fontSize = (13 * fontScale).sp, fontWeight = FontWeight.Bold)
 Spacer(modifier = Modifier.height(4.dp))
 Text("支 $expenseText", color = HorseTheme2026Colors.Expense, fontSize = (13 * fontScale).sp, fontWeight = FontWeight.Bold)
 }
 },
 onClick = onNavigateToTransactions,
 modifier = Modifier.weight(1f),
 cardScale = cardScale,
 fontScale = fontScale
 )
 }

 Row(
 modifier = Modifier.fillMaxWidth(),
 horizontalArrangement = Arrangement.spacedBy(12.dp)
 ) {
 ActionCard(
 title = "账户明细",
 subtitle = "共${accountCount}个",
 icon = Icons.Default.Receipt,
 content = {
 if (accountPreview.isEmpty()) {
 Text(
 text = "暂无账户",
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (12 * fontScale).sp,
 fontWeight = FontWeight.Medium
 )
 } else {
 Column(modifier = Modifier.fillMaxWidth()) {
 accountPreview.forEach { account ->
 Text(
 text = "${account.name} ${NumberUtils.formatMoney(account.balance)}",
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (11 * fontScale).sp,
 maxLines = 1
 )
 }
 }
 }
 },
 onClick = onNavigateToAccounts,
 modifier = Modifier.weight(1f),
 cardScale = cardScale,
 fontScale = fontScale
 )

 ActionCard(
 title = "年度趋势",
 subtitle = "${selectedYear}年财富分析",
 icon = Icons.AutoMirrored.Filled.ShowChart,
 content = {
 Text(
 text = trendText,
 color = HorseTheme2026Colors.Gold,
 fontSize = (14 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 },
 onClick = { onNavigateToYearlyWealth(selectedYear) },
 modifier = Modifier.weight(1f),
 cardScale = cardScale,
 fontScale = fontScale
 )
 }
 }
}

@Composable
fun ActionCard(
 title: String,
 subtitle: String? = null,
 icon: ImageVector,
 content: @Composable () -> Unit,
 onClick: () -> Unit = {},
 modifier: Modifier = Modifier,
 cardScale: Float = 1f,
 fontScale: Float = 1f
) {
 val safeCardScale = if (cardScale < 0.85f) 0.85f else cardScale
 Card(
 modifier = modifier
 .clickable(onClick = onClick)
 .height((130 * safeCardScale).dp),
 shape = RoundedCornerShape((12 * safeCardScale).dp),
 colors = CardDefaults.cardColors(
 containerColor = HorseTheme2026Colors.CardBackground
 ),
 elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
 ) {
 Column(
 modifier = Modifier
 .fillMaxSize()
 .padding((10 * safeCardScale).dp)
 ) {
 Row(
 verticalAlignment = Alignment.CenterVertically,
 horizontalArrangement = Arrangement.spacedBy((4 * safeCardScale).dp)
 ) {
 Icon(
 imageVector = icon,
 contentDescription = title,
 tint = HorseTheme2026Colors.Gold,
 modifier = Modifier.size((14 * fontScale).dp)
 )
 Text(
 text = title,
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (11 * fontScale).sp,
 maxLines = 1
 )
 }
 subtitle?.let {
 Text(
 text = it,
 color = HorseTheme2026Colors.TextPrimary,
 fontSize = (13 * fontScale).sp,
 fontWeight = FontWeight.Medium,
 maxLines = 1
 )
 }
 Spacer(modifier = Modifier.height((4 * safeCardScale).dp))
 content()
 }
 }
}

@Composable
 fun MonthlyTrendCard(
 yearlyTrendData: List<com.example.aiaccounting.ui.components.charts.MonthlyData>,
 cardScale: Float = 1f,
 fontScale: Float = 1f
) {
 var selectedMonth by remember { mutableStateOf<com.example.aiaccounting.ui.components.charts.MonthlyData?>(null) }

 Card(
 modifier = Modifier.fillMaxWidth(),
 shape = RoundedCornerShape(16.dp),
 colors = CardDefaults.cardColors(
 containerColor = HorseTheme2026Colors.CardBackground
 ),
 elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
 text = "年度收支趋势",
 color = HorseTheme2026Colors.TextPrimary,
 fontSize = (15 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 Icon(
 imageVector = Icons.AutoMirrored.Filled.TrendingUp,
 contentDescription = null,
 tint = HorseTheme2026Colors.Gold,
 modifier = Modifier.size((20 * fontScale).dp)
 )
 }

 // 图例
 Row(
 modifier = Modifier
 .fillMaxWidth()
 .padding(top = 8.dp),
 horizontalArrangement = Arrangement.Center
 ) {
 Row(verticalAlignment = Alignment.CenterVertically) {
 Box(modifier = Modifier.size((10 * cardScale).dp).background(Color(0xFF00E676), CircleShape))
 Spacer(modifier = Modifier.width(4.dp))
 Text("收入", color = HorseTheme2026Colors.TextSecondary, fontSize = (10 * fontScale).sp)
 }
 Spacer(modifier = Modifier.width(16.dp))
 Row(verticalAlignment = Alignment.CenterVertically) {
 Box(modifier = Modifier.size((10 * cardScale).dp).background(Color(0xFF00B0FF), CircleShape))
 Spacer(modifier = Modifier.width(4.dp))
 Text("支出", color = HorseTheme2026Colors.TextSecondary, fontSize = (10 * fontScale).sp)
 }
 }

 Spacer(modifier = Modifier.height(8.dp))

 // 基于真实数据的趋势图
 if (yearlyTrendData.isNotEmpty()) {
 // 计算最大值用于缩放
 val maxValue = yearlyTrendData.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

 // 显示7个月的数据
 val displayData = yearlyTrendData.take(7)

 Row(
 modifier = Modifier
 .fillMaxWidth()
 .height((100 * cardScale).dp),
 horizontalArrangement = Arrangement.SpaceEvenly,
 verticalAlignment = Alignment.Bottom
 ) {
 displayData.forEach { data ->
 val incomeHeight = (data.income / maxValue).toFloat().coerceIn(0.05f, 1f)
 val expenseHeight = (data.expense / maxValue).toFloat().coerceIn(0.05f, 1f)
 val isSelected = selectedMonth?.month == data.month

 Column(
 horizontalAlignment = Alignment.CenterHorizontally,
 modifier = Modifier
 .width((36 * fontScale).dp)
 .clickable { selectedMonth = if (isSelected) null else data }
 ) {
 // 收入柱（绿色）
 Box(
 modifier = Modifier
 .width((12 * fontScale).dp)
 .height((incomeHeight * 40 * cardScale).dp)
 .clip(RoundedCornerShape(2.dp))
 .background(
 if (isSelected) Color(0xFF00E676)
 else Color(0xFF00E676).copy(alpha = 0.8f)
 )
 )

 Spacer(modifier = Modifier.height(2.dp))

 // 支出柱（蓝色）
 Box(
 modifier = Modifier
 .width((12 * fontScale).dp)
 .height((expenseHeight * 40 * cardScale).dp)
 .clip(RoundedCornerShape(2.dp))
 .background(
 if (isSelected) Color(0xFF00B0FF)
 else Color(0xFF00B0FF).copy(alpha = 0.8f)
 )
 )
 }
 }
 }

 Spacer(modifier = Modifier.height(4.dp))

 // 月份标签 - 与柱状对齐
 Row(
 modifier = Modifier.fillMaxWidth(),
 horizontalArrangement = Arrangement.SpaceEvenly
 ) {
 displayData.forEach { data ->
 Text(
 text = data.month,
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (10 * fontScale).sp,
 modifier = Modifier.width((36 * fontScale).dp),
 textAlign = TextAlign.Center
 )
 }
 }

 // 显示选中的月份详情
 selectedMonth?.let { data ->
 Spacer(modifier = Modifier.height(8.dp))
 Card(
 modifier = Modifier.fillMaxWidth(),
 shape = RoundedCornerShape(8.dp),
 colors = CardDefaults.cardColors(
 containerColor = HorseTheme2026Colors.Gold.copy(alpha = 0.2f)
 )
 ) {
 Row(
 modifier = Modifier
 .fillMaxWidth()
 .padding(8.dp),
 horizontalArrangement = Arrangement.SpaceEvenly,
 verticalAlignment = Alignment.CenterVertically
 ) {
 Text(
 text = data.month,
 color = HorseTheme2026Colors.TextPrimary,
 fontSize = (14 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 Row {
 Text(
 text = "收: ¥${data.income.toInt()}",
 color = Color(0xFF00E676),
 fontSize = (13 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 Spacer(modifier = Modifier.width(12.dp))
 Text(
 text = "支: ¥${data.expense.toInt()}",
 color = Color(0xFF00B0FF),
 fontSize = (13 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 }
 }
 }
 }
        } else {
 // 无数据提示
 Box(
 modifier = Modifier
 .fillMaxWidth()
 .height(100.dp),
 contentAlignment = Alignment.Center
 ) {
 Text(
 text = "暂无数据",
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (14 * fontScale).sp
 )
 }
 }
 }
 }
}

@Composable
fun CategorySummaryCard(
 recentTransactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
 categories: List<com.example.aiaccounting.data.local.entity.Category>,
 cardScale: Float = 1f,
 fontScale: Float = 1f
) {
 // 从真实交易计算分类统计，使用真实的分类名称
 val categoryStats = rememberCategoryStats(recentTransactions, categories)

 Card(
 modifier = Modifier.fillMaxWidth(),
 shape = RoundedCornerShape(16.dp),
 colors = CardDefaults.cardColors(
 containerColor = HorseTheme2026Colors.CardBackground
 ),
 elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
 text = "支出分类占比",
 color = HorseTheme2026Colors.TextPrimary,
 fontSize = (15 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 Icon(
 imageVector = Icons.Default.PieChart,
 contentDescription = null,
 tint = HorseTheme2026Colors.Gold,
 modifier = Modifier.size((20 * fontScale).dp)
 )
 }
 Spacer(modifier = Modifier.height(12.dp))

 if (categoryStats.isNotEmpty()) {
 categoryStats.take(4).forEachIndexed { index, stat ->
 Row(
 modifier = Modifier.fillMaxWidth(),
 verticalAlignment = Alignment.CenterVertically
 ) {
 Text(
 text = stat.name,
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (12 * fontScale).sp,
 modifier = Modifier.width((50 * fontScale).dp)
 )
 Spacer(modifier = Modifier.width(8.dp))
 LinearProgressIndicator(
 progress = { stat.percentage },
 modifier = Modifier
 .weight(1f)
 .height((6 * cardScale).dp)
 .clip(RoundedCornerShape(3.dp)),
 color = stat.color,
 trackColor = HorseTheme2026Colors.TextSecondary.copy(alpha = 0.1f)
 )
 Spacer(modifier = Modifier.width(8.dp))
 Text(
 text = "${(stat.percentage * 100).toInt()}%",
 color = HorseTheme2026Colors.TextPrimary,
 fontSize = (12 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 }
 if (index < categoryStats.size - 1 && index < 3) {
 Spacer(modifier = Modifier.height(8.dp))
 }
 }
 } else {
 Text(
 text = "暂无支出数据",
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (14 * fontScale).sp
 )
 }
 }
 }
}

// 分类统计数据类
data class OverviewCategoryStat(
 val name: String,
 val amount: Double,
 val percentage: Float,
 val color: Color
)

@Composable
fun rememberCategoryStats(
 transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
 categories: List<com.example.aiaccounting.data.local.entity.Category>
): List<OverviewCategoryStat> {
 return remember(transactions, categories) {
 val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
 val totalExpense = expenseTransactions.sumOf { kotlin.math.abs(it.amount) }

 if (totalExpense <= 0) return@remember emptyList()

 // 创建分类ID到名称的映射
 val categoryMap = categories.associateBy { it.id }

 expenseTransactions
 .groupBy { it.categoryId }
 .map { (categoryId, transList) ->
 val amount = transList.sumOf { kotlin.math.abs(it.amount) }
 // 从分类映射中获取真实名称，如果没有则显示"未分类"
 val categoryName = categoryMap[categoryId]?.name ?: "未分类"
 // 使用分类的颜色，如果没有则使用默认颜色
 val categoryColorStr = categoryMap[categoryId]?.color
 val color = if (categoryColorStr != null) {
 parseColor(categoryColorStr)
 } else {
 when (categoryId % 5) {
 0L -> HorseTheme2026Colors.Expense
 1L -> HorseTheme2026Colors.Gold
 2L -> HorseTheme2026Colors.Income
 3L -> HorseTheme2026Colors.BlueCard
 else -> HorseTheme2026Colors.Warning
 }
 }
 OverviewCategoryStat(
 name = categoryName,
 amount = amount,
 percentage = (amount / totalExpense).toFloat().coerceIn(0f, 1f),
 color = color
 )
 }
 .sortedByDescending { it.amount }
 }
}

@Composable
fun RecentTransactionsCard(
 transactions: List<com.example.aiaccounting.data.local.entity.Transaction>,
 cardScale: Float = 1f,
 fontScale: Float = 1f
) {
 val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
 val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

 Card(
 modifier = Modifier.fillMaxWidth(),
 shape = RoundedCornerShape(16.dp),
 colors = CardDefaults.cardColors(
 containerColor = HorseTheme2026Colors.CardBackground
 ),
 elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
 text = "最近交易",
 color = HorseTheme2026Colors.TextPrimary,
 fontSize = (15 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 Text(
 text = "查看更多",
 color = HorseTheme2026Colors.Gold,
 fontSize = (12 * fontScale).sp,
 modifier = Modifier.clickable { }
 )
 }
 Spacer(modifier = Modifier.height(12.dp))

 if (transactions.isNotEmpty()) {
 transactions.take(5).forEachIndexed { index, transaction ->
 val isExpense = transaction.type == TransactionType.EXPENSE
 val amountColor = if (isExpense) HorseTheme2026Colors.Expense else HorseTheme2026Colors.Income
 val amountPrefix = if (isExpense) "-" else "+"

 Row(
 modifier = Modifier.fillMaxWidth(),
 horizontalArrangement = Arrangement.SpaceBetween,
 verticalAlignment = Alignment.CenterVertically
 ) {
 Row(verticalAlignment = Alignment.CenterVertically) {
 Box(
 modifier = Modifier
 .size((36 * cardScale).dp)
 .clip(RoundedCornerShape(8.dp))
 .background(
 if (isExpense)
 HorseTheme2026Colors.Expense.copy(alpha = 0.2f)
 else
 HorseTheme2026Colors.Income.copy(alpha = 0.2f)
 ),
 contentAlignment = Alignment.Center
 ) {
 Icon(
 imageVector = if (isExpense) Icons.Default.ShoppingCart else Icons.Default.AttachMoney,
 contentDescription = null,
 tint = amountColor,
 modifier = Modifier.size((20 * cardScale).dp)
 )
 }
 Spacer(modifier = Modifier.width(10.dp))
 Column {
 Text(
 text = transaction.note.takeIf { it.isNotBlank() } ?: "未备注",
 color = HorseTheme2026Colors.TextPrimary,
 fontSize = (13 * fontScale).sp,
 fontWeight = FontWeight.Medium
 )
 Text(
 text = dateFormat.format(Date(transaction.date)),
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (11 * fontScale).sp
 )
 }
 }
 Text(
 text = "$amountPrefix¥${String.format("%.2f", transaction.amount)}",
 color = amountColor,
 fontSize = (14 * fontScale).sp,
 fontWeight = FontWeight.Bold
 )
 }
 if (index < transactions.size - 1 && index < 4) {
 Spacer(modifier = Modifier.height(10.dp))
 }
 }
 } else {
 Text(
 text = "暂无交易记录",
 color = HorseTheme2026Colors.TextSecondary,
 fontSize = (14 * fontScale).sp
 )
 }
 }
 }
}
