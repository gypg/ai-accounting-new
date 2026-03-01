package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.AccountViewModel
import com.example.aiaccounting.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupScreen(
    onSetupComplete: () -> Unit,
    accountViewModel: AccountViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("欢迎", "设置账户", "设置分类", "完成")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("初始设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 进度指示器
            LinearProgressIndicator(
                progress = (currentStep + 1) / steps.size.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "步骤 ${currentStep + 1}/${steps.size}: ${steps[currentStep]}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 步骤内容
            when (currentStep) {
                0 -> WelcomeStep()
                1 -> AccountSetupStep()
                2 -> CategorySetupStep()
                3 -> CompleteStep(onSetupComplete)
            }

            Spacer(modifier = Modifier.weight(1f))

            // 导航按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- }
                    ) {
                        Text("上一步")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < steps.size - 1) {
                            currentStep++
                        } else {
                            onSetupComplete()
                        }
                    }
                ) {
                    Text(if (currentStep == steps.size - 1) "开始使用" else "下一步")
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "欢迎使用AI记账",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI记账是一款智能记账应用，支持自然语言输入、多账户管理、详细统计等功能。\n\n让我们花几分钟时间完成初始设置。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AccountSetupStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "设置账户",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "系统已为您预设了常用账户（现金、银行卡、支付宝、微信），您可以在账户管理页面进行修改或添加更多账户。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 预设账户列表
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                PresetAccountItem(Icons.Default.Payments, "现金", "日常现金")
                PresetAccountItem(Icons.Default.CreditCard, "银行卡", "储蓄卡/借记卡")
                PresetAccountItem(Icons.Default.AccountBalanceWallet, "支付宝", "支付宝余额")
                PresetAccountItem(Icons.Default.Chat, "微信", "微信零钱")
            }
        }
    }
}

@Composable
fun PresetAccountItem(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CategorySetupStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "设置分类",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "系统已为您预设了常用收支分类，您可以在分类管理页面进行修改或添加更多分类。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column {
                Text("支出分类", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                Text("餐饮\n交通\n购物\n娱乐\n居住\n医疗\n教育\n通讯\n人情")
            }
            Column {
                Text("收入分类", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("工资\n奖金\n投资\n兼职\n红包\n退款\n其他")
            }
        }
    }
}

@Composable
fun CompleteStep(onSetupComplete: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "设置完成！",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "您已完成初始设置，现在可以开始使用AI记账了。\n\n试试在AI助手页面输入：\"午饭花了35元\"，体验智能记账！",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
