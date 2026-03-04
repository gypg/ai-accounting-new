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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.recentTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val yearlyTrendData by viewModel.yearlyTrendData.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()
    val weekStats by viewModel.weekStats.collectAsState()

    // 底部菜单状态
    var showAddMenu by remember { mutableStateOf(false) }

    // 底部弹出菜单
    AddTransactionMenu(
        isVisible = showAddMenu,
        onDismiss = { showAddMenu = false },
        onAIAccounting = onNavigateToAI,
        onManualAccounting = onNavigateToAddTransaction
    )

    Scaffold(
        floatingActionButton = {
            // AI+ 按钮 - 整合机器人和记账功能
            FloatingActionButton(
                onClick = { showAddMenu = true },
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
                totalAssets = accounts.sumOf { it.balance },
                onEyeClick = { viewModel.toggleBalanceVisibility() },
                isVisible = uiState.isBalanceVisible
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 年度统计
            YearlyStatsCard(
                year = Calendar.getInstance().get(Calendar.YEAR),
                income = monthlyStats.totalIncome,
                expense = monthlyStats.totalExpense,
                balance = monthlyStats.totalIncome - monthlyStats.totalExpense
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 月度概览和账户明细
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 月度概览
                MonthlyOverviewCard(
                    month = Calendar.getInstance().get(Calendar.MONTH) + 1,
                    income = monthlyStats.monthlyIncome,
                    expense = monthlyStats.monthlyExpense,
                    modifier = Modifier.weight(1f)
                )

                // 账户明细
                AccountsOverviewCard(
                    accounts = accounts.take(4),
                    modifier = Modifier.weight(1f)
                )
            }

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
            YearlyTrendCard(monthlyData = yearlyTrendData)

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
    balance: Double
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
                IconButton(onClick = { /* 上一年 */ }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上一年")
                }
                Text(
                    text = "${year}年度",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { /* 下一年 */ }) {
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
fun MonthlyOverviewCard(
    month: Int,
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                text = "${month}月",
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                        // 账户类型图标
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

@Composable
fun TodayWeekStatsCard(
    todayIncome: Double,
    todayExpense: Double,
    weekIncome: Double,
    weekExpense: Double
) {
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
                        text = "本周",
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
    monthlyData: List<com.example.aiaccounting.ui.components.charts.MonthlyData> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${Calendar.getInstance().get(Calendar.YEAR)}年度趋势",
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
                        .height(180.dp)
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
