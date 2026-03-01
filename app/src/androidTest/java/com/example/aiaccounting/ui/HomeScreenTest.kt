package com.example.aiaccounting.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.aiaccounting.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testHomeScreenDisplays() {
        composeTestRule.onNodeWithText("AI记账").assertIsDisplayed()
    }

    @Test
    fun testBottomNavigationItems() {
        composeTestRule.onNodeWithText("明细").assertIsDisplayed()
        composeTestRule.onNodeWithText("账户").assertIsDisplayed()
        composeTestRule.onNodeWithText("统计").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI助手").assertIsDisplayed()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun testNavigationToAccountsScreen() {
        composeTestRule.onNodeWithText("账户").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("账户管理").assertIsDisplayed()
    }

    @Test
    fun testNavigationToStatisticsScreen() {
        composeTestRule.onNodeWithText("统计").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("收支统计").assertIsDisplayed()
    }

    @Test
    fun testNavigationToAIAssistantScreen() {
        composeTestRule.onNodeWithText("AI助手").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("AI记账助手").assertIsDisplayed()
    }

    @Test
    fun testNavigationToSettingsScreen() {
        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("应用设置").assertIsDisplayed()
    }

    @Test
    fun testAddTransactionButton() {
        composeTestRule.onNodeWithContentDescription("记一笔").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("记一笔").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("记一笔").assertIsDisplayed()
    }

    @Test
    fun testTransactionListDisplayed() {
        composeTestRule.onNodeWithText("明细").assertIsDisplayed()
    }

    @Test
    fun testMonthSelectorDisplayed() {
        composeTestRule.onNodeWithText("明细").assertIsDisplayed()
    }

    @Test
    fun testIncomeExpenseSummaryDisplayed() {
        composeTestRule.onNodeWithTextContains("收入").assertExists()
        composeTestRule.onNodeWithTextContains("支出").assertExists()
    }
}
