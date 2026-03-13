package io.github.freshsupasulley.wafflewatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.freshsupasulley.wafflewatch.model.LocationRepository
import io.github.freshsupasulley.wafflewatch.model.WaffleHouseLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationRepository.create(application)

    private val _locations = MutableStateFlow<List<WaffleHouseLocation>>(emptyList())
    val locations: StateFlow<List<WaffleHouseLocation>> = _locations

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
                    _locations.value = cached
                    _timestamp.value = repository.getCachedTimestamp()
                }

                // Then fetch from network
                val response = repository.fetchLocations()
                _locations.value = response.locations
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

    fun clearError() {
        _error.value = null
    }
}
