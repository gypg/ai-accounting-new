package com.example.aiaccounting

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIAccountingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
