@file:Suppress("DEPRECATION")

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.*
import com.example.aiaccounting.ui.components.NewYearHorseBackground
import com.example.aiaccounting.ui.theme.NewYearHorseThemeColors
import com.example.aiaccounting.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

// ==================== 安全的主题颜色包装器 ====================
// 防止主题切换时因颜色未初始化导致的崩溃
private fun safePrimary(): Color = if (NewYearHorseThemeColors.primary.value != 0UL) NewYearHorseThemeColors.primary else Color(0xFFD64040)
private fun safeOnSurface(): Color = if (NewYearHorseThemeColors.onSurface.value != 0UL) NewYearHorseThemeColors.onSurface else Color(0xFF1E293B)
private fun safeOnPrimary(): Color = if (NewYearHorseThemeColors.onPrimary.value != 0UL) NewYearHorseThemeColors.onPrimary else Color(0xFFFFFFFF)
private fun safeOnSurfaceVariant(): Color = if (NewYearHorseThemeColors.onSurfaceVariant.value != 0UL) NewYearHorseThemeColors.onSurfaceVariant else Color(0xFF475569)
private fun safeSurface(): Color = if (NewYearHorseThemeColors.surface.value != 0UL) NewYearHorseThemeColors.surface else Color(0xFFFFFAF5)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun NewYearHorseTransactionScreen(
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

    // Safe theme colors with fallback defaults (prevent crash on first theme switch)
    val safePrimary = safePrimary()
    val safeOnSurface = safeOnSurface()
    val safeOnPrimary = safeOnPrimary()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${currentYear}年${currentMonth}月",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = safeOnSurface
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
                containerColor = safePrimary,
                contentColor = safeOnPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "记账")
            }
        }
    ) { padding ->
        NewYearHorseBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    // 蓝色卡片 - 月度收支 - 真实数据
                    NewYearHorseBlueMonthlyCard(
                        income = uiState.monthIncome,
                        expense = uiState.monthExpense,
                        balance = uiState.monthBalance,
                        primaryColor = safePrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 视图切换按钮
                    NewYearHorseViewToggle(
                        options = viewOptions,
                        selectedIndex = selectedView,
                        onSelect = { selectedView = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 根据选中视图显示不同内容 - 使用真实数据
                    when (selectedView) {
                        0 -> NewYearHorseTransactionListView(transactions = transactions)
                        1 -> NewYearHorseTransactionChartView(transactions = transactions, categories = categories)
                    }
                }

                // 底部装饰
                BottomHorseDecoration(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun NewYearHorseViewToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    surfaceColor: Color = safeSurface(),
    primaryColor: Color = safePrimary(),
    onPrimaryColor: Color = safeOnPrimary(),
    onSurfaceVariantColor: Color = safeOnSurfaceVariant()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(25.dp))
            .background(safeSurface())
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
                        if (isSelected) safePrimary()
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
                        imageVector = if (index == 0) Icons.Filled.List else Icons.Default.PieChart,
                        contentDescription = option,
                        tint = if (isSelected) safeOnPrimary() else safeOnSurfaceVariant(),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = option,
                        color = if (isSelected) safeOnPrimary() else safeOnSurfaceVariant(),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun NewYearHorseTransactionListView(transactions: List<Transaction>) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    if (transactions.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                NewYearHorseTransactionCard(
                    transaction = transaction,
                    dateFormat = dateFormat,
                    surfaceColor = safeSurface(),
                    onSurfaceColor = safeOnSurface(),
                    onSurfaceVariantColor = safeOnSurfaceVariant()
                )
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = safeOnSurfaceVariant(),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无交易记录",
                    color = safeOnSurfaceVariant(),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun NewYearHorseTransactionChartView(
    transactions: List<Transaction>,
    categories: List<com.example.aiaccounting.data.local.entity.Category>
) {
    // 计算分类统计数据，使用真实的分类名称
    val categoryStats = rememberTransactionCategoryStats(transactions, categories)
    val totalExpense = categoryStats.sumOf { it.amount }
    val safeSurface = safeSurface()
    val safeOnSurface = safeOnSurface()
    val safePrimary = safePrimary()
    val safeOnSurfaceVariant = safeOnSurfaceVariant()

    Column(modifier = Modifier.fillMaxSize()) {
        // 饼图卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = safeSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "支出分布",
                        color = safeOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.DonutLarge,
                        contentDescription = null,
                        tint = safePrimary,
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
                        NewYearHorseTransactionPieChart(categoryStats = categoryStats, totalExpense = totalExpense, safeSurface = safeSurface, safeOnSurface = safeOnSurface, safeOnSurfaceVariant = safeOnSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 图例
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        categoryStats.take(4).forEach { stat ->
                            val percent = if (totalExpense > 0) (stat.amount / totalExpense * 100).toInt() else 0
                            NewYearHorsePieLegendItem(color = stat.color, label = stat.name, percent = "$percent%", safeOnSurfaceVariant = safeOnSurfaceVariant, safeOnSurface = safeOnSurface)
                        }
                    }
                } else {
                    Text(
                        text = "暂无支出数据",
                        color = safeOnSurfaceVariant,
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
            colors = CardDefaults.cardColors(containerColor = safeSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "分类支出对比",
                        color = safeOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = safePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (categoryStats.isNotEmpty()) {
                    NewYearHorseTransactionBarChart(categoryStats = categoryStats, safeOnSurface = safeOnSurface, safeOnSurfaceVariant = safeOnSurfaceVariant)
                } else {
                    Text(
                        text = "暂无数据",
                        color = safeOnSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NewYearHorseTransactionPieChart(
    categoryStats: List<TransactionCategoryStat>,
    totalExpense: Double,
    safeSurface: Color = safeSurface(),
    safeOnSurface: Color = safeOnSurface(),
    safeOnSurfaceVariant: Color = safeOnSurfaceVariant()
) {
    if (totalExpense <= 0) return

    Box(modifier = Modifier.size(150.dp)) {
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
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - radius, centerY - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }

            // 中间空心
            drawCircle(
                color = safeSurface,
                radius = radius * 0.5f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }

        // 中心文字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "总支出",
                color = safeOnSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = "¥${String.format("%.2f", totalExpense)}",
                color = safeOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NewYearHorsePieLegendItem(
    color: Color,
    label: String,
    percent: String,
    safeOnSurfaceVariant: Color = safeOnSurfaceVariant(),
    safeOnSurface: Color = safeOnSurface()
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = safeOnSurfaceVariant, fontSize = 12.sp)
        Text(text = percent, color = safeOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NewYearHorseTransactionBarChart(
    categoryStats: List<TransactionCategoryStat>,
    safeOnSurface: Color = safeOnSurface(),
    safeOnSurfaceVariant: Color = safeOnSurfaceVariant()
) {
    val maxAmount = categoryStats.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val totalAmount = categoryStats.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    color = safeOnSurface,
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
                            .background(color = stat.color)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 金额和百分比
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(70.dp)) {
                    Text(
                        text = "¥${String.format("%.2f", stat.amount)}",
                        color = safeOnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val percent = if (totalAmount > 0) (stat.amount / totalAmount * 100).toInt() else 0
                    Text(
                        text = "$percent%",
                        color = safeOnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            if (index < categoryStats.size - 1 && index < 5) {
                HorizontalDivider(
                    color = safeOnSurfaceVariant.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
            }
        }

        // 如果有更多分类，显示提示
        if (categoryStats.size > 6) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "还有 ${categoryStats.size - 6} 个分类...",
                color = safeOnSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun NewYearHorseBlueMonthlyCard(
    income: Double,
    expense: Double,
    balance: Double,
    primaryColor: Color = safePrimary()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White,
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
                            color = Color.White,
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
                            color = Color(0xFFFFE55C),
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
                            color = Color(0xFFFFE55C),
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
fun NewYearHorseTransactionCard(
    transaction: Transaction,
    dateFormat: SimpleDateFormat,
    surfaceColor: Color = safeSurface(),
    onSurfaceColor: Color = safeOnSurface(),
    onSurfaceVariantColor: Color = safeOnSurfaceVariant()
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) Color(0xFFFF6B35) else Color(0xFF4CAF50)
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
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
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
                            if (isExpense) Color(0xFFFF6B35).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f)
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
                        text = transaction.note.takeIf { it.isNotBlank() } ?: "未备注",
                        color = onSurfaceColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateFormat.format(Date(transaction.date)),
                        color = onSurfaceVariantColor,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "$amountPrefix¥${String.format("%.2f", transaction.amount)}",
                color = amountColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 分类统计数据类
data class NewYearHorseTransactionCategoryStat(
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
        val totalExpense = expenseTransactions.sumOf { it.amount }

        // 创建分类ID到名称的映射
        val categoryMap = categories.associateBy { it.id }

        expenseTransactions
            .groupBy { it.categoryId }
            .map { (categoryId, transList) ->
                val amount = transList.sumOf { it.amount }
                // 从分类映射中获取真实名称，如果没有则显示"未分类"
                val category = categoryMap[categoryId]
                val categoryName = category?.name ?: "未分类"
                // 使用分类的颜色，如果没有则使用默认颜色
                val color = category?.color?.let { parseColor(it) } ?: when (categoryId % 5) {
                    0L -> Color(0xFFFF6B35)
                    1L -> Color(0xFFFFD700)
                    2L -> Color(0xFF4CAF50)
                    3L -> Color(0xFF2196F3)
                    else -> Color(0xFF9C27B0)
                }
                TransactionCategoryStat(name = categoryName, amount = amount, color = color)
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
        Color(0xFFFFD700)
    }
}

// FlowRow 简单实现
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = horizontalArrangement) {
            content()
        }
    }
}
