package io.github.freshsupasulley.wafflewatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class IntroScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun introScreen_displaysWelcomeMessage() {
        composeTestRule.setContent {
            IntroScreen(onGetStarted = {})
        }

        composeTestRule.onNodeWithText("Welcome to Waffle Watch").assertIsDisplayed()
    }

    @Test
    fun introScreen_displaysGetStartedButton() {
        composeTestRule.setContent {
            IntroScreen(onGetStarted = {})
        }

        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
    }

    @Test
    fun introScreen_clickGetStarted_callsCallback() {
        var called = false
        composeTestRule.setContent {
            IntroScreen(onGetStarted = { called = true })
        }

        composeTestRule.onNodeWithText("Get Started").performClick()
        assert(called)
    }
}
