package com.example.aiaccounting.di

import android.content.Context
import com.example.aiaccounting.data.service.AIService
import com.example.aiaccounting.data.service.AIVoiceRecognitionService
import com.example.aiaccounting.data.service.SpeechRecognitionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用模块 - 提供应用级别的依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供语音识别服务
     */
    @Provides
    @Singleton
    fun provideSpeechRecognitionService(
        @ApplicationContext context: Context
    ): SpeechRecognitionService {
        return SpeechRecognitionService(context)
    }

    /**
     * 提供AI语音识别服务
     */
    @Provides
    @Singleton
    fun provideAIVoiceRecognitionService(
        aiService: AIService
    ): AIVoiceRecognitionService {
        return AIVoiceRecognitionService(aiService)
    }
}
