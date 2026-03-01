package com.example.aiaccounting.utils

import org.junit.Assert.*
import org.junit.Test

class NumberUtilsTest {

    @Test
    fun `test format money with positive value`() {
        val result = NumberUtils.formatMoney(100.50)
        assertEquals("¥100.50", result)
    }

    @Test
    fun `test format money with zero`() {
        val result = NumberUtils.formatMoney(0.0)
        assertEquals("¥0.00", result)
    }

    @Test
    fun `test format money with negative value`() {
        val result = NumberUtils.formatMoney(-50.25)
        assertEquals("¥-50.25", result)
    }

    @Test
    fun `test format simple with decimal`() {
        val result = NumberUtils.formatSimple(123.456)
        assertEquals("123.46", result)
    }

    @Test
    fun `test parse money with symbol`() {
        val result = NumberUtils.parseMoney("¥1,234.56")
        assertEquals(1234.56, result, 0.01)
    }

    @Test
    fun `test parse money with spaces`() {
        val result = NumberUtils.parseMoney("¥ 1 234.56")
        assertEquals(1234.56, result, 0.01)
    }

    @Test
    fun `test parse money returns null for invalid input`() {
        val result = NumberUtils.parseMoney("abc")
        assertNull(result)
    }

    @Test
    fun `test format and parse round trip`() {
        val original = 1234.56
        val formatted = NumberUtils.formatMoney(original)
        val parsed = NumberUtils.parseMoney(formatted)
        assertEquals(original, parsed, 0.01)
    }
}