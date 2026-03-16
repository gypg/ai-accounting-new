package com.example.aiaccounting.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Security manager for handling encryption, decryption, and key management
 */
class SecurityManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "ai_accounting_master_key"
        private const val SHARED_PREFS_NAME = "secure_prefs"
        private const val PIN_HASH_KEY = "pin_hash"
        private const val PIN_SALT_KEY = "pin_salt"
        private const val FAILED_ATTEMPTS_KEY = "failed_attempts"
        private const val LOCK_TIME_KEY = "lock_time"
        private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
        private const val LAST_AUTH_TIME_KEY = "last_auth_time"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION_MS = 30 * 60 * 1000 // 30 minutes
    }

    private var masterKey: MasterKey? = null
    private var encryptedPrefs: SharedPreferences? = null

    private fun getMasterKey(): MasterKey {
        if (masterKey == null) {
            masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }
        return masterKey!!
    }

    private fun getEncryptedPrefs(): SharedPreferences {
        if (encryptedPrefs == null) {
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                SHARED_PREFS_NAME,
                getMasterKey(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return encryptedPrefs!!
    }

    /**
     * PIN 状态（用于 fail-closed 路由判断）
     */
    sealed class PinState {
        data object Set : PinState()
        data object NotSet : PinState()
        data class Error(val cause: Throwable) : PinState()
    }

    /**
     * 获取 PIN 设置状态。
     *
     * 注意：这个接口用于主流程路由，必须能区分“未设置 PIN”与“安全组件初始化失败”。
     */
    fun getPinState(): PinState {
        return try {
            if (getEncryptedPrefs().contains(PIN_HASH_KEY)) PinState.Set else PinState.NotSet
        } catch (e: Exception) {
            PinState.Error(e)
        }
    }

    /**
     * Check if PIN is set
     */
    fun isPinSet(): Boolean {
        return when (getPinState()) {
            is PinState.Set -> true
            is PinState.NotSet -> false
            is PinState.Error -> false // fail-closed
        }
    }

    /**
     * Set up PIN for the first time
     */
    fun setupPin(pin: String): Boolean {
        if (isPinSet()) {
            return false // PIN already set
        }

        val salt = generateSalt()
        val pinHash = hashPinWithSalt(pin, salt)
        getEncryptedPrefs().edit()
            .putString(PIN_HASH_KEY, pinHash)
            .putString(PIN_SALT_KEY, android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT))
            .putInt(FAILED_ATTEMPTS_KEY, 0)
            .apply()
        return true
    }

    /**
     * Clear PIN - removes PIN protection
     */
    fun clearPin(): Boolean {
        return try {
            getEncryptedPrefs().edit()
                .remove(PIN_HASH_KEY)
                .remove(PIN_SALT_KEY)
                .remove(FAILED_ATTEMPTS_KEY)
                .remove(LOCK_TIME_KEY)
                .remove(LAST_AUTH_TIME_KEY)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate PIN
     */
    fun validatePin(inputPin: String): Boolean {
        // Check if locked
        if (isLocked()) {
            return false
        }

        val storedHash = getEncryptedPrefs().getString(PIN_HASH_KEY, null) ?: return false
        val saltBase64 = getEncryptedPrefs().getString(PIN_SALT_KEY, null)
        val salt = saltBase64?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) } ?: ByteArray(0)
        val inputHash = hashPinWithSalt(inputPin, salt)

        if (inputHash == storedHash) {
            // Reset failed attempts on success
            getEncryptedPrefs().edit()
                .putInt(FAILED_ATTEMPTS_KEY, 0)
                .putLong(LOCK_TIME_KEY, 0)
                .putLong(LAST_AUTH_TIME_KEY, System.currentTimeMillis())
                .apply()
            return true
        } else {
            // Increment failed attempts
            val failedAttempts = getEncryptedPrefs().getInt(FAILED_ATTEMPTS_KEY, 0) + 1
            getEncryptedPrefs().edit()
                .putInt(FAILED_ATTEMPTS_KEY, failedAttempts)
                .apply()

            // Lock if max attempts reached
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                getEncryptedPrefs().edit()
                    .putLong(LOCK_TIME_KEY, System.currentTimeMillis())
                    .apply()
            }
            return false
        }
    }

    /** 标记当前会话已完成身份验证（用于 Widget/Service 前置校验） */
    fun markAuthenticatedNow() {
        getEncryptedPrefs().edit()
            .putLong(LAST_AUTH_TIME_KEY, System.currentTimeMillis())
            .apply()
    }

    /** 统一的“解锁成功”入口：刷新鉴权会话时间窗口。 */
    fun onAuthenticationSucceeded() {
        markAuthenticatedNow()
    }

    /**
     * 是否在允许的时间窗口内完成过身份验证。
     *
     * @param ttlMs 允许的有效期，默认 10 分钟
     */
    fun hasValidAuthSession(ttlMs: Long = 10 * 60 * 1000L): Boolean {
        return try {
            // PIN 未设置时，不需要会话鉴权
            when (getPinState()) {
                is PinState.NotSet -> true
                is PinState.Error -> false
                is PinState.Set -> {
                    if (isLocked()) return false
                    val lastAuth = getEncryptedPrefs().getLong(LAST_AUTH_TIME_KEY, 0L)
                    if (lastAuth <= 0L) return false
                    System.currentTimeMillis() - lastAuth < ttlMs
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Change PIN
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!validatePin(oldPin)) {
            return false
        }

        val newSalt = generateSalt()
        val newPinHash = hashPinWithSalt(newPin, newSalt)
        getEncryptedPrefs().edit()
            .putString(PIN_HASH_KEY, newPinHash)
            .putString(PIN_SALT_KEY, android.util.Base64.encodeToString(newSalt, android.util.Base64.DEFAULT))
            .apply()
        return true
    }

    /**
     * Check if app is locked due to too many failed attempts
     */
    fun isLocked(): Boolean {
        val lockTime = getEncryptedPrefs().getLong(LOCK_TIME_KEY, 0)
        if (lockTime == 0L) return false

        val currentTime = System.currentTimeMillis()
        if (currentTime - lockTime >= LOCK_DURATION_MS) {
            // Lock period expired
            getEncryptedPrefs().edit()
                .putLong(LOCK_TIME_KEY, 0)
                .putInt(FAILED_ATTEMPTS_KEY, 0)
                .apply()
            return false
        }
        return true
    }

    /**
     * Get remaining lock time in milliseconds
     */
    fun getRemainingLockTime(): Long {
        val lockTime = getEncryptedPrefs().getLong(LOCK_TIME_KEY, 0)
        if (lockTime == 0L) return 0

        val elapsed = System.currentTimeMillis() - lockTime
        return maxOf(0, LOCK_DURATION_MS - elapsed)
    }

    /**
     * Get number of failed attempts
     */
    fun getFailedAttempts(): Int {
        return getEncryptedPrefs().getInt(FAILED_ATTEMPTS_KEY, 0)
    }

    /**
     * Hash PIN using SHA-256 with salt
     */
    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Derive database key from PIN using PBKDF2
     */
    fun deriveDatabaseKey(pin: String, salt: ByteArray): ByteArray {
        val iterations = 10000
        val keyLength = 256 // 256 bits = 32 bytes

        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, iterations, keyLength)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = factory.generateSecret(spec)
        return secretKey.encoded
    }

    /**
     * Generate random salt
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        java.security.SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Encrypt data using AES-GCM
     */
    fun encrypt(data: ByteArray, key: SecretKey): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        return Pair(encryptedData, iv)
    }

    /**
     * Decrypt data using AES-GCM
     */
    fun decrypt(encryptedData: ByteArray, iv: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(encryptedData)
    }

    /**
     * Generate or retrieve encryption key from Keystore
     */
    fun getEncryptionKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }

        val secretKey = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
        return secretKey
    }

    /**
     * Check if biometric authentication is enabled
     */
    fun isBiometricEnabled(): Boolean {
        return getEncryptedPrefs().getBoolean(BIOMETRIC_ENABLED_KEY, false)
    }

    /**
     * Enable/disable biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        getEncryptedPrefs().edit()
            .putBoolean(BIOMETRIC_ENABLED_KEY, enabled)
            .apply()
    }

    /**
     * Store encrypted string
     */
    fun storeEncryptedString(prefKey: String, value: String) {
        val encryptionKey = getEncryptionKey()
        val (encrypted, iv) = encrypt(value.toByteArray(), encryptionKey)
        
        // Store both encrypted data and IV
        val encryptedBase64 = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        val ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT)
        
        getEncryptedPrefs().edit()
            .putString("${prefKey}_data", encryptedBase64)
            .putString("${prefKey}_iv", ivBase64)
            .apply()
    }

    /**
     * Retrieve and decrypt string
     */
    fun getEncryptedString(prefKey: String): String? {
        val encryptedBase64 = getEncryptedPrefs().getString("${prefKey}_data", null) ?: return null
        val ivBase64 = getEncryptedPrefs().getString("${prefKey}_iv", null) ?: return null

        val encrypted = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
        val iv = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)

        val encryptionKey = getEncryptionKey()
        val decrypted = decrypt(encrypted, iv, encryptionKey)

        return String(decrypted, Charsets.UTF_8)
    }

    fun removeEncryptedString(prefKey: String) {
        getEncryptedPrefs().edit()
            .remove("${prefKey}_data")
            .remove("${prefKey}_iv")
            .apply()
    }
}
