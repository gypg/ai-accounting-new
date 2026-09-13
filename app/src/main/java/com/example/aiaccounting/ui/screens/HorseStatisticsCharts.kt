package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.ui.theme.AppThemeIds
import com.example.aiaccounting.ui.theme.FreshSciThemeColors
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors
import com.example.aiaccounting.ui.theme.LocalAppThemeId
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// 高对比度颜色定义
private val IncomeColor = Color(0xFF00E676)      // 亮绿色
private val ExpenseColor = Color(0xFF00B0FF)     // 亮蓝色
private val IncomeLineColor = Color(0xFF69F0AE)  // 收入线
private val ExpenseLineColor = Color(0xFF40C4FF) // 支出线

/**
 * 月度趋势图表 - 固定12个月，显示具体数值，带Y轴
 */
@Composable
fun MonthlyTrendChartEnhanced(
    monthlyData: List<MonthlyChartData>,
    chartType: Int, // 0: 线图, 1: 柱状图
    showIncome: Boolean = true,
    showExpense: Boolean = true,
    onDataPointClick: (String, Double, Double) -> Unit = { _, _, _ -> }
) {
    // 确保有12个月的数据（支持 "1月" 或 "01月" 格式）
    val allMonths = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
    
    // 标准化数据中的月份格式（移除前导零）
    val normalizedData = monthlyData.map { 
        val normalizedMonth = it.month.replace(Regex("^0+"), "") // 移除前导零
        it.copy(month = normalizedMonth) 
    }
    val dataMap = normalizedData.associateBy { it.month }
    
    val completeData = allMonths.map { month ->
        dataMap[month] ?: MonthlyChartData(month, 0.0, 0.0)
    }

    // 计算Y轴最大值（向上取整到合适的刻度）
    val maxDataValue = completeData.maxOf {
        maxOf(
            if (showIncome) it.income else 0.0,
            if (showExpense) it.expense else 0.0
        )
    }
    val maxValue = calculateNiceMaxValue(maxDataValue)

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HorseTheme2026Colors.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when {
                    showIncome && showExpense -> "月度收支趋势（12个月）"
                    showIncome -> "月度收入趋势（12个月）"
                    else -> "月度支出趋势（12个月）"
                },
                color = HorseTheme2026Colors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 图表区域（带Y轴）
            Row(modifier = Modifier.fillMaxWidth()) {
                // Y轴
                YAxisLabels(maxValue, modifier = Modifier.width(40.dp))
                
                // 图表
                Box(modifier = Modifier.weight(1f)) {
                    if (chartType == 0) {
                        MonthlyLineChartWithYAxis(
                            data = completeData,
                            maxValue = maxValue,
                            selectedIndex = selectedIndex,
                            showIncome = showIncome,
                            showExpense = showExpense
                        ) { index ->
                            if (index < 0) {
                                selectedIndex = null
                            } else {
                                selectedIndex = index
                                val data = completeData[index]
                                onDataPointClick(data.month, data.income, data.expense)
                            }
                        }
                    } else {
                        MonthlyBarChartWithYAxis(
                            data = completeData,
                            maxValue = maxValue,
                            selectedIndex = selectedIndex,
                            showIncome = showIncome,
                            showExpense = showExpense
                        ) { index ->
                            if (index < 0) {
                                selectedIndex = null
                            } else {
                                selectedIndex = index
                                val data = completeData[index]
                                onDataPointClick(data.month, data.income, data.expense)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图例
            ChartLegend(showIncome = showIncome, showExpense = showExpense)
            
            // 显示选中的数值
            if (selectedIndex != null) {
                val index = selectedIndex
                if (index != null) {
                    val selected = completeData[index]
                    SelectedDataDisplay(selected.month, selected.income, selected.expense)
                }
            }
        }
    }
}

/**
 * 计算合适的Y轴最大值
 */
private fun calculateNiceMaxValue(maxData: Double): Double {
    if (maxData <= 0) return 100.0
    
    val magnitude = 10.0.pow(floor(log10(maxData)))
    val normalized = maxData / magnitude
    
    return when {
        normalized <= 1.0 -> 1.0 * magnitude
        normalized <= 2.0 -> 2.0 * magnitude
        normalized <= 5.0 -> 5.0 * magnitude
        else -> 10.0 * magnitude
    }
}

/**
 * Y轴标签
 */
@Composable
private fun YAxisLabels(maxValue: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(0.7f),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        for (i in 4 downTo 0) {
            val value = maxValue * i / 4
            Text(
                text = formatYAxisValue(value),
                color = HorseTheme2026Colors.TextSecondary,
                fontSize = 9.sp,
                textAlign = TextAlign.Right
            )
        }
    }
}

/**
 * 格式化Y轴数值
 */
private fun formatYAxisValue(value: Double): String {
    return when {
        value >= 10000 -> "${(value / 10000).toInt()}w"
        value >= 1000 -> "${(value / 1000).toInt()}k"
        else -> "${value.toInt()}"
    }
}

/**
 * 月度线图 - 带Y轴和点击显示数值
 */
@Composable
private fun MonthlyLineChartWithYAxis(
    data: List<MonthlyChartData>,
    maxValue: Double,
    selectedIndex: Int?,
    showIncome: Boolean,
    showExpense: Boolean,
    onPointClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // 使用Compose实现线图，支持点击
        Column {
            // 图表区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onPointClick(-1) } // 点击空白处取消选择
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padding = 20f
                    val chartHeight = height - 2 * padding

                    // 绘制网格线
                    for (i in 0..4) {
                        val y = padding + chartHeight * i / 4
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(padding, y),
                            end = Offset(width - padding, y),
                            strokeWidth = 1f
                        )
                    }

                    // 计算点位
                    val points = data.mapIndexed { index, item ->
                        val x = padding + (width - 2 * padding) * index / (data.size - 1).coerceAtLeast(1)
                        val incomeY = height - padding - (item.income / maxValue * chartHeight).toFloat()
                        val expenseY = height - padding - (item.expense / maxValue * chartHeight).toFloat()
                        Triple(index, Offset(x, incomeY), Offset(x, expenseY))
                    }

                    // 绘制收入线
                    if (showIncome) {
                        val incomePath = Path().apply {
                            points.forEachIndexed { i, triple ->
                                val offset = triple.second
                                if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                            }
                        }
                        drawPath(path = incomePath, color = IncomeLineColor, style = Stroke(width = 3f))
                    }

                    // 绘制支出线
                    if (showExpense) {
                        val expensePath = Path().apply {
                            points.forEachIndexed { i, triple ->
                                val offset = triple.third
                                if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                            }
                        }
                        drawPath(path = expensePath, color = ExpenseLineColor, style = Stroke(width = 3f))
                    }

                    // 绘制数据点
                    points.forEach { (index, incomeOffset, expenseOffset) ->
                        val isSelected = selectedIndex == index
                        val radius = if (isSelected) 8f else 5f

                        if (showIncome) {
                            drawCircle(color = Color.White, radius = radius + 2, center = incomeOffset)
                            drawCircle(color = IncomeColor, radius = radius, center = incomeOffset)
                        }
                        if (showExpense) {
                            drawCircle(color = Color.White, radius = radius + 2, center = expenseOffset)
                            drawCircle(color = ExpenseColor, radius = radius, center = expenseOffset)
                        }
                    }
                }
                
                // 点击区域检测
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onPointClick(index) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 月份标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { item ->
                    Text(
                        text = item.month,
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = 9.sp,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 月度柱状图 - 带Y轴，同时显示收入和支出
 */
@Composable
private fun MonthlyBarChartWithYAxis(
    data: List<MonthlyChartData>,
    maxValue: Double,
    selectedIndex: Int?,
    showIncome: Boolean,
    showExpense: Boolean,
    onBarClick: (Int) -> Unit
) {
    Column {
        // 柱状图区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val incomeHeight = (item.income / maxValue * 140).toInt().dp.coerceAtLeast(2.dp)
                val expenseHeight = (item.expense / maxValue * 140).toInt().dp.coerceAtLeast(2.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(28.dp)
                        .clickable { onBarClick(index) }
                ) {
                    if (showIncome) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(incomeHeight)
                                .background(
                                    if (isSelected) IncomeColor else IncomeColor.copy(alpha = 0.8f),
                                    RoundedCornerShape(2.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    if (showIncome && showExpense) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (showExpense) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(expenseHeight)
                                .background(
                                    if (isSelected) ExpenseColor else ExpenseColor.copy(alpha = 0.8f),
                                    RoundedCornerShape(2.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 月份标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { item ->
                Text(
                    text = item.month,
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 每周趋势图表 - 显示周一至周日，带Y轴
 */
@Composable
fun WeeklyTrendChartEnhanced(
    weekData: WeeklyChartData,
    chartType: Int,
    onDataPointClick: (String, Double, Double) -> Unit = { _, _, _ -> }
) {
    val maxDataValue = weekData.dailyData.maxOf { maxOf(it.income, it.expense) }
    val maxValue = calculateNiceMaxValue(maxDataValue)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HorseTheme2026Colors.CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 周标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekData.weekLabel,
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = weekData.dateRange,
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

            // 图表区域（带Y轴）
            Row(modifier = Modifier.fillMaxWidth()) {
                YAxisLabels(maxValue, modifier = Modifier.width(40.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (chartType == 0) {
                        WeeklyLineChart(days, weekData.dailyData, maxValue, selectedIndex) { index ->
                            if (index < 0) {
                                selectedIndex = null
                            } else {
                                selectedIndex = index
                                val data = weekData.dailyData[index]
                                onDataPointClick(days[index], data.income, data.expense)
                            }
                        }
                    } else {
                        WeeklyBarChart(days, weekData.dailyData, maxValue, selectedIndex) { index ->
                            if (index < 0) {
                                selectedIndex = null
                            } else {
                                selectedIndex = index
                                val data = weekData.dailyData[index]
                                onDataPointClick(days[index], data.income, data.expense)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            ChartLegend()
            
            // 显示选中的数值
            if (selectedIndex != null) {
                val index = selectedIndex
                if (index != null) {
                    val selected = weekData.dailyData[index]
                    SelectedDataDisplay(days[index], selected.income, selected.expense)
                }
            }
        }
    }
}

/**
 * 每周线图 - 带点击显示数值
 */
@Composable
private fun WeeklyLineChart(
    days: List<String>,
    data: List<DailyChartData>,
    maxValue: Double,
    selectedIndex: Int?,
    onPointClick: (Int) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clickable { onPointClick(-1) }
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val padding = 20f
                val chartHeight = height - 2 * padding

                val points = data.mapIndexed { index, item ->
                    val x = padding + (width - 2 * padding) * index / (data.size - 1)
                    val incomeY = height - padding - (item.income / maxValue * chartHeight).toFloat()
                    val expenseY = height - padding - (item.expense / maxValue * chartHeight).toFloat()
                    Triple(index, Offset(x, incomeY), Offset(x, expenseY))
                }

                val incomePath = Path().apply {
                    points.forEachIndexed { i, triple ->
                        val offset = triple.second
                        if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                    }
                }
                drawPath(path = incomePath, color = IncomeLineColor, style = Stroke(width = 3f))

                val expensePath = Path().apply {
                    points.forEachIndexed { i, triple ->
                        val offset = triple.third
                        if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                    }
                }
                drawPath(path = expensePath, color = ExpenseLineColor, style = Stroke(width = 3f))

                points.forEach { (index, incomeOffset, expenseOffset) ->
                    val isSelected = selectedIndex == index
                    val radius = if (isSelected) 8f else 5f

                    drawCircle(color = Color.White, radius = radius + 2, center = incomeOffset)
                    drawCircle(color = IncomeColor, radius = radius, center = incomeOffset)
                    drawCircle(color = Color.White, radius = radius + 2, center = expenseOffset)
                    drawCircle(color = ExpenseColor, radius = radius, center = expenseOffset)
                }
            }
            
            // 点击区域
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onPointClick(index) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 每周柱状图 - 同时显示收入和支出
 */
@Composable
private fun WeeklyBarChart(
    days: List<String>,
    data: List<DailyChartData>,
    maxValue: Double,
    selectedIndex: Int?,
    onBarClick: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val incomeHeight = (item.income / maxValue * 110).toInt().dp.coerceAtLeast(2.dp)
                val expenseHeight = (item.expense / maxValue * 110).toInt().dp.coerceAtLeast(2.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(40.dp)
                        .clickable { onBarClick(index) }
                ) {
                    // 收入柱（绿色）
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(incomeHeight)
                            .background(
                                if (isSelected) IncomeColor else IncomeColor.copy(alpha = 0.8f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // 支出柱（蓝色）
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(expenseHeight)
                            .background(
                                if (isSelected) ExpenseColor else ExpenseColor.copy(alpha = 0.8f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 日历形式图表
 */
@Composable
fun CalendarChart(
    year: Int,
    month: Int,
    dailyData: List<DailyChartData>,
    onDateClick: (Int, Double, Double) -> Unit = { _, _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme
    val appThemeId = LocalAppThemeId.current
    val isHorseTheme = appThemeId == AppThemeIds.HORSE_2026
    val isFreshTheme = appThemeId == AppThemeIds.FRESH_SCI
    val containerColor = when {
        isHorseTheme -> HorseTheme2026Colors.CardBackground
        isFreshTheme -> Color(0xFFDCEBFF)
        else -> colorScheme.surfaceVariant
    }
    val titleColor = when {
        isHorseTheme -> HorseTheme2026Colors.TextPrimary
        isFreshTheme -> Color(0xFF28476B)
        else -> colorScheme.onSurface
    }
    val hintColor = when {
        isHorseTheme -> HorseTheme2026Colors.TextSecondary
        isFreshTheme -> Color(0xFF7A8FA8)
        else -> colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${year}年${month}月",
                    color = titleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "点击日期查看详情",
                    color = hintColor,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 星期标题
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        text = day,
                        color = hintColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 日历网格
            val calendar = Calendar.getInstance().apply {
                set(year, month - 1, 1)
            }
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val dataMap = dailyData.associateBy { it.date }

            for (week in 0..5) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayOfWeek in 0..6) {
                        val dayNumber = week * 7 + dayOfWeek - firstDayOfWeek + 1

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..daysInMonth) {
                                val dateStr = String.format("%02d-%02d", month, dayNumber)
                                val dayData = dataMap[dateStr]
                                val hasData = dayData != null && (dayData.income > 0 || dayData.expense > 0)

                                CalendarDayCell(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    day = dayNumber,
                                    income = dayData?.income ?: 0.0,
                                    expense = dayData?.expense ?: 0.0,
                                    hasData = hasData,
                                    onClick = {
                                        onDateClick(
                                            dayNumber,
                                            dayData?.income ?: 0.0,
                                            dayData?.expense ?: 0.0
                                        )
                                    }
                                )
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            ChartLegend()
        }
    }
}

/**
 * 日历日期单元格
 */
@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    day: Int,
    income: Double,
    expense: Double,
    hasData: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val appThemeId = LocalAppThemeId.current
    val isHorseTheme = appThemeId == AppThemeIds.HORSE_2026
    val isFreshTheme = appThemeId == AppThemeIds.FRESH_SCI
    val cellBackgroundColor = when {
        !hasData -> Color.Transparent
        isHorseTheme -> HorseTheme2026Colors.CardBackground.copy(alpha = 0.5f)
        isFreshTheme -> Color.White.copy(alpha = 0.85f)
        else -> colorScheme.surface
    }
    val cellBorderColor = when {
        !hasData -> Color.Transparent
        isHorseTheme -> HorseTheme2026Colors.Gold.copy(alpha = 0.3f)
        isFreshTheme -> FreshSciThemeColors.primary.copy(alpha = 0.45f)
        else -> colorScheme.primary.copy(alpha = 0.35f)
    }
    val dayTextColor = when {
        isHorseTheme -> HorseTheme2026Colors.TextPrimary
        isFreshTheme -> Color(0xFF21344D)
        else -> colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cellBackgroundColor)
            .border(
                width = if (hasData) 1.dp else 0.dp,
                color = cellBorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = hasData, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$day",
                color = dayTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (hasData) {
                if (income > 0) {
                    Text(
                        text = "+${income.toInt()}",
                        color = IncomeColor,
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    )
                }
                if (expense > 0) {
                    Text(
                        text = "-${expense.toInt()}",
                        color = ExpenseColor,
                        fontSize = 8.sp,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * 选中的数据显示
 */
@Composable
private fun SelectedDataDisplay(label: String, income: Double, expense: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = HorseTheme2026Colors.Gold.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = HorseTheme2026Colors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                Text(
                    text = "收: ¥${income.toInt()}",
                    color = IncomeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "支: ¥${expense.toInt()}",
                    color = ExpenseColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 图例
 */
@Composable
private fun ChartLegend(
    showIncome: Boolean = true,
    showExpense: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (showIncome) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(IncomeColor, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("收入", color = HorseTheme2026Colors.TextPrimary, fontSize = 12.sp)
            }
        }
        if (showIncome && showExpense) {
            Spacer(modifier = Modifier.width(24.dp))
        }
        if (showExpense) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(ExpenseColor, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("支出", color = HorseTheme2026Colors.TextPrimary, fontSize = 12.sp)
            }
        }
    }
}

/**
 * 空数据提示
 */
@Composable
fun EmptyChartMessage(message: String = "暂无数据") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = HorseTheme2026Colors.TextSecondary,
            fontSize = 16.sp
        )
    }
}

// 数据类
data class MonthlyChartData(
    val month: String,
    val income: Double,
    val expense: Double
)

data class DailyChartData(
    val date: String,
    val income: Double,
    val expense: Double
)

data class WeeklyChartData(
    val weekLabel: String,
    val dateRange: String,
    val dailyData: List<DailyChartData>
)
