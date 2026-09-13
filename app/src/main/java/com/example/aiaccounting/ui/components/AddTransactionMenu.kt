package com.example.aiaccounting.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 底部弹出菜单 - AI记账和手动记账选项
 * 毛玻璃效果，不完全覆盖屏幕
 */
@Composable
fun AddTransactionMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAIAccounting: () -> Unit,
    onManualAccounting: () -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onDismiss)
            ) {
                // 毛玻璃效果底部面板
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(250, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    ) {
                        // 拖动指示条
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(5.dp)
                                .background(
                                    Color.Gray.copy(alpha = 0.4f),
                                    RoundedCornerShape(3.dp)
                                )
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // 标题
                        Text(
                            text = "选择记账方式",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "智能AI识别或手动输入",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        // AI记账选项
                        MenuOptionCard(
                            icon = Icons.Default.SmartToy,
                            title = "AI 智能记账",
                            subtitle = "语音或文字描述，AI自动识别",
                            iconBackground = MaterialTheme.colorScheme.primary,
                            onClick = {
                                onDismiss()
                                onAIAccounting()
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 手动记账选项
                        MenuOptionCard(
                            icon = Icons.Default.Edit,
                            title = "手动记账",
                            subtitle = "手动输入金额和分类",
                            iconBackground = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                onDismiss()
                                onManualAccounting()
                            }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // 取消按钮
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("取消", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconBackground: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBackground.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = iconBackground
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
