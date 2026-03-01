package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ai.NaturalLanguageParser
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.ConversationRole
import com.example.aiaccounting.ui.viewmodel.AIAssistantViewModel
import com.example.aiaccounting.utils.NumberUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onNavigateBack: () -> Unit,
    viewModel: AIAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }

    // 自然语言解析器
    val parser = remember { NaturalLanguageParser() }
    var parsedResult by remember { mutableStateOf<NaturalLanguageParser.ParsedResult?>(null) }
    var showParsedConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "AI助手") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    IconButton(onClick = { viewModel.clearConversations() }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空对话")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 快捷操作按钮
            QuickActionButtons(
                onQuickAction = { action ->
                    inputText = action
                }
            )

            // 对话列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(conversations.reversed()) { conversation ->
                    ChatBubble(conversation = conversation)
                }
            }

            // 输入区域
            ChatInputArea(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        // 尝试自然语言解析
                        val result = parser.quickParse(inputText)
                        if (result.confidence >= 0.7f && result.amount != null) {
                            // 置信度高，显示确认对话框
                            parsedResult = result
                            showParsedConfirm = true
                        } else {
                            // 置信度低，直接发送给AI
                            viewModel.sendMessage(inputText)
                        }
                        inputText = ""
                    }
                },
                isLoading = uiState.isLoading
            )
        }
    }

    // 解析确认对话框
    if (showParsedConfirm && parsedResult != null) {
        ParsedTransactionDialog(
            result = parsedResult!!,
            onDismiss = { 
                showParsedConfirm = false
                parsedResult = null
            },
            onConfirm = {
                // 确认记账
                val confirmMessage = parser.generateConfirmationMessage(parsedResult!!)
                viewModel.sendMessage("确认记账：${parsedResult!!.amount}元")
                showParsedConfirm = false
                parsedResult = null
            },
            onEdit = {
                // 编辑，发送给AI
                viewModel.sendMessage(inputText)
                showParsedConfirm = false
                parsedResult = null
            }
        )
    }

    // AI配置对话框
    if (showConfigDialog) {
        AIConfigDialog(
            onDismiss = { showConfigDialog = false },
            onConfirm = { apiKey, baseUrl, model ->
                viewModel.configureAI(apiKey, baseUrl, model)
                showConfigDialog = false
            }
        )
    }

    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
}

@Composable
fun QuickActionButtons(
    onQuickAction: (String) -> Unit
) {
    val quickActions = listOf(
        "今天花了" to "今天花了",
        "昨天收入" to "昨天收入",
        "本周支出" to "本周支出",
        "本月统计" to "本月统计"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickActions.forEach { (label, action) ->
            AssistChip(
                onClick = { onQuickAction(action) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun ChatBubble(conversation: AIConversation) {
    val isUser = conversation.role == ConversationRole.USER
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                // AI头像
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = alignment,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                ) {
                    Text(
                        text = conversation.content,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 15.sp
                    )
                }

                // 时间戳
                Text(
                    text = formatTimestamp(conversation.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // 用户头像
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入记账内容或问题...") },
                maxLines = 4,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (inputText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ParsedTransactionDialog(
    result: NaturalLanguageParser.ParsedResult,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认记账信息") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 类型
                result.type?.let {
                    InfoRow(
                        label = "类型",
                        value = if (it.name == "INCOME") "收入" else "支出",
                        valueColor = if (it.name == "INCOME") Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }

                // 金额
                result.amount?.let {
                    InfoRow(
                        label = "金额",
                        value = NumberUtils.formatMoney(it),
                        valueColor = MaterialTheme.colorScheme.primary,
                        isBold = true
                    )
                }

                // 分类
                result.category?.let {
                    InfoRow(label = "分类", value = it)
                }

                // 日期
                result.date?.let {
                    val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                    InfoRow(label = "日期", value = sdf.format(it))
                }

                // 备注
                result.remark?.let {
                    if (it.isNotBlank()) {
                        InfoRow(label = "备注", value = it)
                    }
                }

                // 置信度
                LinearProgressIndicator(
                    progress = result.confidence,
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        result.confidence >= 0.8f -> Color(0xFF4CAF50)
                        result.confidence >= 0.6f -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = "置信度: ${(result.confidence * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确认记账")
            }
        },
        dismissButton = {
            TextButton(onClick = onEdit) {
                Text("编辑")
            }
        }
    )
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AIConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1/") }
    var model by remember { mutableStateOf("gpt-3.5-turbo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI配置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("API地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "提示：API Key将安全加密存储",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(apiKey, baseUrl, model) },
                enabled = apiKey.isNotBlank()
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

private fun formatTimestamp(date: Date): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }
    
    return when {
        isSameDay(now, messageTime) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        isYesterday(now, messageTime) -> {
            "昨天 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        }
        else -> {
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, messageTime: Calendar): Boolean {
    val yesterday = now.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    return isSameDay(yesterday, messageTime)
}
