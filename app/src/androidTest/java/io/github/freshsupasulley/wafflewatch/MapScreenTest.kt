package io.github.freshsupasulley.wafflewatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import io.github.freshsupasulley.wafflewatch.model.LocationStatus
import io.github.freshsupasulley.wafflewatch.model.WaffleHouseLocation
import io.github.freshsupasulley.wafflewatch.ui.MapScreen
import io.github.freshsupasulley.wafflewatch.ui.theme.WaffleWatchTheme
import org.junit.Rule
import org.junit.Test
import org.maplibre.android.MapLibre
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun MapScreen_renders_with_locations_without_crashing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // MapLibre must be initialized on the UI thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            MapLibre.getInstance(context)
        }

        val testLocations = listOf(
            WaffleHouseLocation("100", "Waffle House #100", "123 Main St, Atlanta, GA", 33.749, -84.388, LocationStatus.GREEN, "24 hours"),
            WaffleHouseLocation("101", "Waffle House #101", "456 Oak Ave, Nashville, TN", 36.162, -86.781, LocationStatus.RED, "24 hours"),
        )
        
        val testFeatures = testLocations.map { loc ->
            val props = JsonObject()
            props.addProperty("locationId", loc.locationId)
            Feature.fromGeometry(Point.fromLngLat(loc.longitude, loc.latitude), props)
        }

        composeTestRule.setContent {
            WaffleWatchTheme {
                MapScreen(
                    locations = testLocations,
                    features = testFeatures,
                    timestamp = System.currentTimeMillis(),
                    onRefresh = { },
                )
            }
        }

        composeTestRule.onNodeWithText("All").assertIsDisplayed()
    }
}
