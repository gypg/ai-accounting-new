package com.example.aiaccounting.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.aiaccounting.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 4x3 完整信息小组件（带图表）
 */
class WidgetProvider4x3 : AppWidgetProvider() {

    companion object {
        const val ACTION_EXPENSE = "com.example.aiaccounting.widget.ACTION_EXPENSE_4x3"
        const val ACTION_INCOME = "com.example.aiaccounting.widget.ACTION_INCOME_4x3"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WidgetProvider4x3::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_accounting_4x3)
            
            Log.d("WidgetProvider4x3", "开始更新小组件: appWidgetId=$appWidgetId")

            // 更新时间
            val now = LocalDateTime.now()
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
            views.setTextViewText(R.id.tv_refresh_time, "${now.format(timeFormatter)}更新")

            // 加载数据 - 同步加载
            try {
                val prefs = context.getSharedPreferences("widget_stats", Context.MODE_PRIVATE)
                val income = prefs.getFloat("month_income", 0f)
                val expense = prefs.getFloat("month_expense", 0f)
                val budget = prefs.getFloat("month_budget", 5000f)
                val balance = income - expense

                views.setTextViewText(R.id.tv_income, "¥${String.format("%.2f", income)}")
                views.setTextViewText(R.id.tv_expense, "¥${String.format("%.2f", expense)}")
                views.setTextViewText(R.id.tv_balance, "¥${String.format("%.2f", balance)}")

                // 更新预算进度
                val progress = if (budget > 0) {
                    ((expense / budget) * 100).toInt().coerceIn(0, 100)
                } else 0
                views.setProgressBar(R.id.progress_budget, 100, progress, false)
                views.setTextViewText(R.id.tv_budget_percent, "$progress%")

                // 加载趋势图
                val trendChartBase64 = prefs.getString("trend_chart", null)
                Log.d("WidgetProvider4x3", "趋势图Base64: ${trendChartBase64 != null}")
                
                if (trendChartBase64 != null) {
                    val bitmap = WidgetChartGenerator.base64ToBitmap(trendChartBase64)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.chart_trend, bitmap)
                        views.setViewVisibility(R.id.chart_trend, View.VISIBLE)
                        Log.d("WidgetProvider4x3", "趋势图加载成功")
                    } else {
                        Log.w("WidgetProvider4x3", "趋势图解码失败")
                        views.setViewVisibility(R.id.chart_trend, View.GONE)
                    }
                } else {
                    Log.w("WidgetProvider4x3", "趋势图数据为空")
                    views.setViewVisibility(R.id.chart_trend, View.GONE)
                }

                // 加载饼图
                val pieChartBase64 = prefs.getString("pie_chart", null)
                Log.d("WidgetProvider4x3", "饼图Base64: ${pieChartBase64 != null}")
                
                if (pieChartBase64 != null) {
                    val bitmap = WidgetChartGenerator.base64ToBitmap(pieChartBase64)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.chart_pie, bitmap)
                        views.setViewVisibility(R.id.chart_pie, View.VISIBLE)
                        Log.d("WidgetProvider4x3", "饼图加载成功")
                    } else {
                        Log.w("WidgetProvider4x3", "饼图解码失败")
                        views.setViewVisibility(R.id.chart_pie, View.GONE)
                    }
                } else {
                    Log.w("WidgetProvider4x3", "饼图数据为空")
                    views.setViewVisibility(R.id.chart_pie, View.GONE)
                }

            } catch (e: Exception) {
                Log.e("WidgetProvider4x3", "更新小组件失败: ${e.message}", e)
            }

            // 记支出按钮 - 发送广播
            val expenseIntent = Intent(context, WidgetClickReceiver::class.java).apply {
                action = ACTION_EXPENSE
                putExtra("transaction_type", "expense")
            }
            val expensePendingIntent = PendingIntent.getBroadcast(
                context, 3, expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_expense, expensePendingIntent)

            // 记收入按钮 - 发送广播
            val incomeIntent = Intent(context, WidgetClickReceiver::class.java).apply {
                action = ACTION_INCOME
                putExtra("transaction_type", "income")
            }
            val incomePendingIntent = PendingIntent.getBroadcast(
                context, 4, incomeIntent,
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
