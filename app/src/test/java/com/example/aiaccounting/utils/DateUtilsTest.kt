package com.example.aiaccounting.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class DateUtilsTest {

    @Test
    fun `formatDate formats date correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15)
        val result = DateUtils.formatDate(calendar.time)
        assertEquals("2024-03-15", result)
    }

    @Test
    fun `formatTime formats time correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15, 14, 30)
        val result = DateUtils.formatTime(calendar.time)
        assertEquals("14:30", result)
    }

    @Test
    fun `formatDateTime formats date and time correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15, 14, 30)
        val result = DateUtils.formatDateTime(calendar.time)
        assertEquals("2024-03-15 14:30", result)
    }

    @Test
    fun `formatMonth formats month correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15)
        val result = DateUtils.formatMonth(calendar.time)
        assertEquals("2024-03", result)
    }

    @Test
    fun `parseDate parses valid date string`() {
        val result = DateUtils.parseDate("2024-03-15")
        assertNotNull(result)
        val calendar = Calendar.getInstance()
        calendar.time = result!!
        assertEquals(2024, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH))
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parseDate returns null for invalid string`() {
        val result = DateUtils.parseDate("invalid-date")
        assertNull(result)
    }

    @Test
    fun `getStartOfMonth returns first day of month`() {
        val result = DateUtils.getStartOfMonth(2024, 3)
        val calendar = Calendar.getInstance()
        calendar.time = result
        assertEquals(2024, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
    }

    @Test
    fun `getEndOfMonth returns last moment of month`() {
        val result = DateUtils.getEndOfMonth(2024, 3)
        val calendar = Calendar.getInstance()
        calendar.time = result
        assertEquals(2024, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, calendar.get(Calendar.MONTH))
        assertEquals(31, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calendar.get(Calendar.MINUTE))
        assertEquals(59, calendar.get(Calendar.SECOND))
    }

    @Test
    fun `getStartOfDay returns start of day`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15, 14, 30, 45)
        val result = DateUtils.getStartOfDay(calendar.time)
        val resultCalendar = Calendar.getInstance()
        resultCalendar.time = result
        assertEquals(0, resultCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCalendar.get(Calendar.MINUTE))
        assertEquals(0, resultCalendar.get(Calendar.SECOND))
        assertEquals(0, resultCalendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getEndOfDay returns end of day`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15, 14, 30, 45)
        val result = DateUtils.getEndOfDay(calendar.time)
        val resultCalendar = Calendar.getInstance()
        resultCalendar.time = result
        assertEquals(23, resultCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, resultCalendar.get(Calendar.MINUTE))
        assertEquals(59, resultCalendar.get(Calendar.SECOND))
        assertEquals(999, resultCalendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `formatDateShort formats timestamp correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15)
        val result = DateUtils.formatDateShort(calendar.time.time)
        assertEquals("03-15", result)
    }

    @Test
    fun `formatDateTime with timestamp formats correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 15, 14, 30)
        val result = DateUtils.formatDateTime(calendar.time.time)
        assertEquals("2024-03-15 14:30", result)
    }
}
