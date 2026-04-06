@file:Suppress("DEPRECATION")

package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.components.FreshSciBackground
import com.example.aiaccounting.ui.theme.FreshSciThemeColors
import com.example.aiaccounting.ui.viewmodel.TransactionListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshTransactionScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    onNavigateToAddTransaction: () -> Unit = {}
) {
    var selectedView by remember { mutableStateOf(0) }
    val viewOptions = listOf("列表", "图表")

    val uiState by viewModel.uiState.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val selectedYear = uiState.selectedYear
    val selectedMonth = uiState.selectedMonth

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${selectedYear}年${selectedMonth}月",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2E)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = FreshSciThemeColors.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "记账")
            }
        }
    ) { padding ->
        FreshSciBackground {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    // 顶部月份切换
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedYear}年${selectedMonth}月",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = FreshSciThemeColors.primary
                        )
                        Row {
                            IconButton(onClick = { viewModel.prevMonth() }) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "上月",
                                    tint = FreshSciThemeColors.primary
                                )
                            }
                            IconButton(onClick = { viewModel.nextMonth() }) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "下月",
                                    tint = FreshSciThemeColors.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 统计卡片
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "收入",
                            amount = formatAmount(monthlyStats.income),
                            icon = Icons.Default.ArrowDownward,
                            color = Color(0xFF4CAF50),
                            primaryColor = FreshSciThemeColors.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "支出",
                            amount = formatAmount(monthlyStats.expense),
                            icon = Icons.Default.ArrowUpward,
                            color = Color(0xFFFF6B35),
                            primaryColor = FreshSciThemeColors.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "结余",
                            amount = formatAmount(monthlyStats.income - monthlyStats.expense),
                            icon = Icons.Default.AccountBalanceWallet,
                            color = FreshSciThemeColors.primary,
                            primaryColor = FreshSciThemeColors.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 视图切换
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewOptions.forEachIndexed { index, option ->
                            FilterChip(
                                selected = selectedView == index,
                                onClick = { selectedView = index },
                                label = { Text(option, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = if (selectedView == index) Color.White else FreshSciThemeColors.primary,
                                    containerColor = if (selectedView == index) FreshSciThemeColors.primary else Color(0xFFE8F0FF),
                                    selectedLabelColor = Color.White,
                                    selectedContainerColor = FreshSciThemeColors.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 内容区域
                    if (selectedView == 0) {
                        // 列表视图
                        FreshTransactionList(
                            transactions = filteredTransactions,
                            categories = categories,
                            onNavigateToEdit = { /* TODO: edit */ },
                            onNavigateToAdd = onNavigateToAddTransaction
                        )
                    } else {
                        // 图表视图
                        FreshTransactionChartView(
                            transactions = filteredTransactions,
                            categories = categories
                        )
                    }
                }

                SciFiBottomLine(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    primaryColor = FreshSciThemeColors.primary
                )
            }
        }
    }
}

@Composable
private fun FreshTransactionList(
    transactions: List<Transaction>,
    categories: List<com.example.aiaccounting.data.local.entity.Category>,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(transactions) { transaction ->
            val category = categoryMap[transaction.categoryId]
            val isExpense = transaction.type == TransactionType.EXPENSE
            val amountColor = if (isExpense) Color(0xFFFF6B35) else Color(0xFF4CAF50)
            val amountPrefix = if (isExpense) "-" else "+"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF).copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(amountColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isExpense) Icons.Default.ShoppingCart else Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = amountColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = transaction.note.takeIf { it.isNotBlank() } ?: "未备注",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0D1B2E)
                            )
                            Text(
                                text = category?.name ?: "未分类",
                                fontSize = 12.sp,
                                color = Color(0xFF656D78)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$amountPrefix¥${String.format("%.2f", transaction.amount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                        Text(
                            text = dateFormat.format(Date(transaction.date)),
                            fontSize = 11.sp,
                            color = Color(0xFF95A5A6)
                        )
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                FreshEmptyState(
                    message = "暂无交易记录",
                    icon = Icons.Filled.ReceiptLong,
                    primaryColor = FreshSciThemeColors.primary
                )
            }
        }
    }
}

@Composable
private fun FreshEmptyState(
    message: String,
    icon: ImageVector,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF).copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFF656D78)
            )
        }
    }
}

@Composable
private fun FreshTransactionChartView(
    transactions: List<Transaction>,
    categories: List<com.example.aiaccounting.data.local.entity.Category>
) {
    val categoryStats = remember(transactions, categories) {
        computeFreshCategoryStats(transactions, categories)
    }
    val totalExpense = categoryStats.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 饼图卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF).copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "支出分布",
                            color = Color(0xFF0D1B2E),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = null,
                            tint = FreshSciThemeColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryStats.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FreshPieChart(
                                categoryStats = categoryStats,
                                totalExpense = totalExpense,
                                cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.88f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 图例
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            categoryStats.take(4).forEach { stat ->
                                val percent = if (totalExpense > 0) (stat.amount / totalExpense * 100).toInt() else 0
                                FreshPieLegendItem(
                                    color = stat.color,
                                    label = stat.name,
                                    percent = "$percent%"
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "暂无支出数据",
                            color = Color(0xFF656D78),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }
        }

        // 柱状图卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF).copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "分类支出对比",
                            color = Color(0xFF0D1B2E),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = FreshSciThemeColors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryStats.isNotEmpty()) {
                        FreshBarChart(categoryStats = categoryStats)
                    } else {
                        Text(
                            text = "暂无数据",
                            color = Color(0xFF656D78),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreshPieChart(
    categoryStats: List<FreshCategoryStat>,
    totalExpense: Double,
    cardBackground: Color
) {
    if (totalExpense <= 0) return

    Box(modifier = Modifier.size(150.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 - 10

            var startAngle = -90f
            categoryStats.forEach { stat ->
                val sweepAngle = (stat.amount / totalExpense * 360).toFloat()
                drawArc(
                    color = stat.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }

            // 中间空心
            drawCircle(
                color = cardBackground,
                radius = radius * 0.5f,
                center = Offset(centerX, centerY)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "总支出",
                color = Color(0xFF656D78),
                fontSize = 12.sp
            )
            Text(
                text = "¥${String.format("%.2f", totalExpense)}",
                color = Color(0xFF0D1B2E),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FreshPieLegendItem(color: Color, label: String, percent: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFF656D78),
            fontSize = 12.sp
        )
        Text(
            text = percent,
            color = Color(0xFF0D1B2E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FreshBarChart(categoryStats: List<FreshCategoryStat>) {
    val maxAmount = categoryStats.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0

    Column(modifier = Modifier.fillMaxWidth()) {
        categoryStats.take(6).forEachIndexed { index, stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.name,
                    color = Color(0xFF0D1B2E),
                    fontSize = 12.sp,
                    modifier = Modifier.width(60.dp),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(FreshSciThemeColors.primary.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((stat.amount / maxAmount).toFloat())
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                listOf(
                                    FreshSciThemeColors.primary,
                                    Color(0xFF4CAF50),
                                    Color(0xFFFF6B35),
                                    Color(0xFF2196F3),
                                    Color(0xFF9C27B0),
                                    Color(0xFFFFEB3B)
                                )[index % 6]
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "¥${String.format("%.0f", stat.amount)}",
                    color = Color(0xFF0D1B2E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}

private data class FreshCategoryStat(
    val name: String,
    val amount: Double,
    val color: Color
)

private fun computeFreshCategoryStats(
    transactions: List<Transaction>,
    categories: List<com.example.aiaccounting.data.local.entity.Category>
): List<FreshCategoryStat> {
    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
    val categoryMap = categories.associateBy { it.id }
    val colors = listOf(
        FreshSciThemeColors.primary,
        Color(0xFF4CAF50),
        Color(0xFFFF6B35),
        Color(0xFF2196F3),
        Color(0xFF9C27B0),
        Color(0xFFFFEB3B)
    )

    return expenseTransactions
        .groupBy { it.categoryId }
        .map { (categoryId, transList) ->
            val amount = transList.sumOf { kotlin.math.abs(it.amount) }
            val category = categoryMap[categoryId]
            val categoryName = category?.name ?: "未分类"
            val fallbackColor = freshCategoryFallbackColor(categoryId, colors)
            val color = category?.color?.let {
                try {
                    Color(android.graphics.Color.parseColor(it))
                } catch (e: Exception) {
                    fallbackColor
                }
            } ?: fallbackColor
            FreshCategoryStat(name = categoryName, amount = amount, color = color)
        }
        .sortedByDescending { it.amount }
}

internal fun freshCategoryFallbackColor(categoryId: Long, colors: List<Color>): Color {
    if (colors.isEmpty()) return FreshSciThemeColors.primary
    val safeIndex = Math.floorMod(categoryId.toInt(), colors.size)
    return colors[safeIndex]
}

@Composable
fun StatCard(
    label: String,
    amount: String,
    icon: ImageVector,
    color: Color,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF).copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D1B2E)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = primaryColor.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount >= 10000) {
        "${(amount / 10000).toInt()}万"
    } else {
        "¥${String.format("%.2f", amount)}"
    }
}
