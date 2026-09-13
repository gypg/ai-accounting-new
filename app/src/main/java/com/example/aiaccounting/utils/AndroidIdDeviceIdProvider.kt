package com.example.aiaccounting.utils

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidIdDeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceIdProvider {

    override fun getStableDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }
}
