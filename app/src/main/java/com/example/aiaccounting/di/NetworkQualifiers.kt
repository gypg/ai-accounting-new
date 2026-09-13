package com.example.aiaccounting.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiTestOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InviteGatewayOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VoiceOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GithubReleaseOkHttpClient
