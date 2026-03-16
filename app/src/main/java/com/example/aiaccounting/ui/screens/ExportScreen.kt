package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.ExportViewModel
import com.example.aiaccounting.ui.viewmodel.ExportType
import com.example.aiaccounting.utils.DateUtils
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDateRangeDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(ExportType.CURRENT_MONTH) }
    var exportResult by remember { mutableStateOf<File?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "数据导出") },
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
                .padding(16.dp)
        ) {
            // 导出选项
            ExportOptionsCard(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                onExportClick = {
                    when (selectedType) {
                        ExportType.CUSTOM_RANGE -> showDateRangeDialog = true
                        else -> performExport(
                            viewModel = viewModel,
                            type = selectedType,
                            selectedYear = uiState.selectedYear,
                            selectedMonth = uiState.selectedMonth
                        ) { result ->
                            exportResult = result
                            showResultDialog = true
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 导出说明
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "导出说明",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 导出文件为CSV格式(.csv)\n" +
                               "• 文件保存在应用专属目录\n" +
                               "• 可用Excel或WPS打开查看\n" +
                               "• 包含交易明细、收支汇总等信息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 日期范围选择对话框
    if (showDateRangeDialog) {
        DateRangeDialog(
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { startDate, endDate ->
                viewModel.exportDateRange(startDate.time, endDate.time) { result ->
                    result.onSuccess { file ->
                        exportResult = file
                        showResultDialog = true
                    }
                }
                showDateRangeDialog = false
            }
        )
    }

    // 导出结果对话框
    exportResult?.let { result ->
        if (showResultDialog) {
            AlertDialog(
                onDismissRequest = { showResultDialog = false },
                title = { Text("导出成功") },
                text = {
                    Column {
                        Text("文件已保存到:")
                        Text(
                            result.absolutePath,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResultDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
    }

    // 显示导出进度
    if (uiState.isExporting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("正在导出") },
            text = {
                Column {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("请稍候...")
                }
            },
            confirmButton = { }
        )
    }

    // 显示错误
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("导出失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("确定")
                }
            }
        )
    }
}

private fun performExport(
    viewModel: ExportViewModel,
    type: ExportType,
    selectedYear: Int,
    selectedMonth: Int,
    onResult: (File) -> Unit
) {
    when (type) {
        ExportType.ALL_TRANSACTIONS -> viewModel.exportAllTransactions { result ->
            result.onSuccess { onResult(it) }
        }
        ExportType.CURRENT_MONTH -> viewModel.exportCurrentMonth { result ->
            result.onSuccess { onResult(it) }
        }
        ExportType.SPECIFIC_MONTH -> {
            // 使用当前选中的年月
            viewModel.exportMonth(selectedYear, selectedMonth) { result ->
                result.onSuccess { onResult(it) }
            }
        }
        ExportType.INCOME_ONLY -> viewModel.exportByType(
            com.example.aiaccounting.data.local.entity.TransactionType.INCOME
        ) { result ->
            result.onSuccess { onResult(it) }
        }
        ExportType.EXPENSE_ONLY -> viewModel.exportByType(
            com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE
        ) { result ->
            result.onSuccess { onResult(it) }
        }
        else -> {}
    }
}

@Composable
fun ExportOptionsCard(
    selectedType: ExportType,
    onTypeSelected: (ExportType) -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "导出类型",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                ExportTypeOption(
                    type = ExportType.CURRENT_MONTH,
                    title = "本月数据",
                    description = "导出当前月份的所有交易",
                    selected = selectedType == ExportType.CURRENT_MONTH,
                    onSelect = { onTypeSelected(ExportType.CURRENT_MONTH) }
                )

                ExportTypeOption(
                    type = ExportType.ALL_TRANSACTIONS,
                    title = "全部数据",
                    description = "导出所有历史交易记录",
                    selected = selectedType == ExportType.ALL_TRANSACTIONS,
                    onSelect = { onTypeSelected(ExportType.ALL_TRANSACTIONS) }
                )

                ExportTypeOption(
                    type = ExportType.CUSTOM_RANGE,
                    title = "自定义范围",
                    description = "选择开始和结束日期",
                    selected = selectedType == ExportType.CUSTOM_RANGE,
                    onSelect = { onTypeSelected(ExportType.CUSTOM_RANGE) }
                )

                ExportTypeOption(
                    type = ExportType.INCOME_ONLY,
                    title = "仅收入",
                    description = "只导出收入记录",
                    selected = selectedType == ExportType.INCOME_ONLY,
                    onSelect = { onTypeSelected(ExportType.INCOME_ONLY) }
                )

                ExportTypeOption(
                    type = ExportType.EXPENSE_ONLY,
                    title = "仅支出",
                    description = "只导出支出记录",
                    selected = selectedType == ExportType.EXPENSE_ONLY,
                    onSelect = { onTypeSelected(ExportType.EXPENSE_ONLY) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出数据")
            }
        }
    }
}

@Composable
fun ExportTypeOption(
    type: ExportType,
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DateRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Date, Date) -> Unit
) {
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf(Date()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导出日期范围") },
        text = {
            Column {
                // 开始日期
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始日期: ${DateUtils.formatDate(startDate)}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 结束日期
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("结束日期: ${DateUtils.formatDate(endDate)}")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startDate, endDate) },
                enabled = startDate <= endDate
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 开始日期选择器
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            startDate = Date(it)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 结束日期选择器
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            endDate = Date(it)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
