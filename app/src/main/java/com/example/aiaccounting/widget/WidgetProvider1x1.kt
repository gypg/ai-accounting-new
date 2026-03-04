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

/**
 * 1x1 余额小组件
 */
class WidgetProvider1x1 : AppWidgetProvider() {
    companion object {
        const val ACTION_CLICK = "com.example.aiaccounting.widget.ACTION_CLICK_1x1"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WidgetProvider1x1::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_accounting_1x1)

            // 加载结余数据
            GlobalScope.launch {
                try {
                    val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
                    val income = prefs.getFloat("month_income", 0f)
                    val expense = prefs.getFloat("month_expense", 0f)
                    val balance = income - expense

                    views.setTextViewText(R.id.tv_balance, "¥${String.format("%.0f", balance)}")

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 点击发送广播
            val intent = Intent(context, WidgetClickReceiver::class.java).apply {
                action = ACTION_CLICK
                putExtra("transaction_type", "expense")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
