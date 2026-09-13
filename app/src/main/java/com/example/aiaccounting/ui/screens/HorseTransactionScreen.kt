package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors
import com.example.aiaccounting.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorseTransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateToAddTransaction: () -> Unit = {}
) {
    var selectedView by remember { mutableStateOf(0) }
    val viewOptions = listOf("列表", "图表")

    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${currentYear}年${currentMonth}月",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HorseTheme2026Colors.TextPrimary
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
                containerColor = HorseTheme2026Colors.BlueCard,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "记账")
            }
        }
    ) { padding ->
        HorseBackground {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    // 蓝色卡片 - 月度收支 - 真实数据
                    BlueMonthlyCard(
                        income = uiState.monthIncome,
                        expense = uiState.monthExpense,
                        balance = uiState.monthBalance
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 视图切换按钮
                    ViewToggle(
                        options = viewOptions,
                        selectedIndex = selectedView,
                        onSelect = { selectedView = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 根据选中视图显示不同内容 - 使用真实数据
                    when (selectedView) {
                        0 -> TransactionListView(transactions = transactions)
                        1 -> TransactionChartView(
                            transactions = transactions,
                            categories = categories
                        )
                    }
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
fun ViewToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(HorseTheme2026Colors.CardBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) HorseTheme2026Colors.Gold
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (index == 0) Icons.Default.List else Icons.Default.PieChart,
                        contentDescription = option,
                        tint = if (isSelected) Color.Black else HorseTheme2026Colors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = option,
                        color = if (isSelected) Color.Black else HorseTheme2026Colors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionListView(transactions: List<Transaction>) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    if (transactions.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                SafeHorseTransactionCard(transaction = transaction, dateFormat = dateFormat)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    } else {
        // 空状态
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = HorseTheme2026Colors.TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无交易记录",
                    color = HorseTheme2026Colors.TextSecondary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SafeHorseTransactionCard(
    transaction: Transaction,
    dateFormat: SimpleDateFormat
) {
    val safeNote = remember(transaction.note) { safeTransactionNote(transaction.note) }
    val safeDate = remember(transaction.date) {
        safeTransactionDateText(transaction.date, dateFormat)
    }
    val safeAmount = remember(transaction.amount) {
        safeTransactionAmountText(transaction.amount)
    }

    TransactionCard(
        transaction = transaction,
        dateFormat = dateFormat,
        noteText = safeNote,
        dateText = safeDate,
        amountText = safeAmount
    )
}

internal fun safeTransactionNote(note: String): String {
    return note.takeIf { it.isNotBlank() } ?: "未备注"
}

internal fun safeTransactionDateText(
    timestamp: Long,
    dateFormat: SimpleDateFormat
): String {
    return runCatching { dateFormat.format(Date(timestamp)) }
        .getOrElse { "时间异常" }
}

internal fun safeTransactionAmountText(amount: Double): String {
    if (!amount.isFinite()) return "0.00"
    return runCatching { String.format("%.2f", amount) }
        .getOrElse { "0.00" }
}

@Composable
fun TransactionChartView(
    transactions: List<Transaction>,
    categories: List<com.example.aiaccounting.data.local.entity.Category>
) {
    // 计算分类统计数据，使用真实的分类名称
    val categoryStats = rememberTransactionCategoryStats(transactions, categories)
    val totalExpense = categoryStats.sumOf { it.amount }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 饼图卡片
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
                        text = "支出分布",
                        color = HorseTheme2026Colors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.DonutLarge,
                        contentDescription = null,
                        tint = HorseTheme2026Colors.Gold,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (categoryStats.isNotEmpty()) {
                    // 基于真实数据的饼图
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TransactionPieChart(categoryStats = categoryStats, totalExpense = totalExpense)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 图例
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        categoryStats.take(4).forEach { stat ->
                            val percent = if (totalExpense > 0) (stat.amount / totalExpense * 100).toInt() else 0
                            PieLegendItem(
                                color = stat.color,
                                label = stat.name,
                                percent = "$percent%"
                            )
                        }
                    }
                } else {
                    Text(
                        text = "暂无支出数据",
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 柱状图卡片
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
                        text = "分类支出对比",
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

                if (categoryStats.isNotEmpty()) {
                    TransactionBarChart(categoryStats = categoryStats)
                } else {
                    Text(
                        text = "暂无数据",
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionPieChart(categoryStats: List<TransactionCategoryStat>, totalExpense: Double) {
    if (totalExpense <= 0) return

    Box(
        modifier = Modifier.size(150.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
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
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - radius,
                        centerY - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }

            // 中间空心
            drawCircle(
                color = HorseTheme2026Colors.CardBackground,
                radius = radius * 0.5f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }

        // 中心文字
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "总支出",
                color = HorseTheme2026Colors.TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "¥${String.format("%.2f", totalExpense)}",
                color = HorseTheme2026Colors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PieLegendItem(color: Color, label: String, percent: String) {
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
            color = HorseTheme2026Colors.TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = percent,
            color = HorseTheme2026Colors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TransactionBarChart(categoryStats: List<TransactionCategoryStat>) {
    val maxAmount = categoryStats.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val totalAmount = categoryStats.sumOf { it.amount }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 水平柱状图 - 显示完整分类名称和金额
        categoryStats.take(6).forEachIndexed { index, stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类名称（完整显示）
                Text(
                    text = stat.name,
                    color = HorseTheme2026Colors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(50.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 水平柱状图
                val barWidthPercent = (stat.amount / maxAmount).coerceIn(0.15, 1.0)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(barWidthPercent.toFloat())
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        stat.color,
                                        stat.color.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 金额和百分比
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(70.dp)
                ) {
                    Text(
                        text = "¥${String.format("%.2f", stat.amount)}",
                        color = HorseTheme2026Colors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val percent = if (totalAmount > 0) (stat.amount / totalAmount * 100).toInt() else 0
                    Text(
                        text = "$percent%",
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            if (index < categoryStats.size - 1 && index < 5) {
                HorizontalDivider(
                    color = HorseTheme2026Colors.TextSecondary.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
            }
        }

        // 如果有更多分类，显示提示
        if (categoryStats.size > 6) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "还有 ${categoryStats.size - 6} 个分类...",
                color = HorseTheme2026Colors.TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun BlueMonthlyCard(
    income: Double,
    expense: Double,
    balance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = HorseTheme2026Colors.BlueCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 祥云装饰 - 左侧
            CloudDecoration(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            // 祥云装饰 - 右侧
            CloudDecoration(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp),
                color = Color.White.copy(alpha = 0.3f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(HorseTheme2026Colors.Gold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = HorseTheme2026Colors.Gold,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 收支数据 - 真实数据
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "收入",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+¥${String.format("%.2f", income)}",
                            color = HorseTheme2026Colors.Income,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "支出",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-¥${String.format("%.2f", expense)}",
                            color = HorseTheme2026Colors.Expense,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "结余",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "¥${String.format("%.2f", balance)}",
                            color = HorseTheme2026Colors.Gold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    dateFormat: SimpleDateFormat,
    noteText: String = transaction.note.takeIf { it.isNotBlank() } ?: "未备注",
    dateText: String = dateFormat.format(Date(transaction.date)),
    amountText: String = String.format("%.2f", transaction.amount)
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) HorseTheme2026Colors.Expense else HorseTheme2026Colors.Income
    val amountPrefix = if (isExpense) "-" else "+"

    // 根据分类选择图标
    val icon = when (transaction.categoryId) {
        1L -> Icons.Default.Restaurant
        2L -> Icons.Default.ShoppingCart
        3L -> Icons.Default.DirectionsBus
        4L -> Icons.Default.Movie
        5L -> Icons.Default.Home
        6L -> Icons.Default.LocalHospital
        7L -> Icons.Default.School
        8L -> Icons.Default.Coffee
        else -> Icons.Default.Receipt
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = HorseTheme2026Colors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isExpense)
                                HorseTheme2026Colors.Expense.copy(alpha = 0.2f)
                            else
                                HorseTheme2026Colors.Income.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = noteText,
                        color = HorseTheme2026Colors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateText,
                        color = HorseTheme2026Colors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "$amountPrefix¥$amountText",
                color = amountColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 分类统计数据类
data class TransactionCategoryStat(
    val name: String,
    val amount: Double,
    val color: Color
)

@Composable
private fun rememberTransactionCategoryStats(
    transactions: List<Transaction>,
    categories: List<com.example.aiaccounting.data.local.entity.Category>
): List<TransactionCategoryStat> {
    return remember(transactions, categories) {
        val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }

        // 创建分类ID到名称的映射
        val categoryMap = categories.associateBy { it.id }

        expenseTransactions
            .groupBy { it.categoryId }
            .map { (categoryId, transList) ->
                val amount = transList.sumOf { kotlin.math.abs(it.amount) }
                // 从分类映射中获取真实名称，如果没有则显示"未分类"
                val category = categoryMap[categoryId]
                val categoryName = category?.name ?: "未分类"
                // 使用分类的颜色，如果没有则使用默认颜色
                val color = category?.color?.let { parseColor(it) } ?: when (categoryId % 5) {
                    0L -> HorseTheme2026Colors.Expense
                    1L -> HorseTheme2026Colors.Gold
                    2L -> HorseTheme2026Colors.Income
                    3L -> HorseTheme2026Colors.BlueCard
                    else -> HorseTheme2026Colors.Warning
                }
                TransactionCategoryStat(
                    name = categoryName,
                    amount = amount,
                    color = color
                )
            }
            .sortedByDescending { it.amount }
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

// FlowRow 简单实现
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = horizontalArrangement
        ) {
            content()
        }
    }
}
