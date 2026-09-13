package com.example.aiaccounting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors

/**
 * 马年主题底部导航栏数据类
 */
data class HorseBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

/**
 * 马年主题底部导航栏组件
 * 金色背景，选中项红色背景
 */
@Composable
fun HorseBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        HorseBottomNavItem("overview", "总览", Icons.Default.Home),
        HorseBottomNavItem("transactions", "明细", Icons.Default.List),
        HorseBottomNavItem("statistics", "统计", Icons.Default.BarChart),
        HorseBottomNavItem("settings", "设置", Icons.Default.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HorseTheme2026Colors.NavBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                HorseNavItem(
                    item = item,
                    selected = selected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
fun HorseNavItem(
    item: HorseBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) HorseTheme2026Colors.NavSelectedBackground else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (selected) Color.White else HorseTheme2026Colors.Background,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                color = if (selected) Color.White else HorseTheme2026Colors.Background,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}


