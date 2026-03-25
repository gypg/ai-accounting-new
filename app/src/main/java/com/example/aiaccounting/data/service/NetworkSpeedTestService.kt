package com.example.aiaccounting.data.service

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.di.AiTestOkHttpClient
import com.example.aiaccounting.di.InviteGatewayOkHttpClient
import com.example.aiaccounting.utils.OpenAiUrlUtils
import com.example.aiaccounting.utils.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSpeedTestService @Inject constructor(
    @AiTestOkHttpClient private val aiTestClient: OkHttpClient,
    @InviteGatewayOkHttpClient private val gatewayClient: OkHttpClient
) {

    suspend fun test(config: AIConfig, gatewayBaseUrl: String?): NetworkSpeedTestSummary = withContext(Dispatchers.IO) {
        val targets = buildTargets(config = config, gatewayBaseUrl = gatewayBaseUrl)
        if (targets.isEmpty()) {
            return@withContext NetworkSpeedTestSummary(
                targets = emptyList(),
                fastest = null,
                errorMessage = "请先配置可测速的 API 或网关地址"
            )
        }

        val results = coroutineScope {
            targets.map { target ->
                async {
                    measureTarget(target)
                }
            }.awaitAll()
        }

        val fastest = results
            .filterIsInstance<NetworkSpeedTestResult.Success>()
            .minByOrNull { it.latencyMs }

        val errorMessage = if (fastest != null) {
            null
        } else {
            results.filterIsInstance<NetworkSpeedTestResult.Error>()
                .firstOrNull()?.message ?: "网络测速失败"
        }

        NetworkSpeedTestSummary(
            targets = results,
            fastest = fastest,
            errorMessage = errorMessage
        )
    }

    private fun buildTargets(config: AIConfig, gatewayBaseUrl: String?): List<NetworkSpeedTestTarget> {
        val apiUrl = config.apiUrl.trim()
        val gatewayUrl = gatewayBaseUrl?.trim().orEmpty()

        val apiTarget = apiUrl.takeIf { it.isNotBlank() }?.let {
            NetworkSpeedTestTarget(
                id = "api",
                label = "API 节点",
                url = OpenAiUrlUtils.models(it),
                clientType = NetworkSpeedTestClientType.AI
            )
        }

        val gatewayTarget = gatewayUrl.takeIf { it.isNotBlank() }?.let {
            NetworkSpeedTestTarget(
                id = "gateway",
                label = "邀请码网关",
                url = UrlUtils.join(it, "bootstrap"),
                clientType = NetworkSpeedTestClientType.GATEWAY
            )
        }

        return listOfNotNull(apiTarget, gatewayTarget)
            .distinctBy { it.url }
    }

    private fun measureTarget(target: NetworkSpeedTestTarget): NetworkSpeedTestResult {
        val client = when (target.clientType) {
            NetworkSpeedTestClientType.AI -> aiTestClient
            NetworkSpeedTestClientType.GATEWAY -> gatewayClient
        }

        val startNs = System.nanoTime()

        return try {
            val requestUrl = target.url.toHttpUrlOrNull()
                ?: return NetworkSpeedTestResult.Error(
                    target = target,
                    message = "${target.label} 地址格式无效",
                    failureType = NetworkSpeedTestFailureType.INVALID_URL
                )

            val request = Request.Builder()
                .url(requestUrl)
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                val latencyMs = ((System.nanoTime() - startNs) / 1_000_000).coerceAtLeast(0)
                if (response.isSuccessful || response.code in setOf(401, 403, 404, 405)) {
                    NetworkSpeedTestResult.Success(
                        target = target,
                        latencyMs = latencyMs,
                        statusCode = response.code
                    )
                } else {
                    NetworkSpeedTestResult.Error(
                        target = target,
                        message = "${target.label} 响应异常 (${response.code})",
                        failureType = NetworkSpeedTestFailureType.HTTP_ERROR
                    )
                }
            }
        } catch (error: Exception) {
            NetworkSpeedTestResult.Error(
                target = target,
                message = mapFailureMessage(target, error),
                failureType = classifyFailure(error)
            )
        }
    }

    private fun classifyFailure(error: Throwable): NetworkSpeedTestFailureType {
        return when (error) {
            is SocketTimeoutException -> NetworkSpeedTestFailureType.TIMEOUT
            is UnknownHostException -> NetworkSpeedTestFailureType.DNS_FAILURE
            is ConnectException -> NetworkSpeedTestFailureType.CONNECT_FAILURE
            else -> NetworkSpeedTestFailureType.OTHER
        }
    }

    private fun mapFailureMessage(target: NetworkSpeedTestTarget, error: Throwable): String {
        return when (error) {
            is SocketTimeoutException -> "${target.label} 连接超时"
            is UnknownHostException -> "${target.label} 域名解析失败"
            is ConnectException -> "${target.label} 无法建立连接"
            else -> "${target.label} 测速失败，请稍后重试"
        }
    }
}

data class NetworkSpeedTestSummary(
    val targets: List<NetworkSpeedTestResult>,
    val fastest: NetworkSpeedTestResult.Success?,
    val errorMessage: String?
)

data class PreferredNetworkRoute(
    val targetId: String,
    val label: String,
    val latencyMs: Long,
    val updatedAtMillis: Long,
    val endpointUrl: String
)

data class NetworkSpeedTestTarget(
    val id: String,
    val label: String,
    val url: String,
    val clientType: NetworkSpeedTestClientType
)

enum class NetworkSpeedTestClientType {
    AI,
    GATEWAY
}

enum class NetworkSpeedTestFailureType {
    TIMEOUT,
    DNS_FAILURE,
    CONNECT_FAILURE,
    INVALID_URL,
    HTTP_ERROR,
    OTHER
}

sealed class NetworkSpeedTestResult {
    abstract val target: NetworkSpeedTestTarget

    data class Success(
        override val target: NetworkSpeedTestTarget,
        val latencyMs: Long,
        val statusCode: Int
    ) : NetworkSpeedTestResult()

    data class Error(
        override val target: NetworkSpeedTestTarget,
        val message: String,
        val failureType: NetworkSpeedTestFailureType
    ) : NetworkSpeedTestResult()
}
