package com.example.aiaccounting.data.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * 存储管理器 - 管理应用数据存储位置
 * 所有数据存储在 /sdcard/AIAccounting/ 或 /storage/emulated/0/AIAccounting/ 目录下
 */
object StorageManager {

    private const val APP_FOLDER_NAME = "AIAccounting"
    private const val TAG = "StorageManager"

    /**
     * 获取应用根目录
     * 尝试多个可能的路径
     */
    fun getAppRootDirectory(): File {
        // 尝试多个路径
        val possiblePaths = listOf(
            File(Environment.getExternalStorageDirectory(), APP_FOLDER_NAME),
            File("/storage/emulated/0", APP_FOLDER_NAME),
            File("/sdcard", APP_FOLDER_NAME)
        )

        // 找到第一个可写的路径
        for (path in possiblePaths) {
            try {
                if (!path.exists()) {
                    val created = path.mkdirs()
                    if (created) {
                        Log.d(TAG, "Created directory at: ${path.absolutePath}")
                        return path
                    }
                } else {
                    return path
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create directory at ${path.absolutePath}: ${e.message}")
                continue
            }
        }

        // 如果都失败了，返回第一个路径（让调用者处理错误）
        return possiblePaths.first()
    }

    /**
     * 获取数据库目录 /sdcard/AIAccounting/database/
     */
    fun getDatabaseDirectory(): File {
        val dbDir = File(getAppRootDirectory(), "database")
        if (!dbDir.exists()) {
            val created = dbDir.mkdirs()
            if (!created) {
                Log.e(TAG, "Failed to create database directory")
            }
        }
        return dbDir
    }

    /**
     * 获取数据库文件路径
     */
    fun getDatabaseFile(): File {
        return File(getDatabaseDirectory(), "ai_accounting.db")
    }

    /**
     * 获取SharedPreferences目录 /sdcard/AIAccounting/prefs/
     */
    fun getPrefsDirectory(): File {
        val prefsDir = File(getAppRootDirectory(), "prefs")
        if (!prefsDir.exists()) {
            prefsDir.mkdirs()
        }
        return prefsDir
    }

    /**
     * 获取备份目录 /sdcard/AIAccounting/backups/
     */
    fun getBackupDirectory(): File {
        val backupDir = File(getAppRootDirectory(), "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }

    /**
     * 获取导出目录 /sdcard/AIAccounting/exports/
     */
    fun getExportDirectory(): File {
        val exportDir = File(getAppRootDirectory(), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }

    /**
     * 获取日志目录 /sdcard/AIAccounting/logs/
     */
    fun getLogsDirectory(): File {
        val logsDir = File(getAppRootDirectory(), "logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        return logsDir
    }

    /**
     * 获取缓存目录 /sdcard/AIAccounting/cache/
     */
    fun getCacheDirectory(): File {
        val cacheDir = File(getAppRootDirectory(), "cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    /**
     * 初始化所有存储目录
     */
    fun initializeDirectories(): Boolean {
        return try {
            getAppRootDirectory()
            getDatabaseDirectory()
            getPrefsDirectory()
            getBackupDirectory()
            getExportDirectory()
            getLogsDirectory()
            getCacheDirectory()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize directories: ${e.message}")
            false
        }
    }

    /**
     * 检查存储是否可用
     */
    fun isStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 检查 MANAGE_EXTERNAL_STORAGE
            android.os.Environment.isExternalStorageManager()
        } else {
            // Android 10 及以下检查 WRITE_EXTERNAL_STORAGE
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取存储使用情况
     */
    fun getStorageUsage(): StorageUsage {
        val rootDir = getAppRootDirectory()
        val totalSize = calculateDirectorySize(rootDir)

        return StorageUsage(
            databaseSize = calculateDirectorySize(getDatabaseDirectory()),
            backupsSize = calculateDirectorySize(getBackupDirectory()),
            exportsSize = calculateDirectorySize(getExportDirectory()),
            cacheSize = calculateDirectorySize(getCacheDirectory()),
            totalSize = totalSize
        )
    }

    /**
     * 计算目录大小
     */
    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0

        var size = 0L
        val files = dir.listFiles()
        files?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * 清理缓存
     */
    fun clearCache(): Boolean {
        return try {
            val cacheDir = getCacheDirectory()
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取存储路径信息（用于调试）
     */
    fun getStorageInfo(): String {
        return buildString {
            appendLine("Storage Info:")
            appendLine("ExternalStorageDirectory: ${Environment.getExternalStorageDirectory().absolutePath}")
            appendLine("AppRootDirectory: ${getAppRootDirectory().absolutePath}")
            appendLine("DatabaseFile: ${getDatabaseFile().absolutePath}")
            appendLine("StorageAvailable: ${isStorageAvailable()}")
            appendLine("RootExists: ${getAppRootDirectory().exists()}")
        }
    }
}

/**
 * 存储使用情况数据类
 */
data class StorageUsage(
    val databaseSize: Long,
    val backupsSize: Long,
    val exportsSize: Long,
    val cacheSize: Long,
    val totalSize: Long
) {
    fun formatSize(): String {
        return when {
            totalSize < 1024 -> "$totalSize B"
            totalSize < 1024 * 1024 -> "${totalSize / 1024} KB"
            totalSize < 1024 * 1024 * 1024 -> "${totalSize / (1024 * 1024)} MB"
            else -> "${totalSize / (1024 * 1024 * 1024)} GB"
        }
    }
}
