package com.example.aiaccounting.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.aiaccounting.data.local.entity.AIConversation
import com.example.aiaccounting.data.local.entity.ChatSession
import com.example.aiaccounting.data.local.entity.ConversationRole
import com.example.aiaccounting.ui.viewmodel.AIAssistantViewModel
import kotlinx.coroutines.launch
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
    val sessions by viewModel.sessions.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 侧边栏状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 图片选择器 - 支持多选
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = selectedImageUris + uris
        }
    }

    // 刷新网络状态
    LaunchedEffect(Unit) {
        viewModel.refreshNetworkStatus()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SessionDrawerContent(
                sessions = sessions,
                currentSessionId = uiState.currentSessionId,
                onSessionClick = { sessionId ->
                    scope.launch {
                        viewModel.switchSession(sessionId)
                        drawerState.close()
                    }
                },
                onDeleteSession = { sessionId ->
                    viewModel.deleteSession(sessionId)
                },
                onRenameSession = { sessionId, newTitle ->
                    viewModel.renameSession(sessionId, newTitle)
                },
                onCreateNewSession = {
                    scope.launch {
                        viewModel.createNewSession()
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "AI助手")
                            // 网络状态指示器
                            val (indicatorColor, indicatorText) = when {
                                !uiState.isNetworkAvailable -> 
                                    Color(0xFFFFA726) to "离线"
                                uiState.isAIConfigured && uiState.isNetworkAvailable -> 
                                    Color(0xFF66BB6A) to "智能"
                                else -> 
                                    Color(0xFF42A5F5) to "本地"
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = indicatorColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    indicatorColor
                                )
                            ) {
                                Text(
                                    text = indicatorText,
                                    fontSize = 10.sp,
                                    color = indicatorColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // 话题列表按钮（列表图标改为打开侧边栏）
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.List, contentDescription = "话题列表")
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
                        ChatBubble(
                            conversation = conversation,
                            onCopy = { text ->
                                copyToClipboard(context, text)
                            }
                        )
                    }
                }

                // 输入区域
                ChatInputArea(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() || selectedImageUris.isNotEmpty()) {
                            if (selectedImageUris.isNotEmpty()) {
                                // 发送图片和文字
                                viewModel.sendMessageWithImages(inputText, selectedImageUris, context)
                                selectedImageUris = emptyList()
                            } else {
                                // 只发送文字
                                viewModel.sendMessage(inputText)
                            }
                            inputText = ""
                        }
                    },
                    onImageClick = {
                        imagePicker.launch("image/*")
                    },
                    isLoading = uiState.isLoading,
                    selectedImageUris = selectedImageUris,
                    onClearImage = { uri ->
                        selectedImageUris = selectedImageUris.filter { it != uri }
                    },
                    onClearAllImages = {
                        selectedImageUris = emptyList()
                    }
                )
            }
        }
    }

    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("AI回复", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
}

/**
 * 侧边栏会话列表内容
 */
@Composable
fun SessionDrawerContent(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    onSessionClick: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onCreateNewSession: () -> Unit
) {
    val context = LocalContext.current
    
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "话题列表",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // 新建话题按钮 - 始终显示
            TextButton(
                onClick = onCreateNewSession,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("新建话题", fontSize = 12.sp)
            }
        }
        
        Divider()
        
        // 会话列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(sessions) { session ->
                SessionItem(
                    session = session,
                    isSelected = session.id == currentSessionId,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { 
                        onDeleteSession(session.id)
                        Toast.makeText(context, "已删除话题", Toast.LENGTH_SHORT).show()
                    },
                    onRename = { newTitle ->
                        onRenameSession(session.id, newTitle)
                    }
                )
            }
            
            if (sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无话题\n点击上方新建话题",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个会话项
 */
@Composable
fun SessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除话题") },
            text = { Text("确定要删除话题\"${session.title}\"吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 重命名对话框
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(session.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名话题") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("话题名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRename(newTitle)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = formatSessionTime(session.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            // 更多操作按钮
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = Color.Red) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 格式化会话时间
 */
private fun formatSessionTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            sdf.format(Date(timestamp))
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
fun ChatBubble(
    conversation: AIConversation,
    onCopy: (String) -> Unit
) {
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
                    Column {
                        // 如果有图片，显示图片（支持多张）- 使用优化加载
                        conversation.imageUri?.let { uriString ->
                            // 分割多张图片URI
                            val uris = uriString.split(",")
                            if (uris.size == 1) {
                                // 单张图片 - 使用缩略图优化
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(uris[0])
                                        .crossfade(true)
                                        .size(width = 600, height = 400)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "图片",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // 多张图片网格 - 使用LazyRow优化性能
                                Text(
                                    text = "${uris.size}张图片",
                                    fontSize = 12.sp,
                                    color = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(8.dp)
                                )
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uris.take(6)) { uri ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(uri)
                                                .crossfade(true)
                                                .size(120)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = "图片",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    if (uris.size > 6) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "+${uris.size - 6}",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 文本内容
                        if (conversation.content.isNotBlank()) {
                            Text(
                                text = conversation.content,
                                modifier = Modifier.padding(12.dp),
                                color = textColor
                            )
                        }
                    }
                }
            }

            // 时间和操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isUser) 0.dp else 40.dp, vertical = 2.dp),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 用户消息也显示复制按钮
                if (isUser && conversation.content.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCopy(conversation.content) },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = formatTimestamp(conversation.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                // AI回复显示复制按钮
                if (!isUser && conversation.content.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCopy(conversation.content) },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onImageClick: () -> Unit,
    isLoading: Boolean,
    selectedImageUris: List<Uri>,
    onClearImage: (Uri) -> Unit,
    onClearAllImages: () -> Unit
) {
    // 图片放大查看状态
    var enlargedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // 已选图片预览区域
            if (selectedImageUris.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 80.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    // 标题和清空按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已选择 ${selectedImageUris.size} 张图片",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 图片网格预览 - 使用LazyRow优化性能
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(selectedImageUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable { enlargedImageUri = uri }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(uri)
                                        .crossfade(true)
                                        .size(160)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "已选图片",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { onClearImage(uri) },
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .background(Color.Red, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "清除",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        
                        // 添加更多图片按钮
                        item {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .clickable(onClick = onImageClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加图片",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // 清空按钮放在右侧固定位置
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    TextButton(
                        onClick = onClearAllImages,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "清空",
                            fontSize = 14.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // 图片放大查看弹窗
            if (enlargedImageUri != null) {
                ImageEnlargeDialog(
                    imageUri = enlargedImageUri!!,
                    onDismiss = { enlargedImageUri = null }
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图片按钮
                IconButton(onClick = onImageClick) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "发送图片"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { 
                        Text(if (selectedImageUris.isNotEmpty()) "添加文字描述（可选）..." else "输入消息...") 
                    },
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
                        containerColor = if (inputText.isNotBlank() || selectedImageUris.isNotEmpty()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "发送",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 图片放大查看弹窗
 */
@Composable
fun ImageEnlargeDialog(
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 放大图片
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "放大图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "点击图片任意位置关闭",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
