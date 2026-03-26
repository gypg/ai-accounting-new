package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Tag
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.TagSelector
import com.example.aiaccounting.ui.viewmodel.TransactionViewModel
import com.example.aiaccounting.utils.NumberUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var transaction by remember { mutableStateOf<Transaction?>(null) }

    // 表单状态
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedTags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 加载交易数据
    LaunchedEffect(transactionId) {
        transaction = viewModel.getTransactionById(transactionId)
    }

    LaunchedEffect(transaction) {
        transaction?.let { trans ->
            amount = trans.amount.toString()
            selectedType = trans.type
            note = trans.note ?: ""
            date = Date(trans.date)
            // 账户和分类会在下面匹配
        }
    }

    // 匹配账户和分类
    LaunchedEffect(transaction, accounts, categories) {
        transaction?.let { trans ->
            selectedAccount = accounts.find { it.id == trans.accountId }
            selectedCategory = categories.find { it.id == trans.categoryId }
        }
    }

    // 根据类型筛选分类
    val filteredCategories = categories.filter { it.type == selectedType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "编辑交易") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (transaction == null) {
            // 加载中
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
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

                // AI 留痕信息
                TransactionTraceabilityInfo(transaction = transaction!!)

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

                // 标签选择
                TagSelector(
                    tags = tags,
                    selectedTags = selectedTags,
                    onTagSelected = { tag -> selectedTags = selectedTags + tag },
                    onTagDeselected = { tag -> selectedTags = selectedTags.filter { it.id != tag.id } }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 保存按钮
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        if (amountValue > 0 && selectedAccount != null && selectedCategory != null) {
                            viewModel.updateTransaction(
                                transactionId = transactionId,
                                amount = amountValue,
                                type = selectedType,
                                accountId = selectedAccount!!.id,
                                categoryId = selectedCategory!!.id,
                                date = date,
                                note = note,
                                selectedTags = selectedTags
                            )
                            onSave()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = amount.isNotEmpty() && selectedAccount != null && selectedCategory != null
                ) {
                    Text("保存修改")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
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

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条交易记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transactionId)
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TransactionTraceabilityInfo(transaction: Transaction) {
    val sourceLabel = when (transaction.aiSourceType) {
        "AI_REMOTE" -> "AI 云端"
        "AI_LOCAL" -> "AI 本地"
        else -> "手动"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("来源信息", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("记录来源：$sourceLabel")
            transaction.aiTraceId?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Trace ID：${it.take(8)}…")
            }
        }
    }
}
