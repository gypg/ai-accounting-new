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
import com.example.aiaccounting.ui.theme.NewYearHorseThemeColors

// ==================== 安全的主题颜色包装器 ====================
// 防止主题切换时因颜色未初始化导致的崩溃
private fun safeNewYearHorsePrimary(): Color = if (NewYearHorseThemeColors.primary.value != 0UL) NewYearHorseThemeColors.primary else Color(0xFFD64040)
private fun safeNewYearHorseSurface(): Color = if (NewYearHorseThemeColors.surface.value != 0UL) NewYearHorseThemeColors.surface else Color(0xFFFFFAF5)
private fun safeNewYearHorseOnSurfaceVariant(): Color = if (NewYearHorseThemeColors.onSurfaceVariant.value != 0UL) NewYearHorseThemeColors.onSurfaceVariant else Color(0xFF475569)

/**
 * 新马年科幻节日主题底部导航栏数据类
 */
data class NewYearHorseNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * 新马年科幻节日主题底部导航栏组件
 * 粉紫渐变背景，选中项金色高亮显示
 */
@Composable
fun NewYearHorseBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NewYearHorseNavItem("overview", "总览", Icons.Default.Home, Icons.Default.Home),
        NewYearHorseNavItem("transactions", "明细", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List),
        NewYearHorseNavItem("statistics", "统计", Icons.Default.BarChart, Icons.Default.BarChart),
        NewYearHorseNavItem("settings", "设置", Icons.Default.Settings, Icons.Default.Settings)
    )

    // Safe theme colors
    val safePrimary = safeNewYearHorsePrimary()
    val safeSurface = safeNewYearHorseSurface()
    val safeOnSurfaceVariant = safeNewYearHorseOnSurfaceVariant()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(safeSurface)
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
                NewYearHorseNavItem(
                    item = item,
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    safePrimary = safePrimary,
                    safeOnSurfaceVariant = safeOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NewYearHorseNavItem(
    item: NewYearHorseNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    safePrimary: Color = safeNewYearHorsePrimary(),
    safeOnSurfaceVariant: Color = safeNewYearHorseOnSurfaceVariant()
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) safePrimary.copy(alpha = 0.1f) else Color.Transparent
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
                tint = if (selected) safePrimary else safeOnSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                color = if (selected) safePrimary else safeOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
