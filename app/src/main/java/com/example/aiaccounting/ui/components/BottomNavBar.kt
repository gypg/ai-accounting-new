package com.example.aiaccounting.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 底部导航栏数据类
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * 底部导航栏组件
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    items: List<BottomNavItem>
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

/**
 * 底部导航项定义
 */
object BottomNavItems {
    val items = listOf(
        BottomNavItem(
            route = "overview",
            title = "总览",
            icon = Icons.Default.Home,
            selectedIcon = Icons.Default.Home
        ),
        BottomNavItem(
            route = "transactions",
            title = "明细",
            icon = Icons.Default.Receipt,
            selectedIcon = Icons.Default.Receipt
        ),
        BottomNavItem(
            route = "statistics",
            title = "统计",
            icon = Icons.Default.BarChart,
            selectedIcon = Icons.Default.BarChart
        ),
        BottomNavItem(
            route = "settings",
            title = "设置",
            icon = Icons.Default.Settings,
            selectedIcon = Icons.Default.Settings
        )
    )
}
