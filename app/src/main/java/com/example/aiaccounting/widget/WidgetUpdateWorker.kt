package com.example.aiaccounting.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aiaccounting.data.local.dao.TransactionDao
import com.example.aiaccounting.data.local.entity.Transaction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * WorkManager Worker for periodic widget updates
 * Ensures widget data stays synchronized with app data
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get all transactions for current month
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            val startOfNextMonth = calendar.timeInMillis

            // Fetch transactions
            val transactions = transactionDao.getTransactionsByDateRange(
                startOfMonth,
                startOfNextMonth
            ).first()

            // Update widget data
            WidgetDataSyncHelper.recalculateAndUpdate(applicationContext, transactions)

            // Update all widget providers
            updateAllWidgets()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun updateAllWidgets() {
        // Trigger update for all widget sizes
        WidgetProvider1x1.updateAllWidgets(applicationContext)
        WidgetProvider2x1.updateAllWidgets(applicationContext)
        WidgetProvider3x1.updateAllWidgets(applicationContext)
        WidgetProvider3x2.updateAllWidgets(applicationContext)
        WidgetProvider4x3.updateAllWidgets(applicationContext)
    }

    companion object {
        const val WORK_NAME = "widget_update_work"
    }
}
