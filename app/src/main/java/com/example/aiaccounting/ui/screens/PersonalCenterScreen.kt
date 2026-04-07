package com.example.aiaccounting.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import com.example.aiaccounting.data.local.prefs.AppStateManager
import com.example.aiaccounting.data.model.*
import com.example.aiaccounting.ui.components.AvatarCropper
import com.example.aiaccounting.ui.components.AvatarPreview
import com.example.aiaccounting.ui.viewmodel.SettingsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 个人中心主页面
 * 整合所有个人设置功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalCenterScreen(
    appStateManager: AppStateManager,
    onNavigateBack: () -> Unit,
    onNavigateToSetupPin: () -> Unit = {},
    onThemeChanged: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    val currentTheme = appStateManager.getTheme()
    val context = LocalContext.current
    
    // 用户数据
    var userProfile by remember { 
        mutableStateOf(
            UserProfile(
                userName = appStateManager.getUserName(),
                avatarUrl = appStateManager.getUserAvatar()
            )
        )
    }
    
    // 头像相关状态
    var showAvatarOptions by remember { mutableStateOf(false) }
    var showAvatarCropper by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 主题设置对话框
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // 安全设置对话框
    var showPinOptionsDialog by remember { mutableStateOf(false) }
    var showBiometricDialog by remember { mutableStateOf(false) }
    
    // 个人资料编辑对话框
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editUserName by remember { mutableStateOf(userProfile.userName) }
    var editNickName by remember { mutableStateOf(userProfile.nickName) }
    var editGender by remember { mutableStateOf(userProfile.gender) }
    
    // 账户信息对话框
    var showAccountInfoDialog by remember { mutableStateOf(false) }
    
    // 隐私设置对话框
    var showPrivacySettingsDialog by remember { mutableStateOf(false) }
    var profileVisibility by remember { mutableStateOf(VisibilityLevel.PUBLIC) }
    var allowSearchByPhone by remember { mutableStateOf(true) }
    var allowSearchByEmail by remember { mutableStateOf(false) }
    
    // 通知设置对话框
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var pushNotifications by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(true) }
    var marketingEmails by remember { mutableStateOf(false) }
    
    // 帮助与反馈对话框
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // 关于对话框
    var showAboutDialog by remember { mutableStateOf(false) }
    val openReleasePage: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            showAvatarCropper = true
        }
    }
    
    // 相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            showAvatarCropper = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
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
                .verticalScroll(scrollState)
        ) {
            // 头像和基本信息区域
            ProfileHeaderSection(
                userProfile = userProfile,
                onAvatarClick = { showAvatarOptions = true },
                onEditProfile = { 
                    editUserName = userProfile.userName
                    editNickName = userProfile.nickName
                    editGender = userProfile.gender
                    showEditProfileDialog = true 
                }
            )
            
            // 设置分类列表
            SettingsCategoryList(
                uiState = uiState,
                currentTheme = currentTheme,
                onBasicInfoClick = { 
                    editUserName = userProfile.userName
                    editNickName = userProfile.nickName
                    editGender = userProfile.gender
                    showEditProfileDialog = true 
                },
                onAccountInfoClick = { showAccountInfoDialog = true },
                onSecurityClick = { showPinOptionsDialog = true },
                onPrivacyClick = { showPrivacySettingsDialog = true },
                onPersonalizationClick = { showThemeDialog = true },
                onNotificationClick = { showNotificationSettingsDialog = true },
                onHelpClick = { showHelpDialog = true },
                onAboutClick = { showAboutDialog = true }
            )
        }
    }
    
    // 头像选项对话框
    if (showAvatarOptions) {
        AvatarOptionsDialog(
            hasAvatar = userProfile.avatarUrl != null,
            onGallerySelect = {
                imagePickerLauncher.launch("image/*")
                showAvatarOptions = false
            },
            onCameraSelect = {
                // 创建临时文件URI用于相机拍照
                val tempFile = File.createTempFile("avatar_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                selectedImageUri = uri
                cameraLauncher.launch(uri)
                showAvatarOptions = false
            },
            onDeleteAvatar = {
                appStateManager.setUserAvatar(null)
                userProfile = userProfile.copy(avatarUrl = null)
                showAvatarOptions = false
            },
            onDismiss = { showAvatarOptions = false }
        )
    }
    
    // 头像裁剪对话框
    if (showAvatarCropper && selectedImageUri != null) {
        Dialog(
            onDismissRequest = { showAvatarCropper = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AvatarCropper(
                    imageUri = selectedImageUri!!,
                    onCropComplete = { croppedPath ->
                        // 保存头像
                        appStateManager.setUserAvatar(croppedPath)
                        userProfile = userProfile.copy(avatarUrl = croppedPath)
                        showAvatarCropper = false
                        selectedImageUri = null
                    },
                    onCancel = {
                        showAvatarCropper = false
                        selectedImageUri = null
                    }
                )
            }
        }
    }
    
    // 主题设置对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = { theme ->
                appStateManager.setTheme(theme)
                showThemeDialog = false
                onThemeChanged()
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // PIN码选项对话框
    if (showPinOptionsDialog) {
        PinOptionsDialog(
            isPinSet = uiState.isPinSet,
            onSetupPin = {
                showPinOptionsDialog = false
                onNavigateToSetupPin()
            },
            onChangePin = {
                showPinOptionsDialog = false
                onNavigateToSetupPin()
            },
            onClearPin = {
                viewModel.clearPin()
                showPinOptionsDialog = false
            },
            onBiometricClick = { showBiometricDialog = true },
            isBiometricEnabled = uiState.isBiometricEnabled,
            onDismiss = { showPinOptionsDialog = false }
        )
    }
    
    // 生物识别设置对话框
    if (showBiometricDialog) {
        BiometricDialog(
            isEnabled = uiState.isBiometricEnabled,
            onToggle = { enabled ->
                viewModel.toggleBiometric(enabled)
            },
            onDismiss = { showBiometricDialog = false }
        )
    }
    
    // 编辑个人资料对话框
    if (showEditProfileDialog) {
        EditProfileDialog(
            userName = editUserName,
            nickName = editNickName,
            gender = editGender,
            onUserNameChange = { editUserName = it },
            onNickNameChange = { editNickName = it },
            onGenderChange = { editGender = it },
            onSave = {
                appStateManager.setUserName(editUserName)
                userProfile = userProfile.copy(
                    userName = editUserName,
                    nickName = editNickName,
                    gender = editGender
                )
                showEditProfileDialog = false
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }
    
    // 账户信息对话框
    if (showAccountInfoDialog) {
        AccountInfoDialog(
            userProfile = userProfile,
            onDismiss = { showAccountInfoDialog = false }
        )
    }
    
    // 隐私设置对话框
    if (showPrivacySettingsDialog) {
        PrivacySettingsDialog(
            profileVisibility = profileVisibility,
            allowSearchByPhone = allowSearchByPhone,
            allowSearchByEmail = allowSearchByEmail,
            onProfileVisibilityChange = { profileVisibility = it },
            onAllowSearchByPhoneChange = { allowSearchByPhone = it },
            onAllowSearchByEmailChange = { allowSearchByEmail = it },
            onSave = { showPrivacySettingsDialog = false },
            onDismiss = { showPrivacySettingsDialog = false }
        )
    }
    
    // 通知设置对话框
    if (showNotificationSettingsDialog) {
        NotificationSettingsDialog(
            pushNotifications = pushNotifications,
            emailNotifications = emailNotifications,
            marketingEmails = marketingEmails,
            onPushNotificationsChange = { pushNotifications = it },
            onEmailNotificationsChange = { emailNotifications = it },
            onMarketingEmailsChange = { marketingEmails = it },
            onDismiss = { showNotificationSettingsDialog = false }
        )
    }
    
    // 帮助与反馈对话框
    if (showHelpDialog) {
        HelpFeedbackDialog(
            context = context,
            onDismiss = { showHelpDialog = false }
        )
    }
    
    // 关于对话框
    if (showAboutDialog) {
        AboutDialog(
            context = context,
            isCheckingUpdate = uiState.isCheckingUpdate,
            onCheckUpdate = { viewModel.checkForUpdates() },
            onDismiss = { showAboutDialog = false }
        )
    }

    uiState.updateDialogState?.let { dialogState ->
        UpdateResultDialog(
            state = dialogState,
            onDismiss = viewModel::dismissUpdateDialog,
            onRetry = viewModel::checkForUpdates,
            onOpenReleasePage = openReleasePage
        )
    }
}

/**
 * 个人资料头部区域
 */
@Composable
private fun ProfileHeaderSection(
    userProfile: UserProfile,
    onAvatarClick: () -> Unit,
    onEditProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像
        AvatarPreview(
            avatarPath = userProfile.avatarUrl,
            size = 120.dp,
            onClick = onAvatarClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 用户名
        Text(
            text = userProfile.userName.ifEmpty { "用户" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // 用户ID
        Text(
            text = "ID: user_${userProfile.id.hashCode()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 显示昵称（如果有）
        if (userProfile.nickName.isNotEmpty()) {
            Text(
                text = "昵称: ${userProfile.nickName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 编辑资料按钮
        OutlinedButton(
            onClick = onEditProfile,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("编辑资料")
        }
    }
}

/**
 * 设置分类列表
 */
@Composable
private fun SettingsCategoryList(
    uiState: com.example.aiaccounting.ui.viewmodel.SettingsUiState,
    currentTheme: String,
    onBasicInfoClick: () -> Unit,
    onAccountInfoClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val themeText = when (currentTheme) {
        "light" -> "浅色"
        "dark" -> "深色"
        "amoled" -> "AMOLED纯黑"
        "dynamic" -> "Material You动态"
        else -> "跟随系统"
    }
    
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // 基本信息
        SettingsCategoryCard(
            title = "基本信息",
            items = listOf(
                SettingItem(
                    icon = Icons.Default.Person,
                    title = "个人资料",
                    subtitle = "查看和编辑个人信息",
                    onClick = onBasicInfoClick
                ),
                SettingItem(
                    icon = Icons.Default.Badge,
                    title = "账户信息",
                    subtitle = "查看账户状态和详情",
                    onClick = onAccountInfoClick
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 安全与隐私
        SettingsCategoryCard(
            title = "安全与隐私",
            items = listOf(
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "账户安全",
                    subtitle = if (uiState.isPinSet) "PIN码已设置" else "PIN码未设置",
                    onClick = onSecurityClick
                ),
                SettingItem(
                    icon = Icons.Default.Visibility,
                    title = "隐私设置",
                    subtitle = "资料可见性、授权管理",
                    onClick = onPrivacyClick
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 个性化
        SettingsCategoryCard(
            title = "个性化",
            items = listOf(
                SettingItem(
                    icon = Icons.Default.Palette,
                    title = "外观设置",
                    subtitle = "当前: $themeText",
                    onClick = onPersonalizationClick
                ),
                SettingItem(
                    icon = Icons.Default.Notifications,
                    title = "通知设置",
                    subtitle = "消息推送、邮件通知",
                    onClick = onNotificationClick
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 其他
        SettingsCategoryCard(
            title = "其他",
            items = listOf(
                SettingItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "帮助与反馈",
                    subtitle = "常见问题、意见反馈",
                    onClick = onHelpClick
                ),
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "关于",
                    subtitle = "版本信息、用户协议",
                    onClick = onAboutClick
                )
            )
        )
    }
}

/**
 * 设置分类卡片
 */
@Composable
private fun SettingsCategoryCard(
    title: String,
    items: List<SettingItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            items.forEachIndexed { index, item ->
                SettingItemRow(item)
                if (index < items.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

/**
 * 设置项数据
 */
data class SettingItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

/**
 * 设置项行
 */
@Composable
private fun SettingItemRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 头像选项对话框
 */
@Composable
private fun AvatarOptionsDialog(
    hasAvatar: Boolean,
    onGallerySelect: () -> Unit,
    onCameraSelect: () -> Unit,
    onDeleteAvatar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更换头像") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("从相册选择") },
                    leadingContent = {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onGallerySelect)
                )
                
                ListItem(
                    headlineContent = { Text("拍照") },
                    leadingContent = {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onCameraSelect)
                )
                
                if (hasAvatar) {
                    ListItem(
                        headlineContent = { Text("删除头像") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable(onClick = onDeleteAvatar),
                        colors = ListItemDefaults.colors(
                            headlineColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 主题选择对话框
 */
@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        "system" to "跟随系统",
        "light" to "浅色",
        "dark" to "深色",
        "amoled" to "AMOLED纯黑",
        "horse_2026" to "2026马年主题"
    )
    
    // Android 12+ 支持动态主题
    val dynamicThemes = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        themes + listOf("dynamic" to "Material You动态")
    } else {
        themes
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                dynamicThemes.forEach { (theme, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * PIN码选项对话框
 */
@Composable
private fun PinOptionsDialog(
    isPinSet: Boolean,
    onSetupPin: () -> Unit,
    onChangePin: () -> Unit,
    onClearPin: () -> Unit,
    onBiometricClick: () -> Unit,
    isBiometricEnabled: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账户安全") },
        text = {
            Column {
                // PIN码设置
                ListItem(
                    headlineContent = { Text("PIN码保护") },
                    supportingContent = { Text(if (isPinSet) "已设置" else "未设置") },
                    leadingContent = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingContent = {
                        TextButton(
                            onClick = if (isPinSet) onChangePin else onSetupPin
                        ) {
                            Text(if (isPinSet) "修改" else "设置")
                        }
                    }
                )
                
                if (isPinSet) {
                    TextButton(
                        onClick = onClearPin,
                        modifier = Modifier.padding(start = 56.dp)
                    ) {
                        Text("清除PIN码", color = MaterialTheme.colorScheme.error)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 生物识别
                ListItem(
                    headlineContent = { Text("生物识别解锁") },
                    supportingContent = { Text(if (isBiometricEnabled) "已开启" else "已关闭") },
                    leadingContent = {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                    },
                    trailingContent = {
                        TextButton(onClick = onBiometricClick) {
                            Text("设置")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 生物识别设置对话框
 */
@Composable
private fun BiometricDialog(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(isEnabled) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("生物识别解锁") },
        text = {
            Column {
                Text("使用指纹或面部识别快速解锁应用")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启用生物识别")
                    Switch(
                        checked = enabled,
                        onCheckedChange = { 
                            enabled = it
                            onToggle(it)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

/**
 * 编辑个人资料对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    userName: String,
    nickName: String,
    gender: Gender,
    onUserNameChange: (String) -> Unit,
    onNickNameChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val genderOptions = listOf(
        "男" to Gender.MALE, 
        "女" to Gender.FEMALE, 
        "保密" to Gender.UNSPECIFIED
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑个人资料") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 用户名
                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChange,
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 昵称
                OutlinedTextField(
                    value = nickName,
                    onValueChange = onNickNameChange,
                    label = { Text("昵称（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 性别选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = genderOptions.find { it.second == gender }?.first ?: "保密",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("性别") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        genderOptions.forEach { (label, value) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onGenderChange(value)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
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

/**
 * 账户信息对话框
 */
@Composable
private fun AccountInfoDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("账户信息") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountInfoItem("用户ID", "user_${userProfile.id.hashCode()}")
                AccountInfoItem("用户名", userProfile.userName)
                AccountInfoItem("昵称", userProfile.nickName.ifEmpty { "未设置" })
                AccountInfoItem("性别", when(userProfile.gender) {
                    Gender.MALE -> "男"
                    Gender.FEMALE -> "女"
                    else -> "保密"
                })
                AccountInfoItem("注册时间", dateFormat.format(Date(userProfile.createdAt)))
                AccountInfoItem("最后更新", dateFormat.format(Date(userProfile.updatedAt)))
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
private fun AccountInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 隐私设置对话框
 */
@Composable
private fun PrivacySettingsDialog(
    profileVisibility: VisibilityLevel,
    allowSearchByPhone: Boolean,
    allowSearchByEmail: Boolean,
    onProfileVisibilityChange: (VisibilityLevel) -> Unit,
    onAllowSearchByPhoneChange: (Boolean) -> Unit,
    onAllowSearchByEmailChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val visibilityOptions = listOf(
        "公开" to VisibilityLevel.PUBLIC,
        "仅好友" to VisibilityLevel.FRIENDS,
        "私密" to VisibilityLevel.PRIVATE
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私设置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 资料可见性
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = visibilityOptions.find { it.second == profileVisibility }?.first ?: "公开",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("资料可见性") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        visibilityOptions.forEach { (label, value) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onProfileVisibilityChange(value)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // 允许通过手机号搜索
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("允许通过手机号搜索")
                    Switch(
                        checked = allowSearchByPhone,
                        onCheckedChange = onAllowSearchByPhoneChange
                    )
                }
                
                // 允许通过邮箱搜索
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("允许通过邮箱搜索")
                    Switch(
                        checked = allowSearchByEmail,
                        onCheckedChange = onAllowSearchByEmailChange
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
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

/**
 * 通知设置对话框
 */
@Composable
private fun NotificationSettingsDialog(
    pushNotifications: Boolean,
    emailNotifications: Boolean,
    marketingEmails: Boolean,
    onPushNotificationsChange: (Boolean) -> Unit,
    onEmailNotificationsChange: (Boolean) -> Unit,
    onMarketingEmailsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("通知设置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 推送通知
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("推送通知")
                        Text(
                            "接收交易提醒和系统通知",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = pushNotifications,
                        onCheckedChange = onPushNotificationsChange
                    )
                }
                
                HorizontalDivider()
                
                // 邮件通知
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("邮件通知")
                        Text(
                            "接收月度账单和备份提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = emailNotifications,
                        onCheckedChange = onEmailNotificationsChange
                    )
                }
                
                HorizontalDivider()
                
                // 营销邮件
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("营销邮件")
                        Text(
                            "接收产品更新和优惠信息",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = marketingEmails,
                        onCheckedChange = onMarketingEmailsChange
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 帮助与反馈对话框
 */
@Composable
private fun HelpFeedbackDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("帮助与反馈") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("常见问题") },
                    leadingContent = {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        // 打开常见问题页面
                        onDismiss()
                    }
                )
                
                ListItem(
                    headlineContent = { Text("使用教程") },
                    leadingContent = {
                        Icon(Icons.Default.MenuBook, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                    }
                )
                
                ListItem(
                    headlineContent = { Text("意见反馈") },
                    leadingContent = {
                        Icon(Icons.Default.Feedback, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        // 发送邮件反馈
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_SUBJECT, "AI记账 - 意见反馈")
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }
                )
                
                ListItem(
                    headlineContent = { Text("联系我们") },
                    leadingContent = {
                        Icon(Icons.Default.ContactMail, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 关于对话框
 */
@Composable
fun UpdateResultDialog(
    state: com.example.aiaccounting.ui.viewmodel.UpdateDialogState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenReleasePage: (String) -> Unit
) {
    when (state) {
        is com.example.aiaccounting.ui.viewmodel.UpdateDialogState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("当前已是最新版本") },
                text = { Text("当前版本 ${state.currentVersion} 已是最新版本。") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("知道了")
                    }
                }
            )
        }

        is com.example.aiaccounting.ui.viewmodel.UpdateDialogState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("发现新版本 ${state.releaseInfo.versionName}") },
                text = {
                    Column {
                        Text("当前版本：${state.currentVersion}")
                        state.releaseInfo.publishedAt?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("发布时间：$it")
                        }
                        if (state.releaseInfo.body.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.releaseInfo.body)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onOpenReleasePage(state.releaseInfo.htmlUrl) }) {
                        Text("前往下载")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("稍后再说")
                    }
                }
            )
        }

        is com.example.aiaccounting.ui.viewmodel.UpdateDialogState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("暂时无法获取更新") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onRetry) {
                        Text("重试")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}

/**
 * 关于对话框
 */
@Composable
private fun AboutDialog(
    context: Context,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("AI记账")
                Text(
                    "版本 $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("用户协议") },
                    modifier = Modifier.clickable { onDismiss() }
                )
                
                ListItem(
                    headlineContent = { Text("隐私政策") },
                    modifier = Modifier.clickable { onDismiss() }
                )
                
                ListItem(
                    headlineContent = { Text("开源许可") },
                    modifier = Modifier.clickable { onDismiss() }
                )
                
                ListItem(
                    headlineContent = { Text("检查更新") },
                    supportingContent = {
                        if (isCheckingUpdate) {
                            Text("正在检查最新版本…")
                        }
                    },
                    trailingContent = {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    },
                    modifier = Modifier.clickable {
                        if (!isCheckingUpdate) {
                            onCheckUpdate()
                        }
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
