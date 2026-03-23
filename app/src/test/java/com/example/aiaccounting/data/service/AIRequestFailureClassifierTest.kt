package com.example.aiaccounting.data.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class AIRequestFailureClassifierTest {

    private val classifier = AIRequestFailureClassifier()

    @Test
    fun classify_returnsTimeout_forSocketTimeoutException() {
        assertEquals(AIRequestFailureCategory.TIMEOUT, classifier.classify(SocketTimeoutException("timeout")))
    }

    @Test
    fun classify_returnsConnectFailure_forConnectException() {
        assertEquals(AIRequestFailureCategory.CONNECT_FAILURE, classifier.classify(ConnectException("connect failed")))
    }

    @Test
    fun classify_returnsDnsFailure_forUnknownHostException() {
        assertEquals(AIRequestFailureCategory.DNS_FAILURE, classifier.classify(UnknownHostException("unknown host")))
    }

    @Test
    fun classify_returnsSslFailure_forSslException() {
        assertEquals(AIRequestFailureCategory.SSL_FAILURE, classifier.classify(SSLException("ssl broken")))
    }

    @Test
    fun classifyMessage_returnsServerError_forHttp5xxMessage() {
        assertEquals(AIRequestFailureCategory.SERVER_ERROR, classifier.classifyMessage("API请求失败(503): upstream unavailable"))
    }

    @Test
    fun classifyMessage_returnsNotFound_forPlain404WithoutModelSignal() {
        assertEquals(AIRequestFailureCategory.NOT_FOUND, classifier.classifyMessage("API请求失败(404): route missing"))
    }

    @Test
    fun classifyMessage_returnsRateLimited_for429Message() {
        assertEquals(AIRequestFailureCategory.RATE_LIMITED, classifier.classifyMessage("429 too many requests"))
    }
}
