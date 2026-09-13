package com.example.aiaccounting.data.service

import com.example.aiaccounting.di.InviteGatewayOkHttpClient
import com.example.aiaccounting.utils.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class InviteGatewayService @Inject constructor(
    @InviteGatewayOkHttpClient private val client: OkHttpClient
) {

    sealed class BootstrapResult {
        data class Success(
            val token: String,
            val apiBaseUrl: String,
            val rpm: Int
        ) : BootstrapResult()

        /**
         * @param code Worker 返回的 error 字段（如 invite_already_used / invalid_inviteCode 等）。
         */
        data class ApiError(val code: String? = null) : BootstrapResult()

        data class NetworkError(val message: String) : BootstrapResult()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun bootstrap(
        inviteCode: String,
        deviceId: String,
        gatewayBaseUrl: String
    ): BootstrapResult = withContext(Dispatchers.IO) {
        val url = UrlUtils.join(gatewayBaseUrl, "bootstrap")

        val payload = JSONObject()
            .put("inviteCode", inviteCode.trim())
            .put("deviceId", deviceId.trim())
            .toString()

        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(jsonMediaType))
            .header("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                // Try parse JSON even on error
                val json = try {
                    if (body.isBlank()) null else JSONObject(body)
                } catch (_: Exception) {
                    null
                }

                if (!response.isSuccessful) {
                    val code = json?.optString("error")?.takeIf { it.isNotBlank() }
                    return@withContext BootstrapResult.ApiError(code)
                }

                val token = json?.optString("token")?.trim().orEmpty()
                val apiBaseUrl = json?.optString("apiBaseUrl")?.trim().orEmpty()
                val rpm = json?.optInt("rpm", 60) ?: 60

                if (token.isBlank() || apiBaseUrl.isBlank()) {
                    return@withContext BootstrapResult.NetworkError("网关返回数据不完整")
                }

                return@withContext BootstrapResult.Success(
                    token = token,
                    apiBaseUrl = apiBaseUrl,
                    rpm = rpm
                )
            }
        } catch (e: Exception) {
            val message = when (e) {
                is SocketTimeoutException -> "连接超时：当前网络访问网关不稳定（模拟器可能需要代理/桥接模式）"
                is UnknownHostException -> "无法解析网关域名：请检查网络或网关地址"
                is ConnectException -> "无法连接到网关：请检查网络或稍后再试"
                else -> e.message ?: "网络请求失败"
            }
            BootstrapResult.NetworkError(message)
        }
    }
}
