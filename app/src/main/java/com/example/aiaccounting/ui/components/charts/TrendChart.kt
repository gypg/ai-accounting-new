package com.example.aiaccounting.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

internal fun calculateTrendChartMaxValue(
    data: List<MonthlyData>,
    showIncome: Boolean,
    showExpense: Boolean
): Double {
    val visibleIncomeMax = if (showIncome) data.maxOfOrNull { it.income } ?: 0.0 else 0.0
    val visibleExpenseMax = if (showExpense) data.maxOfOrNull { it.expense } ?: 0.0 else 0.0
    val visibleMax = max(visibleIncomeMax, visibleExpenseMax)
    return visibleMax * 1.2
}

internal fun formatTrendAxisLabel(value: Double): String {
    return when {
        value >= 10_000 -> "¥${String.format("%.1f", value / 10_000)}w"
        value >= 1_000 -> "¥${String.format("%.1f", value / 1_000)}k"
        value > 0 -> "¥${String.format("%.0f", value)}"
        else -> "0"
    }
}

/**
 * 月度数据点
 */
data class MonthlyData(
    val month: String,
    val income: Double,
    val expense: Double
)

/**
 * 趋势图组件 - 显示收支趋势
 * 修复：增大数据点、优化单数据点显示、添加数据标签、点击显示数值
 */
@Composable
fun TrendChart(
    data: List<MonthlyData>,
    modifier: Modifier = Modifier,
    showIncome: Boolean = true,
    showExpense: Boolean = true,
    onDataPointClick: ((MonthlyData) -> Unit)? = null
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)

    // 选中的数据点
    var selectedData by remember { mutableStateOf<MonthlyData?>(null) }

    Column(
        modifier = modifier
    ) {
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIncome) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(incomeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "收入",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            if (showExpense) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(expenseColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "支出",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Y轴数值标签区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Y轴标签
            val maxIncome = data.maxOfOrNull { it.income } ?: 0.0
            val maxExpense = data.maxOfOrNull { it.expense } ?: 0.0
            val maxValue = calculateTrendChartMaxValue(
                data = data,
                showIncome = showIncome,
                showExpense = showExpense
            )

            Column(
                modifier = Modifier.width(56.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                if (maxValue > 0) {
                    Text(formatTrendAxisLabel(maxValue), fontSize = 10.sp, color = Color.Gray)
                    Text(formatTrendAxisLabel(maxValue * 0.75), fontSize = 10.sp, color = Color.Gray)
                    Text(formatTrendAxisLabel(maxValue * 0.5), fontSize = 10.sp, color = Color.Gray)
                    Text(formatTrendAxisLabel(maxValue * 0.25), fontSize = 10.sp, color = Color.Gray)
                }
                Text("0", fontSize = 10.sp, color = Color.Gray)
            }

            // 图表区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val padding = 8f

                    val chartWidth = canvasWidth - padding * 2
                    val chartHeight = canvasHeight - padding * 2

                    if (maxValue <= 0) return@Canvas

                    // 处理单数据点情况
                    val effectiveDataSize = max(data.size, 2)
                    val xStep = chartWidth / (effectiveDataSize - 1)

                    // 绘制网格线
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = padding + chartHeight * i / gridLines
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(padding, y),
                            end = Offset(canvasWidth - padding, y),
                            strokeWidth = 1f
                        )
                    }

                    // 绘制收入折线
                    if (showIncome && maxIncome > 0) {
                        val incomePath = Path()
                        
                        data.forEachIndexed { index, monthlyData ->
                            val x = if (data.size == 1) {
                                padding + chartWidth / 2
                            } else {
                                padding + index * xStep
                            }
                            
                            val y = padding + chartHeight * (1 - monthlyData.income / maxValue).toFloat()
                            val animatedY = y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value)

                            if (index == 0) {
                                incomePath.moveTo(x, animatedY)
                            } else {
                                incomePath.lineTo(x, animatedY)
                            }
                        }

                        drawPath(
                            path = incomePath,
                            color = incomeColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )

                        // 绘制收入数据点（增大尺寸）
                        data.forEachIndexed { index, monthlyData ->
                            val x = if (data.size == 1) {
                                padding + chartWidth / 2
                            } else {
                                padding + index * xStep
                            }
                            
                            val y = padding + chartHeight * (1 - monthlyData.income / maxValue).toFloat()
                            val animatedY = y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value)

                            // 外圈（白色描边）
                            drawCircle(
                                color = Color.White,
                                radius = 10f,
                                center = Offset(x, animatedY)
                            )
                            
                            // 内圈（彩色填充）
                            drawCircle(
                                color = incomeColor,
                                radius = 7f,
                                center = Offset(x, animatedY)
                            )
                        }
                    }

                    // 绘制支出折线
                    if (showExpense && maxExpense > 0) {
                        val expensePath = Path()
                        
                        data.forEachIndexed { index, monthlyData ->
                            val x = if (data.size == 1) {
                                padding + chartWidth / 2
                            } else {
                                padding + index * xStep
                            }
                            
                            val y = padding + chartHeight * (1 - monthlyData.expense / maxValue).toFloat()
                            val animatedY = y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value)

                            if (index == 0) {
                                expensePath.moveTo(x, animatedY)
                            } else {
                                expensePath.lineTo(x, animatedY)
                            }
                        }

                        drawPath(
                            path = expensePath,
                            color = expenseColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )

                        // 绘制支出数据点（增大尺寸）
                        data.forEachIndexed { index, monthlyData ->
                            val x = if (data.size == 1) {
                                padding + chartWidth / 2
                            } else {
                                padding + index * xStep
                            }
                            
                            val y = padding + chartHeight * (1 - monthlyData.expense / maxValue).toFloat()
                            val animatedY = y * animationProgress.value + (padding + chartHeight) * (1 - animationProgress.value)

                            // 外圈（白色描边）
                            drawCircle(
                                color = Color.White,
                                radius = 10f,
                                center = Offset(x, animatedY)
                            )
                            
                            // 内圈（彩色填充）
                            drawCircle(
                                color = expenseColor,
                                radius = 7f,
                                center = Offset(x, animatedY)
                            )
                        }
                    }

                    // 绘制X轴
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(padding, padding + chartHeight),
                        end = Offset(canvasWidth - padding, padding + chartHeight),
                        strokeWidth = 2f
                    )
                }
                
                // 点击检测层 - 放在Canvas上面
                if (onDataPointClick != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("trend_chart_click_layer")
                            .pointerInput(data) {
                                detectTapGestures { offset ->
                                    val canvasWidth = size.width.toFloat()
                                    val padding = 8f
                                    val chartWidth = canvasWidth - padding * 2
                                    val effectiveDataSize = max(data.size, 2)
                                    val xStep = chartWidth / (effectiveDataSize - 1)

                                    val index = if (data.size == 1) {
                                        0
                                    } else {
                                        ((offset.x - padding) / xStep).toInt()
                                            .coerceIn(0, data.size - 1)
                                    }
                                    selectedData = data[index]
                                    onDataPointClick(data[index])
                                }
                            }
                    )
                }
            }
        }

        // X轴标签 - 使用与图表相同的间距逻辑
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (data.size == 1) {
                Text(
                    text = data[0].month,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                // 显示所有数据点的月份标签，确保与图表对齐
                data.forEachIndexed { index, monthlyData ->
                    val shouldShow = when {
                        data.size <= 6 -> true
                        data.size <= 12 -> index % 2 == 0
                        else -> index % 3 == 0
                    }
                    
                    if (shouldShow) {
                        Text(
                            text = monthlyData.month,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // 保持间距一致，显示空文本
                        Text(
                            text = "",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        // 显示选中的数据点详情
        selectedData?.let { selected ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                        text = selected.month,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row {
                        if (showIncome && selected.income > 0) {
                            Text(
                                text = "收: ¥${String.format("%.0f", selected.income)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = incomeColor
                            )
                        }
                        
                        if (showIncome && selected.income > 0 && showExpense && selected.expense > 0) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        
                        if (showExpense && selected.expense > 0) {
                            Text(
                                text = "支: ¥${String.format("%.0f", selected.expense)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = expenseColor
                            )
                        }
                    }
                }
            }
        }
        
        // 显示数据摘要（最新月份）
        if (data.isNotEmpty()) {
            val latestData = data.last()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (showIncome && latestData.income > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "本月收入",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${String.format("%.0f", latestData.income)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = incomeColor
                        )
                    }
                }
                if (showExpense && latestData.expense > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "本月支出",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "¥${String.format("%.0f", latestData.expense)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = expenseColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * 柱状图组件 - 显示月度对比
 */
@Composable
fun BarChart(
    data: List<MonthlyData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = Color(0xFFF44336)

    Column(
        modifier = modifier
    ) {
        // 图例
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(incomeColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "收入",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(expenseColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "支出",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Y轴标签和图表
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 8.dp)
        ) {
            val maxValue = max(
                data.maxOfOrNull { it.income } ?: 0.0,
                data.maxOfOrNull { it.expense } ?: 0.0
            ) * 1.2

            // Y轴标签
            Column(
                modifier = Modifier.width(56.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                if (maxValue > 0) {
                    Text(formatTrendAxisLabel(maxValue), fontSize = 10.sp, color = Color.Gray)
                    Text(formatTrendAxisLabel(maxValue * 0.75), fontSize = 10.sp, color = Color.Gray)
                    Text(formatTrendAxisLabel(maxValue * 0.5), fontSize = 10.sp, color = Color.Gray)
                    Text(formatTrendAxisLabel(maxValue * 0.25), fontSize = 10.sp, color = Color.Gray)
                }
                Text("0", fontSize = 10.sp, color = Color.Gray)
            }

            // 图表
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val padding = 8f

                    val chartWidth = canvasWidth - padding * 2
                    val chartHeight = canvasHeight - padding * 2

                    if (maxValue <= 0) return@Canvas

                    // 绘制网格线
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = padding + chartHeight * i / gridLines
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(padding, y),
                            end = Offset(canvasWidth - padding, y),
                            strokeWidth = 1f
                        )
                    }

                    // 单数据点居中显示
                    val effectiveDataSize = max(data.size, 1)
                    val barGroupWidth = chartWidth / effectiveDataSize
                    val barWidth = min(barGroupWidth * 0.3f, 40f)
                    val spacing = barGroupWidth * 0.1f

                    data.forEachIndexed { index, monthlyData ->
                        val groupX = if (data.size == 1) {
                            padding + chartWidth / 2 - barGroupWidth / 2
                        } else {
                            padding + index * barGroupWidth
                        }

                        // 收入柱
                        val incomeHeight = if (maxValue > 0) {
                            (monthlyData.income / maxValue * chartHeight).toFloat() * animationProgress.value
                        } else 0f

                        if (incomeHeight > 0) {
                            drawRect(
                                color = incomeColor,
                                topLeft = Offset(
                                    groupX + spacing,
                                    padding + chartHeight - incomeHeight
                                ),
                                size = androidx.compose.ui.geometry.Size(barWidth, incomeHeight)
                            )
                        }

                        // 支出柱
                        val expenseHeight = if (maxValue > 0) {
                            (monthlyData.expense / maxValue * chartHeight).toFloat() * animationProgress.value
                        } else 0f

                        if (expenseHeight > 0) {
                            drawRect(
                                color = expenseColor,
                                topLeft = Offset(
                                    groupX + spacing + barWidth + 4f,
                                    padding + chartHeight - expenseHeight
                                ),
                                size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight)
                            )
                        }
                    }

                    // 基线
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(padding, padding + chartHeight),
                        end = Offset(canvasWidth - padding, padding + chartHeight),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // X轴标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp, top = 4.dp),
            horizontalArrangement = if (data.size == 1) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            if (data.size == 1) {
                Text(
                    text = data[0].month,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                data.forEachIndexed { index, monthlyData ->
                    val shouldShow = when {
                        data.size <= 6 -> true
                        data.size <= 12 -> index % 2 == 0
                        else -> index % 3 == 0
                    }
                    
                    if (shouldShow) {
                        Text(
                            text = monthlyData.month,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
