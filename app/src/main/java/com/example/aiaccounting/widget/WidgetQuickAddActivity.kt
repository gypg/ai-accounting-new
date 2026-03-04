package com.example.aiaccounting.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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
import com.example.aiaccounting.data.local.entity.TransactionType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * 快速记账弹窗 Activity
 * 使用 SharedPreferences 存储待同步的交易数据
 */
class WidgetQuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏透明
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val transactionType = intent.getStringExtra("transaction_type") ?: "expense"

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
        try {
            // 创建交易记录 JSON
            val transaction = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("accountId", 1L)
                put("categoryId", if (type == TransactionType.EXPENSE) 1L else 2L)
                put("type", type.name)
                put("amount", amount)
                put("date", LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                put("note", note)
                put("timestamp", System.currentTimeMillis())
            }

            // 保存到 SharedPreferences
            val prefs = getSharedPreferences("widget_pending_transactions", Context.MODE_PRIVATE)
            val existingData = prefs.getString("pending", "[]")
            val jsonArray = JSONArray(existingData)
            jsonArray.put(transaction)

            prefs.edit().putString("pending", jsonArray.toString()).apply()

            // 同时更新统计数据
            val statsPrefs = getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
            val editor = statsPrefs.edit()
            when (type) {
                TransactionType.EXPENSE -> {
                    val currentExpense = statsPrefs.getFloat("month_expense", 0f)
                    editor.putFloat("month_expense", (currentExpense + amount).toFloat())
                }
                TransactionType.INCOME -> {
                    val currentIncome = statsPrefs.getFloat("month_income", 0f)
                    editor.putFloat("month_income", (currentIncome + amount).toFloat())
                }
                else -> {}
            }
            editor.apply()

            runOnUiThread {
                Toast.makeText(this, "记账成功", Toast.LENGTH_SHORT).show()
                updateAllWidgets()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "记账失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
