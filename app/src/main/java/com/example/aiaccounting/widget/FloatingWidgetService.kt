package com.example.aiaccounting.widget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.example.aiaccounting.R
import com.example.aiaccounting.data.local.entity.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * 悬浮窗服务
 * 在桌面上显示记账对话框，不跳转Activity
 */
class FloatingWidgetService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val transactionType = intent?.getStringExtra("transaction_type") ?: "expense"

        // 检查悬浮窗权限
        if (!canDrawOverlays()) {
            // 请求权限
            val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            permissionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(permissionIntent)
            stopSelf()
            return START_NOT_STICKY
        }

        // 显示悬浮窗
        showFloatingWindow(transactionType)

        return START_NOT_STICKY
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun showFloatingWindow(transactionType: String) {
        // 如果已经有悬浮窗，先移除
        floatingView?.let {
            windowManager?.removeView(it)
        }

        // 加载布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_accounting_dialog, null)

        // 设置窗口参数
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.6f
        }

        // 设置视图
        setupViews(floatingView!!, transactionType)

        // 添加悬浮窗
        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupViews(view: View, initialType: String) {
        // 关闭按钮
        view.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
            closeFloatingWindow()
        }

        // 类型选择
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_type)
        val radioExpense = view.findViewById<RadioButton>(R.id.radio_expense)
        val radioIncome = view.findViewById<RadioButton>(R.id.radio_income)

        // 设置初始类型
        if (initialType == "expense") {
            radioExpense?.isChecked = true
        } else {
            radioIncome?.isChecked = true
        }

        // 金额输入
        val editAmount = view.findViewById<EditText>(R.id.edit_amount)

        // 备注输入
        val editNote = view.findViewById<EditText>(R.id.edit_note)

        // 确认按钮
        view.findViewById<Button>(R.id.btn_confirm)?.setOnClickListener {
            val amountStr = editAmount?.text?.toString() ?: ""
            val amount = amountStr.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                Toast.makeText(this, "请输入有效金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val note = editNote?.text?.toString() ?: ""
            val type = if (radioExpense?.isChecked == true) {
                TransactionType.EXPENSE
            } else {
                TransactionType.INCOME
            }

            saveTransaction(amount, note, type)
        }

        // 点击背景关闭
        view.findViewById<LinearLayout>(R.id.container_background)?.setOnClickListener {
            closeFloatingWindow()
        }

        // 防止点击内容区域关闭
        view.findViewById<LinearLayout>(R.id.container_content)?.setOnClickListener {
            // 不执行任何操作，只是拦截点击事件
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

            // 更新所有小组件
            updateAllWidgets()

            Toast.makeText(this, "记账成功", Toast.LENGTH_SHORT).show()
            closeFloatingWindow()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "记账失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAllWidgets() {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        val providers = arrayOf(
            WidgetProvider1x1::class.java,
            WidgetProvider2x1::class.java,
            WidgetProvider3x1::class.java,
            WidgetProvider3x2::class.java,
            WidgetProvider4x3::class.java
        )

        providers.forEach { providerClass ->
            val componentName = android.content.ComponentName(this, providerClass)
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

    private fun closeFloatingWindow() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            windowManager?.removeView(it)
        }
    }
}
