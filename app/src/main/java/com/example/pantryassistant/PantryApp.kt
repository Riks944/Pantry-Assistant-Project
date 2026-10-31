package com.example.pantryassistant

import android.app.Application
import com.example.pantryassistant.data.PantryDatabase
import com.example.pantryassistant.data.PantryRepository
import com.example.pantryassistant.worker.LowStockWorker
import com.example.pantryassistant.worker.NotificationHelper

/** Simple manual dependency container. */
class PantryApp : Application() {
    val repository: PantryRepository by lazy {
        PantryRepository(PantryDatabase.getInstance(this))
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        LowStockWorker.schedulePeriodic(this)
    }
}
