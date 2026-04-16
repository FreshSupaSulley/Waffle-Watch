package io.github.freshsupasulley.wafflewatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import io.github.freshsupasulley.wafflewatch.model.LocationRepository
import io.github.freshsupasulley.wafflewatch.model.WaffleHouseLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationRepository.create(application)

    private val _locations = MutableStateFlow<List<WaffleHouseLocation>>(emptyList())
    val locations: StateFlow<List<WaffleHouseLocation>> = _locations

    // Performance Optimization: Pre-calculate GeoJSON features on a background thread
    private val _features = MutableStateFlow<List<Feature>>(emptyList())
    val features: StateFlow<List<Feature>> = _features

    private val _timestamp = MutableStateFlow<Long?>(null)
    val timestamp: StateFlow<Long?> = _timestamp

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadLocations()
    }

    fun loadLocations() {
        viewModelScope.launch {
            _error.value = null
            _isLoading.value = true
            try {
                // Load cache first for instant UI
                val cached = repository.getCachedLocations()
                if (cached.isNotEmpty()) {
                    updateLocationsState(cached)
                    _timestamp.value = repository.getCachedTimestamp()
                }

                // Then fetch from network
                val response = repository.fetchLocations()
                updateLocationsState(response.locations)
                _timestamp.value = response.timestamp
            } catch (e: Exception) {
                // Only show error if we have no cached data
                if (_locations.value.isEmpty()) {
                    _error.value = e.message ?: "Network error"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateLocationsState(newList: List<WaffleHouseLocation>) {
        _locations.value = newList
        // Offload heavy mapping to Default dispatcher (CPU intensive)
        _features.value = withContext(Dispatchers.Default) {
            newList.map { it.toFeature() }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun WaffleHouseLocation.toFeature(): Feature {
        val props = JsonObject()
        props.addProperty("locationId", locationId)
        props.addProperty("name", name)
        props.addProperty("address", address)
        props.addProperty("status", status.name)
        props.addProperty("formattedHours", formattedHours)
        return Feature.fromGeometry(Point.fromLngLat(longitude, latitude), props)
    }
}
