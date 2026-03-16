package com.example.aiaccounting.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiaccounting.MainActivity
import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 快速记账弹窗 Activity
 * 使用 SharedPreferences 存储待同步的交易数据
 */
@AndroidEntryPoint
class WidgetQuickAddActivity : ComponentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var aiOperationExecutor: AIOperationExecutor

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var widgetUpdateService: WidgetUpdateService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏透明
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val transactionType = intent.getStringExtra("transaction_type") ?: "expense"

        if (!securityManager.hasValidAuthSession()) {
            Toast.makeText(this, "请先解锁应用后再记账", Toast.LENGTH_SHORT).show()
            val action = if (transactionType == "expense") "add_expense" else "add_income"
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("action", action)
            })
            finish()
            return
        }

        setContent {
            MaterialTheme {
                // 全屏玻璃背景
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    QuickAddDialogGlass(
                        transactionType = transactionType,
                        onDismiss = { finish() },
                        onSave = { amount, note, type ->
                            saveTransaction(amount, note, type)
                        }
                    )
                }
            }
        }
    }

    private fun saveTransaction(amount: Double, note: String, type: TransactionType) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!securityManager.hasValidAuthSession()) {
                    Toast.makeText(this@WidgetQuickAddActivity, "请先解锁应用后再记账", Toast.LENGTH_SHORT).show()
                    val action = if (type == TransactionType.EXPENSE) "add_expense" else "add_income"
                    startActivity(Intent(this@WidgetQuickAddActivity, MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("action", action)
                    })
                    finish()
                    return@launch
                }

                val accountId = resolveAccountId()
                if (accountId == null) {
                    Toast.makeText(this@WidgetQuickAddActivity, "请先在应用内创建账户", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val categoryId = resolveCategoryId(type)
                if (categoryId == null) {
                    Toast.makeText(this@WidgetQuickAddActivity, "请先在应用内创建分类", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val operation = AIOperation.AddTransaction(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    date = System.currentTimeMillis(),
                    note = note
                )

                when (val result = aiOperationExecutor.executeOperation(operation)) {
                    is AIOperationExecutor.AIOperationResult.Success -> {
                        widgetUpdateService.updateWidgetStats(this@WidgetQuickAddActivity)
                        Toast.makeText(this@WidgetQuickAddActivity, "记账成功", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is AIOperationExecutor.AIOperationResult.Error -> {
                        Toast.makeText(this@WidgetQuickAddActivity, "记账失败: ${result.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@WidgetQuickAddActivity, "记账失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun resolveAccountId(): Long? {
        val accounts = accountRepository.getAllAccountsList()
        return accounts.firstOrNull { !it.isArchived && it.isDefault }?.id
            ?: accounts.firstOrNull { !it.isArchived }?.id
    }

    private suspend fun resolveCategoryId(type: TransactionType): Long? {
        val categories = categoryRepository.getAllCategoriesList()
        val existing = categories.firstOrNull { it.type == type }
        if (existing != null) return existing.id

        val fallbackName = when (type) {
            TransactionType.INCOME -> "其他收入"
            TransactionType.EXPENSE -> "其他支出"
            TransactionType.TRANSFER -> "转账"
        }

        return when (val result = aiOperationExecutor.executeOperation(
            AIOperation.AddCategory(name = fallbackName, type = type)
        )) {
            is AIOperationExecutor.AIOperationResult.Success -> {
                val refreshed = categoryRepository.getAllCategoriesList()
                refreshed.firstOrNull { it.type == type && it.name == fallbackName }?.id
                    ?: refreshed.firstOrNull { it.type == type }?.id
            }
            is AIOperationExecutor.AIOperationResult.Error -> null
        }
    }

    @Deprecated("Widget stats should be updated via WidgetUpdateService; kept temporarily for compatibility")
    private fun updateAllWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val providers = arrayOf(
            WidgetProvider1x1::class.java,
            WidgetProvider2x1::class.java,
            WidgetProvider3x1::class.java,
            WidgetProvider3x2::class.java,
            WidgetProvider4x3::class.java
        )

        providers.forEach { providerClass ->
            val componentName = ComponentName(this, providerClass)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.forEach { appWidgetId ->
                when (providerClass) {
                    WidgetProvider1x1::class.java ->
                        WidgetProvider1x1.updateAppWidget(this, appWidgetManager, appWidgetId)
                    WidgetProvider2x1::class.java ->
                        WidgetProvider2x1.updateAppWidget(this, appWidgetManager, appWidgetId)
                    WidgetProvider3x1::class.java ->
                        WidgetProvider3x1.updateAppWidget(this, appWidgetManager, appWidgetId)
                    WidgetProvider3x2::class.java ->
                        WidgetProvider3x2.updateAppWidget(this, appWidgetManager, appWidgetId)
                    WidgetProvider4x3::class.java ->
                        WidgetProvider4x3.updateAppWidget(this, appWidgetManager, appWidgetId)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDialogGlass(
    transactionType: String,
    onDismiss: () -> Unit,
    onSave: (Double, String, TransactionType) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(if (transactionType == "expense") TransactionType.EXPENSE else TransactionType.INCOME) }
    var showError by remember { mutableStateOf(false) }

    val expenseGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF5252), Color(0xFFFF4081))
    )
    val incomeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF69F0AE), Color(0xFF00E676))
    )

    // 玻璃效果卡片
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D44).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "快速记账",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 类型选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TypeButtonGlass(
                    text = "支出",
                    isSelected = selectedType == TransactionType.EXPENSE,
                    gradient = expenseGradient,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    modifier = Modifier.weight(1f)
                )
                TypeButtonGlass(
                    text = "收入",
                    isSelected = selectedType == TransactionType.INCOME,
                    gradient = incomeGradient,
                    onClick = { selectedType = TransactionType.INCOME },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 金额输入
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    showError = false
                },
                label = { Text("金额", color = Color.White.copy(alpha = 0.6f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedType == TransactionType.EXPENSE) Color(0xFFFF5252) else Color(0xFF69F0AE),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = if (selectedType == TransactionType.EXPENSE) Color(0xFFFF5252) else Color(0xFF69F0AE),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                prefix = { Text("¥", fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f)) }
            )

            if (showError) {
                Text(
                    text = "请输入有效金额",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 备注输入
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）", color = Color.White.copy(alpha = 0.6f)) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 保存按钮
            val saveGradient = if (selectedType == TransactionType.EXPENSE) expenseGradient else incomeGradient
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0) {
                        showError = true
                    } else {
                        onSave(amountValue, note, selectedType)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(saveGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "确认记账",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TypeButtonGlass(
    text: String,
    isSelected: Boolean,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundModifier = if (isSelected) {
        modifier.background(gradient)
    } else {
        modifier.background(Color.White.copy(alpha = 0.1f))
    }

    Box(
        modifier = backgroundModifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = Color.White
        )
    }
}
