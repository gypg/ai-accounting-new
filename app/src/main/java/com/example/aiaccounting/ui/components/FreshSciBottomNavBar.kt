package com.example.aiaccounting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import com.example.aiaccounting.ui.theme.FreshSciThemeColors

/**
 * 浅色科幻清新主题底部导航栏数据类
 */
data class FreshSciNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * 浅色科幻清新主题底部导航栏组件
 * 天蓝渐变背景，选中项高亮显示
 */
@Composable
fun FreshSciBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        FreshSciNavItem("overview", "总览", Icons.Default.Home, Icons.Default.Home),
        FreshSciNavItem("transactions", "明细", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List),
        FreshSciNavItem("statistics", "统计", Icons.Default.BarChart, Icons.Default.BarChart),
        FreshSciNavItem("settings", "设置", Icons.Default.Settings, Icons.Default.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(FreshSciThemeColors.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                FreshSciNavItem(
                    item = item,
                    selected = selected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
fun FreshSciNavItem(
    item: FreshSciNavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) FreshSciThemeColors.primary.copy(alpha = 0.1f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = item.title,
                tint = if (selected) FreshSciThemeColors.primary else FreshSciThemeColors.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                color = if (selected) FreshSciThemeColors.primary else FreshSciThemeColors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
