package com.example.aiaccounting.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverviewTrendNavigationTest {

    @Test
    fun `parseTrendMonth parses valid month labels`() {
        assertEquals(1, parseTrendMonth("1月"))
        assertEquals(12, parseTrendMonth("12月"))
        assertEquals(3, parseTrendMonth(" 3月 "))
    }

    @Test
    fun `parseTrendMonth rejects invalid month labels`() {
        assertNull(parseTrendMonth(""))
        assertNull(parseTrendMonth("abc"))
        assertNull(parseTrendMonth("0月"))
        assertNull(parseTrendMonth("13月"))
    }
}