package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
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
import com.example.aiaccounting.ui.viewmodel.TransactionListViewModel
import com.example.aiaccounting.ui.theme.LocalIsDreamyTheme
import com.example.aiaccounting.ui.theme.LocalUiScale
import com.example.aiaccounting.ui.components.GlassCard
import com.example.aiaccounting.utils.DateUtils
import com.example.aiaccounting.utils.NumberUtils
import java.util.*

/**
 * 明细页面 - 手机适配版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit = {},
    initialYear: Int? = null,
    initialMonth: Int? = null,
    initialDay: Int? = null,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    var showFilterPanel by remember { mutableStateOf(false) }

    val isDreamyTheme = LocalIsDreamyTheme.current
    val uiScale = LocalUiScale.current
    val cardScale = uiScale.cardScale
    val fontScale = uiScale.fontScale

    LaunchedEffect(initialYear, initialMonth) {
        if (initialYear != null && initialMonth != null) {
            viewModel.setYearMonth(initialYear, initialMonth)
        }
    }

    LaunchedEffect(initialYear, initialMonth, initialDay) {
        if (initialYear != null && initialMonth != null) {
            viewModel.selectDate(initialDay)
        }
    }

    LaunchedEffect(isDreamyTheme) {
        viewModel.logTransactionScreenEnter(
            theme = if (isDreamyTheme) "dreamy" else "default",
            selectedView = "list"
        )
    }


    Scaffold(
        containerColor = if (isDreamyTheme) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "明细",
                            fontSize = (20 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDreamyTheme) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = (12 * cardScale).dp)
        ) {
            // 月份选择器和统计
            MonthSelectorCard(
                year = uiState.selectedYear,
                month = uiState.selectedMonth,
                income = monthlyStats.income,
                expense = monthlyStats.expense,
                onPrevMonth = { viewModel.prevMonth() },
                onNextMonth = { viewModel.nextMonth() },
                cardScale = cardScale,
                fontScale = fontScale
            )

            // 筛选面板
            if (showFilterPanel) {
                FilterPanel(
                    selectedType = uiState.filterType,
                    onTypeSelected = { viewModel.setFilterType(it) },
                    sourceFilter = uiState.sourceFilter,
                    onSourceFilterChanged = { viewModel.setSourceFilter(it) },
                    sortBy = uiState.sortBy,
                    onSortByChanged = { viewModel.setSortBy(it) },
                    onExportClick = onNavigateToExport,
                    cardScale = cardScale
                )
            }

            // 交易列表
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无账单",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            categoryName = viewModel.getCategoryName(transaction.categoryId),
                            onClick = { onNavigateToEditTransaction(transaction.id) },
                            cardScale = cardScale
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelectorCard(
    year: Int,
    month: Int,
    income: Double,
    expense: Double,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    cardScale: Float = 1f,
    fontScale: Float = 1f
) {
    val isDreamyTheme = LocalIsDreamyTheme.current


    if (isDreamyTheme) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * cardScale).dp)
        ) {
            Column(
                modifier = Modifier.padding((16 * cardScale).dp)
            ) {
                // 月份选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "上个月",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${year}年${month}月",
                        fontSize = (18 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "下个月",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 收支统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "收入",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "+${NumberUtils.formatMoney(income)}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "支出",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "-${NumberUtils.formatMoney(expense)}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "结余",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = NumberUtils.formatMoney(income - expense),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding((16 * cardScale).dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding((16 * cardScale).dp)
        ) {
            // 月份选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "上个月",
                        tint = Color.White
                    )
                }
                Text(
                    text = "${year}年${month}月",
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "下个月",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 收支统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "收入",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "+${NumberUtils.formatMoney(income)}",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "支出",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "-${NumberUtils.formatMoney(expense)}",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "结余",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = NumberUtils.formatMoney(income - expense),
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPanel(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    sourceFilter: String,
    onSourceFilterChanged: (String) -> Unit,
    sortBy: String,
    onSortByChanged: (String) -> Unit,
    onExportClick: () -> Unit,
    cardScale: Float = 1f
) {
    val isDreamyTheme = LocalIsDreamyTheme.current


    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding((16 * cardScale).dp)
        ) {
            // 类型筛选
            Text(
                text = "类型",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    label = "全部",
                    selected = selectedType == "all",
                    onClick = { onTypeSelected("all") }
                )
                FilterChip(
                    label = "收入",
                    selected = selectedType == "income",
                    onClick = { onTypeSelected("income") }
                )
                FilterChip(
                    label = "支出",
                    selected = selectedType == "expense",
                    onClick = { onTypeSelected("expense") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "来源",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    label = "全部",
                    selected = sourceFilter == "all",
                    onClick = { onSourceFilterChanged("all") }
                )
                FilterChip(
                    label = "AI",
                    selected = sourceFilter == "ai",
                    onClick = { onSourceFilterChanged("ai") }
                )
                FilterChip(
                    label = "手动",
                    selected = sourceFilter == "manual",
                    onClick = { onSourceFilterChanged("manual") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 排序
            Text(
                text = "排序",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    label = "按时间",
                    selected = sortBy == "time",
                    onClick = { onSortByChanged("time") }
                )
                FilterChip(
                    label = "按金额",
                    selected = sortBy == "amount",
                    onClick = { onSortByChanged("amount") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 导出按钮
            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出报表")
            }
        }
    }

    if (isDreamyTheme) {
        GlassCard(modifier = cardModifier, content = content)
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    categoryName: String,
    onClick: () -> Unit,
    cardScale: Float = 1f
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((16 * cardScale).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = transaction.note.ifBlank { "无备注" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    TransactionSourceBadge(transaction.aiSourceType)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = DateUtils.formatDateShort(transaction.date),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                // 显示标签
                if (transaction.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        transaction.tags.split(",").forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.height(20.dp)
                            ) {
                                Text(
                                    text = tag.trim(),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 金额
            Text(
                text = "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${NumberUtils.formatMoney(transaction.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun TransactionSourceBadge(sourceType: String) {
    val badgeLabel = transactionSourceBadgeLabel(sourceType)

    if (badgeLabel != null) {
        val color = when (sourceType) {
            "AI_REMOTE" -> MaterialTheme.colorScheme.primary
            "AI_LOCAL" -> Color(0xFF7B61FF)
            else -> Color.Transparent
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                text = badgeLabel,
                fontSize = 10.sp,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

internal fun transactionSourceBadgeLabel(sourceType: String): String? {
    return when (sourceType) {
        "AI_REMOTE" -> "AI云端"
        "AI_LOCAL" -> "AI本地"
        else -> null
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (selected) Color.White else Color.Black
        )
    }
}
