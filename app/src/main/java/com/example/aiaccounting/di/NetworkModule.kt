package com.example.aiaccounting.di

import com.example.aiaccounting.data.remote.AIRepository
import com.example.aiaccounting.data.remote.AIService
import com.example.aiaccounting.data.remote.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Network module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofitClient(): RetrofitClient {
        return RetrofitClient.getInstance()
    }

    @Provides
    @Singleton
    fun provideAIService(retrofitClient: RetrofitClient): AIService {
        return retrofitClient.create(AIService::class.java)
    }

    @Provides
    @Singleton
    fun provideAIRepository(): AIRepository {
        return AIRepository()
    }
}