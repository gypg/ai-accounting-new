package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors
import com.example.aiaccounting.ui.theme.LocalUiScale
import com.example.aiaccounting.ui.viewmodel.StatisticsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseStatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    uiScaleKey: Int = 0,
    onUiScaleChanged: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var contentTabIndex by remember { mutableStateOf(0) }
    val contentTabs = listOf("趋势", "分类", "明细", "统计")

    val statistics by viewModel.statistics.collectAsState()

    // UI scaling
    val uiScale = LocalUiScale.current
    val cardScale = uiScale.cardScale
    val fontScale = uiScale.fontScale

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "统计",
                        fontSize = (22 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = HorseTheme2026Colors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        HorseBackground {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // 总收入/总支出卡片 - 真实数据
                    IncomeExpenseCard(
                        totalIncome = statistics.totalIncome,
                        totalExpense = statistics.totalExpense,
                        cardScale = cardScale,
                        fontScale = fontScale
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 时间筛选器
                    TimeFilterBar(
                        currentFilter = uiState.timeFilter,
                        onFilterSelected = { viewModel.setTimeFilter(it) },
                        cardScale = cardScale,
                        fontScale = fontScale
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorseIncomeExpenseModeToggle(
                        selectedMode = uiState.selectedTab,
                        onModeSelected = { viewModel.setSelectedTab(it) },
                        cardScale = cardScale,
                        fontScale = fontScale
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 标签页
                    TabRow(
                        selectedTabIndex = contentTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = HorseTheme2026Colors.Gold,
                        indicator = { tabPositions ->
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[contentTabIndex])
                                    .height(3.dp)
                                    .background(HorseTheme2026Colors.Gold)
                            )
                        }
                    ) {
                        contentTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = contentTabIndex == index,
                                onClick = { contentTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = (13 * fontScale).sp,
                                        fontWeight = if (contentTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (contentTabIndex == index)
                                            HorseTheme2026Colors.Gold
                                        else
                                            HorseTheme2026Colors.TextSecondary
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 根据选中的标签显示不同内容 - 使用真实数据
                    when (contentTabIndex) {
                        0 -> ExpenseTrendContent(
                            monthlyTrend = statistics.monthlyTrend,
                            dailyTrend = statistics.dailyTrend,
                            weeklyTrend = statistics.weeklyTrend,
                            timeFilter = uiState.timeFilter,
                            cardScale = cardScale,
                            fontScale = fontScale,
                            showIncome = uiState.selectedTab == "income",
                            showExpense = uiState.selectedTab == "expense",
                            title = if (uiState.selectedTab == "income") "收入趋势" else "支出趋势"
                        )
                        1 -> ExpenseCategoryContent(
                            categoryStats = statistics.categoryStats,
                            cardScale = cardScale,
                            fontScale = fontScale,
                            title = if (uiState.selectedTab == "income") "收入分类" else "支出分类"
                        )
                        2 -> ExpenseDetailContent(
                            transactions = statistics.transactions,
                            cardScale = cardScale,
                            fontScale = fontScale,
                            title = if (uiState.selectedTab == "income") "收入明细" else "支出明细",
                            emptyText = if (uiState.selectedTab == "income") "暂无收入明细" else "暂无支出明细"
                        )
                        3 -> CategoryStatsContent(
                            totalAmount = if (uiState.selectedTab == "income") statistics.totalIncome else statistics.totalExpense,
                            categoryStats = statistics.categoryStats,
                            cardScale = cardScale,
                            fontScale = fontScale,
                            modeLabel = if (uiState.selectedTab == "income") "收入" else "支出"
                        )
                    }

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

@Composable
fun HorseIncomeExpenseModeToggle(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    cardScale: Float = 1f,
    fontScale: Float = 1f
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = selectedMode == "income",
            onClick = { onModeSelected("income") },
            label = {
                Text(
                    text = "收入模式",
                    fontSize = (12 * fontScale).sp,
                    color = if (selectedMode == "income") Color.Black else HorseTheme2026Colors.TextSecondary
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HorseTheme2026Colors.Gold,
                selectedLabelColor = Color.Black,
                containerColor = HorseTheme2026Colors.CardBackground
            ),
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedMode == "expense",
            onClick = { onModeSelected("expense") },
            label = {
                Text(
                    text = "支出模式",
                    fontSize = (12 * fontScale).sp,
                    color = if (selectedMode == "expense") Color.Black else HorseTheme2026Colors.TextSecondary
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HorseTheme2026Colors.Gold,
                selectedLabelColor = Color.Black,
                containerColor = HorseTheme2026Colors.CardBackground
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TimeFilterBar(
    currentFilter: String,
    onFilterSelected: (String) -> Unit,
    cardScale: Float = 1f,
    fontScale: Float = 1f
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val currentDateStr = dateFormat.format(calendar.time)

    // 获取当前显示的日期范围描述
    val dateRangeText = when (currentFilter) {
        "today" -> "今日"
        "yesterday" -> "昨日"
        "this_week" -> "本周"
        "this_month" -> "本月"
        "this_year" -> "今年"
        "current" -> currentDateStr
        "last" -> "上月"
        "3months" -> "近3个月"
        "6months" -> "近6个月"
        "1year" -> "近1年"
        "all" -> "全部"
        else -> currentDateStr
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 当前日期显示（黑框位置）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = HorseTheme2026Colors.CardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "统计时间",
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = (12 * fontScale).sp
                    )
                    Text(
                        text = dateRangeText,
                        color = HorseTheme2026Colors.TextPrimary,
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "选择日期",
                        tint = HorseTheme2026Colors.Gold,
                        modifier = Modifier.size((24 * cardScale).dp)
                    )
                }
            }
        }
        
        // 日期选择器弹窗
        if (showDatePicker) {
            DatePickerDialogWithDay(
                currentYear = selectedYear,
                currentMonth = selectedMonth,
                onDismiss = { showDatePicker = false },
                onDateSelected = { year, month, day ->
                    selectedYear = year
                    selectedMonth = month
                    // 生成日期格式字符串并触发筛选
                    val dateFilter = String.format("%04d-%02d-%02d", year, month, day)
                    onFilterSelected(dateFilter)
                    showDatePicker = false
                }
            )
        }

        // 快捷时间选择
        val quickFilters = listOf(
            "today" to "今日",
            "yesterday" to "昨日",
            "this_week" to "本周",
            "this_month" to "本月",
            "this_year" to "今年"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickFilters.forEach { (key, label) ->
                val isSelected = currentFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(key) },
                    label = {
                        Text(
                            text = label,
                            fontSize = (11 * fontScale).sp,
                            color = if (isSelected) Color.Black else HorseTheme2026Colors.TextSecondary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HorseTheme2026Colors.Gold,
                        selectedLabelColor = Color.Black,
                        containerColor = HorseTheme2026Colors.CardBackground
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 更多时间选项
        val moreFilters = listOf(
            "last" to "上月",
            "3months" to "3个月",
            "6months" to "6个月",
            "1year" to "1年",
            "all" to "全部"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            moreFilters.forEach { (key, label) ->
                val isSelected = currentFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(key) },
                    label = {
                        Text(
                            text = label,
                            fontSize = (11 * fontScale).sp,
                            color = if (isSelected) Color.Black else HorseTheme2026Colors.TextSecondary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HorseTheme2026Colors.Gold,
                        selectedLabelColor = Color.Black,
                        containerColor = HorseTheme2026Colors.CardBackground
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseCard(
    totalIncome: Double,
    totalExpense: Double,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 总收入
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "总收入",
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = (14 * fontScale).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¥${String.format("%.2f", totalIncome)}",
                    color = HorseTheme2026Colors.Income,
                    fontSize = (28 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                // 收入图标
                StatIcon(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    backgroundColor = HorseTheme2026Colors.Income.copy(alpha = 0.2f),
                    iconColor = HorseTheme2026Colors.Income,
                    cardScale = cardScale
                )
            }

            // 总支出 - 使用高对比度颜色
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "总支出",
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = (14 * fontScale).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¥${String.format("%.2f", totalExpense)}",
                    color = HorseTheme2026Colors.Expense,
                    fontSize = (28 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
                // 支出图标
                StatIcon(
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    backgroundColor = HorseTheme2026Colors.Expense.copy(alpha = 0.2f),
                    iconColor = HorseTheme2026Colors.Expense,
                    cardScale = cardScale
                )
            }
        }
    }
}

@Composable
fun StatIcon(
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    cardScale: Float = 1f
) {
    Box(
        modifier = modifier
            .size((40 * cardScale).dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size((24 * cardScale).dp)
        )
    }
}

// 支出趋势内容 - 使用增强版图表组件
@Composable
fun ExpenseTrendContent(
    monthlyTrend: List<com.example.aiaccounting.ui.components.charts.MonthlyData>,
    dailyTrend: List<com.example.aiaccounting.ui.viewmodel.DailyData>,
    weeklyTrend: List<com.example.aiaccounting.ui.viewmodel.WeeklyData>,
    timeFilter: String,
    cardScale: Float = 1f,
    fontScale: Float = 1f,
    showIncome: Boolean = false,
    showExpense: Boolean = true,
    title: String = "支出趋势"
) {
    var chartType by remember { mutableStateOf(0) } // 0: 线图, 1: 柱状图
    var trendType by remember { mutableStateOf(0) } // 0: 月度, 1: 每周, 2: 日历
    
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    Column {
        // 图表类型和时间维度切换
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HorseTheme2026Colors.CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = HorseTheme2026Colors.TextPrimary,
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { chartType = 0 }) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "线图",
                                tint = if (chartType == 0) HorseTheme2026Colors.Gold else HorseTheme2026Colors.TextSecondary,
                                modifier = Modifier.size((24 * cardScale).dp)
                            )
                        }
                        IconButton(onClick = { chartType = 1 }) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "柱状图",
                                tint = if (chartType == 1) HorseTheme2026Colors.Gold else HorseTheme2026Colors.TextSecondary,
                                modifier = Modifier.size((24 * cardScale).dp)
                            )
                        }
                    }
                }

                // 时间维度切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("月度", "每周", "日历").forEachIndexed { index, label ->
                        FilterChip(
                            selected = trendType == index,
                            onClick = { trendType = index },
                            label = { Text(label, fontSize = (12 * fontScale).sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HorseTheme2026Colors.Gold,
                                selectedLabelColor = Color.Black,
                                containerColor = HorseTheme2026Colors.Background
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 根据选择显示不同的图表
        when (trendType) {
            0 -> {
                // 月度图表 - 根据时间筛选动态显示
                val monthlyData = if (monthlyTrend.isEmpty()) {
                    // 如果没有数据，显示空状态
                    emptyList()
                } else {
                    monthlyTrend.map { 
                        MonthlyChartData(it.month, it.income, it.expense)
                    }
                }
                if (monthlyData.isNotEmpty()) {
                    MonthlyTrendChartEnhanced(
                        monthlyData = monthlyData,
                        chartType = chartType,
                        showIncome = showIncome,
                        showExpense = showExpense
                    )
                } else {
                    EmptyChartMessage("该时间范围内无月度数据")
                }
            }
            1 -> {
                // 每周图表 - 显示当前周
                val currentWeekData = generateCurrentWeekData(dailyTrend)
                WeeklyTrendChartEnhanced(currentWeekData, chartType)
            }
            2 -> {
                // 日历图表 - 根据时间筛选动态调整年月
                val calendarData = dailyTrend.map {
                    DailyChartData(it.date, it.income, it.expense)
                }
                // 根据时间筛选确定显示的年月
                val (displayYear, displayMonth) = when {
                    timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                        // 特定日期格式 2026-03-08
                        val parts = timeFilter.split("-")
                        Pair(parts[0].toInt(), parts[1].toInt())
                    }
                    timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
                        // 年月格式 2026-03
                        val parts = timeFilter.split("-")
                        Pair(parts[0].toInt(), parts[1].toInt())
                    }
                    else -> Pair(currentYear, currentMonth)
                }
                CalendarChart(displayYear, displayMonth, calendarData)
            }
        }
    }
}

/**
 * 生成当前周数据
 * 计算包含当前日期的一周（周一到周日）
 * 例如：今天是3月8日（周六），则显示3月2日-3月8日
 */
private fun generateCurrentWeekData(dailyTrend: List<com.example.aiaccounting.ui.viewmodel.DailyData>): WeeklyChartData {
    val today = Calendar.getInstance()
    val currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
    
    // 计算本周一（如果今天是周日，DAY_OF_WEEK=1，需要回退6天；其他情况回退到周一）
    val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
    
    val monday = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    // 计算本周日（周一 + 6天）
    val sunday = Calendar.getInstance().apply {
        timeInMillis = monday.timeInMillis
        add(Calendar.DAY_OF_MONTH, 6)
    }
    
    val dateFormat = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
    val weekRange = "${dateFormat.format(monday.time)} - ${dateFormat.format(sunday.time)}"
    
    // 生成7天的数据（周一到周日）
    val weekDays = mutableListOf<DailyChartData>()
    val tempCal = Calendar.getInstance()
    tempCal.timeInMillis = monday.timeInMillis
    
    for (i in 0..6) {
        val dateStr = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(tempCal.time)
        val dayData = dailyTrend.find { it.date == dateStr }
        weekDays.add(DailyChartData(dateStr, dayData?.income ?: 0.0, dayData?.expense ?: 0.0))
        tempCal.add(Calendar.DAY_OF_MONTH, 1)
    }
    
    return WeeklyChartData("本周", weekRange, weekDays)
}

@Composable
fun MonthlyTrendChart(monthlyTrend: List<com.example.aiaccounting.ui.components.charts.MonthlyData>, chartType: Int) {
    if (monthlyTrend.isEmpty()) {
        EmptyChartMessage()
        return
    }

    val maxExpense = monthlyTrend.maxOfOrNull { it.expense }?.coerceAtLeast(1.0) ?: 1.0
    val maxIncome = monthlyTrend.maxOfOrNull { it.income }?.coerceAtLeast(1.0) ?: 1.0
    val maxValue = maxOf(maxExpense, maxIncome)

    // 【颜色优化】使用高对比度颜色，与红色背景明显区分
    val incomeColor = Color(0xFF00E676)      // 亮绿色 - 高对比度
    val expenseColor = Color(0xFF00B0FF)     // 亮蓝色 - 高对比度
    val incomeLineColor = Color(0xFF69F0AE)  // 收入线颜色
    val expenseLineColor = Color(0xFF40C4FF) // 支出线颜色

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (chartType == 0) {
            // 线图 - 使用高对比度颜色
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val padding = 30f

                // 绘制收入折线
                val incomePoints = monthlyTrend.mapIndexed { index, data ->
                    val x = padding + (width - 2 * padding) * index / (monthlyTrend.size - 1).coerceAtLeast(1)
                    val y = height - padding - (height - 2 * padding) * (data.income / maxValue).toFloat()
                    Offset(x, y)
                }

                // 绘制支出折线
                val expensePoints = monthlyTrend.mapIndexed { index, data ->
                    val x = padding + (width - 2 * padding) * index / (monthlyTrend.size - 1).coerceAtLeast(1)
                    val y = height - padding - (height - 2 * padding) * (data.expense / maxValue).toFloat()
                    Offset(x, y)
                }

                // 绘制收入线（亮绿色）
                if (incomePoints.size > 1) {
                    val incomePath = Path().apply {
                        incomePoints.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y)
                            else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(
                        path = incomePath,
                        color = incomeLineColor,
                        style = Stroke(width = 4f)
                    )
                }

                // 绘制支出线（亮蓝色）
                if (expensePoints.size > 1) {
                    val expensePath = Path().apply {
                        expensePoints.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y)
                            else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(
                        path = expensePath,
                        color = expenseLineColor,
                        style = Stroke(width = 4f)
                    )
                }

                // 绘制数据点（带白色描边）
                incomePoints.forEach { offset ->
                    drawCircle(color = Color.White, radius = 7f, center = offset)
                    drawCircle(color = incomeLineColor, radius = 5f, center = offset)
                }
                expensePoints.forEach { offset ->
                    drawCircle(color = Color.White, radius = 7f, center = offset)
                    drawCircle(color = expenseLineColor, radius = 5f, center = offset)
                }
            }
        } else {
            // 柱状图 - 使用高对比度颜色
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyTrend.forEach { data ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 收入柱（亮绿色）
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height((data.income / maxValue * 80).toInt().dp.coerceAtLeast(4.dp))
                                .background(incomeColor, RoundedCornerShape(3.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // 支出柱（亮蓝色）
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height((data.expense / maxValue * 80).toInt().dp.coerceAtLeast(4.dp))
                                .background(expenseColor, RoundedCornerShape(3.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.month,
                            color = HorseTheme2026Colors.TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 图例 - 使用与图表一致的高对比度颜色
    val incomeLegendColor = Color(0xFF00E676)  // 亮绿色
    val expenseLegendColor = Color(0xFF00B0FF) // 亮蓝色

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(incomeLegendColor, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("收入", color = HorseTheme2026Colors.TextPrimary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(expenseLegendColor, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("支出", color = HorseTheme2026Colors.TextPrimary, fontSize = 12.sp)
        }
    }
}

@Composable
fun WeeklyTrendChart(weeklyTrend: List<com.example.aiaccounting.ui.viewmodel.WeeklyData>, chartType: Int) {
    if (weeklyTrend.isEmpty()) {
        EmptyChartMessage()
        return
    }

    val maxExpense = weeklyTrend.maxOfOrNull { it.expense }?.coerceAtLeast(1.0) ?: 1.0
    val maxIncome = weeklyTrend.maxOfOrNull { it.income }?.coerceAtLeast(1.0) ?: 1.0
    val maxValue = maxOf(maxExpense, maxIncome)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (chartType == 0) {
            // 线图
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val padding = 30f

                val incomePoints = weeklyTrend.mapIndexed { index, data ->
                    val x = padding + (width - 2 * padding) * index / (weeklyTrend.size - 1).coerceAtLeast(1)
                    val y = height - padding - (height - 2 * padding) * (data.income / maxValue).toFloat()
                    Offset(x, y)
                }

                val expensePoints = weeklyTrend.mapIndexed { index, data ->
                    val x = padding + (width - 2 * padding) * index / (weeklyTrend.size - 1).coerceAtLeast(1)
                    val y = height - padding - (height - 2 * padding) * (data.expense / maxValue).toFloat()
                    Offset(x, y)
                }

                if (incomePoints.size > 1) {
                    val incomePath = Path().apply {
                        incomePoints.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y)
                            else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(path = incomePath, color = HorseTheme2026Colors.Income, style = Stroke(width = 3f))
                }

                if (expensePoints.size > 1) {
                    val expensePath = Path().apply {
                        expensePoints.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y)
                            else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(path = expensePath, color = HorseTheme2026Colors.Expense, style = Stroke(width = 3f))
                }

                incomePoints.forEach { drawCircle(color = HorseTheme2026Colors.Income, radius = 5f, center = it) }
                expensePoints.forEach { drawCircle(color = HorseTheme2026Colors.Expense, radius = 5f, center = it) }
            }
        } else {
            // 柱状图
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyTrend.forEach { data ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height((data.income / maxValue * 80).toInt().dp.coerceAtLeast(4.dp))
                                .background(HorseTheme2026Colors.Income, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height((data.expense / maxValue * 80).toInt().dp.coerceAtLeast(4.dp))
                                .background(HorseTheme2026Colors.Expense, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.week,
                            color = HorseTheme2026Colors.TextSecondary,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(HorseTheme2026Colors.Income, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("收入", color = HorseTheme2026Colors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(HorseTheme2026Colors.Expense, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("支出", color = HorseTheme2026Colors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun DailyTrendChart(dailyTrend: List<com.example.aiaccounting.ui.viewmodel.DailyData>, chartType: Int, timeFilter: String) {
    // 根据时间筛选决定显示多少天
    val daysToShow = when (timeFilter) {
        "current", "last" -> 7
        "3months" -> 30
        "6months" -> 30
        "1year" -> 30
        else -> 7
    }

    val displayData = dailyTrend.takeLast(daysToShow)

    if (displayData.isEmpty()) {
        EmptyChartMessage()
        return
    }

    val maxExpense = displayData.maxOfOrNull { it.expense }?.coerceAtLeast(1.0) ?: 1.0
    val maxIncome = displayData.maxOfOrNull { it.income }?.coerceAtLeast(1.0) ?: 1.0
    val maxValue = maxOf(maxExpense, maxIncome)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (chartType == 0) {
            // 线图
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val padding = 30f

                val incomePoints = displayData.mapIndexed { index, data ->
                    val x = padding + (width - 2 * padding) * index / (displayData.size - 1).coerceAtLeast(1)
                    val y = height - padding - (height - 2 * padding) * (data.income / maxValue).toFloat()
                    Offset(x, y)
                }

                val expensePoints = displayData.mapIndexed { index, data ->
                    val x = padding + (width - 2 * padding) * index / (displayData.size - 1).coerceAtLeast(1)
                    val y = height - padding - (height - 2 * padding) * (data.expense / maxValue).toFloat()
                    Offset(x, y)
                }

                if (incomePoints.size > 1) {
                    val incomePath = Path().apply {
                        incomePoints.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y)
                            else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(path = incomePath, color = HorseTheme2026Colors.Income, style = Stroke(width = 3f))
                }

                if (expensePoints.size > 1) {
                    val expensePath = Path().apply {
                        expensePoints.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y)
                            else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(path = expensePath, color = HorseTheme2026Colors.Expense, style = Stroke(width = 3f))
                }

                incomePoints.forEach { drawCircle(color = HorseTheme2026Colors.Income, radius = 4f, center = it) }
                expensePoints.forEach { drawCircle(color = HorseTheme2026Colors.Expense, radius = 4f, center = it) }
            }
        } else {
            // 柱状图
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                displayData.forEach { data ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height((data.income / maxValue * 80).toInt().dp.coerceAtLeast(2.dp))
                                .background(HorseTheme2026Colors.Income, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height((data.expense / maxValue * 80).toInt().dp.coerceAtLeast(2.dp))
                                .background(HorseTheme2026Colors.Expense, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = data.date.takeLast(2),
                            color = HorseTheme2026Colors.TextSecondary,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(HorseTheme2026Colors.Income, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("收入", color = HorseTheme2026Colors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(HorseTheme2026Colors.Expense, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("支出", color = HorseTheme2026Colors.TextSecondary, fontSize = 12.sp)
        }
    }
}

/**
 * 日期选择器弹窗 - 支持选择年月日
 */
@Composable
fun DatePickerDialogWithDay(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(1) }
    var showDaySelection by remember { mutableStateOf(false) }
    
    // 计算当月天数
    val calendar = Calendar.getInstance()
    calendar.set(selectedYear, selectedMonth - 1, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (showDaySelection) "选择日期" else "选择年月",
                color = HorseTheme2026Colors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // 显示当前选择
                Text(
                    text = "${selectedYear}年${selectedMonth}月${if (showDaySelection) "${selectedDay}日" else ""}",
                    color = HorseTheme2026Colors.Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (!showDaySelection) {
                    // 年份选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "上一年",
                                tint = HorseTheme2026Colors.Gold
                            )
                        }
                        Text(
                            text = "$selectedYear",
                            color = HorseTheme2026Colors.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "下一年",
                                tint = HorseTheme2026Colors.Gold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 月份选择
                    Text(
                        text = "月份",
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 月份网格 3x4
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0..2) {
                                val month = row * 3 + col + 1
                                if (month <= 12) {
                                    val isSelected = selectedMonth == month
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) HorseTheme2026Colors.Gold
                                                else HorseTheme2026Colors.CardBackground
                                            )
                                            .clickable { 
                                                selectedMonth = month
                                                showDaySelection = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${month}月",
                                            color = if (isSelected) Color.Black else HorseTheme2026Colors.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // 日期选择
                    Text(
                        text = "日期",
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 日期网格 7x5
                    val weeks = (daysInMonth + 6) / 7
                    for (week in 0 until weeks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in 0..6) {
                                val day = week * 7 + dayOfWeek + 1
                                if (day <= daysInMonth) {
                                    val isSelected = selectedDay == day
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) HorseTheme2026Colors.Gold
                                                else HorseTheme2026Colors.CardBackground
                                            )
                                            .clickable { selectedDay = day },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$day",
                                            color = if (isSelected) Color.Black else HorseTheme2026Colors.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 返回月份选择
                    TextButton(
                        onClick = { showDaySelection = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "返回选择月份",
                            color = HorseTheme2026Colors.TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (showDaySelection) {
                TextButton(onClick = { onDateSelected(selectedYear, selectedMonth, selectedDay) }) {
                    Text(
                        text = "确定",
                        color = HorseTheme2026Colors.Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                TextButton(onClick = { showDaySelection = true }) {
                    Text(
                        text = "下一步",
                        color = HorseTheme2026Colors.Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = HorseTheme2026Colors.TextSecondary
                )
            }
        },
        containerColor = HorseTheme2026Colors.Background
    )
}

@Composable
fun EmptyChartMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暂无数据",
            color = HorseTheme2026Colors.TextSecondary,
            fontSize = 16.sp
        )
    }
}

// 支出分类内容 - 使用真实数据
@Composable
fun ExpenseCategoryContent(
    categoryStats: List<com.example.aiaccounting.ui.viewmodel.CategoryStat>,
    cardScale: Float = 1f,
    fontScale: Float = 1f,
    title: String = "支出分类"
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
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = HorseTheme2026Colors.Gold,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 饼图展示
            if (categoryStats.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(categoryStats = categoryStats)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 分类列表
                categoryStats.forEachIndexed { index, stat ->
                    val icon = when (stat.name) {
                        "餐饮" -> Icons.Default.Restaurant
                        "购物" -> Icons.Default.ShoppingCart
                        "交通" -> Icons.Default.DirectionsBus
                        "娱乐" -> Icons.Default.Movie
                        "居住" -> Icons.Default.Home
                        "医疗" -> Icons.Default.LocalHospital
                        "教育" -> Icons.Default.School
                        else -> Icons.Default.MoreHoriz
                    }
                    CategoryProgressItem(
                        name = stat.name,
                        icon = icon,
                        progress = stat.percentage,
                        amount = "¥${String.format("%.2f", stat.amount)}",
                        color = Color(android.graphics.Color.parseColor(stat.color))
                    )
                    if (index < categoryStats.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                Text(
                    text = if (title == "收入分类") "暂无收入数据" else "暂无支出数据",
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PieChart(categoryStats: List<com.example.aiaccounting.ui.viewmodel.CategoryStat>) {
    val total = categoryStats.sumOf { it.amount }
    if (total <= 0) return

    Box(
        modifier = Modifier.size(120.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = 0f
            categoryStats.forEach { stat ->
                val sweepAngle = (stat.amount / total * 360).toFloat()
                drawArc(
                    color = Color(android.graphics.Color.parseColor(stat.color)),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
    }
}

// 支出明细内容 - 使用真实数据
@Composable
fun ExpenseDetailContent(
    transactions: List<Transaction>,
    cardScale: Float = 1f,
    fontScale: Float = 1f,
    title: String = "支出明细",
    emptyText: String = "暂无支出明细"
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

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
                    text = title,
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = HorseTheme2026Colors.Gold,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isNotEmpty()) {
                transactions.take(10).forEachIndexed { index, transaction ->
                    DetailItem(
                        title = transaction.note.takeIf { it.isNotBlank() } ?: "未备注",
                        time = dateFormat.format(Date(transaction.date)),
                        amount = transaction.amount,
                        type = transaction.type
                    )
                    if (index < transactions.size - 1 && index < 9) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = HorseTheme2026Colors.TextSecondary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // 显示示例数据或空状态
                Text(
                    text = emptyText,
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// 分类统计内容 - 使用真实数据
@Composable
fun CategoryStatsContent(
    totalAmount: Double,
    categoryStats: List<com.example.aiaccounting.ui.viewmodel.CategoryStat>,
    cardScale: Float = 1f,
    fontScale: Float = 1f,
    modeLabel: String = "支出"
) {
    val transactionCount = categoryStats.size
    val maxAmount = categoryStats.maxOfOrNull { it.amount } ?: 0.0
    val avgDailyAmount = totalAmount / 30.0 // 简化计算

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
                    text = "分类统计",
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = HorseTheme2026Colors.Gold,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 统计卡片网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "总${modeLabel}",
                    value = "¥${String.format("%.2f", totalAmount)}",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "平均日${modeLabel}",
                    value = "¥${String.format("%.2f", avgDailyAmount)}",
                    icon = Icons.Default.Today,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "最大${modeLabel}",
                    value = "¥${String.format("%.2f", maxAmount)}",
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "分类数量",
                    value = "${transactionCount}类",
                    icon = Icons.Default.Numbers,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 柱状图展示
            if (categoryStats.isNotEmpty()) {
                Text(
                    text = "分类${modeLabel}对比",
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val maxAmount = categoryStats.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                // 分类支出对比柱状图 - 显示完整金额
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 显示每个分类的详细信息
                    categoryStats.take(6).forEachIndexed { index, stat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 分类名称和图标
                            Row(
                                modifier = Modifier.width(80.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            Color(android.graphics.Color.parseColor(stat.color)),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stat.name,
                                    color = HorseTheme2026Colors.TextPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            // 柱状图
                            val barWidthPercent = (stat.amount / maxAmount).coerceIn(0.05, 1.0)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(barWidthPercent.toFloat())
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(android.graphics.Color.parseColor(stat.color)),
                                                    Color(android.graphics.Color.parseColor(stat.color)).copy(alpha = 0.6f)
                                                )
                                            )
                                        )
                                )
                            }

                            // 金额和百分比
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.width(80.dp)
                            ) {
                                Text(
                                    text = "¥${String.format("%.2f", stat.amount)}",
                                    color = HorseTheme2026Colors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(stat.percentage * 100).toInt()}%",
                                    color = HorseTheme2026Colors.TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (index < categoryStats.size - 1 && index < 5) {
                            HorizontalDivider(
                                color = HorseTheme2026Colors.TextSecondary.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryProgressItem(
    name: String,
    icon: ImageVector,
    progress: Float,
    amount: String,
    color: Color = HorseTheme2026Colors.Gold
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = amount,
                    color = HorseTheme2026Colors.Expense,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = HorseTheme2026Colors.TextSecondary.copy(alpha = 0.1f)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            color = HorseTheme2026Colors.TextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DetailItem(
    title: String,
    time: String,
    amount: Double,
    type: TransactionType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HorseTheme2026Colors.Expense.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = HorseTheme2026Colors.Expense,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = time,
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        val amountText = when (type) {
            TransactionType.INCOME -> "+¥${String.format("%.2f", kotlin.math.abs(amount))}"
            TransactionType.EXPENSE -> "-¥${String.format("%.2f", kotlin.math.abs(amount))}"
            TransactionType.TRANSFER -> "¥${String.format("%.2f", kotlin.math.abs(amount))}"
        }
        val amountColor = when (type) {
            TransactionType.INCOME -> HorseTheme2026Colors.Income
            TransactionType.EXPENSE -> HorseTheme2026Colors.Expense
            TransactionType.TRANSFER -> HorseTheme2026Colors.Gold
        }
        Text(
            text = amountText,
            color = amountColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = HorseTheme2026Colors.Background.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = HorseTheme2026Colors.Gold,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = HorseTheme2026Colors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = HorseTheme2026Colors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
