package com.example.aiaccounting.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

class AutoBackupManager @Inject constructor(
    private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val PREFS_NAME = "auto_backup_prefs"
    private val KEY_ENABLED = "auto_backup_enabled"
    private val KEY_FREQUENCY = "auto_backup_frequency"
    private val KEY_LAST_BACKUP = "last_backup_time"

    companion object {
        const val FREQUENCY_DAILY = 0
        const val FREQUENCY_WEEKLY = 1
        const val FREQUENCY_MONTHLY = 2
        const val ACTION_AUTO_BACKUP = "com.example.aiaccounting.ACTION_SCHEDULED_BACKUP"
    }

    fun isAutoBackupEnabled(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

        if (enabled) {
            scheduleNextBackup()
        } else {
            cancelBackup()
        }
    }

    fun getBackupFrequency(): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FREQUENCY, FREQUENCY_DAILY)
    }

    fun setBackupFrequency(frequency: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FREQUENCY, frequency).apply()

        if (isAutoBackupEnabled()) {
            scheduleNextBackup()
        }
    }

    fun scheduleNextBackup() {
        val frequency = getBackupFrequency()
        val nextBackupTime = calculateNextBackupTime(frequency)

        val intent = Intent(context, BackupReceiver::class.java).apply {
            action = ACTION_AUTO_BACKUP
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextBackupTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextBackupTime,
                pendingIntent
            )
        }
    }

    private fun calculateNextBackupTime(frequency: Int): Long {
        val calendar = Calendar.getInstance()

        when (frequency) {
            FREQUENCY_DAILY -> {
                // 每天凌晨2点
                calendar.set(Calendar.HOUR_OF_DAY, 2)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)

                // 如果今天的时间已过，设置为明天
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            FREQUENCY_WEEKLY -> {
                // 每周日凌晨2点
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 2)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)

                // 如果本周的时间已过，设置为下周
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            FREQUENCY_MONTHLY -> {
                // 每月1日凌晨2点
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 2)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)

                // 如果本月的时间已过，设置为下月
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.MONTH, 1)
                }
            }
        }

        return calendar.timeInMillis
    }

    private fun cancelBackup() {
        val intent = Intent(context, BackupReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun getLastBackupTime(): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP, 0)
    }

    fun updateLastBackupTime() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
    }

    class BackupReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_AUTO_BACKUP) {
                // 启动备份服务
                BackupService.startAutoBackup(context)

                // 重新调度下一次备份
                val manager = AutoBackupManager(context)
                manager.updateLastBackupTime()
                manager.scheduleNextBackup()
            }
        }
    }
}
