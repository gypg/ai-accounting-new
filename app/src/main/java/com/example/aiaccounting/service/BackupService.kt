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
        private const val BACKUP_INFO_FILE = "backup_info.json"
        private const val DATABASE_FILE_NAME = "ai_accounting.db"
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

                // Get database file
                val dbFile = getDatabasePath(DATABASE_FILE_NAME)
                val filesToBackup = mutableListOf<File>()

                // Create backup info
                val backupInfo = BackupInfo(
                    version = 1,
                    timestamp = timestamp,
                    appVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, 0).versionCode
                    }
                )
                val backupInfoFile = File(cacheDir, BACKUP_INFO_FILE)
                backupInfoFile.writeText(com.google.gson.Gson().toJson(backupInfo))
                filesToBackup.add(backupInfoFile)

                // Add database file if exists
                if (dbFile.exists()) {
                    filesToBackup.add(dbFile)
                    // Also add WAL and SHM files if they exist
                    val walFile = File("$dbFile-wal")
                    val shmFile = File("$dbFile-shm")
                    if (walFile.exists()) filesToBackup.add(walFile)
                    if (shmFile.exists()) filesToBackup.add(shmFile)
                }

                // Zip all files
                zipFiles(filesToBackup, backupFile)

                // Encrypt backup
                val encryptedFile = File("$backupFile.enc")
                encryptFile(backupFile, encryptedFile)

                // Cleanup temp files
                backupFile.delete()
                backupInfoFile.delete()

                updateNotification("备份完成: ${encryptedFile.name}", isComplete = true)
                sendBackupBroadcast(true, "备份成功: ${encryptedFile.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotification("备份失败: ${e.message}", isComplete = true)
                sendBackupBroadcast(false, "备份失败: ${e.message}")
            }
        }
    }

    /**
     * Backup info data class
     */
    data class BackupInfo(
        val version: Int,
        val timestamp: String,
        val appVersion: Int
    )

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

                // Verify backup info
                val backupInfoFile = File(extractDir, BACKUP_INFO_FILE)
                if (!backupInfoFile.exists()) {
                    throw IllegalStateException("无效的备份文件")
                }

                // Close current database connection before restoring
                closeDatabase()

                // Restore database files
                val extractedDbFile = File(extractDir, DATABASE_FILE_NAME)
                if (extractedDbFile.exists()) {
                    val targetDbFile = getDatabasePath(DATABASE_FILE_NAME)
                    
                    // Backup current database before overwriting
                    val currentBackup = File(cacheDir, "current_db_backup.tmp")
                    if (targetDbFile.exists()) {
                        targetDbFile.copyTo(currentBackup, overwrite = true)
                    }

                    try {
                        // Copy restored database
                        extractedDbFile.copyTo(targetDbFile, overwrite = true)
                        
                        // Restore WAL file if exists
                        val extractedWal = File(extractDir, "$DATABASE_FILE_NAME-wal")
                        val targetWal = File("$targetDbFile-wal")
                        if (extractedWal.exists()) {
                            extractedWal.copyTo(targetWal, overwrite = true)
                        }
                        
                        // Restore SHM file if exists
                        val extractedShm = File(extractDir, "$DATABASE_FILE_NAME-shm")
                        val targetShm = File("$targetDbFile-shm")
                        if (extractedShm.exists()) {
                            extractedShm.copyTo(targetShm, overwrite = true)
                        }
                        
                        // Delete current backup on success
                        currentBackup.delete()
                    } catch (e: Exception) {
                        // Restore original database on failure
                        if (currentBackup.exists()) {
                            currentBackup.copyTo(targetDbFile, overwrite = true)
                        }
                        throw e
                    }
                }

                // Cleanup
                decryptedFile.delete()
                extractDir.deleteRecursively()

                updateNotification("恢复完成，请重启应用", isComplete = true)
                sendRestoreBroadcast(true, "恢复成功，请重启应用以完成数据加载")
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotification("恢复失败: ${e.message}", isComplete = true)
                sendRestoreBroadcast(false, "恢复失败: ${e.message}")
            }
        }
    }

    /**
     * Close database connection before restore
     */
    private fun closeDatabase() {
        // Note: Database will be closed when the app restarts
        // This is a placeholder for any cleanup needed
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
                val entryName = entry?.name ?: continue
                val destFile = File(destDir, entryName)
                destFile.parentFile?.mkdirs()
                destFile.outputStream().use { it.write(zis.readBytes()) }
            }
        }
    }

    private fun encryptFile(inputFile: File, outputFile: File) {
        val secretKey = securityManager.getEncryptionKey()
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
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

            val secretKey = securityManager.getEncryptionKey()
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
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
