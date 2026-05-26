package com.ganesh.stationfinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.stationfinder.data.model.OCMStation
import com.ganesh.stationfinder.data.repository.StationRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

sealed class StationUiState {
    object Loading : StationUiState()
    data class Success(val stations: List<OCMStation>) : StationUiState()
    data class Error(val message: String) : StationUiState()
}

class StationViewModel : ViewModel() {
    private val repository = StationRepository()
    private var searchJob: Job? = null
    private var lastFetchedLocation: LatLng? = null

    private val _uiState = MutableStateFlow<StationUiState>(StationUiState.Loading)
    val uiState: StateFlow<StationUiState> = _uiState.asStateFlow()

    var isManualSearch: Boolean = false
        private set

    fun fetchNearbyStations(location: LatLng, distance: Double = 20.0, manual: Boolean = false) {
        isManualSearch = manual
        // Cancel previous search if still running
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            _uiState.value = StationUiState.Loading
            try {
                val stations = repository.getNearbyStations(location.latitude, location.longitude, distance)
                _uiState.value = StationUiState.Success(stations)
                lastFetchedLocation = location
            } catch (e: Exception) {
                _uiState.value = StationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchNearbyStationsDebounced(location: LatLng, zoom: Float) {
        // Only fetch if moved more than ~500 meters or zoom changed significantly
        val distanceMoved = lastFetchedLocation?.let { 
            calculateDistance(it, location)
        } ?: Float.MAX_VALUE

        if (distanceMoved < 500f) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(1500) // 1.5 second debounce
            val radius = calculateRadiusFromZoom(zoom)
            fetchNearbyStations(location, radius, manual = false)
        }
    }

    fun fetchNearbyStationsForZoom(location: LatLng, zoom: Float, manual: Boolean = false) {
        val radius = calculateRadiusFromZoom(zoom)
        fetchNearbyStations(location, radius, manual)
    }

    private fun calculateRadiusFromZoom(zoom: Float): Double {
        return when {
            zoom >= 15f -> 5.0
            zoom >= 12f -> 15.0
            zoom >= 10f -> 30.0
            zoom >= 8f -> 100.0
            zoom >= 6f -> 300.0
            zoom >= 4f -> 1000.0
            else -> 3000.0
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        return results[0]
    }
}
