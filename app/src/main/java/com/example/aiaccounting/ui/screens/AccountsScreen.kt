package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.R
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.AccountType
import com.example.aiaccounting.ui.components.ColorSelector
import com.example.aiaccounting.ui.viewmodel.AccountViewModel
import com.example.aiaccounting.utils.NumberUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "账户管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加账户")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 总资产卡片
            TotalAssetsCard(accounts = accounts)

            // 账户列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onEdit = {
                            editingAccount = account
                            showEditDialog = true
                        },
                        onDelete = {
                            accountToDelete = account
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    // 添加账户对话框
    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, balance, icon, color ->
                viewModel.createAccount(name, type, balance, icon, color)
                showAddDialog = false
            }
        )
    }

    // 编辑账户对话框
    if (showEditDialog && editingAccount != null) {
        EditAccountDialog(
            account = editingAccount!!,
            onDismiss = { 
                showEditDialog = false
                editingAccount = null
            },
            onConfirm = { account ->
                viewModel.updateAccount(account)
                showEditDialog = false
                editingAccount = null
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm && accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm = false
                accountToDelete = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除账户\"${accountToDelete!!.name}\"吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(accountToDelete!!)
                        showDeleteConfirm = false
                        accountToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    accountToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // 可以在这里显示Snackbar
            viewModel.clearError()
        }
    }
}

@Composable
fun TotalAssetsCard(accounts: List<Account>) {
    val assetTypes = setOf(AccountType.CASH, AccountType.BANK, AccountType.DEBIT_CARD, AccountType.ALIPAY, AccountType.WECHAT)
    val totalAssets = accounts.filter { it.type in assetTypes }.sumOf { it.balance }
    val totalLiabilities = accounts.filter { it.type == AccountType.CREDIT_CARD }.sumOf { kotlin.math.abs(it.balance) }
    val netWorth = totalAssets - totalLiabilities

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "净资产",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = NumberUtils.formatMoney(netWorth),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (netWorth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总资产",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = NumberUtils.formatMoney(totalAssets),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "总负债",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = NumberUtils.formatMoney(totalLiabilities),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
            // 账户图标 - 使用品牌色和卡组织图标
            AccountIconBox(type = account.type, accountName = account.name)

            Spacer(modifier = Modifier.width(16.dp))

            // 账户信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = account.type.displayName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // 余额
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = NumberUtils.formatMoney(account.balance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (account.balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                if (account.isDefault) {
                    Text(
                        text = "默认",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 操作按钮
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 账户图标组件 - 使用品牌经典颜色和卡组织图标
 */
@Composable
fun AccountIconBox(type: AccountType, accountName: String = "") {
    // 检测卡组织
    val cardNetwork = detectCardNetwork(accountName)

    // 品牌经典颜色
    val brandColor = when (type) {
        AccountType.WECHAT -> Color(0xFF07C160)      // 微信经典绿色
        AccountType.ALIPAY -> Color(0xFF1677FF)      // 支付宝经典蓝色
        AccountType.CASH -> Color(0xFFFF9800)        // 现金橙色
        AccountType.BANK -> Color(0xFF2196F3)        // 银行卡蓝色
        AccountType.CREDIT_CARD -> when (cardNetwork) {
            CardNetwork.VISA -> Color(0xFF1A1F71)        // Visa蓝
            CardNetwork.MASTERCARD -> Color(0xFFEB001B)  // Mastercard红
            CardNetwork.UNIONPAY -> Color(0xFFE21836)    // 银联红
            CardNetwork.AMEX -> Color(0xFF016FD0)        // 运通蓝
            else -> Color(0xFF9C27B0)                     // 默认紫色
        }
        AccountType.DEBIT_CARD -> when (cardNetwork) {
            CardNetwork.VISA -> Color(0xFF1A1F71)
            CardNetwork.MASTERCARD -> Color(0xFFEB001B)
            CardNetwork.UNIONPAY -> Color(0xFFE21836)
            CardNetwork.AMEX -> Color(0xFF016FD0)
            else -> Color(0xFF4CAF50)
        }
        AccountType.OTHER -> Color(0xFF607D8B)       // 其他灰色
    }

    // 图标背景色（浅色版本）
    val backgroundColor = when (type) {
        AccountType.WECHAT -> Color(0xFFE8F5E9)      // 微信浅绿背景
        AccountType.ALIPAY -> Color(0xFFE6F7FF)      // 支付宝浅蓝背景
        AccountType.CASH -> Color(0xFFFFF3E0)        // 现金浅橙背景
        AccountType.BANK -> Color(0xFFE3F2FD)        // 银行卡浅蓝背景
        AccountType.CREDIT_CARD, AccountType.DEBIT_CARD -> when (cardNetwork) {
            CardNetwork.VISA -> Color(0xFFE8EAF6)      // Visa浅蓝背景
            CardNetwork.MASTERCARD -> Color(0xFFFFEBEE) // Mastercard浅红背景
            CardNetwork.UNIONPAY -> Color(0xFFFFEBEE)   // 银联浅红背景
            CardNetwork.AMEX -> Color(0xFFE3F2FD)       // 运通浅蓝背景
            else -> Color(0xFFF3E5F5)                    // 默认浅紫背景
        }
        AccountType.OTHER -> Color(0xFFECEFF1)       // 其他浅灰背景
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // 根据账户类型显示对应图标
        when (type) {
            // 微信使用品牌图标
            AccountType.WECHAT -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_wechat),
                    contentDescription = "微信",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )
            }
            // 支付宝使用品牌图标
            AccountType.ALIPAY -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alipay),
                    contentDescription = "支付宝",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )
            }
            // 信用卡/借记卡显示卡组织图标
            AccountType.CREDIT_CARD, AccountType.DEBIT_CARD -> {
                if (cardNetwork != CardNetwork.UNKNOWN) {
                    CardNetworkIcon(cardNetwork = cardNetwork, modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        imageVector = getAccountIcon(type),
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            // 其他类型使用默认图标
            else -> {
                Icon(
                    imageVector = getAccountIcon(type),
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 卡组织类型
 */
enum class CardNetwork {
    VISA, MASTERCARD, UNIONPAY, AMEX, UNKNOWN
}

/**
 * 检测卡组织 - 根据账户名称识别
 */
fun detectCardNetwork(accountName: String): CardNetwork {
    val name = accountName.uppercase()
    return when {
        name.contains("VISA") || name.contains("维萨") -> CardNetwork.VISA
        name.contains("MASTER") || name.contains("万事达") -> CardNetwork.MASTERCARD
        name.contains("UNION") || name.contains("银联") || name.contains("云闪付") -> CardNetwork.UNIONPAY
        name.contains("AMEX") || name.contains("AMERICAN") || name.contains("运通") || name.contains("美国运通") -> CardNetwork.AMEX
        else -> CardNetwork.UNKNOWN
    }
}

/**
 * 卡组织图标组件
 */
@Composable
fun CardNetworkIcon(cardNetwork: CardNetwork, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawableRes = when (cardNetwork) {
        CardNetwork.VISA -> R.drawable.ic_visa
        CardNetwork.MASTERCARD -> R.drawable.ic_mastercard
        CardNetwork.UNIONPAY -> R.drawable.ic_unionpay
        CardNetwork.AMEX -> R.drawable.ic_amex
        CardNetwork.UNKNOWN -> null
    }

    if (drawableRes != null) {
        Icon(
            painter = painterResource(id = drawableRes),
            contentDescription = cardNetwork.name,
            modifier = modifier,
            tint = Color.Unspecified // 保持原始颜色
        )
    } else {
        // 未知卡组织显示默认信用卡图标
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = "信用卡",
            modifier = modifier,
            tint = Color(0xFF9C27B0)
        )
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CASH) }
    var balance by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(AccountColors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加账户") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 账户名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 账户类型
                Text("账户类型", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                AccountTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                // 初始余额
                OutlinedTextField(
                    value = balance,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                            balance = it
                        }
                    },
                    label = { Text("初始余额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )

                // 颜色选择
                Text("账户颜色", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    colors = AccountColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            selectedType,
                            balance.toDoubleOrNull() ?: 0.0,
                            "account_balance",
                            selectedColor
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var balance by remember { mutableStateOf(account.balance.toString()) }
    var selectedColor by remember { mutableStateOf(account.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑账户") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balance,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                            balance = it
                        }
                    },
                    label = { Text("账户余额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )

                Text("账户颜色", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    colors = AccountColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            account.copy(
                                name = name,
                                balance = balance.toDoubleOrNull() ?: account.balance,
                                color = selectedColor
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AccountTypeSelector(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccountType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
                leadingIcon = if (selectedType == type) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }
    }
}

fun getAccountIcon(type: AccountType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        AccountType.CASH -> Icons.Default.Money
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.DEBIT_CARD -> Icons.Default.CreditCard
        AccountType.ALIPAY -> Icons.Default.AccountBalanceWallet
        AccountType.WECHAT -> Icons.Default.Chat
        AccountType.OTHER -> Icons.Default.AccountBalance
    }
}

val AccountColors = listOf(
    "#FF6B9FFF",  // 蓝色
    "#FF4CAF50",  // 绿色
    "#FFF44336",  // 红色
    "#FFFF9800",  // 橙色
    "#FF9C27B0",  // 紫色
    "#FF00BCD4",  // 青色
    "#FF795548",  // 棕色
    "#FF607D8B"   // 蓝灰色
)

val AccountType.displayName: String
    get() = when (this) {
        AccountType.CASH -> "现金"
        AccountType.BANK -> "银行卡"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.DEBIT_CARD -> "借记卡"
        AccountType.ALIPAY -> "支付宝"
        AccountType.WECHAT -> "微信"
        AccountType.OTHER -> "其他"
    }
