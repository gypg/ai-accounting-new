package com.example.aiaccounting.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.importer.ImportResult
import com.example.aiaccounting.data.importer.ImportType
import com.example.aiaccounting.data.local.entity.Transaction
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.viewmodel.ImportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "未知文件"
            viewModel.previewFile(it, fileName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入账单") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 导入类型选择
            ImportTypeSelector(
                selectedType = uiState.selectedImportType,
                onTypeSelected = { viewModel.selectImportType(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 导入说明
            ImportInstructions(
                instructions = viewModel.getImportInstructions(uiState.selectedImportType)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 选择文件按钮
            Button(
                onClick = {
                    filePickerLauncher.launch("*/*")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isImporting
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("选择文件导入")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 加载状态
            if (uiState.isImporting) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 导入结果
            uiState.importResult?.let { result ->
                ImportResultCard(
                    result = result,
                    onDismiss = { viewModel.clearResult() }
                )
            }
        }

        // 预览对话框
        if (uiState.showPreview) {
            ImportPreviewDialog(
                transactions = uiState.previewTransactions,
                fileName = uiState.fileName,
                onConfirm = { viewModel.confirmImport() },
                onDismiss = { viewModel.cancelPreview() }
            )
        }
    }
}

@Composable
private fun ImportTypeSelector(
    selectedType: ImportType,
    onTypeSelected: (ImportType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "选择导入类型",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ImportTypeItem(
                    type = ImportType.ALIPAY,
                    icon = "支",
                    label = "支付宝",
                    isSelected = selectedType == ImportType.ALIPAY,
                    onClick = { onTypeSelected(ImportType.ALIPAY) }
                )

                ImportTypeItem(
                    type = ImportType.WECHAT,
                    icon = "微",
                    label = "微信",
                    isSelected = selectedType == ImportType.WECHAT,
                    onClick = { onTypeSelected(ImportType.WECHAT) }
                )

                ImportTypeItem(
                    type = ImportType.CSV,
                    icon = "CSV",
                    label = "通用CSV",
                    isSelected = selectedType == ImportType.CSV,
                    onClick = { onTypeSelected(ImportType.CSV) }
                )
            }
        }
    }
}

@Composable
private fun ImportTypeItem(
    type: ImportType,
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected && type == ImportType.ALIPAY -> Color(0xFF1677FF)
        isSelected && type == ImportType.WECHAT -> Color(0xFF07C160)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )
    }
}

@Composable
private fun ImportInstructions(
    instructions: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "导入说明",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ImportResultCard(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    when (result) {
        is ImportResult.Success -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "导入成功！",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "成功导入 ${result.importedCount} 条记录",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (result.skippedCount > 0) {
                        Text(
                            text = "跳过 ${result.skippedCount} 条重复或无效记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            }
        }

        is ImportResult.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "导入失败",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewDialog(
    transactions: List<Transaction>,
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "导入预览",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${transactions.size} 条记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // 交易列表
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(transactions) { transaction ->
                        PreviewTransactionItem(transaction = transaction)
                        Divider()
                    }
                }

                // 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("确认导入")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTransactionItem(transaction: Transaction) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val amountColor = if (transaction.type == TransactionType.INCOME) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFF44336)
    }
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifEmpty { "无备注" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = dateFormat.format(Date(transaction.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "$sign${String.format("%.2f", transaction.amount)}",
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
            fontWeight = FontWeight.Bold
        )
    }
}

// 辅助函数：获取文件名
private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            result = result?.substring(cut!! + 1)
        }
    }
    return result
}
