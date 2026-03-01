package com.example.aiaccounting.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.StatisticsViewModel
import com.example.aiaccounting.utils.NumberUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("概览", "分类", "趋势")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab切换
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // 内容区域
            when (selectedTab) {
                0 -> OverviewTab(uiState = uiState)
                1 -> CategoryTab(uiState = uiState)
                2 -> TrendTab(uiState = uiState)
            }
        }
    }
}

@Composable
fun OverviewTab(uiState: StatisticsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 月度概览卡片
        MonthlyOverviewCard(uiState)

        // 收支对比饼图
        if (uiState.monthlyIncome > 0 || uiState.monthlyExpense > 0) {
            IncomeExpensePieChart(uiState)
        }

        // 最近6个月收支对比
        if (uiState.monthlyTrend.isNotEmpty()) {
            MonthlyTrendChart(uiState.monthlyTrend)
        }
    }
}

@Composable
fun MonthlyOverviewCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "本月概览",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 结余
            val balance = uiState.monthlyIncome - uiState.monthlyExpense
            Text(
                text = NumberUtils.formatMoney(balance),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )

            Text(
                text = "结余",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = NumberUtils.formatMoney(uiState.monthlyIncome),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "收入",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = NumberUtils.formatMoney(uiState.monthlyExpense),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                    Text(
                        text = "支出",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 预算使用率
            if (uiState.monthlyBudget > 0) {
                val budgetUsage = (uiState.monthlyExpense / uiState.monthlyBudget * 100).toInt()
                LinearProgressIndicator(
                    progress = (budgetUsage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (budgetUsage > 100) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "预算使用: $budgetUsage%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun IncomeExpensePieChart(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "收支占比",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                factory = { context ->
                    PieChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        isRotationEnabled = true
                        isHighlightPerTapEnabled = true
                        legend.isEnabled = true
                        legend.textSize = 12f
                    }
                },
                update = { chart ->
                    val entries = listOf(
                        PieEntry(uiState.monthlyIncome.toFloat(), "收入"),
                        PieEntry(uiState.monthlyExpense.toFloat(), "支出")
                    )

                    val dataSet = PieDataSet(entries, "").apply {
                        colors = listOf(
                            ColorTemplate.rgb("#4CAF50"),  // 绿色 - 收入
                            ColorTemplate.rgb("#F44336")   // 红色 - 支出
                        )
                        valueTextSize = 14f
                        valueTextColor = android.graphics.Color.WHITE
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return "¥${value.toInt()}"
                            }
                        }
                    }

                    chart.data = PieData(dataSet)
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
fun MonthlyTrendChart(trendData: List<MonthlyData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "收支趋势",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                factory = { context ->
                    BarChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        legend.isEnabled = true
                        legend.textSize = 12f

                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            granularity = 1f
                            setDrawGridLines(false)
                        }

                        axisLeft.apply {
                            setDrawGridLines(true)
                            axisMinimum = 0f
                        }

                        axisRight.isEnabled = false
                    }
                },
                update = { chart ->
                    val incomeEntries = trendData.mapIndexed { index, data ->
                        BarEntry(index.toFloat(), data.income.toFloat())
                    }

                    val expenseEntries = trendData.mapIndexed { index, data ->
                        BarEntry(index.toFloat(), data.expense.toFloat())
                    }

                    val incomeDataSet = BarDataSet(incomeEntries, "收入").apply {
                        color = ColorTemplate.rgb("#4CAF50")
                        valueTextSize = 10f
                    }

                    val expenseDataSet = BarDataSet(expenseEntries, "支出").apply {
                        color = ColorTemplate.rgb("#F44336")
                        valueTextSize = 10f
                    }

                    chart.xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index in trendData.indices) {
                                trendData[index].month
                            } else ""
                        }
                    }

                    chart.data = BarData(incomeDataSet, expenseDataSet).apply {
                        barWidth = 0.3f
                    }
                    chart.groupBars(0f, 0.4f, 0f)
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
fun CategoryTab(uiState: StatisticsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 支出分类饼图
        if (uiState.expenseByCategory.isNotEmpty()) {
            CategoryPieChart(
                title = "支出分类",
                data = uiState.expenseByCategory,
                colors = ColorTemplate.MATERIAL_COLORS.toList()
            )
        }

        // 收入分类饼图
        if (uiState.incomeByCategory.isNotEmpty()) {
            CategoryPieChart(
                title = "收入分类",
                data = uiState.incomeByCategory,
                colors = ColorTemplate.COLORFUL_COLORS.toList()
            )
        }

        // 分类详情列表
        if (uiState.expenseByCategory.isNotEmpty()) {
            CategoryDetailList(
                title = "支出详情",
                data = uiState.expenseByCategory,
                total = uiState.monthlyExpense
            )
        }
    }
}

@Composable
fun CategoryPieChart(
    title: String,
    data: List<CategoryStat>,
    colors: List<Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                factory = { context ->
                    PieChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        isRotationEnabled = true
                        isHighlightPerTapEnabled = true
                        legend.isEnabled = true
                        legend.textSize = 10f
                        legend.isWordWrapEnabled = true
                    }
                },
                update = { chart ->
                    val entries = data.map { stat ->
                        PieEntry(stat.amount.toFloat(), stat.name)
                    }

                    val dataSet = PieDataSet(entries, "").apply {
                        this.colors = colors.take(data.size)
                        valueTextSize = 12f
                        valueTextColor = android.graphics.Color.WHITE
                        valueFormatter = PercentFormatter(chart)
                    }

                    chart.data = PieData(dataSet)
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
fun CategoryDetailList(
    title: String,
    data: List<CategoryStat>,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            data.forEach { stat ->
                val percentage = if (total > 0) (stat.amount / total * 100).toInt() else 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 颜色指示器
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = Color(stat.color),
                                shape = MaterialTheme.shapes.small
                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // 分类名称
                    Text(
                        text = stat.name,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // 金额和百分比
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = NumberUtils.formatMoney(stat.amount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$percentage%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (stat != data.last()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun TrendTab(uiState: StatisticsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 年度趋势折线图
        if (uiState.yearlyTrend.isNotEmpty()) {
            YearlyTrendChart(uiState.yearlyTrend)
        }

        // 月度对比
        if (uiState.monthlyComparison.isNotEmpty()) {
            MonthlyComparisonCard(uiState.monthlyComparison)
        }
    }
}

@Composable
fun YearlyTrendChart(trendData: List<MonthlyData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "年度收支趋势",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                factory = { context ->
                    LineChart(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        description.isEnabled = false
                        legend.isEnabled = true
                        legend.textSize = 12f

                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            granularity = 1f
                            setDrawGridLines(false)
                        }

                        axisLeft.apply {
                            setDrawGridLines(true)
                        }

                        axisRight.isEnabled = false
                    }
                },
                update = { chart ->
                    val incomeEntries = trendData.mapIndexed { index, data ->
                        Entry(index.toFloat(), data.income.toFloat())
                    }

                    val expenseEntries = trendData.mapIndexed { index, data ->
                        Entry(index.toFloat(), data.expense.toFloat())
                    }

                    val balanceEntries = trendData.mapIndexed { index, data ->
                        Entry(index.toFloat(), (data.income - data.expense).toFloat())
                    }

                    val incomeDataSet = LineDataSet(incomeEntries, "收入").apply {
                        color = ColorTemplate.rgb("#4CAF50")
                        setDrawCircles(true)
                        setDrawValues(false)
                        lineWidth = 2f
                    }

                    val expenseDataSet = LineDataSet(expenseEntries, "支出").apply {
                        color = ColorTemplate.rgb("#F44336")
                        setDrawCircles(true)
                        setDrawValues(false)
                        lineWidth = 2f
                    }

                    val balanceDataSet = LineDataSet(balanceEntries, "结余").apply {
                        color = ColorTemplate.rgb("#2196F3")
                        setDrawCircles(true)
                        setDrawValues(false)
                        lineWidth = 2f
                    }

                    chart.xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index in trendData.indices) {
                                trendData[index].month
                            } else ""
                        }
                    }

                    chart.data = LineData(incomeDataSet, expenseDataSet, balanceDataSet)
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
fun MonthlyComparisonCard(comparisonData: List<MonthlyComparison>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "月度对比",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            comparisonData.forEach { data ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = data.month,
                        fontSize = 14.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "+${NumberUtils.formatMoney(data.income)}",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "-${NumberUtils.formatMoney(data.expense)}",
                            fontSize = 14.sp,
                            color = Color(0xFFF44336)
                        )
                    }
                }

                if (data != comparisonData.last()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// 数据类
data class StatisticsUiState(
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val expenseByCategory: List<CategoryStat> = emptyList(),
    val incomeByCategory: List<CategoryStat> = emptyList(),
    val monthlyTrend: List<MonthlyData> = emptyList(),
    val yearlyTrend: List<MonthlyData> = emptyList(),
    val monthlyComparison: List<MonthlyComparison> = emptyList()
)

data class CategoryStat(
    val name: String,
    val amount: Double,
    val color: Int
)

data class MonthlyData(
    val month: String,
    val income: Double,
    val expense: Double
)

data class MonthlyComparison(
    val month: String,
    val income: Double,
    val expense: Double
)
