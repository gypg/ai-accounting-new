package com.example.aiaccounting.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.widget.Toast

/**
 * 小组件点击广播接收器
 * 用于启动悬浮窗服务
 */
class WidgetClickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val transactionType = intent.getStringExtra("transaction_type") ?: "expense"

        // 检查悬浮窗权限
        if (!canDrawOverlays(context)) {
            Toast.makeText(context, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show()
            // 打开设置页面
            val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(permissionIntent)
            return
        }

        // 启动悬浮窗服务
        val serviceIntent = Intent(context, FloatingWidgetService::class.java).apply {
            putExtra("transaction_type", transactionType)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "启动记账窗口失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}
