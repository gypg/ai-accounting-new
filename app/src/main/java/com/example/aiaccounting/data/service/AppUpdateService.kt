package com.example.aiaccounting.data.service

import com.example.aiaccounting.BuildConfig
import com.example.aiaccounting.di.GithubReleaseOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateService @Inject constructor(
    @GithubReleaseOkHttpClient private val client: OkHttpClient
) {

    data class AppReleaseInfo(
        val tagName: String,
        val versionName: String,
        val releaseName: String,
        val body: String,
        val htmlUrl: String,
        val publishedAt: String?,
        val apkAssetName: String?,
        val apkDownloadUrl: String?,
        val isPrerelease: Boolean
    )

    sealed class ReleaseCheckResult {
        data class Success(val releaseInfo: AppReleaseInfo) : ReleaseCheckResult()
        data object UpToDate : ReleaseCheckResult()
        data class NetworkError(val message: String) : ReleaseCheckResult()
        data class ApiError(val message: String) : ReleaseCheckResult()
        data class InvalidResponse(val message: String) : ReleaseCheckResult()
    }

    suspend fun checkLatestRelease(
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): ReleaseCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .get()
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = when (response.code) {
                        403, 429 -> "GitHub 更新接口访问过于频繁，请稍后再试"
                        404 -> "当前还没有可用的 GitHub Release，请先发布 Release 后再检查更新"
                        else -> "检查更新失败：${response.code}"
                    }
                    return@withContext ReleaseCheckResult.ApiError(message)
                }

                if (body.isBlank()) {
                    return@withContext ReleaseCheckResult.InvalidResponse("版本接口返回为空")
                }

                val releaseJson = try {
                    JSONObject(body)
                } catch (_: Exception) {
                    return@withContext ReleaseCheckResult.InvalidResponse("版本接口返回格式异常")
                }

                val releaseInfo = parseReleaseInfo(releaseJson)
                    ?: return@withContext ReleaseCheckResult.InvalidResponse("版本信息不完整")

                return@withContext if (isRemoteVersionNewer(currentVersionName, releaseInfo.versionName)) {
                    ReleaseCheckResult.Success(releaseInfo)
                } else {
                    ReleaseCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            val message = when (e) {
                is SocketTimeoutException -> "检查更新超时，请稍后重试"
                is UnknownHostException -> "无法连接 GitHub，请检查网络后重试"
                is ConnectException -> "无法连接更新服务，请稍后再试"
                else -> e.message ?: "检查更新失败"
            }
            ReleaseCheckResult.NetworkError(message)
        }
    }

    internal fun parseReleaseInfo(json: JSONObject): AppReleaseInfo? {
        val tagName = json.optString("tag_name").trim()
        val versionName = normalizeVersion(tagName)
        val htmlUrl = json.optString("html_url").trim()
        if (versionName.isBlank() || htmlUrl.isBlank()) {
            return null
        }

        val assets = json.optJSONArray("assets") ?: JSONArray()
        val apkAsset = findApkAsset(assets)

        return AppReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            releaseName = json.optString("name").trim().ifBlank { tagName },
            body = json.optString("body").trim(),
            htmlUrl = htmlUrl,
            publishedAt = json.optString("published_at").trim().ifBlank { null },
            apkAssetName = apkAsset?.optString("name")?.trim()?.ifBlank { null },
            apkDownloadUrl = apkAsset?.optString("browser_download_url")?.trim()?.ifBlank { null },
            isPrerelease = json.optBoolean("prerelease", false)
        )
    }

    internal fun isRemoteVersionNewer(currentVersionName: String, remoteVersionName: String): Boolean {
        val currentParts = parseVersionParts(normalizeVersion(currentVersionName))
        val remoteParts = parseVersionParts(normalizeVersion(remoteVersionName))

        if (currentParts != null && remoteParts != null) {
            val maxSize = maxOf(currentParts.size, remoteParts.size)
            for (index in 0 until maxSize) {
                val current = currentParts.getOrElse(index) { 0 }
                val remote = remoteParts.getOrElse(index) { 0 }
                if (remote > current) return true
                if (remote < current) return false
            }
            return false
        }

        return normalizeVersion(currentVersionName) != normalizeVersion(remoteVersionName)
    }

    internal fun normalizeVersion(version: String): String {
        return version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('+')
            .substringBefore('-')
            .trim()
    }

    private fun parseVersionParts(version: String): List<Int>? {
        if (version.isBlank()) return null
        val parts = version.split('.')
        if (parts.isEmpty()) return null
        val parsed = mutableListOf<Int>()
        for (part in parts) {
            val number = part.toIntOrNull() ?: return null
            parsed += number
        }
        return parsed
    }

    private fun findApkAsset(assets: JSONArray): JSONObject? {
        var releaseFallback: JSONObject? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name").trim()
            if (name.endsWith(".apk", ignoreCase = true)) {
                return asset
            }
            if (releaseFallback == null && name.contains("release", ignoreCase = true)) {
                releaseFallback = asset
            }
        }
        return releaseFallback
    }

    companion object {
        private const val USER_AGENT = "AIAccounting-Android"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/gypg/ai-accounting-new/releases/latest"
    }
}
