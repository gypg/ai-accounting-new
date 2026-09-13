package com.example.aiaccounting.di

import com.example.aiaccounting.utils.AndroidIdDeviceIdProvider
import com.example.aiaccounting.utils.DeviceIdProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceIdModule {

    @Binds
    abstract fun bindDeviceIdProvider(impl: AndroidIdDeviceIdProvider): DeviceIdProvider
}
