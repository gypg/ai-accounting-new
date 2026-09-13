package com.example.aiaccounting.utils

import java.text.DecimalFormat

object NumberUtils {
    private val decimalFormat = DecimalFormat("#,##0.00")
    private val simpleFormat = DecimalFormat("#0.00")
    
    fun formatMoney(amount: Double): String {
        return "¥${decimalFormat.format(amount)}"
    }
    
    fun formatSimple(amount: Double): String {
        return simpleFormat.format(amount)
    }
    
    fun parseMoney(moneyString: String): Double? {
        return try {
            val cleanString = moneyString.replace("[¥,\\s]".toRegex(), "")
            cleanString.toDouble()
        } catch (e: Exception) {
            null
        }
    }
}
