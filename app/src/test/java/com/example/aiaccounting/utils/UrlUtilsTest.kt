package com.example.aiaccounting.utils

import org.junit.Assert.*
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `normalizeBase removes trailing slash`() {
        val result = UrlUtils.normalizeBase("https://api.example.com/")
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `normalizeBase keeps url without trailing slash`() {
        val result = UrlUtils.normalizeBase("https://api.example.com")
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `normalizeBase trims whitespace`() {
        val result = UrlUtils.normalizeBase("  https://api.example.com/  ")
        assertEquals("https://api.example.com", result)
    }

    @Test
    fun `join combines base and path`() {
        val result = UrlUtils.join("https://api.example.com", "v1/chat")
        assertEquals("https://api.example.com/v1/chat", result)
    }

    @Test
    fun `join handles base with trailing slash`() {
        val result = UrlUtils.join("https://api.example.com/", "v1/chat")
        assertEquals("https://api.example.com/v1/chat", result)
    }

    @Test
    fun `join handles path with leading slash`() {
        val result = UrlUtils.join("https://api.example.com", "/v1/chat")
        assertEquals("https://api.example.com/v1/chat", result)
    }

    @Test
    fun `join handles both slashes`() {
        val result = UrlUtils.join("https://api.example.com/", "/v1/chat")
        assertEquals("https://api.example.com/v1/chat", result)
    }

    @Test
    fun `join trims whitespace`() {
        val result = UrlUtils.join("  https://api.example.com/  ", "  v1/chat  ")
        assertEquals("https://api.example.com/v1/chat", result)
    }
}
