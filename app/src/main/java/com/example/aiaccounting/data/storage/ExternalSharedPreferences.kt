package com.example.aiaccounting.data.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.File

/**
 * 外部存储的 SharedPreferences 实现
 * 数据存储在 /sdcard/AIAccounting/prefs/ 目录下
 */
class ExternalSharedPreferences private constructor(
    private val prefsDir: File,
    private val name: String
) : SharedPreferences {

    private val prefsFile = File(prefsDir, "$name.json")
    private val data = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    init {
        loadFromFile()
    }

    companion object {
        fun getInstance(context: Context, name: String): ExternalSharedPreferences {
            val prefsDir = StorageManager.getPrefsDirectory()
            return ExternalSharedPreferences(prefsDir, name)
        }
    }

    private fun loadFromFile() {
        if (!prefsFile.exists()) return
        
        try {
            val json = JSONObject(prefsFile.readText())
            data.clear()
            json.keys().forEach { key ->
                data[key] = json.get(key)
            }
        } catch (e: Exception) {
            // 加载失败，使用空数据
        }
    }

    private fun saveToFile() {
        try {
            val json = JSONObject()
            data.forEach { (key, value) ->
                when (value) {
                    is String -> json.put(key, value)
                    is Int -> json.put(key, value)
                    is Long -> json.put(key, value)
                    is Float -> json.put(key, value)
                    is Double -> json.put(key, value)
                    is Boolean -> json.put(key, value)
                    null -> json.put(key, JSONObject.NULL)
                }
            }
            prefsFile.writeText(json.toString())
        } catch (e: Exception) {
            // 保存失败
        }
    }

    override fun getAll(): Map<String, *> = data.toMap()

    override fun getString(key: String, defValue: String?): String? {
        return data[key] as? String ?: defValue
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        @Suppress("UNCHECKED_CAST")
        return data[key] as? Set<String> ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        return (data[key] as? Number)?.toInt() ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        return (data[key] as? Number)?.toLong() ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return (data[key] as? Number)?.toFloat() ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return data[key] as? Boolean ?: defValue
    }

    override fun contains(key: String): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        listeners.remove(listener)
    }

    private fun notifyListeners(key: String) {
        listeners.forEach { it.onSharedPreferenceChanged(this, key) }
    }

    inner class Editor : SharedPreferences.Editor {
        private val pendingChanges = mutableMapOf<String, Any?>()
        private val pendingRemovals = mutableSetOf<String>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putStringSet(
            key: String,
            values: Set<String>?
        ): SharedPreferences.Editor {
            pendingChanges[key] = values
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            pendingChanges[key] = value
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            pendingRemovals.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            pendingRemovals.addAll(data.keys)
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            pendingRemovals.forEach { key ->
                data.remove(key)
                notifyListeners(key)
            }
            pendingChanges.forEach { (key, value) ->
                data[key] = value
                notifyListeners(key)
            }
            pendingRemovals.clear()
            pendingChanges.clear()
            saveToFile()
        }
    }
}
