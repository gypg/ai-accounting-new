package com.example.aiaccounting.data.service

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal enum class AIRequestFailureCategory {
    INVALID_API_KEY,
    FORBIDDEN,
    NOT_FOUND,
    RATE_LIMITED,
    SERVER_ERROR,
    DNS_FAILURE,
    CONNECT_FAILURE,
    SSL_FAILURE,
    TIMEOUT,
    OTHER
}

internal class AIRequestFailureClassifier {

    fun classify(throwable: Throwable): AIRequestFailureCategory {
        return when (throwable) {
            is SocketTimeoutException -> AIRequestFailureCategory.TIMEOUT
            is ConnectException -> AIRequestFailureCategory.CONNECT_FAILURE
            is UnknownHostException -> AIRequestFailureCategory.DNS_FAILURE
            is SSLException -> AIRequestFailureCategory.SSL_FAILURE
            else -> classifyMessage(throwable.message.orEmpty())
        }
    }

    fun classifyMessage(message: String): AIRequestFailureCategory {
        val normalized = message.lowercase()
        return when {
            normalized.contains("401") -> AIRequestFailureCategory.INVALID_API_KEY
            normalized.contains("403") -> AIRequestFailureCategory.FORBIDDEN
            normalized.contains("404") -> AIRequestFailureCategory.NOT_FOUND
            normalized.contains("429") -> AIRequestFailureCategory.RATE_LIMITED
            normalized.contains("500") || normalized.contains("502") || normalized.contains("503") -> AIRequestFailureCategory.SERVER_ERROR
            normalized.contains("unknownhostexception") || normalized.contains("无法解析主机") -> AIRequestFailureCategory.DNS_FAILURE
            normalized.contains("connectexception") || normalized.contains("连接失败") -> AIRequestFailureCategory.CONNECT_FAILURE
            normalized.contains("ssl") || normalized.contains("证书") -> AIRequestFailureCategory.SSL_FAILURE
            normalized.contains("timeout") || normalized.contains("超时") -> AIRequestFailureCategory.TIMEOUT
            else -> AIRequestFailureCategory.OTHER
        }
    }
}
