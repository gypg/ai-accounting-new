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
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 3x1 时间收支小组件
 */
class WidgetProvider3x1 : AppWidgetProvider() {

    companion object {
        const val ACTION_CLICK = "com.example.aiaccounting.widget.ACTION_CLICK_3x1"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WidgetProvider3x1::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_accounting_3x1)

            // 更新时间
            val now = LocalDateTime.now()
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
            val dateFormatter = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)

            views.setTextViewText(R.id.tv_time, now.format(timeFormatter))
            views.setTextViewText(R.id.tv_date, now.format(dateFormatter))

            // 加载财务数据
            GlobalScope.launch {
                try {
                    val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
                    val income = prefs.getFloat("month_income", 0f)
                    val expense = prefs.getFloat("month_expense", 0f)

                    views.setTextViewText(R.id.tv_income, "¥${String.format("%.0f", income)}")
                    views.setTextViewText(R.id.tv_expense, "¥${String.format("%.0f", expense)}")

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
