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
import com.example.aiaccounting.data.local.AppDatabase
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
    lateinit var database: AppDatabase

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
                val backupPath = intent.getStringExtra(EXTRA_BACKUP_PATH)
                if (backupPath != null) {
                    startForeground(NOTIFICATION_ID, createNotification("正在备份数据..."))
                    serviceScope.launch {
                        performBackup(backupPath)
                    }
                }
            }
            ACTION_RESTORE -> {
                val restorePath = intent.getStringExtra(EXTRA_RESTORE_PATH)
                if (restorePath != null) {
                    startForeground(NOTIFICATION_ID, createNotification("正在恢复数据..."))
                    serviceScope.launch {
                        performRestore(restorePath)
                    }
                }
            }
            ACTION_AUTO_BACKUP -> {
                startForeground(NOTIFICATION_ID, createNotification("正在自动备份..."))
                serviceScope.launch {
                    performAutoBackup()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "数据备份",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于显示数据备份和恢复进度"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI记账")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()
    }

    private suspend fun performBackup(backupPath: String) {
        try {
            updateNotification("正在备份数据库...")

            // 获取数据库文件路径
            val dbFile = getDatabasePath("ai_accounting.db")
            val dbShmFile = File(dbFile.parent, "ai_accounting.db-shm")
            val dbWalFile = File(dbFile.parent, "ai_accounting.db-wal")

            // 创建临时目录
            val tempDir = File(cacheDir, "backup_temp")
            tempDir.mkdirs()

            // 复制数据库文件到临时目录
            val tempDbFile = File(tempDir, "database.db")
            dbFile.copyTo(tempDbFile, overwrite = true)

            // 创建备份元数据
            val metadata = BackupMetadata(
                version = getAppVersion(),
                timestamp = Date(),
                deviceId = getDeviceId(),
                encrypted = true
            )

            // 保存元数据
            val metadataFile = File(tempDir, "metadata.json")
            metadataFile.writeText(metadata.toJson())

            // 创建ZIP文件
            val backupFile = File(backupPath)
            backupFile.parentFile?.mkdirs()

            zipFiles(tempDir, backupFile)

            // 加密备份文件
            val encryptedFile = File("$backupPath.enc")
            encryptFile(backupFile, encryptedFile)
            backupFile.delete()

            // 清理临时文件
            tempDir.deleteRecursively()

            updateNotification("备份完成", isComplete = true)
            sendBackupBroadcast(true, "备份成功")

        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification("备份失败: ${e.message}", isComplete = true)
            sendBackupBroadcast(false, e.message ?: "备份失败")
        } finally {
            stopForeground(true)
            stopSelf()
        }
    }

    private suspend fun performRestore(restorePath: String) {
        try {
            updateNotification("正在准备恢复...")

            val encryptedFile = File(restorePath)
            if (!encryptedFile.exists()) {
                throw IllegalArgumentException("备份文件不存在")
            }

            // 解密文件
            val decryptedFile = File(cacheDir, "restore_temp.zip")
            decryptFile(encryptedFile, decryptedFile)

            // 解压文件
            val tempDir = File(cacheDir, "restore_temp")
            tempDir.mkdirs()
            unzipFile(decryptedFile, tempDir)

            // 读取元数据
            val metadataFile = File(tempDir, "metadata.json")
            if (!metadataFile.exists()) {
                throw IllegalArgumentException("备份文件损坏")
            }

            val metadata = BackupMetadata.fromJson(metadataFile.readText())

            // 验证备份文件
            if (metadata.version > getAppVersion()) {
                throw IllegalArgumentException("备份文件版本过高，请升级应用")
            }

            updateNotification("正在恢复数据库...")

            // 关闭数据库连接
            database.close()

            // 恢复数据库文件
            val dbFile = getDatabasePath("ai_accounting.db")
            val tempDbFile = File(tempDir, "database.db")

            if (tempDbFile.exists()) {
                tempDbFile.copyTo(dbFile, overwrite = true)
            }

            // 清理临时文件
            decryptedFile.delete()
            tempDir.deleteRecursively()

            updateNotification("恢复完成", isComplete = true)
            sendRestoreBroadcast(true, "恢复成功")

        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification("恢复失败: ${e.message}", isComplete = true)
            sendRestoreBroadcast(false, e.message ?: "恢复失败")
        } finally {
            stopForeground(true)
            stopSelf()
        }
    }

    private suspend fun performAutoBackup() {
        try {
            val backupDir = File(filesDir, "auto_backups")
            backupDir.mkdirs()

            // 生成备份文件名
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val backupFileName = "auto_backup_${dateFormat.format(Date())}.zip.enc"
            val backupPath = File(backupDir, backupFileName).absolutePath

            // 执行备份
            performBackup(backupPath)

            // 清理旧备份（保留最近10个）
            cleanupOldBackups(backupDir, maxCount = 10)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanupOldBackups(backupDir: File, maxCount: Int) {
        val backups = backupDir.listFiles { file ->
            file.name.startsWith("auto_backup_") && file.name.endsWith(".zip.enc")
        }?.sortedBy { it.lastModified() } ?: return

        if (backups.size > maxCount) {
            backups.take(backups.size - maxCount).forEach { it.delete() }
        }
    }

    private fun zipFiles(sourceDir: File, zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(sourceDir).path
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun unzipFile(zipFile: File, destDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
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
                // 写入IV
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
            // 读取IV
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

    private fun getAppVersion(): Int {
        return try {
            packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
        } catch (e: Exception) {
            1
        }
    }

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    data class BackupMetadata(
        val version: Int,
        val timestamp: Date,
        val deviceId: String,
        val encrypted: Boolean
    ) {
        fun toJson(): String {
            return """
                {
                    "version": $version,
                    "timestamp": ${timestamp.time},
                    "deviceId": "$deviceId",
                    "encrypted": $encrypted
                }
            """.trimIndent()
        }

        companion object {
            fun fromJson(json: String): BackupMetadata {
                // 简化解析，实际使用Gson
                val version = json.substringAfter("\"version\": ").substringBefore(",").toInt()
                val timestamp = Date(json.substringAfter("\"timestamp\": ").substringBefore(",").toLong())
                val deviceId = json.substringAfter("\"deviceId\": \"").substringBefore("\"")
                val encrypted = json.substringAfter("\"encrypted\": ").substringBefore("}").toBoolean()

                return BackupMetadata(version, timestamp, deviceId, encrypted)
            }
        }
    }
}
