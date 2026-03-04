package com.example.aiaccounting.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.aiaccounting.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * 3x2 收支详情小组件（带预算进度条）
 */
class WidgetProvider3x2 : AppWidgetProvider() {

    companion object {
        const val ACTION_EXPENSE = "com.example.aiaccounting.widget.ACTION_EXPENSE"
        const val ACTION_INCOME = "com.example.aiaccounting.widget.ACTION_INCOME"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WidgetProvider3x2::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_accounting_3x2)

            // 更新月份标题
            val month = LocalDateTime.now().monthValue
            views.setTextViewText(R.id.tv_title, "${month}月收支")

            // 加载数据
            GlobalScope.launch {
                try {
                    val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
                    val income = prefs.getFloat("month_income", 0f)
                    val expense = prefs.getFloat("month_expense", 0f)
                    val budget = prefs.getFloat("month_budget", 5000f)
                    val balance = income - expense

                    views.setTextViewText(R.id.tv_income, "¥${String.format("%.0f", income)}")
                    views.setTextViewText(R.id.tv_expense, "¥${String.format("%.0f", expense)}")
                    views.setTextViewText(R.id.tv_balance, "¥${String.format("%.0f", balance)}")

                    // 更新预算进度
                    val progress = if (budget > 0) {
                        ((expense / budget) * 100).toInt().coerceIn(0, 100)
                    } else 0
                    views.setProgressBar(R.id.progress_budget, 100, progress, false)
                    views.setTextViewText(R.id.tv_budget_info, "¥${String.format("%.0f", expense)} / ¥${String.format("%.0f", budget)}")

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 记支出按钮 - 发送广播
            val expenseIntent = Intent(context, WidgetClickReceiver::class.java).apply {
                action = ACTION_EXPENSE
                putExtra("transaction_type", "expense")
            }
            val expensePendingIntent = PendingIntent.getBroadcast(
                context, 1, expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_expense, expensePendingIntent)

            // 记收入按钮 - 发送广播
            val incomeIntent = Intent(context, WidgetClickReceiver::class.java).apply {
                action = ACTION_INCOME
                putExtra("transaction_type", "income")
            }
            val incomePendingIntent = PendingIntent.getBroadcast(
                context, 2, incomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_income, incomePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
