package com.example.aiaccounting.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.R
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors

/**
 * 2026马年主题装饰组件
 */

/**
 * 马年主题背景图片组件
 */
@Composable
fun HorseBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 背景图片
        Image(
            painter = painterResource(id = R.drawable.horse_background),
            contentDescription = "马年主题背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 内容层
        content()
    }
}

/**
 * 3D金马图片组件
 */
@Composable
fun GoldenHorse3D(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_horse_golden),
        contentDescription = "金马",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/**
 * AI+ 按钮组件 - 鲜红色底色，白色字体，细长横向椭圆，上方有3D小马
 */
@Composable
fun AIButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // AI+ 按钮（作为底部基础）
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(25.dp))
                .background(HorseTheme2026Colors.Expense) // 鲜红色
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI+",
                color = Color.White, // 白色字体
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 3D小马（放在按钮上方，大小为按钮的三倍）
        Image(
            painter = painterResource(id = R.drawable.ic_horse_golden),
            contentDescription = "小马",
            modifier = Modifier
                .size(240.dp) // 三倍大小 (80 * 3)
                .align(Alignment.TopCenter)
                .offset(x = (-20).dp, y = (-20).dp), // 向左和向下移动
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 祥云装饰
 */
@Composable
fun CloudDecoration(
    modifier: Modifier = Modifier,
    color: Color = HorseTheme2026Colors.CloudBlue
) {
    Canvas(modifier = modifier) {
        drawCloudPattern(color)
    }
}

private fun DrawScope.drawCloudPattern(color: Color) {
    val cloudPath = Path().apply {
        // 绘制祥云路径
        moveTo(size.width * 0.2f, size.height * 0.5f)
        
        // 第一个云卷
        cubicTo(
            size.width * 0.1f, size.height * 0.3f,
            size.width * 0.3f, size.height * 0.2f,
            size.width * 0.4f, size.height * 0.4f
        )
        
        // 连接部分
        cubicTo(
            size.width * 0.45f, size.height * 0.5f,
            size.width * 0.5f, size.height * 0.45f,
            size.width * 0.55f, size.height * 0.5f
        )
        
        // 第二个云卷
        cubicTo(
            size.width * 0.5f, size.height * 0.3f,
            size.width * 0.7f, size.height * 0.2f,
            size.width * 0.8f, size.height * 0.4f
        )
        
        // 收尾
        cubicTo(
            size.width * 0.85f, size.height * 0.5f,
            size.width * 0.9f, size.height * 0.45f,
            size.width * 0.9f, size.height * 0.5f
        )
    }
    
    drawPath(
        path = cloudPath,
        color = color.copy(alpha = 0.6f),
        style = Stroke(width = 2f)
    )
}

/**
 * 中国传统回纹边框装饰
 */
@Composable
fun ChineseBorderDecoration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        HorseTheme2026Colors.BorderGold,
                        HorseTheme2026Colors.BorderGold,
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * 灯笼装饰
 */
@Composable
fun LanternDecoration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(HorseTheme2026Colors.LanternRed),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = HorseTheme2026Colors.Gold,
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * 顶部装饰栏（带灯笼）
 */
@Composable
fun TopDecorationBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // 左侧灯笼
        LanternDecoration()
        
        // 右侧灯笼
        LanternDecoration()
    }
}

/**
 * 底部装饰区域 - 带3D金马
 */
@Composable
fun BottomHorseDecoration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        // 左侧祥云
        CloudDecoration(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = (-40).dp)
        )
        
        // 中间3D金马 - 与导航栏融合
        GoldenHorse3D(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
                .size(150.dp)
        )
        
        // 右侧祥云
        CloudDecoration(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = (-40).dp)
        )
    }
}

/**
 * 简化版金马图标 - 使用Canvas绘制（备用）
 */
@Composable
fun GoldenHorseIcon(
    modifier: Modifier = Modifier,
    size: Int = 60
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HorseTheme2026Colors.GoldLight,
                        HorseTheme2026Colors.Gold
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 简化的马形 - 使用马头轮廓
        Canvas(modifier = Modifier.fillMaxSize(0.7f)) {
            val horsePath = Path().apply {
                // 马头简笔画路径
                moveTo(size * 0.2f, size * 0.6f)
                lineTo(size * 0.3f, size * 0.3f)  // 马头顶
                lineTo(size * 0.5f, size * 0.2f)  // 耳朵
                lineTo(size * 0.6f, size * 0.35f) // 额头
                lineTo(size * 0.75f, size * 0.4f) // 鼻子
                lineTo(size * 0.7f, size * 0.6f)  // 下巴
                lineTo(size * 0.5f, size * 0.55f) // 脖子
                close()
            }
            
            drawPath(
                path = horsePath,
                color = HorseTheme2026Colors.Background
            )
        }
    }
}
