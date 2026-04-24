package io.github.freshsupasulley.wafflewatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.freshsupasulley.wafflewatch.ui.NoInternetScreen
import org.junit.Rule
import org.junit.Test

class NoInternetScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noInternetScreen_displaysMessage() {
        composeTestRule.setContent {
            NoInternetScreen(onRetry = {})
        }

        composeTestRule.onNodeWithText("No internet connection. Please connect to the internet.").assertIsDisplayed()
    }

    @Test
    fun noInternetScreen_displaysRetryButton() {
        composeTestRule.setContent {
            NoInternetScreen(onRetry = {})
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun noInternetScreen_clickRetry_triggersCallback() {
        var retryClicked = false
        composeTestRule.setContent {
            NoInternetScreen(onRetry = { retryClicked = true })
        }

        composeTestRule.onNodeWithText("Retry").performClick()
        assert(retryClicked)
    }
}
