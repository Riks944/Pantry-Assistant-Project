package com.example.pantryassistant.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.pantryassistant.PantryApp
import java.util.concurrent.TimeUnit

/**
 * Background check: reads the low-stock items from Room and, if any are found,
 * posts a notification that opens the shopping list.
 */
class LowStockWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as PantryApp).repository
        val lowItems = repository.getLowStockItems()
        if (lowItems.isNotEmpty()) {
            NotificationHelper.showLowStockNotification(applicationContext, lowItems.map { it.name })
        }
        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "low_stock_periodic_check"

        /** Runs the check twice a day. KEEP avoids rescheduling on every launch. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<LowStockWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Runs the check right away (used by the "Check now" button for demos). */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<LowStockWorker>().build())
        }
    }
}
