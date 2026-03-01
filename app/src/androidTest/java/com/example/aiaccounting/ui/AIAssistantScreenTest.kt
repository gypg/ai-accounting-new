package com.example.aiaccounting.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.aiaccounting.ai.AIAssistantViewModel
import com.example.aiaccounting.ai.ParsedTransaction
import com.example.aiaccounting.data.TransactionType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@MediumTest
class AIAssistantScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testAIAssistantScreenComponents() {
        composeTestRule.setContent {
            AIAssistantScreen(
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("AI记账助手").assertIsDisplayed()
        composeTestRule.onNodeWithText("试试这样说...").assertIsDisplayed()
        composeTestRule.onNodeWithText("今天午饭花了35元").assertIsDisplayed()
        composeTestRule.onNodeWithText("发送").assertIsDisplayed()
    }

    @Test
    fun testQuickActionButtons() {
        composeTestRule.setContent {
            AIAssistantScreen(
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("今天午饭花了35元").assertIsDisplayed()
        composeTestRule.onNodeWithText("打车回家20块").assertIsDisplayed()
        composeTestRule.onNodeWithText("发工资5000元").assertIsDisplayed()
        composeTestRule.onNodeWithText("买咖啡15元").assertIsDisplayed()
    }

    @Test
    fun testInputFieldAndSendButton() {
        composeTestRule.setContent {
            AIAssistantScreen(
                onNavigateBack = {}
            )
        }

        val inputField = composeTestRule.onNodeWithContentDescription("输入记账内容")
        inputField.assertIsDisplayed()
        inputField.performTextInput("今天花了50元")
        
        composeTestRule.onNodeWithText("发送").assertIsEnabled()
    }

    @Test
    fun testQuickActionClick() {
        composeTestRule.setContent {
            AIAssistantScreen(
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("今天午饭花了35元").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("发送").assertIsEnabled()
    }
}

// Preview test for ParsedTransactionDialog
class ParsedTransactionDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testParsedTransactionDialog() {
        val parsedTransaction = ParsedTransaction(
            amount = 35.0,
            type = TransactionType.EXPENSE,
            category = "餐饮",
            subCategory = "午餐",
            date = java.time.LocalDateTime.now(),
            note = "今天午饭花了35元",
            confidence = 0.95,
            originalText = "今天午饭花了35元"
        )

        composeTestRule.setContent {
            ParsedTransactionDialog(
                parsedTransaction = parsedTransaction,
                onConfirm = {},
                onDismiss = {},
                onEdit = {}
            )
        }

        composeTestRule.onNodeWithText("确认记账").assertIsDisplayed()
        composeTestRule.onNodeWithText("金额: ¥35.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("类型: 支出").assertIsDisplayed()
        composeTestRule.onNodeWithText("分类: 餐饮").assertIsDisplayed()
        composeTestRule.onNodeWithText("确认").assertIsDisplayed()
        composeTestRule.onNodeWithText("编辑").assertIsDisplayed()
        composeTestRule.onNodeWithText("取消").assertIsDisplayed()
    }

    @Test
    fun testLowConfidenceDialog() {
        val parsedTransaction = ParsedTransaction(
            amount = 100.0,
            type = TransactionType.EXPENSE,
            category = "其他",
            confidence = 0.5,
            originalText = "花了100"
        )

        composeTestRule.setContent {
            ParsedTransactionDialog(
                parsedTransaction = parsedTransaction,
                onConfirm = {},
                onDismiss = {},
                onEdit = {}
            )
        }

        composeTestRule.onNodeWithText("置信度较低").assertIsDisplayed()
        composeTestRule.onNodeWithText("建议编辑确认").assertIsDisplayed()
    }
}
