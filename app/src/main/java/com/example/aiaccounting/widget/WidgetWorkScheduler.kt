package com.example.aiaccounting.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Scheduler for widget background update work
 * Manages periodic synchronization between app data and widgets
 */
object WidgetWorkScheduler {

    /**
     * Schedule periodic widget updates
     * Updates every 15 minutes (minimum interval for WorkManager)
     */
    fun scheduleWidgetUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val widgetUpdateWork = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            15, TimeUnit.MINUTES,  // Minimum interval
            5, TimeUnit.MINUTES    // Flex interval
        )
            .setConstraints(constraints)
            .addTag(WidgetUpdateWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            widgetUpdateWork
        )
    }

    /**
     * Cancel scheduled widget updates
     */
    fun cancelWidgetUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdateWorker.WORK_NAME)
    }

    /**
     * Check if widget updates are scheduled
     */
    fun isScheduled(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(WidgetUpdateWorker.WORK_NAME)
        return workInfos.get()?.any { !it.state.isFinished } == true
    }
}
