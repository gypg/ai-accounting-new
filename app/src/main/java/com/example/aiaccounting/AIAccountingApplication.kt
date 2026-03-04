package com.example.aiaccounting

import android.app.Application
import com.example.aiaccounting.widget.WidgetWorkScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIAccountingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Schedule periodic widget background updates
        WidgetWorkScheduler.scheduleWidgetUpdates(this)
    }
}
