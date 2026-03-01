package com.example.aiaccounting.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.ExportType
import com.example.aiaccounting.ui.viewmodel.ExportViewModel
import java.io.File

/**
 * Export Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var exportedFile by { mutableStateOf<File?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出数据") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "选择导出类型",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Export Options
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ExportOptionCard(
                        title = "导出所有交易",
                        description = "导出所有交易记录到Excel文件",
                        icon = Icons.Default.Description,
                        onClick = {
                            viewModel.exportAllTransactions { result ->
                                result.onSuccess { file ->
                                    exportedFile = file
                                    showSuccessDialog = true
                                }.onFailure { error ->
                                    // Error is handled in uiState
                                }
                            }
                        },
                        isLoading = uiState.isExporting && uiState.selectedExportType == ExportType.ALL_TRANSACTIONS
                    )
                }

                item {
                    ExportOptionCard(
                        title = "导出本月数据",
                        description = "导出当前月份的交易汇总",
                        icon = Icons.Default.CalendarMonth,
                        onClick = {
                            viewModel.exportCurrentMonth { result ->
                                result.onSuccess { file ->
                                    exportedFile = file
                                    showSuccessDialog = true
                                }
                            }
                        },
                        isLoading = uiState.isExporting && uiState.selectedExportType == ExportType.CURRENT_MONTH
                    )
                }

                item {
                    ExportOptionCard(
                        title = "导出指定月份",
                        description = "选择特定月份导出数据",
                        icon = Icons.Default.DateRange,
                        onClick = {
                            viewModel.exportMonth(
                                uiState.selectedYear,
                                uiState.selectedMonth
                            ) { result ->
                                result.onSuccess { file ->
                                    exportedFile = file
                                    showSuccessDialog = true
                                }
                            }
                        },
                        isLoading = uiState.isExporting && uiState.selectedExportType == ExportType.SPECIFIC_MONTH
                    )
                }

                // Month Selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "选择月份",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Year Selector
                                var yearExpanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { yearExpanded = true }
                                    ) {
                                        Text("${uiState.selectedYear}年")
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }

                                    DropdownMenu(
                                        expanded = yearExpanded,
                                        onDismissRequest = { yearExpanded = false }
                                    ) {
                                        uiState.availableYears.forEach { year ->
                                            DropdownMenuItem(
                                                text = { Text("${year}年") },
                                                onClick = {
                                                    viewModel.setSelectedYear(year)
                                                    yearExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Month Selector
                                var monthExpanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { monthExpanded = true }
                                    ) {
                                        Text("${uiState.selectedMonth}月")
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }

                                    DropdownMenu(
                                        expanded = monthExpanded,
                                        onDismissRequest = { monthExpanded = false }
                                    ) {
                                        uiState.availableMonths.forEach { month ->
                                            DropdownMenuItem(
                                                text = { Text("${month}月") },
                                                onClick = {
                                                    viewModel.setSelectedMonth(month)
                                                    monthExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ExportOptionCard(
                        title = "仅导出收入",
                        description = "导出所有收入记录",
                        icon = Icons.Default.TrendingUp,
                        onClick = {
                            viewModel.exportByType(
                                com.example.aiaccounting.data.local.entity.TransactionType.INCOME
                            ) { result ->
                                result.onSuccess { file ->
                                    exportedFile = file
                                    showSuccessDialog = true
                                }
                            }
                        },
                        isLoading = uiState.isExporting && uiState.selectedExportType == ExportType.INCOME_ONLY
                    )
                }

                item {
                    ExportOptionCard(
                        title = "仅导出支出",
                        description = "导出所有支出记录",
                        icon = Icons.Default.TrendingDown,
                        onClick = {
                            viewModel.exportByType(
                                com.example.aiaccounting.data.local.entity.TransactionType.EXPENSE
                            ) { result ->
                                result.onSuccess { file ->
                                    exportedFile = file
                                    showSuccessDialog = true
                                }
                            }
                        },
                        isLoading = uiState.isExporting && uiState.selectedExportType == ExportType.EXPENSE_ONLY
                    )
                }
            }

            // Error Message
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog && exportedFile != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("导出成功")
            },
            text = {
                Column {
                    Text("文件已保存到：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportedFile!!.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * Export Option Card
 */
@Composable
fun ExportOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isLoading) it else it.clickable(onClick = onClick) },
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}