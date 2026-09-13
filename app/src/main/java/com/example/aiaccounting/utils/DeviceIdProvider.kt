package com.example.aiaccounting.utils

interface DeviceIdProvider {
    fun getStableDeviceId(): String
}
