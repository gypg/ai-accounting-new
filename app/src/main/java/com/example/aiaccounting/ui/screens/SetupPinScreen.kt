package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.ui.viewmodel.PinViewModel

@Composable
fun SetupPinScreen(
    onSetupComplete: (String) -> Unit,
    viewModel: PinViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "设置PIN码",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "请设置4位数字PIN码用于保护您的数据",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // PIN输入
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    pin = it
                    errorMessage = null
                }
            },
            label = { Text("输入PIN码") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 确认PIN
        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    confirmPin = it
                    errorMessage = null
                }
            },
            label = { Text("确认PIN码") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 错误提示
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 确认按钮
        Button(
            onClick = {
                when {
                    pin.length != 4 -> errorMessage = "PIN码必须是4位数字"
                    pin != confirmPin -> errorMessage = "两次输入的PIN码不一致"
                    else -> {
                        viewModel.setupPin(pin)
                        onSetupComplete(pin)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length == 4 && confirmPin.length == 4
        ) {
            Text("确认")
        }
    }
}
