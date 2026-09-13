package com.example.aiaccounting.data.service

import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateServiceTest {

    private val service = AppUpdateService(OkHttpClient())

    @Test
    fun normalizeVersion_stripsLeadingVAndSuffix() {
        assertEquals("1.8.4", service.normalizeVersion("v1.8.4-beta+12"))
        assertEquals("1.8.4", service.normalizeVersion(" V1.8.4 "))
    }

    @Test
    fun isRemoteVersionNewer_returnsTrueWhenRemoteIsHigher() {
        assertTrue(service.isRemoteVersionNewer("1.8.3", "1.8.4"))
        assertTrue(service.isRemoteVersionNewer("1.8.3", "v1.9.0"))
    }

    @Test
    fun isRemoteVersionNewer_returnsFalseWhenEqualOrLower() {
        assertFalse(service.isRemoteVersionNewer("1.8.3", "1.8.3"))
        assertFalse(service.isRemoteVersionNewer("1.8.3", "1.8.2"))
    }

    @Test
    fun parseReleaseInfo_returnsParsedReleaseWithApkAsset() {
        val json = JSONObject(
            """
            {
              "tag_name": "v1.8.4",
              "name": "v1.8.4",
              "body": "修复问题",
              "html_url": "https://github.com/gypg/ai-accounting-new/releases/tag/v1.8.4",
              "published_at": "2026-04-07T10:00:00Z",
              "prerelease": false,
              "assets": [
                {
                  "name": "AI记账_v1.8.4_release.apk",
                  "browser_download_url": "https://example.com/app.apk"
                }
              ]
            }
            """.trimIndent()
        )

        val releaseInfo = service.parseReleaseInfo(json)

        assertNotNull(releaseInfo)
        assertEquals("1.8.4", releaseInfo?.versionName)
        assertEquals("AI记账_v1.8.4_release.apk", releaseInfo?.apkAssetName)
        assertEquals("https://example.com/app.apk", releaseInfo?.apkDownloadUrl)
    }

    @Test
    fun parseReleaseInfo_returnsNullWhenTagOrUrlMissing() {
        val missingTag = JSONObject("""{"html_url":"https://example.com"}""")
        val missingUrl = JSONObject("""{"tag_name":"v1.8.4"}""")

        assertNull(service.parseReleaseInfo(missingTag))
        assertNull(service.parseReleaseInfo(missingUrl))
    }
}
