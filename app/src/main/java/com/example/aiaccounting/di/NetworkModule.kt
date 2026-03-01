package com.example.aiaccounting.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Network module for dependency injection
 * Note: Remote services temporarily disabled
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Remote services temporarily disabled
    // TODO: Re-implement AI service integration when needed
}
