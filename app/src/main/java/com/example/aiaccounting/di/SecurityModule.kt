package com.example.aiaccounting.di

import android.content.Context
import com.example.aiaccounting.data.local.database.DatabaseFactory
import com.example.aiaccounting.security.SecurityChecker
import com.example.aiaccounting.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Security module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideSecurityChecker(@ApplicationContext context: Context): SecurityChecker {
        return SecurityChecker(context)
    }

    @Provides
    @Singleton
    fun provideDatabaseFactory(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): DatabaseFactory {
        return DatabaseFactory(context, securityManager)
    }
}
