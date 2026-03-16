package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.ui.components.ButlerSelectionDialog
import com.example.aiaccounting.ui.components.CurrentButlerInfo
import com.example.aiaccounting.ui.viewmodel.AIAssistantViewModel

/**
 * 管家设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButlerSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMarket: () -> Unit = {},
    viewModel: AIAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showButlerSelectionDialog by remember { mutableStateOf(false) }
    
    // 获取当前管家
    val currentButler = uiState.currentButler
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "AI管家",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(16.dp)
        ) {
            // 当前管家信息
            Text(
                text = "当前管家",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (currentButler != null) {
                CurrentButlerInfo(
                    butler = currentButler,
                    onClick = { showButlerSelectionDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 管家说明
            Text(
                text = "关于管家",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI管家是您的专属财务助手",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "每位管家都有独特的性格和说话风格，可以帮助您：",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val features = listOf(
                        "智能记账 - 识别语音和文字中的消费信息",
                        "账户管理 - 创建和管理多个资金账户",
                        "预算提醒 - 监控支出，避免超支",
                        "财务分析 - 分析消费习惯，给出建议"
                    )
                    
                    features.forEach { feature ->
                        Text(
                            text = "• $feature",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 切换管家按钮
            Button(
                onClick = { showButlerSelectionDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("切换管家")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 管家市场入口
            OutlinedButton(
                onClick = onNavigateToMarket,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("管家市场")
            }
        }
    }
    
    // 管家选择弹窗
    if (showButlerSelectionDialog) {
        val allButlers = viewModel.getAllButlers()
        
        ButlerSelectionDialog(
            butlers = allButlers,
            selectedButlerId = currentButler?.id ?: "",
            onButlerSelected = { selectedButler: Butler ->
                viewModel.switchButler(selectedButler.id)
                showButlerSelectionDialog = false
            },
            onDismiss = { showButlerSelectionDialog = false }
        )
    }
}
