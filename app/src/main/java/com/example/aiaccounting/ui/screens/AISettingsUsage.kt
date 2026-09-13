package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.data.repository.AIUsageStats

/**
 * AISettingsScreen 使用统计组件
 *
 * 包含以下组件：
 * - UsageStatsCard: 使用统计卡片
 * - StatItem: 统计项组件
 * - StatDetailRow: 统计详情行
 */

@Composable
internal fun UsageStatsCard(
    stats: AIUsageStats,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "使用统计",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = onResetClick) {
                    Text("重置", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (stats.totalCalls > 0) {
                // 统计项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Call,
                        value = "${stats.totalCalls}",
                        label = "总调用"
                    )
                    StatItem(
                        icon = Icons.Default.CheckCircle,
                        value = "${stats.successRate.toInt()}%",
                        label = "成功率"
                    )
                    StatItem(
                        icon = Icons.Default.AttachMoney,
                        value = stats.formattedCostCNY(),
                        label = "预估费用"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 详细信息
                Column {
                    StatDetailRow(
                        label = "成功调用",
                        value = "${stats.successCalls} 次"
                    )
                    StatDetailRow(
                        label = "失败调用",
                        value = "${stats.failedCalls} 次"
                    )
                    StatDetailRow(
                        label = "总Token数",
                        value = "${stats.totalTokens}"
                    )
                    StatDetailRow(
                        label = "首次使用",
                        value = stats.firstUseTimeFormatted()
                    )
                    StatDetailRow(
                        label = "最后使用",
                        value = stats.lastCallTimeFormatted()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无使用记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
