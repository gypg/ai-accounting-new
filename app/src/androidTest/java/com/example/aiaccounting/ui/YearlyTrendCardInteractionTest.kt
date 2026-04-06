package com.example.aiaccounting.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.aiaccounting.ui.components.charts.MonthlyData
import com.example.aiaccounting.ui.screens.YearlyTrendCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class YearlyTrendCardInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tapOnTrendPointCallsOnMonthClick() {
        var clickedMonth: Int? = null
        var cardClicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                YearlyTrendCard(
                    year = 2026,
                    monthlyData = listOf(
                        MonthlyData(month = "1月", income = 100.0, expense = 50.0),
                        MonthlyData(month = "2月", income = 200.0, expense = 80.0),
                        MonthlyData(month = "3月", income = 300.0, expense = 120.0)
                    ),
                    onClick = { cardClicks++ },
                    onMonthClick = { clickedMonth = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("trend_chart_click_layer")
            .assertExists()
            .performTouchInput {
                click()
            }

        composeTestRule.runOnIdle {
            assertEquals(2, clickedMonth)
            assertEquals(0, cardClicks)
        }
    }

    @Test
    fun tapOnCardHeaderCallsCardClickWithoutMonthClick() {
        var clickedMonth: Int? = null
        var cardClicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                YearlyTrendCard(
                    year = 2026,
                    monthlyData = listOf(
                        MonthlyData(month = "1月", income = 100.0, expense = 50.0),
                        MonthlyData(month = "2月", income = 200.0, expense = 80.0),
                        MonthlyData(month = "3月", income = 300.0, expense = 120.0)
                    ),
                    onClick = { cardClicks++ },
                    onMonthClick = { clickedMonth = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("yearly_trend_card")
            .assertExists()
            .performTouchInput {
                click(Offset(30f, 30f))
            }

        composeTestRule.runOnIdle {
            assertEquals(null, clickedMonth)
            assertEquals(1, cardClicks)
        }
    }
}