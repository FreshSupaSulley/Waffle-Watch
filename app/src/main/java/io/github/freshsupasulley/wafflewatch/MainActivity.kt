package io.github.freshsupasulley.wafflewatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import io.github.freshsupasulley.wafflewatch.model.WaffleHouseLocation
import io.github.freshsupasulley.wafflewatch.model.parseLocations
import io.github.freshsupasulley.wafflewatch.ui.MapScreen
import io.github.freshsupasulley.wafflewatch.ui.theme.WaffleWatchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        enableEdgeToEdge()
        setContent {
            WaffleWatchTheme {
                WaffleWatchApp()
            }
        }
    }
}

@Composable
fun WaffleWatchApp() {
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var locations by remember { mutableStateOf<List<WaffleHouseLocation>>(emptyList()) }

    LaunchedEffect(Unit) {
        locations = withContext(Dispatchers.IO) {
            //  TODO: Change this from using sample_data to using dynamic data
            val json = context.assets.open("sample_data.json").bufferedReader().readText()
            parseLocations(json)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                )
            }
        },
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> MapScreen(locations)
            AppDestinations.FAVORITES -> Text("Favorites (coming soon)")
            AppDestinations.PROFILE -> Text("Profile (coming soon)")
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}
