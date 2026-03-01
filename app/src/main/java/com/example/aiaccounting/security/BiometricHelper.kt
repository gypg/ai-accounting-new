package com.example.aiaccounting.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Helper for biometric authentication (fingerprint/face recognition)
 */
class BiometricHelper(private val context: Context) {

    companion object {
        private const val TITLE = "生物识别验证"
        private const val SUBTITLE = "请使用指纹或面部识别解锁"
        private const val NEGATIVE_BUTTON = "使用PIN码"
        private const val DESCRIPTION = "验证您的身份以访问应用"
    }

    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private val biometricManager = BiometricManager.from(context)

    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Check if device has biometric hardware
     */
    fun hasBiometricHardware(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> true
            else -> false
        }
    }

    /**
     * Check if biometric is enrolled
     */
    fun isBiometricEnrolled(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Get error message for biometric status
     */
    fun getBiometricErrorMessage(): String? {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "设备不支持生物识别"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "生物识别硬件不可用"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "未设置生物识别"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "需要安全更新"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "不支持生物识别"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "生物识别状态未知"
            else -> null
        }
    }

    /**
     * Show biometric prompt
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onUsePin: () -> Unit = {}
    ) {
        if (!isBiometricAvailable()) {
            onFailure(getBiometricErrorMessage() ?: "生物识别不可用")
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(TITLE)
            .setSubtitle(SUBTITLE)
            .setDescription(DESCRIPTION)
            .setNegativeButtonText(NEGATIVE_BUTTON)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailure("生物识别验证失败")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onUsePin()
                    } else {
                        onFailure(errString.toString())
                    }
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Check if biometric is supported on this device
     */
    fun isSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        return hasBiometricHardware()
    }
}
