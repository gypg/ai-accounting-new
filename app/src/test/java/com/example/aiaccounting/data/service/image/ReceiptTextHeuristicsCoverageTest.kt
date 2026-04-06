package com.example.aiaccounting.data.service.image

import com.example.aiaccounting.data.service.ImageProcessingService
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptTextHeuristicsCoverageTest {

    @Test
    fun extractKeyLines_keeps_long_receipt_line_coverage() {
        val text = listOf(
            "2026年4月1日（周三）",
            "-6.00 餐饮（早餐：煎饼果子）——现金",
            "-38.00 餐饮（午餐：日式定食）——微信",
            "-9.90 购物（淘宝：愚人节整蛊玩具）——支付宝",
            "-25.00 娱乐（腾讯视频会员续费）——微信",
            "-12.00 交通（地铁充值）——自动售票机（现金）",
            "+188.00 红包（愚人节朋友转账\"骗\"回来的钱）——微信转账",
            "+5000.00 工资（4月预发部分）——银行代发"
        ).joinToString("\n")

        val keyLines = ReceiptTextHeuristics.extractKeyLines(text)

        assertTrue(keyLines.any { it.contains("-6.00") })
        assertTrue(keyLines.any { it.contains("-38.00") })
        assertTrue(keyLines.any { it.contains("-9.90") })
        assertTrue(keyLines.any { it.contains("-25.00") })
        assertTrue(keyLines.any { it.contains("-12.00") })
        assertTrue(keyLines.any { it.contains("+188.00") })
        assertTrue(keyLines.any { it.contains("+5000.00") })
    }

    @Test
    fun extractReceiptSignals_keeps_more_amount_candidates_for_long_receipt() {
        val text = listOf(
            "6.00", "38.00", "9.90", "25.00", "12.00", "188.00", "5000.00"
        ).joinToString("\n")

        val signals = ReceiptTextHeuristics.extractReceiptSignals(text)

        assertTrue(signals.amounts.size >= 7)
        assertTrue(signals.amounts.contains("5000.00"))
    }
}
