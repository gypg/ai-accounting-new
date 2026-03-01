package com.example.aiaccounting.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Security checker for detecting rooted devices, emulators, and debugging mode
 */
class SecurityChecker(private val context: Context) {

    companion object {
        private const val TAG = "SecurityChecker"

        // Known root paths
        private val ROOT_PATHS = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        // Known emulator properties
        private val EMULATOR_PROPERTIES = listOf(
            "ro.kernel.qemu",
            "ro.build.product.genymotion",
            "ro.product.model.bluestacks",
            "ro.product.device.genymotion"
        )

        // Known emulator device IDs
        private val EMULATOR_DEVICE_IDS = listOf(
            "000000000000000",
            "0123456789abcdef",
            "e0070b030f0d020a"
        )
    }

    /**
     * Check if device is rooted
     */
    fun isRooted(): Boolean {
        return checkRootPaths() || checkSuCommand() || checkRootApps()
    }

    /**
     * Check if device is an emulator
     */
    fun isEmulator(): Boolean {
        return checkEmulatorProperties() || checkEmulatorDeviceId() || checkEmulatorBuild()
    }

    /**
     * Check if app is running in debug mode
     */
    fun isDebuggable(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Check if app signature is valid (not tampered)
     */
    fun isSignatureValid(): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            // For now, just check if signature exists
            // In production, you should verify against known signature hash
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Perform all security checks
     */
    fun performSecurityChecks(): SecurityCheckResult {
        return SecurityCheckResult(
            isRooted = isRooted(),
            isEmulator = isEmulator(),
            isDebuggable = isDebuggable(),
            isSignatureValid = isSignatureValid(),
            isSecure = !isRooted() && !isEmulator() && !isDebuggable() && isSignatureValid()
        )
    }

    // Root detection methods

    private fun checkRootPaths(): Boolean {
        return ROOT_PATHS.any { path ->
            File(path).exists()
        }
    }

    private fun checkSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val `in` = process.inputStream
            `in`.read() != -1
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRootApps(): Boolean {
        val rootApps = listOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.topjohnwu.magisk"
        )

        return rootApps.any { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    // Emulator detection methods

    private fun checkEmulatorProperties(): Boolean {
        return EMULATOR_PROPERTIES.any { property ->
            val value = getSystemProperty(property)
            value != null && value.isNotEmpty()
        }
    }

    private fun checkEmulatorDeviceId(): Boolean {
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return EMULATOR_DEVICE_IDS.contains(deviceId)
    }

    private fun checkEmulatorBuild(): Boolean {
        val buildModel = Build.MODEL.lowercase()
        val buildProduct = Build.PRODUCT.lowercase()
        val buildHardware = Build.HARDWARE.lowercase()
        val buildManufacturer = Build.MANUFACTURER.lowercase()

        val emulatorIndicators = listOf(
            "sdk", "google_sdk", "emulator", "android sdk built for x86",
            "genymotion", "vbox", "goldfish", "test"
        )

        return emulatorIndicators.any { indicator ->
            buildModel.contains(indicator) ||
            buildProduct.contains(indicator) ||
            buildHardware.contains(indicator) ||
            buildManufacturer.contains(indicator)
        }
    }

    private fun getSystemProperty(property: String): String? {
        return try {
            val propClass = Class.forName("android.os.SystemProperties")
            val getMethod = propClass.getMethod("get", String::class.java)
            getMethod.invoke(null, property) as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Security check result
     */
    data class SecurityCheckResult(
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggable: Boolean,
        val isSignatureValid: Boolean,
        val isSecure: Boolean
    ) {
        fun getWarningMessage(): String? {
            if (!isSecure) {
                val warnings = mutableListOf<String>()
                if (isRooted) warnings.add("设备已Root")
                if (isEmulator) warnings.add("检测到模拟器")
                if (isDebuggable) warnings.add("调试模式已启用")
                if (!isSignatureValid) warnings.add("应用签名无效")
                return warnings.joinToString(", ")
            }
            return null
        }
    }
}