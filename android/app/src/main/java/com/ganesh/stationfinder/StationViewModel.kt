package com.ganesh.stationfinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.stationfinder.data.model.OCMStation
import com.ganesh.stationfinder.data.model.Review
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

    // Filter Preference
    private val _selectedConnectorFilter = MutableStateFlow<String?>(null)
    val selectedConnectorFilter: StateFlow<String?> = _selectedConnectorFilter.asStateFlow()

    // Reviews State
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview.asStateFlow()

    // Route Planner State
    private val _routeStations = MutableStateFlow<List<OCMStation>>(emptyList())
    val routeStations: StateFlow<List<OCMStation>> = _routeStations.asStateFlow()

    private val _isRouteLoading = MutableStateFlow(false)
    val isRouteLoading: StateFlow<Boolean> = _isRouteLoading.asStateFlow()

    fun selectConnectorFilter(connector: String?) {
        _selectedConnectorFilter.value = connector
    }

    fun fetchReviews(stationId: Long) {
        viewModelScope.launch {
            _reviews.value = repository.getReviews(stationId)
        }
    }

    fun submitReview(stationId: Long, reviewerName: String, rating: Double, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmittingReview.value = true
            val review = repository.addReview(stationId, reviewerName, rating, comment)
            _isSubmittingReview.value = false
            if (review != null) {
                fetchReviews(stationId)
                // Refresh nearby stations list to show updated average rating
                lastFetchedLocation?.let { fetchNearbyStations(it, manual = false) }
                onSuccess()
            }
        }
    }

    fun fetchStationsAlongRoute(waypoints: String, connectorType: String?) {
        viewModelScope.launch {
            _isRouteLoading.value = true
            _routeStations.value = repository.getStationsAlongRoute(waypoints, connectorType)
            _isRouteLoading.value = false
        }
    }

    fun clearRoute() {
        _routeStations.value = emptyList()
    }

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
