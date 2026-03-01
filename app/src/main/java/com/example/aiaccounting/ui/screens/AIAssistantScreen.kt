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
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.ConversationRole
import com.example.aiaccounting.ui.viewmodel.AIAssistantViewModel
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
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                isLoading = uiState.isLoading
            )
        }
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
        "分析我的消费" to Icons.Default.Analytics,
        "本月预算建议" to Icons.Default.AccountBalance,
        "如何节省开支" to Icons.Default.Savings
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickActions.forEach { (label, icon) ->
            OutlinedButton(
                onClick = { onQuickAction(label) },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text = label, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(conversation: AIConversation) {
    val isUser = conversation.role == ConversationRole.USER
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .widthIn(max = 300.dp)
        ) {
            // 头像和名称
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isUser) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI助手",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "我",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 消息内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isUser) 0.dp else 40.dp, vertical = 0.dp),
                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = backgroundColor
                ) {
                    Text(
                        text = conversation.content,
                        modifier = Modifier.padding(12.dp),
                        color = textColor
                    )
                }
            }

            // 时间戳
            Text(
                text = formatTimestamp(conversation.timestamp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isUser) 0.dp else 40.dp, vertical = 2.dp),
                textAlign = if (isUser) TextAlign.End else TextAlign.Start
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                maxLines = 3,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 2.dp
                )
            } else {
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
