package com.example.aiaccounting.di

import android.content.Context
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
}
