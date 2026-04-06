package com.example.aiaccounting.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppNavigationRouteTest {

    @Test
    fun `add transaction route defaults to direct add`() {
        assertEquals(
            "add_transaction?entrySource=direct_add",
            Screen.AddTransaction.createRoute()
        )
    }

    @Test
    fun `add transaction route preserves provided entry source`() {
        assertEquals(
            "add_transaction?entrySource=overview_menu",
            Screen.AddTransaction.createRoute("overview_menu")
        )
    }

    @Test
    fun `monthly transactions route preserves year and month`() {
        assertEquals(
            "transactions_month/2026/4",
            Screen.MonthlyTransactions.createRoute(2026, 4)
        )
    }

    @Test
    fun `monthly transactions route appends day query when provided`() {
        assertEquals(
            "transactions_month/2026/4?day=6",
            Screen.MonthlyTransactions.createRoute(2026, 4, 6)
        )
    }

    @Test
    fun `monthly transactions route accepts yearly trend month navigation`() {
        assertEquals(
            "transactions_month/2026/11",
            Screen.MonthlyTransactions.createRoute(2026, 11, null)
        )
    }

    @Test
    fun `monthly transactions route rejects invalid month`() {
        assertThrows(IllegalArgumentException::class.java) {
            Screen.MonthlyTransactions.createRoute(2026, 13)
        }
    }

    @Test
    fun `monthly transactions route rejects invalid day`() {
        assertThrows(IllegalArgumentException::class.java) {
            Screen.MonthlyTransactions.createRoute(2026, 4, 32)
        }
    }
}
