package com.example.aiaccounting

import android.app.Application
import com.example.aiaccounting.logging.AppLogLogger
import com.example.aiaccounting.widget.WidgetWorkScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class AIAccountingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogging()

        // Schedule periodic widget background updates
        WidgetWorkScheduler.scheduleWidgetUpdates(this)
    }

    private fun installCrashLogging() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val details = buildString {
                appendLine("thread=${thread.name}")
                appendLine("exception=${throwable::class.java.name}")
                appendLine("message=${throwable.message ?: "<no-message>"}")
                appendLine("stack=")
                append(throwable.stackTraceToString())
            }.take(MAX_CRASH_LOG_DETAILS_LENGTH)

            runCatching {
                EntryPointAccessors.fromApplication(this, CrashLoggerEntryPoint::class.java)
                    .appLogLogger()
                    .errorBlocking(
                        source = "global_crash_handler",
                        category = "uncaught_exception",
                        message = throwable.message ?: throwable::class.java.simpleName,
                        details = details,
                        entityType = "thread",
                        entityId = thread.name
                    )
            }

            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CrashLoggerEntryPoint {
        fun appLogLogger(): AppLogLogger
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppLogEntryPoint {
        fun appLogLogger(): AppLogLogger
    }

    private companion object {
        const val MAX_CRASH_LOG_DETAILS_LENGTH = 12_000
    }
}
