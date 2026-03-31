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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.components.charts.PieChart
import com.example.aiaccounting.ui.components.charts.TrendChart
import com.example.aiaccounting.ui.components.charts.BarChart
import com.example.aiaccounting.ui.viewmodel.StatisticsViewModel
import com.example.aiaccounting.ui.viewmodel.CategoryStat
import com.example.aiaccounting.ui.theme.NewYearHorseThemeColors
import com.example.aiaccounting.ui.components.NewYearHorseBackground
import com.example.aiaccounting.utils.NumberUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * 新马年科幻主题统计页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewYearHorseStatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Defer showing dialog until after first composition to avoid theme color access during transition
    var dialogReady by remember { mutableStateOf(false) }
    LaunchedEffect(showDatePickerDialog) {
        if (showDatePickerDialog) {
            dialogReady = true
        }
    }

    // Safe theme colors with fallback defaults
    // Use safe default colors to prevent crash on first theme switch
    val safePrimary = if (NewYearHorseThemeColors.primary.value != 0UL) NewYearHorseThemeColors.primary else Color(0xFFD64040)
    val safeOnSurface = if (NewYearHorseThemeColors.onSurface.value != 0UL) NewYearHorseThemeColors.onSurface else Color(0xFF1E293B)
    val safeSurface = if (NewYearHorseThemeColors.surface.value != 0UL) NewYearHorseThemeColors.surface else Color(0xFFFFFAF5)
    val safeOnSurfaceVariant = if (NewYearHorseThemeColors.onSurfaceVariant.value != 0UL) NewYearHorseThemeColors.onSurfaceVariant else Color(0xFF475569)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "统计",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = safeOnSurface
                    )
                },
                actions = {
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选", tint = safeOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        NewYearHorseBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // 显示当前选择的时间范围
                val timeDisplayText = NewYearHorseGetTimeDisplayText(uiState.timeFilter)
                if (timeDisplayText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = safeSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "统计时间：$timeDisplayText", fontSize = 14.sp, color = safeOnSurface)
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = safePrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // 总收入和总支出卡片
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NewYearHorseStatCard(
                        title = "总收入",
                        amount = statistics.totalIncome,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        surfaceColor = safeSurface,
                        onSurfaceVariantColor = safeOnSurfaceVariant
                    )
                    NewYearHorseStatCard(
                        title = "总支出",
                        amount = statistics.totalExpense,
                        color = Color(0xFFFF6B35),
                        modifier = Modifier.weight(1f),
                        surfaceColor = safeSurface,
                        onSurfaceVariantColor = safeOnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 时间趋势图卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = safeSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "时间趋势", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = safeOnSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        TrendChart(data = statistics.monthlyTrend)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 收支分类饼图
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = safeSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "收支构成", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = safeOnSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        PieChart(data = statistics.categoryStats)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 分类支出详情
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = safeSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "分类支出详情", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = safeOnSurface)
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn {
                            items(statistics.categoryStats) { stat ->
                                NewYearHorseCategoryItem(
                                    stat = stat,
                                    onSurfaceColor = safeOnSurface,
                                    onSurfaceVariantColor = safeOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Only show dialog after first composition frame to avoid theme transition crash
    if (showDatePickerDialog && dialogReady) {
        NewYearHorseDatePickerDialog(
            onDismiss = { showDatePickerDialog = false; dialogReady = false },
            surfaceColor = safeSurface,
            onSurfaceColor = safeOnSurface,
            onSurfaceVariantColor = safeOnSurfaceVariant,
            primaryColor = safePrimary
        )
    }
}

@Composable
private fun NewYearHorseStatCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontSize = 12.sp, color = onSurfaceVariantColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = NumberUtils.formatMoney(amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun NewYearHorseCategoryItem(
    stat: CategoryStat,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(12.dp).clip(CircleShape).background(parseColorString(stat.color))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = stat.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = onSurfaceColor)
                Text(text = "${stat.percentage}%", fontSize = 12.sp, color = onSurfaceVariantColor)
            }
        }
        Text(text = NumberUtils.formatMoney(stat.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
    }
}

@Composable
private fun NewYearHorseDatePickerDialog(
    onDismiss: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp).width(300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "选择时间范围", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "时间范围选择器", fontSize = 14.sp, color = onSurfaceVariantColor)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "关闭", color = primaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun NewYearHorseGetTimeDisplayText(timeFilter: String): String {
    return when (timeFilter) {
        "today" -> "今天"
        "this_week" -> "本周"
        "this_month" -> "本月"
        "this_year" -> "今年"
        else -> ""
    }
}

private fun parseColorString(colorString: String): Color {
    return try {
        val color = android.graphics.Color.parseColor(colorString)
        Color(color)
    } catch (e: Exception) {
        Color(0xFFFFD700)
    }
}
