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
import com.example.aiaccounting.data.local.entity.AIOperationTrace
import com.example.aiaccounting.data.local.entity.Account
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.Tag
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.TagSelector
import com.example.aiaccounting.ui.viewmodel.TransactionViewModel
import com.example.aiaccounting.utils.DateUtils
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
    var showTraceDetails by remember { mutableStateOf(false) }

    // 加载交易数据
    LaunchedEffect(transactionId) {
        transaction = viewModel.getTransactionById(transactionId)
    }

    LaunchedEffect(transaction) {
        transaction?.let { trans ->
            amount = trans.amount.toString()
            selectedType = trans.type
            note = trans.note
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

    LaunchedEffect(showTraceDetails, transaction?.aiTraceId) {
        val traceId = transaction?.aiTraceId?.takeIf { it.isNotBlank() }
        if (showTraceDetails && traceId != null) {
            viewModel.loadTraceDetails(traceId)
        } else if (!showTraceDetails) {
            viewModel.clearTraceDetails()
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
                TransactionTraceabilityInfo(
                    transaction = transaction!!,
                    onViewTraceDetails = {
                        transaction?.aiTraceId?.takeIf { it.isNotBlank() }?.let {
                            showTraceDetails = true
                        }
                    }
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

    if (showTraceDetails) {
        TraceDetailsDialog(
            traceId = transaction?.aiTraceId,
            traces = uiState.traceDetails,
            isLoading = uiState.isTraceLoading,
            error = uiState.traceError,
            onDismiss = {
                showTraceDetails = false
                viewModel.clearTraceDetails()
            }
        )
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
private fun TransactionTraceabilityInfo(
    transaction: Transaction,
    onViewTraceDetails: () -> Unit
) {
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
            val traceId = transaction.aiTraceId?.takeIf { it.isNotBlank() }
            traceId?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Trace ID：${it.take(8)}…")
            }
            if (transaction.aiSourceType != "MANUAL" && traceId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onViewTraceDetails) {
                    Text("查看 AI 过程")
                }
            }
        }
    }
}

@Composable
private fun TraceDetailsDialog(
    traceId: String?,
    traces: List<AIOperationTrace>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 执行过程") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "这里会展示这笔交易对应的 AI 执行留痕。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                traceId?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Trace ID：$it", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null -> {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    traces.isEmpty() -> {
                        Text("未找到这笔交易对应的 AI 留痕记录")
                    }
                    else -> {
                        traces.forEachIndexed { index, trace ->
                            TraceTimelineItem(trace = trace)
                            if (index != traces.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun TraceTimelineItem(trace: AIOperationTrace) {
    val statusLabel = if (trace.success) "执行成功" else "执行失败"
    val statusColor = if (trace.success) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val sourceLabel = when (trace.sourceType) {
        "AI_REMOTE" -> "云端 AI"
        "AI_LOCAL" -> "本地 AI"
        else -> trace.sourceType.ifBlank { "未知来源" }
    }
    val actionLabel = when (trace.actionType) {
        "CREATE_TRANSACTION", "ADD_TRANSACTION" -> "记账"
        "UPDATE_TRANSACTION" -> "修改交易"
        "DELETE_TRANSACTION" -> "删除交易"
        "CREATE_ACCOUNT", "ADD_ACCOUNT" -> "创建账户"
        "CREATE_CATEGORY", "ADD_CATEGORY" -> "创建分类"
        "AUTO_CREATE_SUPPORTING_ENTITY" -> "自动补建实体"
        else -> trace.actionType.ifBlank { "执行操作" }
    }
    val entityLabel = when (trace.entityType) {
        "TRANSACTION" -> "交易"
        "ACCOUNT" -> "账户"
        "CATEGORY" -> "分类"
        else -> trace.entityType.ifBlank { "对象" }
    }
    val timelineTitle = trace.summary.ifBlank { "$actionLabel · $entityLabel" }
    val detailsText = trace.details?.takeIf { it.isNotBlank() }
    val errorText = trace.errorMessage?.takeIf { it.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(timelineTitle, style = MaterialTheme.typography.titleSmall)
        Text(
            "$sourceLabel · $actionLabel · $entityLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            DateUtils.formatDateTime(trace.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(statusLabel, color = statusColor, style = MaterialTheme.typography.bodySmall)
        detailsText?.let {
            Text("详情：$it", style = MaterialTheme.typography.bodyMedium)
        }
        errorText?.let {
            Text("失败原因：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
