package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.ui.viewmodel.InviteBindResult

/**
 * AISettingsScreen 邀请码组件
 *
 * 包含以下组件：
 * - InviteCodeBindCard: 邀请码绑定卡片
 * - InviteBoundStatusCard: 邀请码绑定状态卡片
 */

@Composable
internal fun InviteCodeBindCard(
    inviteCode: String,
    gatewayBaseUrl: String,
    isBinding: Boolean,
    bindResult: InviteBindResult?,
    onInviteCodeChange: (String) -> Unit,
    onGatewayBaseUrlChange: (String) -> Unit,
    onBind: () -> Unit,
    onDismissResult: () -> Unit
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "邀请码绑定",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "只输入邀请码，自动配置网关",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "收起" else "高级", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inviteCode,
                onValueChange = onInviteCodeChange,
                label = { Text("邀请码") },
                placeholder = { Text("inv_xxx") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = gatewayBaseUrl,
                    onValueChange = onGatewayBaseUrlChange,
                    label = { Text("网关地址") },
                    placeholder = { Text("https://api.gdmon.dpdns.org") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onBind,
                modifier = Modifier.fillMaxWidth(),
                enabled = inviteCode.trim().isNotBlank() && !isBinding
            ) {
                if (isBinding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBinding) "绑定中..." else "绑定并启用")
            }

            bindResult?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))

                when (result) {
                    is InviteBindResult.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "绑定成功（${result.rpm} rpm）\n${result.hint}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissResult) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    is InviteBindResult.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = result.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissResult) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InviteBoundStatusCard(
    inviteCodeMasked: String,
    gatewayBaseUrl: String,
    apiBaseUrl: String,
    rpmText: String,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已通过邀请码绑定",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                val sub = listOfNotNull(
                    inviteCodeMasked.takeIf { it.isNotBlank() },
                    rpmText.takeIf { it.isNotBlank() }
                ).joinToString(" · ")

                Text(
                    text = sub.ifBlank { "已自动启用，可返回 AI 助手页面直接使用" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClear) {
                Text("解除绑定", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
