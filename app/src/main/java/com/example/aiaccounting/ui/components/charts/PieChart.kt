package com.example.aiaccounting.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.ui.viewmodel.CategoryStat
import kotlin.math.min

/**
 * 饼图组件
 */
@Composable
fun PieChart(
    data: List<CategoryStat>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    holeRadius: Float = 0.5f
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val total = data.sumOf { it.amount }
    val colors = data.map { stat ->
        try {
            Color(android.graphics.Color.parseColor(stat.color))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // 饼图
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val radius = min(canvasWidth, canvasHeight) / 2f
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f

                var startAngle = -90f

                data.forEachIndexed { index, stat ->
                    val sweepAngle = if (total > 0) {
                        (stat.amount / total * 360f).toFloat() * animationProgress.value
                    } else {
                        0f
                    }

                    if (sweepAngle > 0) {
                        // 绘制扇形
                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(centerX - radius, centerY - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }

                    startAngle += sweepAngle / animationProgress.value
                }

                // 绘制中心空心圆（甜甜圈效果）
                if (holeRadius > 0) {
                    drawCircle(
                        color = surfaceColor,
                        radius = radius * holeRadius,
                        center = Offset(centerX, centerY)
                    )
                }
            }

            // 中心文字
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "总计",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "¥${String.format("%.0f", total)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 图例
        if (showLabels) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                data.forEachIndexed { index, stat ->
                    PieChartLegendItem(
                        color = colors[index],
                        name = stat.name,
                        amount = stat.amount,
                        percentage = stat.percentage
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PieChartLegendItem(
    color: Color,
    name: String,
    amount: Double,
    percentage: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 颜色指示器
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 分类名称
        Text(
            text = name,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        // 金额
        Text(
            text = "¥${String.format("%.2f", amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 百分比
        Text(
            text = "${(percentage * 100).toInt()}%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
