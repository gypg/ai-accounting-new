package com.example.aiaccounting.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 解决 Android 模拟器与 Cloudflare 节点间由于系统级 IPv6 黑洞导致的超时问题（优先使用 IPv4）
    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
            } catch (e: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    @Provides
    @Singleton
    @AiOkHttpClient
    fun provideAiOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)  // 5分钟读取超时，支持长消息
            .writeTimeout(60, TimeUnit.SECONDS)  // 1分钟写入超时
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build()
    }

    @Provides
    @Singleton
    @AiTestOkHttpClient
    fun provideAiTestOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(3, 3, TimeUnit.MINUTES))
            .build()
    }

    @Provides
    @Singleton
    @InviteGatewayOkHttpClient
    fun provideInviteGatewayOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(3, 3, TimeUnit.MINUTES))
            .build()
    }

    @Provides
    @Singleton
    @VoiceOkHttpClient
    fun provideVoiceOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
