package com.example.aiaccounting.di

import android.content.Context
import com.example.aiaccounting.data.exporter.ExcelExporter
import com.example.aiaccounting.data.exporter.LogCsvExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Exporter module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object ExporterModule {

    @Provides
    @Singleton
    fun provideExcelExporter(@ApplicationContext context: Context): ExcelExporter {
        return ExcelExporter(context)
    }

    @Provides
    @Singleton
    fun provideLogCsvExporter(@ApplicationContext context: Context): LogCsvExporter {
        return LogCsvExporter(context)
    }
}
