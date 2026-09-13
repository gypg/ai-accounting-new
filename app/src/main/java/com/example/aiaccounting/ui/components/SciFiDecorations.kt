package com.example.aiaccounting.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.ui.theme.FreshSciColorScheme
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * 科幻风格装饰组件库
 * 适用于 fresh_sci（浅色科幻清新）主题
 */

// ==================== 浅色科幻清新装饰色 ====================
object FreshSciDecorColors {
    val Primary = Color(0xFF5B8DEF)
    val Glow = Color(0xFF8BD4FF)
    val Line = Color(0xFFB0C8F0)
    val LineGradient = Color(0xFF5B8DEF)
}

// ==================== 新马年科幻装饰色 ====================
object NewYearHorseSciDecorColors {
    val Primary = Color(0xFFD64040)
    val Gold = Color(0xFFE6C278)
    val Glow = Color(0xFFE8A0C8)
    val Line = Color(0xFFE7C6D9)
    val LineGradient = Color(0xFFB86BFF)
}

/**
 * 科幻风格顶部装饰条
 * 包含顶部横线和角落连接线
 */
@Composable
fun SciFiTopDecoration(
    primaryColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val width = size.width
            val height = size.height

            // 顶部水平主线条（带渐变褪变）
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        glowColor.copy(alpha = 0.4f),
                        primaryColor.copy(alpha = 0.6f),
                        glowColor.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, height * 0.5f),
                end = Offset(width, height * 0.5f),
                strokeWidth = 1.5f
            )

            // 左侧垂直装饰线
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ),
                start = Offset(24.dp.toPx(), 0f),
                end = Offset(24.dp.toPx(), height * 0.7f),
                strokeWidth = 1f
            )

            // 左侧横线短装饰
            drawLine(
                color = primaryColor.copy(alpha = 0.3f),
                start = Offset(24.dp.toPx(), height * 0.7f),
                end = Offset(80.dp.toPx(), height * 0.7f),
                strokeWidth = 1f
            )

            // 右侧垂直装饰线
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ),
                start = Offset(width - 24.dp.toPx(), 0f),
                end = Offset(width - 24.dp.toPx(), height * 0.7f),
                strokeWidth = 1f
            )

            // 右侧横线短装饰
            drawLine(
                color = primaryColor.copy(alpha = 0.3f),
                start = Offset(width - 80.dp.toPx(), height * 0.7f),
                end = Offset(width - 24.dp.toPx(), height * 0.7f),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * 科幻风格角落装饰
 * 在右上角和左下角绘制几何线条
 */
@Composable
fun SciFiCornerDecorations(
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth()) {
        val width = size.width
        val height = size.height

        // 右上角装饰
        drawSciFiCorner(
            color = primaryColor,
            center = Offset(width - 16.dp.toPx(), 80.dp.toPx()),
            radius = 40.dp.toPx()
        )

        // 左下角装饰
        drawSciFiCorner(
            color = primaryColor.copy(alpha = 0.4f),
            center = Offset(24.dp.toPx(), height - 80.dp.toPx()),
            radius = 30.dp.toPx()
        )
    }
}

private fun DrawScope.drawSciFiCorner(
    color: Color,
    center: Offset,
    radius: Float
) {
    // 圆形线条框
    drawCircle(
        color = color.copy(alpha = 0.25f),
        center = center,
        radius = radius,
        style = Stroke(width = 1f)
    )
    drawCircle(
        color = color.copy(alpha = 0.15f),
        center = center,
        radius = radius * 0.65f,
        style = Stroke(width = 1f)
    )

    // 十字交叉线
    val crossSize = radius * 0.4f
    drawLine(
        color = color.copy(alpha = 0.2f),
        start = Offset(center.x - crossSize, center.y),
        end = Offset(center.x + crossSize, center.y),
        strokeWidth = 1f
    )
    drawLine(
        color = color.copy(alpha = 0.2f),
        start = Offset(center.x, center.y - crossSize),
        end = Offset(center.x, center.y + crossSize),
        strokeWidth = 1f
    )
}

/**
 * 底部科幻装饰线
 */
@Composable
fun SciFiBottomDecoration(
    primaryColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val width = size.width
        val centerY = size.height * 0.5f

        // 底部水平主线条
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    glowColor.copy(alpha = 0.3f),
                    primaryColor.copy(alpha = 0.5f),
                    glowColor.copy(alpha = 0.3f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )
    }
}

/**
 * 科幻风格分隔线（用于页面内模块分隔）
 */
@Composable
fun SciFiDivider(
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.5f),
                        primaryColor.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    )
}

// ==================== 通用 UI 组件 ====================

/**
 * 磨砂玻璃卡片 — 浅色科幻清新 / 新马年科幻 主题通用
 * 半透明卡片背景 + 主题色边框 + 可选 blur，实现磨砂玻璃视觉效果
 *
 * @param primaryColor 主题主色（用于边框和阴影）
 * @param borderColor 边框颜色，默认 primaryColor（马年主题传 HorseYearGold 实现鎏金边框）
 * @param cardAlpha 卡片背景透明度，默认 0.88f
 * @param cornerRadius 圆角，默认 16.dp
 * @param blurRadius blur 模糊半径（Compose 1.7+），默认 16f
 */
@Composable
fun SciFiGlassCard(
    modifier: Modifier = Modifier,
    primaryColor: Color = FreshSciColorScheme.primary,
    cardBackgroundColor: Color = Color.White,
    borderColor: Color? = null,
    cardAlpha: Float = 0.88f,
    cornerRadius: Int = 16,
    blurRadius: Float = 16f,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .blur(blurRadius.dp),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor.copy(alpha = cardAlpha)
        ),
        border = BorderStroke(1.dp, (borderColor ?: primaryColor).copy(alpha = if (borderColor != null) 0.5f else 0.18f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * 设置分组卡片 — 设置页专用
 * 包含分组标题 + 多个设置项，项之间有分隔线
 */
@Composable
fun SciFiSettingsGroup(
    title: String,
    primaryColor: Color = FreshSciColorScheme.primary,
    items: List<SciFiSettingItemData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.88f)
        ),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEachIndexed { index, item ->
                SciFiSettingItem(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    primaryColor = primaryColor,
                    onClick = item.onClick
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = primaryColor.copy(alpha = 0.1f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

data class SciFiSettingItemData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
fun SciFiSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primaryColor: Color = FreshSciColorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = primaryColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF292F33)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF656D78)
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFB0B8C4),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 科幻主题底部导航栏
 * 磨砂白背景 + 顶部圆角 + 主题色选中态
 */
@Composable
fun SciFiBottomNavBar(
    items: List<SciFiNavItem>,
    selectedRoute: String,
    onItemClick: (String) -> Unit,
    primaryColor: Color = FreshSciColorScheme.primary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color.White.copy(alpha = 0.90f))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = selectedRoute == item.route
                val color = if (selected) primaryColor else Color(0xFFB0B8C4)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemClick(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            color = color,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * 浅色科幻清新主题背景
 * 豆包设计背景图 + 科幻装饰，覆盖所有内容层
 */
@Composable
fun FreshSciBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 豆包设计背景图（注意右下角水印）
        Image(
            painter = painterResource(id = com.example.aiaccounting.R.drawable.bg_sci_fi),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 科幻角落装饰
        SciFiCornerDecorations(
            primaryColor = FreshSciDecorColors.Primary,
            modifier = Modifier.matchParentSize()
        )
        // 顶部科幻装饰线
        SciFiTopDecoration(
            primaryColor = FreshSciDecorColors.Primary,
            glowColor = FreshSciDecorColors.Glow
        )
        // 底部科幻装饰线
        SciFiBottomDecoration(
            primaryColor = FreshSciDecorColors.Primary,
            glowColor = FreshSciDecorColors.Glow,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        content()
    }
}

/**
 * 新马年科幻主题背景
 * 豆包设计背景图 + 科幻装饰，覆盖所有内容层
 */
@Composable
fun NewYearHorseBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 豆包设计背景图（注意右下角水印）
        Image(
            painter = painterResource(id = com.example.aiaccounting.R.drawable.bg_horse_year),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 科幻角落装饰
        SciFiCornerDecorations(
            primaryColor = NewYearHorseSciDecorColors.Primary,
            modifier = Modifier.matchParentSize()
        )
        // 顶部科幻装饰线
        SciFiTopDecoration(
            primaryColor = NewYearHorseSciDecorColors.LineGradient,
            glowColor = NewYearHorseSciDecorColors.Glow
        )
        // 底部科幻装饰线
        SciFiBottomDecoration(
            primaryColor = NewYearHorseSciDecorColors.LineGradient,
            glowColor = NewYearHorseSciDecorColors.Glow,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        content()
    }
}

data class SciFiNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * 科幻风格底部装饰线
 * 用于页面底部的简单分隔线效果
 */
@Composable
fun SciFiBottomLine(
    modifier: Modifier = Modifier,
    primaryColor: Color = FreshSciDecorColors.Primary
) {
    HorizontalDivider(
        color = primaryColor.copy(alpha = 0.2f),
        thickness = 1.dp,
        modifier = modifier
    )
}

/**
 * 浅色科幻清新主题 AI 按钮
 * 天蓝渐变背景，白色字体，无马年图案
 */
@Composable
fun FreshSciAIButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = FreshSciDecorColors.Primary,
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = "AI助手",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
