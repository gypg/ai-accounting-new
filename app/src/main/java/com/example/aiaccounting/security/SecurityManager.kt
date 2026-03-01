package com.example.aiaccounting.security

import android.content.Context
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
        private const val FAILED_ATTEMPTS_KEY = "failed_attempts"
        private const val LOCK_TIME_KEY = "lock_time"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCK_DURATION_MS = 30 * 60 * 1000 // 30 minutes
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            SHARED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Check if PIN is set
     */
    fun isPinSet(): Boolean {
        return encryptedPrefs.contains(PIN_HASH_KEY)
    }

    /**
     * Set up PIN for the first time
     */
    fun setupPin(pin: String): Boolean {
        if (isPinSet()) {
            return false // PIN already set
        }
        
        val pinHash = hashPin(pin)
        encryptedPrefs.edit()
            .putString(PIN_HASH_KEY, pinHash)
            .putInt(FAILED_ATTEMPTS_KEY, 0)
            .apply()
        return true
    }

    /**
     * Validate PIN
     */
    fun validatePin(inputPin: String): Boolean {
        // Check if locked
        if (isLocked()) {
            return false
        }

        val storedHash = encryptedPrefs.getString(PIN_HASH_KEY, null) ?: return false
        val inputHash = hashPin(inputPin)

        if (inputHash == storedHash) {
            // Reset failed attempts on success
            encryptedPrefs.edit()
                .putInt(FAILED_ATTEMPTS_KEY, 0)
                .putLong(LOCK_TIME_KEY, 0)
                .apply()
            return true
        } else {
            // Increment failed attempts
            val failedAttempts = encryptedPrefs.getInt(FAILED_ATTEMPTS_KEY, 0) + 1
            encryptedPrefs.edit()
                .putInt(FAILED_ATTEMPTS_KEY, failedAttempts)
                .apply()

            // Lock if max attempts reached
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                encryptedPrefs.edit()
                    .putLong(LOCK_TIME_KEY, System.currentTimeMillis())
                    .apply()
            }
            return false
        }
    }

    /**
     * Change PIN
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!validatePin(oldPin)) {
            return false
        }
        
        val newPinHash = hashPin(newPin)
        encryptedPrefs.edit()
            .putString(PIN_HASH_KEY, newPinHash)
            .apply()
        return true
    }

    /**
     * Check if app is locked due to too many failed attempts
     */
    fun isLocked(): Boolean {
        val lockTime = encryptedPrefs.getLong(LOCK_TIME_KEY, 0)
        if (lockTime == 0L) return false

        val currentTime = System.currentTimeMillis()
        if (currentTime - lockTime >= LOCK_DURATION_MS) {
            // Lock period expired
            encryptedPrefs.edit()
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
        val lockTime = encryptedPrefs.getLong(LOCK_TIME_KEY, 0)
        if (lockTime == 0L) return 0

        val elapsed = System.currentTimeMillis() - lockTime
        return maxOf(0, LOCK_DURATION_MS - elapsed)
    }

    /**
     * Get number of failed attempts
     */
    fun getFailedAttempts(): Int {
        return encryptedPrefs.getInt(FAILED_ATTEMPTS_KEY, 0)
    }

    /**
     * Hash PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
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
     * Store encrypted string
     */
    fun storeEncryptedString(prefKey: String, value: String) {
        val encryptionKey = getEncryptionKey()
        val (encrypted, iv) = encrypt(value.toByteArray(), encryptionKey)
        
        // Store both encrypted data and IV
        val encryptedBase64 = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        val ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT)
        
        encryptedPrefs.edit()
            .putString("${prefKey}_data", encryptedBase64)
            .putString("${prefKey}_iv", ivBase64)
            .apply()
    }

    /**
     * Retrieve and decrypt string
     */
    fun getEncryptedString(prefKey: String): String? {
        val encryptedBase64 = encryptedPrefs.getString("${prefKey}_data", null) ?: return null
        val ivBase64 = encryptedPrefs.getString("${prefKey}_iv", null) ?: return null
        
        val encrypted = android.util.Base64.decode(encryptedBase64, android.util.Base64.DEFAULT)
        val iv = android.util.Base64.decode(ivBase64, android.util.Base64.DEFAULT)
        
        val encryptionKey = getEncryptionKey()
        val decrypted = decrypt(encrypted, iv, encryptionKey)
        
        return String(decrypted)
    }
}
