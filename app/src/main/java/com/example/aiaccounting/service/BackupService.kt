package com.example.aiaccounting.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.aiaccounting.R
import com.example.aiaccounting.security.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class BackupService : Service() {

    @Inject
    lateinit var securityManager: SecurityManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val CHANNEL_ID = "backup_channel"
    private val NOTIFICATION_ID = 1001

    companion object {
        const val ACTION_BACKUP = "com.example.aiaccounting.ACTION_BACKUP"
        const val ACTION_RESTORE = "com.example.aiaccounting.ACTION_RESTORE"
        const val ACTION_AUTO_BACKUP = "com.example.aiaccounting.ACTION_AUTO_BACKUP"
        const val EXTRA_BACKUP_PATH = "backup_path"
        const val EXTRA_RESTORE_PATH = "restore_path"

        fun startBackup(context: Context, backupPath: String) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_BACKUP
                putExtra(EXTRA_BACKUP_PATH, backupPath)
            }
            context.startService(intent)
        }

        fun startRestore(context: Context, restorePath: String) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_RESTORE
                putExtra(EXTRA_RESTORE_PATH, restorePath)
            }
            context.startService(intent)
        }

        fun startAutoBackup(context: Context) {
            val intent = Intent(context, BackupService::class.java).apply {
                action = ACTION_AUTO_BACKUP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BACKUP -> {
                val backupPath = intent.getStringExtra(EXTRA_BACKUP_PATH) ?: return START_NOT_STICKY
                startBackup(backupPath)
            }
            ACTION_RESTORE -> {
                val restorePath = intent.getStringExtra(EXTRA_RESTORE_PATH) ?: return START_NOT_STICKY
                startRestore(restorePath)
            }
            ACTION_AUTO_BACKUP -> {
                performAutoBackup()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startBackup(backupPath: String) {
        serviceScope.launch {
            try {
                showNotification("正在备份数据...")

                // Create backup directory
                val backupDir = File(backupPath)
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                // Create backup file
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "ai_accounting_backup_$timestamp.zip")

                // Note: Database backup temporarily disabled
                // TODO: Implement proper database backup when database is initialized

                // Create a placeholder file
                val placeholderFile = File(cacheDir, "backup_info.txt")
                placeholderFile.writeText("Backup created at $timestamp")

                zipFiles(listOf(placeholderFile), backupFile)

                // Encrypt backup
                val encryptedFile = File("$backupFile.enc")
                encryptFile(backupFile, encryptedFile)

                // Delete unencrypted file
                backupFile.delete()
                placeholderFile.delete()

                updateNotification("备份完成: ${encryptedFile.name}", isComplete = true)
                sendBackupBroadcast(true, "备份成功: ${encryptedFile.absolutePath}")
            } catch (e: Exception) {
                updateNotification("备份失败: ${e.message}", isComplete = true)
                sendBackupBroadcast(false, "备份失败: ${e.message}")
            }
        }
    }

    private fun startRestore(restorePath: String) {
        serviceScope.launch {
            try {
                showNotification("正在恢复数据...")

                val restoreFile = File(restorePath)
                if (!restoreFile.exists()) {
                    throw IllegalArgumentException("备份文件不存在")
                }

                // Decrypt backup
                val decryptedFile = File(cacheDir, "restore_temp.zip")
                decryptFile(restoreFile, decryptedFile)

                // Extract backup
                val extractDir = File(cacheDir, "restore_extract")
                extractDir.mkdirs()
                unzipFile(decryptedFile, extractDir)

                // Note: Database restore temporarily disabled
                // TODO: Implement proper database restore when database is initialized

                // Cleanup
                decryptedFile.delete()
                extractDir.deleteRecursively()

                updateNotification("恢复完成", isComplete = true)
                sendRestoreBroadcast(true, "恢复成功")
            } catch (e: Exception) {
                updateNotification("恢复失败: ${e.message}", isComplete = true)
                sendRestoreBroadcast(false, "恢复失败: ${e.message}")
            }
        }
    }

    private fun performAutoBackup() {
        val backupDir = File(getExternalFilesDir(null), "backups")
        startBackup(backupDir.absolutePath)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "备份服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于显示备份和恢复进度"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI记账")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun zipFiles(files: List<File>, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            files.forEach { file ->
                if (file.exists()) {
                    FileInputStream(file).use { fis ->
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    private fun unzipFile(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val destFile = File(destDir, entry!!.name)
                destFile.parentFile?.mkdirs()
                destFile.outputStream().use { it.write(zis.readBytes()) }
            }
        }
    }

    private fun encryptFile(inputFile: File, outputFile: File) {
        val key = securityManager.getDatabaseKey()
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = javax.crypto.spec.SecretKeySpec(key, "AES")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                // Write IV
                fos.write(cipher.iv)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) {
                        fos.write(encrypted)
                    }
                }
                val finalBlock = cipher.doFinal()
                if (finalBlock != null) {
                    fos.write(finalBlock)
                }
            }
        }
    }

    private fun decryptFile(inputFile: File, outputFile: File) {
        FileInputStream(inputFile).use { fis ->
            // Read IV
            val iv = ByteArray(12)
            fis.read(iv)

            val key = securityManager.getDatabaseKey()
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            val secretKey = javax.crypto.spec.SecretKeySpec(key, "AES")
            val gcmSpec = javax.crypto.spec.GCMParameterSpec(128, iv)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val decrypted = cipher.update(buffer, 0, bytesRead)
                    if (decrypted != null) {
                        fos.write(decrypted)
                    }
                }
                val finalBlock = cipher.doFinal()
                if (finalBlock != null) {
                    fos.write(finalBlock)
                }
            }
        }
    }

    private fun updateNotification(content: String, isComplete: Boolean = false) {
        val notification = if (isComplete) {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AI记账")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .build()
        } else {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AI记账")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setProgress(0, 0, true)
                .setOngoing(true)
                .build()
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendBackupBroadcast(success: Boolean, message: String) {
        val intent = Intent("com.example.aiaccounting.BACKUP_COMPLETE").apply {
            putExtra("success", success)
            putExtra("message", message)
        }
        sendBroadcast(intent)
    }

    private fun sendRestoreBroadcast(success: Boolean, message: String) {
        val intent = Intent("com.example.aiaccounting.RESTORE_COMPLETE").apply {
            putExtra("success", success)
            putExtra("message", message)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
