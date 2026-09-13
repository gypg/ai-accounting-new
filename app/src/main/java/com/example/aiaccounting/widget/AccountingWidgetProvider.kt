package com.example.aiaccounting.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.example.aiaccounting.MainActivity
import com.example.aiaccounting.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * 记账桌面小组件
 * 根据大小动态显示不同内容
 */
class AccountingWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_EXPENSE = "com.example.aiaccounting.widget.ADD_EXPENSE"
        const val ACTION_ADD_INCOME = "com.example.aiaccounting.widget.ADD_INCOME"
        const val ACTION_AI_CHAT = "com.example.aiaccounting.widget.AI_CHAT"
        const val ACTION_REFRESH = "com.example.aiaccounting.widget.REFRESH"

        /**
         * 手动更新所有小组件
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AccountingWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        /**
         * 更新单个小组件
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // 获取小组件当前尺寸
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            // 根据尺寸选择布局
            val layoutId = when {
                // 小尺寸 (2x1): 只显示快捷按钮
                width < 200 && height < 100 -> R.layout.widget_accounting_small
                // 中等尺寸: 显示收支信息
                width < 300 || height < 200 -> R.layout.widget_accounting_medium
                // 大尺寸: 显示完整信息
                else -> R.layout.widget_accounting
            }

            val views = RemoteViews(context.packageName, layoutId)

            // 设置点击事件
            setupClickEvents(context, views, layoutId)

            // 设置月份标题
            val monthTitle = "${Calendar.getInstance().get(Calendar.MONTH) + 1}月收支"
            if (layoutId != R.layout.widget_accounting_small) {
                views.setTextViewText(R.id.tv_month_title, monthTitle)
            }

            // 加载数据
            loadWidgetData(context, views, appWidgetManager, appWidgetId, layoutId)
        }

        /**
         * 设置点击事件
         */
        private fun setupClickEvents(context: Context, views: RemoteViews, layoutId: Int) {
            // 记支出按钮
            val addExpenseIntent = Intent(context, AccountingWidgetProvider::class.java).apply {
                action = ACTION_ADD_EXPENSE
            }
            val addExpensePendingIntent = PendingIntent.getBroadcast(
                context, 1, addExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_add_expense, addExpensePendingIntent)

            // 记收入按钮
            val addIncomeIntent = Intent(context, AccountingWidgetProvider::class.java).apply {
                action = ACTION_ADD_INCOME
            }
            val addIncomePendingIntent = PendingIntent.getBroadcast(
                context, 2, addIncomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_add_income, addIncomePendingIntent)

            // AI聊天按钮
            val aiChatIntent = Intent(context, AccountingWidgetProvider::class.java).apply {
                action = ACTION_AI_CHAT
            }
            val aiChatPendingIntent = PendingIntent.getBroadcast(
                context, 3, aiChatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_ai_chat, aiChatPendingIntent)

            // 刷新按钮（仅大尺寸和中等尺寸）
            if (layoutId != R.layout.widget_accounting_small) {
                val refreshIntent = Intent(context, AccountingWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, 4, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            }
        }

        /**
         * 加载小组件数据
         */
        private fun loadWidgetData(
            context: Context,
            views: RemoteViews,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            layoutId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
                    val totalIncome = prefs.getFloat("month_income", 0f).toDouble()
                    val totalExpense = prefs.getFloat("month_expense", 0f).toDouble()
                    val balance = totalIncome - totalExpense
                    val budgetLimit = prefs.getFloat("month_budget_limit", 0f).toDouble()

                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

                    withContext(Dispatchers.Main) {
                        // 根据布局更新不同的视图
                        when (layoutId) {
                            R.layout.widget_accounting_small -> {
                                // 小尺寸不需要更新数据，只显示按钮
                            }
                            R.layout.widget_accounting_medium -> {
                                views.setTextViewText(R.id.tv_month_income, currencyFormat.format(totalIncome))
                                views.setTextViewText(R.id.tv_month_expense, currencyFormat.format(totalExpense))
                                views.setTextViewText(R.id.tv_month_balance, currencyFormat.format(balance))
                            }
                            R.layout.widget_accounting -> {
                                views.setTextViewText(R.id.tv_month_income, currencyFormat.format(totalIncome))
                                views.setTextViewText(R.id.tv_month_expense, currencyFormat.format(totalExpense))
                                views.setTextViewText(R.id.tv_month_balance, currencyFormat.format(balance))
                                views.setTextViewText(R.id.tv_budget_info, "${currencyFormat.format(totalExpense)} / ${currencyFormat.format(budgetLimit)}")
                                val budgetPercent = if (budgetLimit > 0) {
                                    ((totalExpense / budgetLimit) * 100).toInt().coerceAtMost(100)
                                } else 0
                                views.setProgressBar(R.id.progress_budget, 100, budgetPercent, false)
                            }
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        // 当小组件大小改变时重新更新
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_ADD_EXPENSE -> {
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("action", "add_expense")
                }
                context.startActivity(openAppIntent)
            }
            ACTION_ADD_INCOME -> {
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("action", "add_income")
                }
                context.startActivity(openAppIntent)
            }
            ACTION_AI_CHAT -> {
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("action", "ai_chat")
                }
                context.startActivity(openAppIntent)
            }
            ACTION_REFRESH -> {
                updateAllWidgets(context)
            }
        }
    }
}
