package com.example.aiaccounting.ui.viewmodel

import com.example.aiaccounting.data.model.AIConfig
import com.example.aiaccounting.data.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull

internal sealed class RemoteStreamCollectionResult {
    data class Success(val response: String) : RemoteStreamCollectionResult()
    data object Timeout : RemoteStreamCollectionResult()
    data class Failure(val message: String) : RemoteStreamCollectionResult()
}

internal class AIAssistantRemoteStreamCollector(
    private val sendChatStream: (List<ChatMessage>, AIConfig) -> Flow<String>,
    private val recordUsageFailure: suspend () -> Unit,
    private val appLogLogger: com.example.aiaccounting.logging.AppLogLogger? = null,
    private val timeoutMillis: Long = 720_000L  // 12分钟总预算，覆盖长请求的读取、重试与模型回退
) {
    private companion object {
        private const val USER_SAFE_TRANSPORT_FAILURE = "服务暂时不可用，请稍后重试。"
    }

    suspend fun collect(
        messages: List<ChatMessage>,
        config: AIConfig,
        traceId: String? = null
    ): RemoteStreamCollectionResult {
        appLogLogger?.info(
            source = "AI",
            category = "remote_request_start",
            message = "远端请求开始",
            details = "messageCount=${messages.size},provider=${config.provider.name},model=${config.model.ifBlank { "AUTO" }},timeoutMs=$timeoutMillis",
            traceId = traceId
        )
        return try {
            val result = withTimeoutOrNull(timeoutMillis) {
                sendChatStream(messages, config).toList().joinToString(separator = "")
            }

            if (result == null) {
                recordUsageFailure()
                appLogLogger?.warning(
                    source = "AI",
                    category = "remote_request_timeout",
                    message = "远端请求超时",
                    details = "messageCount=${messages.size},provider=${config.provider.name},model=${config.model.ifBlank { "AUTO" }},timeoutMs=$timeoutMillis",
                    traceId = traceId
                )
                return RemoteStreamCollectionResult.Timeout
            }

            appLogLogger?.info(
                source = "AI",
                category = "remote_request_finish",
                message = "远端请求完成",
                details = "responseLength=${result.length},provider=${config.provider.name},model=${config.model.ifBlank { "AUTO" }}",
                traceId = traceId
            )
            RemoteStreamCollectionResult.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordUsageFailure()
            appLogLogger?.error(
                source = "AI",
                category = "remote_request_transport_failure",
                message = "远端请求失败",
                details = "provider=${config.provider.name},model=${config.model.ifBlank { "AUTO" }},exception=${e::class.java.simpleName},message=${e.message}",
                traceId = traceId
            )
            RemoteStreamCollectionResult.Failure(USER_SAFE_TRANSPORT_FAILURE)
        }
    }
}
