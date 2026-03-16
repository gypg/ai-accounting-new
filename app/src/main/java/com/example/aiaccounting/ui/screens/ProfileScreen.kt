package com.example.aiaccounting.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aiaccounting.data.local.prefs.AppStateManager

/**
 * 个人中心界面 - 简化版，只保留个人资料
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    appStateManager: AppStateManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf(appStateManager.getUserName()) }
    var userAvatar by remember { mutableStateOf(appStateManager.getUserAvatar()) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAvatarOptions by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = saveImageToInternalStorage(context, it)
            savedPath?.let { path ->
                appStateManager.setUserAvatar(path)
                userAvatar = path
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showAvatarOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (userAvatar != null) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "头像",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                // 编辑图标
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "更换头像",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 用户名
            Text(
                text = userName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 编辑按钮
            OutlinedButton(
                onClick = {
                    newUserName = userName
                    showEditNameDialog = true
                }
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("修改用户名")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 用户信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    ProfileInfoItem(
                        icon = Icons.Default.Person,
                        label = "用户名",
                        value = userName
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileInfoItem(
                        icon = Icons.Default.Email,
                        label = "用户ID",
                        value = "user_${appStateManager.hashCode()}"
                    )
                }
            }
        }
    }

    // 编辑用户名对话框
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("修改用户名") },
            text = {
                OutlinedTextField(
                    value = newUserName,
                    onValueChange = { newUserName = it },
                    label = { Text("用户名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUserName.isNotBlank()) {
                            appStateManager.setUserName(newUserName)
                            userName = newUserName
                            showEditNameDialog = false
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 头像选项对话框
    if (showAvatarOptions) {
        AlertDialog(
            onDismissRequest = { showAvatarOptions = false },
            title = { Text("更换头像") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showAvatarOptions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从相册选择")
                    }
                    if (userAvatar != null) {
                        TextButton(
                            onClick = {
                                appStateManager.setUserAvatar(null)
                                userAvatar = null
                                showAvatarOptions = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("删除头像")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarOptions = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun saveImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
