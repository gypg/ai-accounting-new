package com.example.aiaccounting.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.ui.viewmodel.AccountViewModel

/**
 * Initial Setup Screen - Guide user to create first account
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupScreen(
    onSetupComplete: () -> Unit,
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    var accountName by remember { mutableStateOf("现金") }
    var accountType by remember { mutableStateOf(AccountType.ASSET) }
    var initialBalance by remember { mutableStateOf("0") }
    var selectedIcon by remember { mutableStateOf("💰") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }

    val accountIcons = listOf("💰", "💳", "🏦", "💵", "📱", "🏠")
    val accountColors = listOf("#4CAF50", "#2196F3", "#FF9800", "#F44336", "#9C27B0", "#00BCD4")

    val isFormValid = accountName.isNotBlank() && initialBalance.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("初始设置") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "欢迎使用AI记账",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "让我们先创建您的第一个账户",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Account Name
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("账户名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Type
            Text(
                text = "账户类型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = accountType == AccountType.ASSET,
                        onClick = { accountType = AccountType.ASSET },
                        label = { Text("资产") },
                        leadingIcon = {
                            if (accountType == AccountType.ASSET) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null)
                            }
                        }
                    )

                    FilterChip(
                        selected = accountType == AccountType.LIABILITY,
                        onClick = { accountType = AccountType.LIABILITY },
                        label = { Text("负债") },
                        leadingIcon = {
                            if (accountType == AccountType.LIABILITY) {
                                Icon(Icons.Default.CreditCard, contentDescription = null)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Initial Balance
            OutlinedTextField(
                value = initialBalance,
                onValueChange = { 
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        initialBalance = it
                    }
                },
                label = { Text("初始余额") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Selection
            Text(
                text = "选择图标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                accountIcons.forEach { icon ->
                    FilterChip(
                        selected = selectedIcon == icon,
                        onClick = { selectedIcon = icon },
                        label = { Text(icon, style = MaterialTheme.typography.titleMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Selection
            Text(
                text = "选择颜色",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                accountColors.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.shape.CircleShape
                        androidx.compose.foundation.background(
                            color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color)),
                            shape = RoundedCornerShape(50)
                        ).let {
                            androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                                .let {
                                    androidx.compose.foundation.background(
                                        color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color)),
                                        shape = RoundedCornerShape(50)
                                    )
                                }
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Create Button
            Button(
                onClick = {
                    if (isFormValid) {
                        accountViewModel.createAccount(
                            name = accountName,
                            type = accountType,
                            initialBalance = initialBalance.toDoubleOrNull() ?: 0.0,
                            icon = selectedIcon,
                            color = selectedColor
                        )
                        onSetupComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) {
                Text("创建账户并开始使用")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    // Skip and use default
                    accountViewModel.createAccount(
                        name = "默认账户",
                        type = AccountType.ASSET,
                        initialBalance = 0.0,
                        icon = "💰",
                        color = "#4CAF50"
                    )
                    onSetupComplete()
                }
            ) {
                Text("使用默认设置")
            }
        }
    }
}