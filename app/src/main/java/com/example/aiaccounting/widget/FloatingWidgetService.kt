package com.example.aiaccounting.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.aiaccounting.MainActivity
import com.example.aiaccounting.R
import com.example.aiaccounting.ai.AIOperation
import com.example.aiaccounting.ai.AIOperationExecutor
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.data.repository.AccountRepository
import com.example.aiaccounting.data.repository.CategoryRepository
import com.example.aiaccounting.security.SecurityManager
import com.example.aiaccounting.widget.WidgetUpdateService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 悬浮窗服务
 * 在桌面上显示记账对话框，不跳转Activity
 */
@AndroidEntryPoint
class FloatingWidgetService : Service() {

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

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    
    // 协程作用域用于管理异步任务
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "floating_widget_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于显示记账悬浮窗"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI记账悬浮窗")
            .setContentText("点击返回应用")
            .setSmallIcon(R.drawable.ic_add)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 8+ 需要启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification())
        }

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

        // 必须先完成身份验证，避免小组件/悬浮窗绕过 PIN
        if (!securityManager.hasValidAuthSession()) {
            Toast.makeText(this, "请先解锁应用后再记账", Toast.LENGTH_SHORT).show()
            openAppForAuth(transactionType)
            stopSelf()
            return START_NOT_STICKY
        }

        // 显示悬浮窗
        showFloatingWindow(transactionType)

        return START_NOT_STICKY
    }

    private fun openAppForAuth(transactionType: String) {
        val action = if (transactionType == "expense") "add_expense" else "add_income"
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("action", action)
        }
        startActivity(openAppIntent)
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
        serviceScope.launch {
            try {
                if (!securityManager.hasValidAuthSession()) {
                    Toast.makeText(this@FloatingWidgetService, "请先解锁应用后再记账", Toast.LENGTH_SHORT).show()
                    openAppForAuth(if (type == TransactionType.EXPENSE) "expense" else "income")
                    closeFloatingWindow()
                    return@launch
                }

                val accountId = resolveAccountId()
                if (accountId == null) {
                    Toast.makeText(this@FloatingWidgetService, "请先在应用内创建账户", Toast.LENGTH_SHORT).show()
                    closeFloatingWindow()
                    return@launch
                }

                val categoryId = resolveCategoryId(type)
                if (categoryId == null) {
                    Toast.makeText(this@FloatingWidgetService, "请先在应用内创建分类", Toast.LENGTH_SHORT).show()
                    closeFloatingWindow()
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
                        widgetUpdateService.updateWidgetStats(this@FloatingWidgetService)
                        Toast.makeText(this@FloatingWidgetService, "记账成功", Toast.LENGTH_SHORT).show()
                        closeFloatingWindow()
                    }
                    is AIOperationExecutor.AIOperationResult.Error -> {
                        Toast.makeText(this@FloatingWidgetService, "记账失败: ${result.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@FloatingWidgetService, "记账失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            // 忽略视图已移除的异常
            android.util.Log.w("FloatingWidget", "Error removing view: ${e.message}")
        } finally {
            floatingView = null
            windowManager = null
            params = null
        }
        
        // 取消所有协程
        serviceScope.cancel()
    }
}
