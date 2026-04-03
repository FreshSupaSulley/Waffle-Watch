package io.github.freshsupasulley.wafflewatch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.freshsupasulley.wafflewatch.ui.MapScreen
import io.github.freshsupasulley.wafflewatch.ui.theme.WaffleWatchTheme
import kotlinx.coroutines.launch
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
fun WaffleWatchApp(viewModel: LocationViewModel = viewModel()) {
    var showIntro by rememberSaveable { mutableStateOf(true) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val locations by viewModel.locations.collectAsState()
    val timestamp by viewModel.timestamp.collectAsState()
    val fetchError by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val shakeDetector = ShakeDetector {
            scope.launch {
                snackbarHostState.showSnackbar("Refreshing locations...")
            }
            viewModel.loadLocations()
        }

        sensorManager.registerListener(
            shakeDetector,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager.unregisterListener(shakeDetector)
        }
    }

    LaunchedEffect(fetchError) {
        val error = fetchError ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Failed to load locations: $error",
            actionLabel = "Retry",
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.loadLocations()
        }
    }

    if (showIntro) {
        IntroScreen(onGetStarted = { showIntro = false })
    } else {
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
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentDestination) {
                    AppDestinations.HOME -> MapScreen(
                        locations = locations,
                        timestamp = timestamp,
                        onRefresh = {
                            viewModel.clearError()
                            viewModel.loadLocations()
                        },
                    )
                    AppDestinations.INTRO -> IntroScreen(onGetStarted = {
                        currentDestination = AppDestinations.HOME
                    })
//                    AppDestinations.FAVORITES -> Text("Favorites (coming soon)")
//                    AppDestinations.PROFILE -> Text("Profile (coming soon)")
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    INTRO("About", Icons.Default.Info),
//    FAVORITES("Favorites", Icons.Default.Favorite),
//    PROFILE("Profile", Icons.Default.AccountBox),
}
