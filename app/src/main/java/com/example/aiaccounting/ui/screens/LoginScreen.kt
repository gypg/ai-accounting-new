package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.PinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: PinViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    val isAuthenticated by viewModel.currentPin.collectAsState()

    // 监听验证结果
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated != null) {
            onLoginSuccess(pin)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo和标题
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Logo图标
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 应用名称
                Text(
                    text = "AI记账",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 英文标语
                Text(
                    text = "Where flow goes, life shows",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 中文标语
                Text(
                    text = "流水知去处，岁月自成诗",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }

            // 登录卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 欢迎标题
                    Text(
                        text = "欢迎回来",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "请输入PIN码登录",
                        fontSize = 14.sp,
                        color = Color(0xFF888888)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // PIN码输入框
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                pin = it
                                errorMessage = null
                            }
                        },
                        label = { Text("PIN码") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            focusedLabelColor = Color(0xFF2196F3)
                        )
                    )

                    // 错误提示
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 忘记密码
                    TextButton(
                        onClick = { showForgotPasswordDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "忘记密码?",
                            color = Color(0xFF2196F3),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 登录按钮
                    Button(
                        onClick = {
                            if (pin.length >= 4) {
                                isLoading = true
                                viewModel.validatePin(pin) { success ->
                                    isLoading = false
                                    if (!success) {
                                        errorMessage = "PIN码错误"
                                    }
                                }
                            } else {
                                errorMessage = "请输入至少4位PIN码"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = pin.length >= 4 && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "登录",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // 忘记密码对话框
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("忘记密码") },
            text = {
                Column {
                    Text("由于数据安全考虑，忘记密码将导致：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 所有本地数据将被清除")
                    Text("• 需要重新设置PIN码")
                    Text("• 需要重新初始化账户和分类")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("请确保您已备份重要数据！", color = Color(0xFFF44336))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        onForgotPassword()
                    }
                ) {
                    Text("确认重置", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
