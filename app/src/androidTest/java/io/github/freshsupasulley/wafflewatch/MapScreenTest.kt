package io.github.freshsupasulley.wafflewatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import io.github.freshsupasulley.wafflewatch.model.LocationStatus
import io.github.freshsupasulley.wafflewatch.model.WaffleHouseLocation
import io.github.freshsupasulley.wafflewatch.ui.MapScreen
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.MapLibre

class MapScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun MapScreen_renders_with_locations_without_crashing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MapLibre.getInstance(context)
        val testLocations = listOf(
            WaffleHouseLocation("100", "Waffle House #100", "123 Main St, Atlanta, GA", 33.749, -84.388, LocationStatus.GREEN, "24 hours"),
            WaffleHouseLocation("101", "Waffle House #101", "456 Oak Ave, Nashville, TN", 36.162, -86.781, LocationStatus.RED, "24 hours"),
        )

        composeTestRule.setContent {
            MapScreen(
                locations = testLocations,
                onRefresh = { testLocations },
            )
        }

        composeTestRule.onNodeWithText("All").assertIsDisplayed()
    }
}
