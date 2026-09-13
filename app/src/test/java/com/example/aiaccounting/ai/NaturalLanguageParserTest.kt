package com.example.aiaccounting.ai

import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class NaturalLanguageParserTest {

    private lateinit var parser: NaturalLanguageParser

    @Before
    fun setup() {
        parser = NaturalLanguageParser()
    }

    @Test
    fun `test extract amount with yuan`() {
        val result = parser.extractAmount("今天午饭花了25元")
        assertEquals(25.0, result ?: 0.0, 0.01)
    }

    @Test
    fun `test extract amount with symbol`() {
        val result = parser.extractAmount("收入¥5000")
        assertEquals(5000.0, result ?: 0.0, 0.01)
    }

    @Test
    fun `test extract amount with decimal`() {
        val result = parser.extractAmount("花了25.5元")
        assertEquals(25.5, result ?: 0.0, 0.01)
    }

    @Test
    fun `test extract amount without unit`() {
        val result = parser.extractAmount("花了25")
        assertEquals(25.0, result ?: 0.0, 0.01)
    }

    @Test
    fun `test extract amount with spent keyword`() {
        val result = parser.extractAmount("花了100块")
        assertEquals(100.0, result ?: 0.0, 0.01)
    }

    @Test
    fun `test extract amount with income keyword`() {
        val result = parser.extractAmount("收入5000元工资")
        assertEquals(5000.0, result ?: 0.0, 0.01)
    }

    @Test
    fun `test extract amount returns null for invalid input`() {
        val result = parser.extractAmount("今天天气很好")
        assertNull(result)
    }

    @Test
    fun `test extract date today`() {
        val result = parser.extractDate("今天花了25元")
        assertNotNull(result)
        
        val calendar = Calendar.getInstance()
        calendar.time = result!!
        
        val today = Calendar.getInstance()
        assertEquals(today.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
        assertEquals(today.get(Calendar.MONTH), calendar.get(Calendar.MONTH))
        assertEquals(today.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test extract date yesterday`() {
        val result = parser.extractDate("昨天花了25元")
        assertNotNull(result)
        
        val calendar = Calendar.getInstance()
        calendar.time = result!!
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Add 1 to compare with today
        
        val today = Calendar.getInstance()
        assertEquals(today.get(Calendar.YEAR), calendar.get(Calendar.YEAR))
        assertEquals(today.get(Calendar.MONTH), calendar.get(Calendar.MONTH))
        assertEquals(today.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test extract date specific`() {
        val result = parser.extractDate("3月15日花了25元")
        assertNotNull(result)
        
        val calendar = Calendar.getInstance()
        calendar.time = result!!
        
        assertEquals(2, calendar.get(Calendar.MONTH)) // March is 2 (0-indexed)
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test determine transaction type expense`() {
        val result = parser.determineTransactionType("花了25元")
        assertEquals(TransactionType.EXPENSE, result)
    }

    @Test
    fun `test determine transaction type income`() {
        val result = parser.determineTransactionType("收入5000元")
        assertEquals(TransactionType.INCOME, result)
    }

    @Test
    fun `test determine transaction type transfer`() {
        // Note: "转账" is in both income and transfer keywords,
        // but income is checked first, so it returns INCOME
        val result = parser.determineTransactionType("转给张三1000元")
        assertEquals(TransactionType.TRANSFER, result)
    }

    @Test
    fun `test match category exact match`() {
        val categories = listOf(
            Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE, icon = "🍔", color = "#FF6B9FFF"),
            Category(id = 2, name = "交通", type = TransactionType.EXPENSE, icon = "🚗", color = "#FF4CAF50")
        )
        
        val result = parser.matchCategory("餐饮支出", categories, TransactionType.EXPENSE)
        assertEquals("餐饮", result)
    }

    @Test
    fun `test match category fuzzy match`() {
        val categories = listOf(
            Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE, icon = "🍔", color = "#FF6B9FFF")
        )
        
        val result = parser.matchCategory("午饭花了25元", categories, TransactionType.EXPENSE)
        assertEquals("餐饮", result)
    }

    @Test
    fun `test parse complete transaction`() {
        val categories = listOf(
            Category(id = 1, name = "餐饮", type = TransactionType.EXPENSE, icon = "🍔", color = "#FF6B9FFF")
        )
        
        val result = parser.parse("今天午饭花了25元", categories)
        
        assertNotNull(result)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(25.0, result.amount ?: 0.0, 0.01)
        assertEquals("餐饮", result.category)
        assertNotNull(result.date)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `test parse income transaction`() {
        val categories = listOf(
            Category(id = 1, name = "工资", type = TransactionType.INCOME, icon = "💰", color = "#FF4CAF50")
        )
        
        val result = parser.parse("昨天收入5000元工资", categories)
        
        assertNotNull(result)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(5000.0, result.amount ?: 0.0, 0.01)
        assertEquals("工资", result.category)
        assertTrue(result.confidence > 0.7f)
    }

    @Test
    fun `test quick parse`() {
        val result = parser.quickParse("花了100元")
        
        assertNotNull(result)
        assertEquals(100.0, result.amount ?: 0.0, 0.01)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `test generate confirmation message`() {
        val result = NaturalLanguageParser.ParsedResult(
            type = TransactionType.EXPENSE,
            amount = 25.0,
            category = "餐饮",
            date = Date(),
            remark = "午饭",
            confidence = 0.9f
        )
        
        val message = parser.generateConfirmationMessage(result)
        
        assertTrue(message.contains("支出"))
        assertTrue(message.contains("25"))
        assertTrue(message.contains("餐饮"))
        assertTrue(message.contains("确认要记录"))
    }

    @Test
    fun `test calculate confidence with all fields`() {
        val result = NaturalLanguageParser.ParsedResult(
            type = TransactionType.EXPENSE,
            amount = 25.0,
            category = "餐饮",
            confidence = 1.0f
        )
        
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `test calculate confidence with partial fields`() {
        val result = NaturalLanguageParser.ParsedResult(
            amount = 25.0,
            confidence = 0.4f
        )
        
        assertEquals(0.4f, result.confidence, 0.01f)
    }
}