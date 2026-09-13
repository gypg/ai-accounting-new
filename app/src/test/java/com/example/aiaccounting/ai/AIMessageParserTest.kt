package com.example.aiaccounting.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AIMessageParserTest {

    private val parser = AIMessageParser()

    @Test
    fun extractAmount_parsesYuan() {
        assertEquals(12.0, parser.extractAmount("午饭花了12元") ?: error("amount should not be null"), 0.0001)
    }

    @Test
    fun extractAmount_parsesDecimal() {
        assertEquals(12.5, parser.extractAmount("买咖啡 12.5") ?: error("amount should not be null"), 0.0001)
    }

    @Test
    fun extractAmount_returnsNullWhenMissing() {
        assertNull(parser.extractAmount("今天心情不错"))
    }

    @Test
    fun extractNote_prefersPatternMatch() {
        assertEquals("星巴克咖啡", parser.extractNote("买了星巴克咖啡") )
    }

    @Test
    fun extractLimit_parsesNumber() {
        assertEquals(10, parser.extractLimit("最近10笔交易"))
    }

    @Test
    fun extractDateRange_today_returnsNonNullStartEnd() {
        val (start, end) = parser.extractDateRange("今天支出")
        assertNotNull(start)
        assertNotNull(end)
    }
}
