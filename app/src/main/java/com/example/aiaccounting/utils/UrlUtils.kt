package com.example.aiaccounting.utils

object UrlUtils {
    fun normalizeBase(url: String): String = url.trim().removeSuffix("/")

    fun join(baseUrl: String, path: String): String {
        val base = normalizeBase(baseUrl)
        val p = path.trim().removePrefix("/")
        return "$base/$p"
    }
}
