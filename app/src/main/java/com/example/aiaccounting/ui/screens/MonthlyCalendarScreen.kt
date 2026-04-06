package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.utils.NumberUtils
import com.example.aiaccounting.ui.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCalendarScreen(
    year: Int,
    month: Int,
    onBack: () -> Unit,
    onNavigateToDayTransactions: (Int, Int, Int) -> Unit = { _, _, _ -> },
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val statistics by viewModel.statistics.collectAsState()
    var displayYear by remember(year) { mutableIntStateOf(year) }
    var displayMonth by remember(month) { mutableIntStateOf(month) }
    var selectedDay by remember { mutableIntStateOf(-1) }

    LaunchedEffect(displayYear, displayMonth) {
        selectedDay = -1
        viewModel.setTimeFilter(String.format("%04d-%02d", displayYear, displayMonth))
    }

    val calendarData = statistics.dailyTrend.map {
        DailyChartData(it.date, it.income, it.expense)
    }
    val selectedDayData = statistics.dailyTrend.firstOrNull {
        it.date == String.format("%02d-%02d", displayMonth, selectedDay)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${displayYear}年${displayMonth}月日历", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("月度概览", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("收入：¥${NumberUtils.formatMoney(statistics.totalIncome)}", color = MaterialTheme.colorScheme.primary)
                    Text("支出：¥${NumberUtils.formatMoney(statistics.totalExpense)}", color = MaterialTheme.colorScheme.error)
                    Text(
                        "结余：¥${NumberUtils.formatMoney(statistics.totalIncome - statistics.totalExpense)}",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayMonth == 1) {
                                    displayYear -= 1
                                    displayMonth = 12
                                } else {
                                    displayMonth -= 1
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                        }
                        Text(
                            text = "${displayYear}年${displayMonth}月",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                if (displayMonth == 12) {
                                    displayYear += 1
                                    displayMonth = 1
                                } else {
                                    displayMonth += 1
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                        }
                    }

                    CalendarChart(
                        year = displayYear,
                        month = displayMonth,
                        dailyData = calendarData,
                        onDateClick = { day, _, _ ->
                            selectedDay = day
                        }
                    )
                }
            }

            if (selectedDay > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${displayMonth}月${selectedDay}日详情",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "收入：¥${NumberUtils.formatMoney(selectedDayData?.income ?: 0.0)}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "支出：¥${NumberUtils.formatMoney(selectedDayData?.expense ?: 0.0)}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (selectedDayData == null || ((selectedDayData.income == 0.0) && (selectedDayData.expense == 0.0))) {
                                "当天暂无收支记录"
                            } else {
                                "点击进入当天明细"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { selectedDay = -1 }) {
                                Text("收起")
                            }
                            Button(
                                onClick = { onNavigateToDayTransactions(displayYear, displayMonth, selectedDay) },
                                enabled = selectedDayData != null && (selectedDayData.income > 0 || selectedDayData.expense > 0)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text("查看当天明细")
                            }
                        }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("点击日历中的日期查看当天收支详情", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
