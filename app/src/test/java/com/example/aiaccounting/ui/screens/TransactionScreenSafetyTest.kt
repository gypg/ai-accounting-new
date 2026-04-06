package com.example.aiaccounting.ui.screens

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.FieldPosition
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionScreenSafetyTest {

    @Test
    fun `freshCategoryFallbackColor normalizes negative category id`() {
        val colors = listOf(Color.Red, Color.Green, Color.Blue)

        val result = freshCategoryFallbackColor(categoryId = -1L, colors = colors)

        assertEquals(Color.Blue, result)
    }

    @Test
    fun `safeTransactionNote falls back for blank text`() {
        assertEquals("未备注", safeTransactionNote("   "))
    }

    @Test
    fun `safeTransactionAmountText falls back for non finite amount`() {
        val result = safeTransactionAmountText(Double.NaN)

        assertEquals("0.00", result)
    }

    @Test
    fun `safeTransactionDateText falls back when formatter throws`() {
        val throwingFormat = object : SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) {
            override fun format(date: Date, toAppendTo: StringBuffer, fieldPosition: FieldPosition): StringBuffer {
                throw IllegalStateException("boom")
            }

            override fun parse(source: String, pos: ParsePosition): Date? {
                return null
            }
        }

        val result = safeTransactionDateText(0L, throwingFormat)

        assertEquals("时间异常", result)
    }

    @Test
    fun `transactionSourceBadgeLabel returns null for manual transaction`() {
        assertNull(transactionSourceBadgeLabel("MANUAL"))
    }

    @Test
    fun `transactionSourceBadgeLabel returns remote badge for ai remote`() {
        val badge = transactionSourceBadgeLabel("AI_REMOTE")

        assertEquals("AI云端", badge)
    }
}
