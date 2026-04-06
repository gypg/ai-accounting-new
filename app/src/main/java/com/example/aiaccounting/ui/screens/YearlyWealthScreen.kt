package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.aiaccounting.ui.components.charts.PieChart
import com.example.aiaccounting.ui.components.charts.TrendChart
import com.example.aiaccounting.utils.NumberUtils
import com.example.aiaccounting.ui.viewmodel.OverviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyWealthScreen(
    year: Int,
    onBack: () -> Unit,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val yearlyTrendData by viewModel.yearlyTrendData.collectAsState()
    val yearlyAnalysis by viewModel.yearlyAnalysis.collectAsState()
    val yearlyAnalysisLoading by viewModel.yearlyAnalysisLoading.collectAsState()
    val yearlyAnalysisError by viewModel.yearlyAnalysisError.collectAsState()
    var displayYear by remember(year) { mutableIntStateOf(year) }

    LaunchedEffect(displayYear) {
        viewModel.setSelectedYear(displayYear)
    }

    val balance = monthlyStats.totalIncome - monthlyStats.totalExpense
    val expenseCategories = viewModel.buildYearlyExpenseCategoryStats()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${displayYear}年度趋势", fontWeight = FontWeight.Bold) },
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayYear -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一年")
                    }
                    Text(
                        text = "${displayYear}年财富趋势",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { displayYear += 1 }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一年")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("年度汇总", fontWeight = FontWeight.Bold)
                    Text("收入：¥${NumberUtils.formatMoney(monthlyStats.totalIncome)}", color = MaterialTheme.colorScheme.primary)
                    Text("支出：¥${NumberUtils.formatMoney(monthlyStats.totalExpense)}", color = MaterialTheme.colorScheme.error)
                    Text("结余：¥${NumberUtils.formatMoney(balance)}", color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = if (balance >= 0) "这一年整体仍保持净增长，可重点优化支出结构。" else "这一年整体处于净流出，建议重点检查高频大额支出。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("年度趋势", fontWeight = FontWeight.Bold)
                    TrendChart(
                        data = yearlyTrendData,
                        modifier = Modifier.fillMaxWidth(),
                        showIncome = true,
                        showExpense = true
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("亏损去向 / 支出流向", fontWeight = FontWeight.Bold)
                    if (expenseCategories.isEmpty()) {
                        Text("该年度暂无支出分类数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        PieChart(
                            data = expenseCategories,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("AI 年度分析", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (yearlyAnalysis == null) "点击后调用云端 AI 生成年度财富分析" else "已缓存本年度 AI 分析，可随时重新生成",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        Button(onClick = { viewModel.generateYearlyAnalysis() }, enabled = !yearlyAnalysisLoading) {
                            if (yearlyAnalysisLoading) {
                                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("一键 AI 分析")
                            }
                        }
                    }

                    yearlyAnalysisError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    Text(
                        text = yearlyAnalysis?.analysisText ?: "暂无已保存的年度 AI 分析结果。",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
