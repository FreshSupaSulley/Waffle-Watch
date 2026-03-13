package io.github.freshsupasulley.wafflewatch.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonObject
import io.github.freshsupasulley.wafflewatch.model.LocationStatus
import io.github.freshsupasulley.wafflewatch.model.WaffleHouseLocation
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private fun WaffleHouseLocation.toFeature(): Feature {
    val props = JsonObject()
    props.addProperty("locationId", locationId)
    props.addProperty("name", name)
    props.addProperty("address", address)
    props.addProperty("status", status.name)
    props.addProperty("formattedHours", formattedHours)
    return Feature.fromGeometry(Point.fromLngLat(longitude, latitude), props)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    locations: List<WaffleHouseLocation>,
    timestamp: Long?,
    onRefresh: suspend () -> Unit,
) {
    val context = LocalContext.current
    var displayedLocations by remember { mutableStateOf(locations) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var activeFilters by remember { mutableStateOf(LocationStatus.entries.toSet()) }
    var selectedLocation by remember { mutableStateOf<WaffleHouseLocation?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapStyle by remember { mutableStateOf<Style?>(null) }
    val allFeatures = remember(displayedLocations) { displayedLocations.map { it.toFeature() } }

    // Sync external prop → local state on initial/external change
    LaunchedEffect(locations) { displayedLocations = locations }

    // Push new features to GeoJSON source after refresh
    LaunchedEffect(allFeatures, mapStyle) {
        val style = mapStyle ?: return@LaunchedEffect
        (style.getSource("wh-source") as? GeoJsonSource)
            ?.setGeoJson(FeatureCollection.fromFeatures(allFeatures))
    }

    // Toggle layer visibility when filters change
    LaunchedEffect(activeFilters, mapStyle) {
        val style = mapStyle ?: return@LaunchedEffect
        listOf(
            "wh-layer-green" to LocationStatus.GREEN,
            "wh-layer-yellow" to LocationStatus.YELLOW,
            "wh-layer-red" to LocationStatus.RED,
        ).forEach { (layerId, status) ->
            style.getLayer(layerId)?.setProperties(
                PropertyFactory.visibility(
                    if (status in activeFilters) Property.VISIBLE else Property.NONE
                )
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                try {
                    onRefresh()
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        MapLibreMapView(
            modifier = Modifier.fillMaxSize(),
            allFeatures = allFeatures,
            locationsById = remember(displayedLocations) { displayedLocations.associateBy { it.locationId } },
            onMapReady = { map, style ->
                mapLibreMap = map
                mapStyle = style
            },
            onLocationSelected = { selectedLocation = it },
        )

        // Filter chips overlay
        LazyRow(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                val allSelected = activeFilters.size == LocationStatus.entries.size
                FilterChip(
                    selected = allSelected,
                    onClick = {
                        activeFilters = if (allSelected) emptySet() else LocationStatus.entries.toSet()
                    },
                    label = { Text("All") },
                )
            }
            items(LocationStatus.entries) { status ->
                FilterChip(
                    selected = status in activeFilters,
                    onClick = {
                        activeFilters = if (status in activeFilters) {
                            activeFilters - status
                        } else {
                            activeFilters + status
                        }
                    },
                    label = {
                        Text(status.name.lowercase().replaceFirstChar { it.uppercase() })
                    },
                    leadingIcon = {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(10.dp)
                                .background(
                                    color = when (status) {
                                        LocationStatus.GREEN -> Color(0xFF4CAF50)
                                        LocationStatus.YELLOW -> Color(0xFFFFC107)
                                        LocationStatus.RED -> Color(0xFFF44336)
                                    },
                                    shape = CircleShape,
                                )
                        )
                    },
                )
            }
        }

        // Locate Me FAB
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                val map = mapLibreMap ?: return@rememberLauncherForActivityResult
                val style = mapStyle ?: return@rememberLauncherForActivityResult
                enableLocationComponent(map, style, context)
            }
        }

        FloatingActionButton(
            onClick = {
                val map = mapLibreMap ?: return@FloatingActionButton
                val style = mapStyle ?: return@FloatingActionButton
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    enableLocationComponent(map, style, context)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Locate Me")
        }
    }

    // Location detail bottom sheet (outside PullToRefreshBox)
    selectedLocation?.let { location ->
        ModalBottomSheet(onDismissRequest = { selectedLocation = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val statusColor = when (location.status) {
                    LocationStatus.GREEN -> Color(0xFF4CAF50)
                    LocationStatus.YELLOW -> Color(0xFFFFC107)
                    LocationStatus.RED -> Color(0xFFF44336)
                }
                val statusLabel = when (location.status) {
                    LocationStatus.GREEN -> "Open"
                    LocationStatus.YELLOW -> "Limited"
                    LocationStatus.RED -> "Closed"
                }
                Surface(shape = RoundedCornerShape(4.dp), color = statusColor) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                val hoursParts = location.formattedHours.split("|", limit = 2)
                val hoursText = hoursParts.joinToString(" · ") { it.trim() }
                Text(hoursText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val refreshedText = timestamp?.let {
                    val fmt = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
                    "Last refreshed: ${fmt.format(Date(it))}"
                } ?: "Last refreshed: unknown"
                Text(
                    text = refreshedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun enableLocationComponent(map: MapLibreMap, style: Style, context: android.content.Context) {
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return
    try {
        map.locationComponent.apply {
            if (!isLocationComponentActivated) {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style).build()
                )
            }
            isLocationComponentEnabled = true
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }
    } catch (_: SecurityException) {
        // Permission was revoked between check and use — no-op
    }
}

@Composable
private fun MapLibreMapView(
    modifier: Modifier = Modifier,
    allFeatures: List<Feature>,
    locationsById: Map<String, WaffleHouseLocation>,
    onMapReady: (MapLibreMap, Style) -> Unit,
    onLocationSelected: (WaffleHouseLocation?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val onMapReadyRef = rememberUpdatedState(onMapReady)
    val onLocationSelectedRef = rememberUpdatedState(onLocationSelected)
    val locationsByIdRef = rememberUpdatedState(locationsById)

    val mapView = remember {
        MapView(context, MapLibreMapOptions.createFromAttributes(context))
    }

    AndroidView(factory = { mapView }, modifier = modifier)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
                // Modern color scheme
                (style.getLayer("background") as? BackgroundLayer)?.setProperties(
                    PropertyFactory.backgroundColor("#C8DCE8") // muted ocean blue
                )
                // Hide base style layers we replace ourselves
                listOf("countries-fill", "coastline", "countries-boundary", "countries-label",
                       "geolines", "geolines-label", "crimea-fill").forEach { id ->
                    style.getLayer(id)?.setProperties(PropertyFactory.visibility(Property.NONE))
                }

                // State polygon source
                style.addSource(
                    GeoJsonSource(
                        "us-states-source",
                        URI("https://raw.githubusercontent.com/PublicaMundi/MappingAPI/master/data/geojson/us-states.json")
                    )
                )
                // US land fill — continental states only (no Alaska or Hawaii)
                style.addLayer(
                    FillLayer("us-states-fill", "us-states-source").withProperties(
                        PropertyFactory.fillColor("#F0EAD6"),
                    ).withFilter(
                        Expression.not(Expression.any(
                            Expression.eq(Expression.get("name"), Expression.literal("Alaska")),
                            Expression.eq(Expression.get("name"), Expression.literal("Hawaii")),
                        ))
                    )
                )
                style.addLayer(
                    LineLayer("us-states-layer", "us-states-source").withProperties(
                        PropertyFactory.lineColor("#888888"),
                        PropertyFactory.lineWidth(1.0f),
                        PropertyFactory.lineOpacity(0.7f),
                    ).withFilter(
                        Expression.not(Expression.any(
                            Expression.eq(Expression.get("name"), Expression.literal("Alaska")),
                            Expression.eq(Expression.get("name"), Expression.literal("Hawaii")),
                        ))
                    )
                )
                style.addSource(
                    GeoJsonSource("us-centroids", URI("asset://us_state_centroids.geojson"))
                )
                style.addLayer(
                    SymbolLayer("us-states-labels", "us-centroids").withProperties(
                        PropertyFactory.textField(Expression.get("name")),
                        PropertyFactory.textSize(11f),
                        PropertyFactory.textColor("#555555"),
                        PropertyFactory.textHaloColor("#FFFFFF"),
                        PropertyFactory.textHaloWidth(1.5f),
                        PropertyFactory.textAllowOverlap(false),
                    )
                )

                // Add unified GeoJSON source
                style.addSource(
                    GeoJsonSource("wh-source", FeatureCollection.fromFeatures(allFeatures))
                )

                // One circle layer per status (makes visibility toggling simple)
                listOf(
                    Triple("wh-layer-green", "GREEN", "#4CAF50"),
                    Triple("wh-layer-yellow", "YELLOW", "#FFC107"),
                    Triple("wh-layer-red", "RED", "#F44336"),
                ).forEach { (layerId, statusName, color) ->
                    style.addLayer(
                        CircleLayer(layerId, "wh-source")
                            .withProperties(
                                PropertyFactory.circleRadius(8f),
                                PropertyFactory.circleColor(color),
                                PropertyFactory.circleStrokeWidth(2f),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                                PropertyFactory.circleOpacity(0.9f),
                            )
                            .withFilter(
                                Expression.eq(
                                    Expression.get("status"),
                                    Expression.literal(statusName),
                                )
                            )
                    )
                }

                // Restrict panning and zoom to the continental US
                map.setLatLngBoundsForCameraTarget(
                    LatLngBounds.from(49.5, -66.0, 24.0, -125.0) // north, east, south, west
                )
                map.setMinZoomPreference(3.0)

                // Center on continental US
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(36.0, -96.0))
                    .zoom(3.5)
                    .build()

                onMapReadyRef.value(map, style)
            }

            // Tap a marker to show bottom sheet
            map.addOnMapClickListener { latLng ->
                val screenPoint = map.projection.toScreenLocation(latLng)
                val hitRadius = 20f
                val hitRect = RectF(
                    screenPoint.x - hitRadius,
                    screenPoint.y - hitRadius,
                    screenPoint.x + hitRadius,
                    screenPoint.y + hitRadius,
                )
                val features = map.queryRenderedFeatures(
                    hitRect, "wh-layer-green", "wh-layer-yellow", "wh-layer-red"
                )
                if (features.isNotEmpty()) {
                    val locationId = features.first().getStringProperty("locationId")
                    if (locationId != null) {
                        onLocationSelectedRef.value(locationsByIdRef.value[locationId])
                    }
                    true
                } else {
                    false
                }
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
