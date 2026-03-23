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
    private val timeoutMillis: Long = 60_000L
) {

    suspend fun collect(messages: List<ChatMessage>, config: AIConfig): RemoteStreamCollectionResult {
        return try {
            val result = withTimeoutOrNull(timeoutMillis) {
                sendChatStream(messages, config).toList().joinToString(separator = "")
            }

            if (result == null) {
                recordUsageFailure()
                return RemoteStreamCollectionResult.Timeout
            }

            RemoteStreamCollectionResult.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordUsageFailure()
            RemoteStreamCollectionResult.Failure(e.message ?: "未知错误")
        }
    }
}
