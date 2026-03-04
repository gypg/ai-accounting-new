package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.components.charts.PieChart
import com.example.aiaccounting.ui.components.charts.TrendChart
import com.example.aiaccounting.ui.components.charts.BarChart
import com.example.aiaccounting.ui.viewmodel.StatisticsViewModel
import com.example.aiaccounting.utils.NumberUtils

/**
 * 统计页面 - 手机适配版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "统计",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 显示当前选择的时间范围
            val timeDisplayText = getTimeDisplayText(uiState.timeFilter)
            if (timeDisplayText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "统计时间：$timeDisplayText",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 总收入和总支出卡片
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "总收入",
                    amount = statistics.totalIncome,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "总支出",
                    amount = statistics.totalExpense,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            // 日期选择弹窗
            if (showDatePickerDialog) {
                DatePickerDialog(
                    timeFilter = uiState.timeFilter,
                    onTimeFilterSelected = { 
                        viewModel.setTimeFilter(it)
                        showDatePickerDialog = false
                    },
                    onDismiss = { showDatePickerDialog = false }
                )
            }

            // 收入/支出切换标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TabButton(
                    text = "收入",
                    selected = uiState.selectedTab == "income",
                    onClick = { viewModel.setSelectedTab("income") },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "支出",
                    selected = uiState.selectedTab == "expense",
                    onClick = { viewModel.setSelectedTab("expense") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 趋势图表
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "收支趋势",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (statistics.monthlyTrend.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无趋势数据",
                                color = Color.Gray
                            )
                        }
                    } else {
                        TrendChart(
                            data = statistics.monthlyTrend,
                            modifier = Modifier.fillMaxWidth(),
                            showIncome = true,
                            showExpense = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 饼图 - 分类占比
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (uiState.selectedTab == "income") "收入分类" else "支出分类",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (statistics.categoryStats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无分类数据",
                                color = Color.Gray
                            )
                        }
                    } else {
                        PieChart(
                            data = statistics.categoryStats.take(6),
                            modifier = Modifier.fillMaxWidth(),
                            showLabels = true,
                            holeRadius = 0.5f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 分类明细
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "分类明细",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (statistics.categoryStats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无分类数据",
                                color = Color.Gray
                            )
                        }
                    } else {
                        statistics.categoryStats.forEach { categoryStat ->
                            CategoryStatRow(
                                name = categoryStat.name,
                                amount = categoryStat.amount,
                                percentage = categoryStat.percentage,
                                color = categoryStat.color
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = NumberUtils.formatMoney(amount),
                fontSize = 20.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF2196F3) else Color(0xFFF5F5F5))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (selected) Color.White else Color.Black
        )
    }
}

@Composable
fun FilterPanel(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    timeFilter: String,
    onTimeFilterSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 时间筛选 - 使用下拉选择器
            Text(
                text = "时间范围",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 使用下拉选择器选择年月日
            DateRangeSelector(
                timeFilter = timeFilter,
                onTimeFilterSelected = onTimeFilterSelected
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 快速筛选
            Text(
                text = "快速筛选",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val quickFilters = listOf(
                "本月" to "current",
                "上月" to "last",
                "近3月" to "3months",
                "近6月" to "6months",
                "近1年" to "1year",
                "全部" to "all"
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickFilters.forEach { (label, value) ->
                    FilterChip(
                        label = label,
                        selected = timeFilter == value,
                        onClick = { onTimeFilterSelected(value) }
                    )
                }
            }
        }
    }
}

@Composable
fun DateRangeSelector(
    timeFilter: String,
    onTimeFilterSelected: (String) -> Unit
) {
    val calendar = java.util.Calendar.getInstance()
    val currentYear = calendar.get(java.util.Calendar.YEAR)
    val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
    
    // 解析当前筛选值
    val (selectedYear, selectedMonth, selectedDay) = when {
        timeFilter.matches(Regex("\\d{4}")) -> {
            Triple(timeFilter.toInt(), null, null)
        }
        timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
            val parts = timeFilter.split("-")
            Triple(parts[0].toInt(), parts[1].toInt(), null)
        }
        timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
            val parts = timeFilter.split("-")
            Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
        timeFilter == "current" -> Triple(currentYear, currentMonth, null)
        else -> Triple(currentYear, null, null)
    }
    
    // 年份列表（最近10年）
    val years = (currentYear downTo currentYear - 9).toList()
    
    // 月份列表
    val months = (1..12).toList()
    
    // 日期列表
    val daysInMonth = if (selectedYear != null && selectedMonth != null) {
        java.util.Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    } else 31
    val days = (1..daysInMonth).toList()
    
    Column {
        // 年份选择 - 抽屉式列表
        Text(
            text = "年份",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            years.forEach { year ->
                val isSelected = selectedYear == year
                YearMonthDayItem(
                    text = "${year}年",
                    isSelected = isSelected,
                    onClick = { 
                        onTimeFilterSelected(year.toString())
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 月份选择 - 抽屉式列表
        Text(
            text = "月份",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            months.forEach { month ->
                val isSelected = selectedMonth == month
                YearMonthDayItem(
                    text = "${month}月",
                    isSelected = isSelected,
                    onClick = { 
                        selectedYear?.let { year ->
                            onTimeFilterSelected(String.format("%04d-%02d", year, month))
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 日期选择 - 抽屉式列表（可选）
        Text(
            text = "日期",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 全部日期选项
        YearMonthDayItem(
            text = "全部日期",
            isSelected = selectedMonth != null && selectedDay == null,
            onClick = { 
                selectedYear?.let { year ->
                    selectedMonth?.let { month ->
                        onTimeFilterSelected(String.format("%04d-%02d", year, month))
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.forEach { day ->
                val isSelected = selectedDay == day
                YearMonthDayItem(
                    text = "${day}日",
                    isSelected = isSelected,
                    onClick = { 
                        selectedYear?.let { year ->
                            selectedMonth?.let { month ->
                                onTimeFilterSelected(String.format("%04d-%02d-%02d", year, month, day))
                            }
                        }
                    }
                )
            }
        }
        
        // 显示当前选择
        if (selectedYear != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val displayText = when {
                selectedDay != null && selectedMonth != null -> 
                    String.format("已选择：%04d年%02d月%02d日", selectedYear, selectedMonth, selectedDay)
                selectedMonth != null -> 
                    String.format("已选择：%04d年%02d月", selectedYear, selectedMonth)
                else -> 
                    "已选择：${selectedYear}年"
            }
            Text(
                text = displayText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun YearMonthDayItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else Color(0xFFF5F5F5)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) Color.White else Color.Black,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun CategoryStatRow(
    name: String,
    amount: Double,
    percentage: Float,
    color: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 颜色指示器
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    try {
                        Color(android.graphics.Color.parseColor(color))
                    } catch (e: Exception) {
                        Color.Gray
                    },
                    RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 分类名称
        Text(
            text = name,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        // 金额
        Text(
            text = NumberUtils.formatMoney(amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 百分比
        Text(
            text = "${(percentage * 100).toInt()}%",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 8.dp.roundToPx()
        val vGapPx = 8.dp.roundToPx()

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRow.isNotEmpty() && currentRowWidth + hGapPx + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }

        val height = rowHeights.sum() + (rowHeights.size - 1).coerceAtLeast(0) * vGapPx

        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = when (horizontalArrangement) {
                    Arrangement.Start, Arrangement.SpaceBetween -> 0
                    Arrangement.End -> constraints.maxWidth - rowWidths[rowIndex]
                    Arrangement.Center -> (constraints.maxWidth - rowWidths[rowIndex]) / 2
                    else -> 0
                }

                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }
                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}

/**
 * 获取时间显示文本
 */
fun getTimeDisplayText(timeFilter: String): String {
    return when {
        timeFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
            val parts = timeFilter.split("-")
            "${parts[0]}年${parts[1]}月${parts[2]}日"
        }
        timeFilter.matches(Regex("\\d{4}-\\d{2}")) -> {
            val parts = timeFilter.split("-")
            "${parts[0]}年${parts[1]}月"
        }
        timeFilter.matches(Regex("\\d{4}")) -> {
            "${timeFilter}年"
        }
        timeFilter == "current" -> "本月"
        timeFilter == "last" -> "上月"
        timeFilter == "3months" -> "近3个月"
        timeFilter == "6months" -> "近6个月"
        timeFilter == "1year" -> "近1年"
        timeFilter == "all" -> "全部"
        else -> ""
    }
}

/**
 * 日期选择弹窗 - 毛玻璃背景，中间方框
 */
@Composable
fun DatePickerDialog(
    timeFilter: String,
    onTimeFilterSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = java.util.Calendar.getInstance()
    val currentYear = calendar.get(java.util.Calendar.YEAR)
    val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1

    // 内部状态，不立即应用到外部
    var tempYear by remember { mutableStateOf<Int?>(null) }
    var tempMonth by remember { mutableStateOf<Int?>(null) }
    var tempDay by remember { mutableStateOf<Int?>(null) }
    
    // 当前步骤：0=年, 1=月, 2=日
    var currentStep by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // 中间方框
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题
                    Text(
                        text = "选择日期查看",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 步骤指示器（可点击切换）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepIndicator(
                            text = "年",
                            isActive = currentStep >= 0,
                            isSelected = tempYear != null,
                            onClick = { 
                                tempYear = null
                                tempMonth = null
                                tempDay = null
                                currentStep = 0 
                            }
                        )
                        StepLine(isActive = currentStep >= 1)
                        StepIndicator(
                            text = "月",
                            isActive = currentStep >= 1,
                            isSelected = tempMonth != null,
                            onClick = { 
                                if (tempYear != null) {
                                    tempMonth = null
                                    tempDay = null
                                    currentStep = 1 
                                }
                            }
                        )
                        StepLine(isActive = currentStep >= 2)
                        StepIndicator(
                            text = "日",
                            isActive = currentStep >= 2,
                            isSelected = tempDay != null,
                            onClick = { 
                                if (tempYear != null && tempMonth != null) {
                                    tempDay = null
                                    currentStep = 2 
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 显示当前选择
                    if (tempYear != null) {
                        val displayText = when {
                            tempDay != null && tempMonth != null -> 
                                String.format("%04d年%02d月%02d日", tempYear, tempMonth, tempDay)
                            tempMonth != null -> 
                                String.format("%04d年%02d月", tempYear, tempMonth)
                            else -> 
                                "${tempYear}年"
                        }
                        Text(
                            text = displayText,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 根据当前步骤显示不同内容
                    when (currentStep) {
                        0 -> YearSelector(
                            selectedYear = tempYear,
                            currentYear = currentYear,
                            onYearSelected = { year ->
                                tempYear = year
                                currentStep = 1
                            }
                        )
                        1 -> MonthSelector(
                            selectedYear = tempYear ?: currentYear,
                            selectedMonth = tempMonth,
                            onMonthSelected = { year, month ->
                                tempMonth = month
                                currentStep = 2
                            },
                            onBack = { 
                                tempYear = null
                                currentStep = 0 
                            }
                        )
                        2 -> DaySelector(
                            selectedYear = tempYear ?: currentYear,
                            selectedMonth = tempMonth ?: 1,
                            selectedDay = tempDay,
                            onDaySelected = { year, month, day ->
                                tempDay = day
                                // 选择了具体日期，应用并关闭
                                onTimeFilterSelected(String.format("%04d-%02d-%02d", year, month, day))
                            },
                            onBack = { 
                                tempMonth = null
                                currentStep = 1 
                            },
                            onSelectAllDays = { year, month ->
                                // 选择全部日期，应用年月并关闭
                                onTimeFilterSelected(String.format("%04d-%02d", year, month))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 取消按钮
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        
                        // 确定按钮（根据选择状态显示不同文字）
                        Button(
                            onClick = {
                                when {
                                    tempDay != null && tempMonth != null && tempYear != null ->
                                        onTimeFilterSelected(String.format("%04d-%02d-%02d", tempYear, tempMonth, tempDay))
                                    tempMonth != null && tempYear != null ->
                                        onTimeFilterSelected(String.format("%04d-%02d", tempYear, tempMonth))
                                    tempYear != null ->
                                        onTimeFilterSelected(tempYear.toString())
                                }
                            },
                            enabled = tempYear != null
                        ) {
                            Text(
                                when {
                                    tempDay != null -> "确定选择"
                                    tempMonth != null -> "选择全部日期"
                                    tempYear != null -> "选择全部月份"
                                    else -> "确定"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(
    text: String,
    isActive: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> Color(0xFFE0E0E0)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected || isActive) Color.White else Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StepLine(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(2.dp)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else Color(0xFFE0E0E0)
            )
    )
}

@Composable
fun YearSelector(
    selectedYear: Int?,
    currentYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val years = (currentYear downTo currentYear - 9).toList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "请选择年份",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(years) { year ->
                val isSelected = selectedYear == year
                DateSelectorItem(
                    text = "${year}年",
                    isSelected = isSelected,
                    onClick = { onYearSelected(year) }
                )
            }
        }
    }
}

@Composable
fun MonthSelector(
    selectedYear: Int,
    selectedMonth: Int?,
    onMonthSelected: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    val months = (1..12).toList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 返回按钮和标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${selectedYear}年 - 请选择月份",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(months) { month ->
                val isSelected = selectedMonth == month
                DateSelectorItem(
                    text = "${month}月",
                    isSelected = isSelected,
                    onClick = { onMonthSelected(selectedYear, month) }
                )
            }
        }
    }
}

@Composable
fun DaySelector(
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int?,
    onDaySelected: (Int, Int, Int) -> Unit,
    onBack: () -> Unit,
    onSelectAllDays: (Int, Int) -> Unit
) {
    val daysInMonth = java.util.Calendar.getInstance().apply {
        set(selectedYear, selectedMonth - 1, 1)
    }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val days = (1..daysInMonth).toList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 返回按钮和标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${selectedYear}年${selectedMonth}月 - 请选择日期",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 全部日期选项
        DateSelectorItem(
            text = "全部日期",
            isSelected = selectedDay == null,
            onClick = { onSelectAllDays(selectedYear, selectedMonth) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { day ->
                val isSelected = selectedDay == day
                DateSelectorItem(
                    text = "${day}日",
                    isSelected = isSelected,
                    onClick = { onDaySelected(selectedYear, selectedMonth, day) }
                )
            }
        }
    }
}

@Composable
fun DateSelectorItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color(0xFFF5F5F5)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isSelected) Color.White else Color.Black,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}


