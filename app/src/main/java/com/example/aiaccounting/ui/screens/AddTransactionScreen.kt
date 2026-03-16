package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.FlowRow
import com.example.aiaccounting.ui.viewmodel.TransactionViewModel
import com.example.aiaccounting.utils.DateUtils
import com.example.aiaccounting.utils.NumberUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 根据类型筛选分类
    val filteredCategories = categories.filter { it.type == selectedType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "记一笔") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 类型选择
            TypeSelector(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )

            // 金额输入
            AmountInput(
                amount = amount,
                onAmountChange = { amount = it }
            )

            // 账户选择
            AccountSelector(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onAccountSelected = { selectedAccount = it }
            )

            // 分类选择
            CategorySelector(
                categories = filteredCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // 日期选择
            DateSelector(
                date = date,
                onDateClick = { showDatePicker = true }
            )

            // 备注输入
            NoteInput(
                note = note,
                onNoteChange = { note = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            val canSave = amount.isNotBlank() && 
                         amount.toDoubleOrNull() != null && 
                         amount.toDoubleOrNull()!! > 0 && 
                         selectedAccount != null && 
                         selectedCategory != null
            
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    val account = selectedAccount
                    val category = selectedCategory
                    if (amountValue > 0 && account != null && category != null) {
                        viewModel.addTransaction(
                            amount = amountValue,
                            type = selectedType,
                            accountId = account.id,
                            categoryId = category.id,
                            date = date,
                            note = note
                        )
                        onSave()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = canSave
            ) {
                Text("保存")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Date(it)
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TransactionType.values().forEach { type ->
                val isSelected = type == selectedType
                val color = when (type) {
                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(type) },
                    label = {
                        Text(
                            when (type) {
                                TransactionType.INCOME -> "收入"
                                TransactionType.EXPENSE -> "支出"
                                TransactionType.TRANSFER -> "转账"
                            }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.2f),
                        selectedLabelColor = color
                    ),
                    leadingIcon = if (isSelected) {
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
}

@Composable
fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "金额",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    // 只允许数字和小数点
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        onAmountChange(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = {
                    Text(
                        text = "¥",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                singleLine = true
            )
        }
    }
}

@Composable
fun AccountSelector(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "账户",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (accounts.isEmpty()) {
                Text(
                    text = "暂无账户，请先添加账户",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { account ->
                        val isSelected = account.id == selectedAccount?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAccountSelected(account) },
                            label = { Text(account.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (account.type) {
                                        com.example.aiaccounting.data.local.entity.AccountType.CASH -> Icons.Default.Payments
                                        com.example.aiaccounting.data.local.entity.AccountType.BANK -> Icons.Default.AccountBalance
                                        com.example.aiaccounting.data.local.entity.AccountType.DEBIT_CARD -> Icons.Default.CreditCard
                                        com.example.aiaccounting.data.local.entity.AccountType.ALIPAY -> Icons.Default.AccountBalanceWallet
                                        com.example.aiaccounting.data.local.entity.AccountType.WECHAT -> Icons.Default.Chat
                                        com.example.aiaccounting.data.local.entity.AccountType.CREDIT_CARD -> Icons.Default.CreditScore
                                        com.example.aiaccounting.data.local.entity.AccountType.OTHER -> Icons.Default.AccountBalance
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "分类",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "暂无分类",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category.id == selectedCategory?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    date: Date,
    onDateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onDateClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "日期",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = DateUtils.formatDate(date),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun NoteInput(
    note: String,
    onNoteChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "备注",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("添加备注（可选）") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                },
                minLines = 2,
                maxLines = 4
            )
        }
    }
}
