package com.ganesh.stationfinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.stationfinder.data.model.OCMStation
import com.ganesh.stationfinder.data.model.Review
import com.ganesh.stationfinder.data.model.StationMarker
import com.ganesh.stationfinder.data.repository.StationRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.LinkedHashMap
import com.ganesh.stationfinder.util.FavoriteManager

sealed class StationUiState {
    object Loading : StationUiState()
    data class Success(val stations: List<OCMStation>) : StationUiState()
    data class Error(val message: String) : StationUiState()
}

sealed class MarkerUiState {
    object Idle : MarkerUiState()
    object Loading : MarkerUiState()
    data class Success(val markers: List<StationMarker>, val tooMany: Boolean = false) : MarkerUiState()
    data class Error(val message: String) : MarkerUiState()
}

class StationViewModel : ViewModel() {
    private val repository = StationRepository()
    private var searchJob: Job? = null
    var lastFetchedLocation: LatLng? = null
        private set

    // --- Map markers (lightweight) ---
    private val _markerState = MutableStateFlow<MarkerUiState>(MarkerUiState.Idle)
    val markerState: StateFlow<MarkerUiState> = _markerState.asStateFlow()

    // --- Carousel (full stations, max 5) ---
    private val _carouselStations = MutableStateFlow<List<OCMStation>>(emptyList())
    val carouselStations: StateFlow<List<OCMStation>> = _carouselStations.asStateFlow()

    // --- Bounding box cache (LRU, max 10 entries) ---
    private val viewportCache = LinkedHashMap<String, Pair<List<StationMarker>, Boolean>>(10, 0.75f, true)

    private var lastViewportKey: String? = null
    private var viewportJob: Job? = null
    private var carouselJob: Job? = null

    // --- Selected Station Detail for Map pin click ---
    private val _selectedStationDetail = MutableStateFlow<OCMStation?>(null)
    val selectedStationDetail: StateFlow<OCMStation?> = _selectedStationDetail.asStateFlow()

    // --- Saved Stations (full OCMStation objects) ---
    private val _savedStations = MutableStateFlow<List<OCMStation>>(emptyList())
    val savedStations: StateFlow<List<OCMStation>> = _savedStations.asStateFlow()

    private val _isSavedLoading = MutableStateFlow(false)
    val isSavedLoading: StateFlow<Boolean> = _isSavedLoading.asStateFlow()

    fun fetchSavedStations(context: android.content.Context, lat: Double, lng: Double) {
        viewModelScope.launch {
            _isSavedLoading.value = true
            val ids = FavoriteManager.getFavorites(context)
            if (ids.isEmpty()) {
                _savedStations.value = emptyList()
            } else {
                try {
                    val list = ids.mapNotNull { id ->
                        repository.getStationDetail(id, lat, lng)
                    }
                    _savedStations.value = list
                } catch (e: Exception) {
                    android.util.Log.e("ViewModel", "Error fetching saved stations details", e)
                    _savedStations.value = emptyList()
                }
            }
            _isSavedLoading.value = false
        }
    }

    fun fetchViewportMarkers(neLat: Double, neLng: Double, swLat: Double, swLng: Double) {
        // Round to 3 decimal places for cache key (~111m precision)
        val key = "${(neLat*1000).toInt()},${(neLng*1000).toInt()},${(swLat*1000).toInt()},${(swLng*1000).toInt()}"

        // Skip if same viewport
        if (key == lastViewportKey) return
        lastViewportKey = key

        // Check cache
        viewportCache[key]?.let { cached ->
            _markerState.value = MarkerUiState.Success(cached.first, cached.second)
            return
        }

        viewportJob?.cancel()
        viewportJob = viewModelScope.launch {
            delay(800) // debounce
            _markerState.value = MarkerUiState.Loading
            try {
                val (markers, tooMany) = repository.getStationsInViewport(neLat, neLng, swLat, swLng)
                viewportCache[key] = Pair(markers, tooMany)
                if (viewportCache.size > 10) {
                    viewportCache.remove(viewportCache.keys.first())
                }
                _markerState.value = MarkerUiState.Success(markers, tooMany)
            } catch (e: Exception) {
                _markerState.value = MarkerUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchCarouselStations(lat: Double, lng: Double, radius: Double = 10.0, immediate: Boolean = false) {
        carouselJob?.cancel()
        carouselJob = viewModelScope.launch {
            if (!immediate) {
                delay(800) // debounce
            }
            try {
                val stations = repository.getNearbyStations(lat, lng, radius)
                _carouselStations.value = stations.take(5)
            } catch (e: Exception) {
                _carouselStations.value = emptyList()
            }
        }
    }

    /**
     * Compute carousel stations instantly from already-loaded viewport markers.
     * Sorts by proximity to [pinLat]/[pinLng] (the clicked pin) but calculates
     * display distance from [userLat]/[userLng] (user's actual GPS location).
     */
    fun computeCarouselFromMarkers(
        markers: List<StationMarker>,
        pinLat: Double, pinLng: Double,
        userLat: Double, userLng: Double
    ) {
        // Cancel any pending debounced backend fetch so it doesn't overwrite us
        carouselJob?.cancel()
        carouselJob = null

        // Sort markers by distance to the clicked pin
        val sorted = markers.sortedBy { marker ->
            haversineKm(pinLat, pinLng, marker.latitude, marker.longitude)
        }.take(5)

        // Convert StationMarker → OCMStation with distance from user location
        _carouselStations.value = sorted.map { marker ->
            val distFromUser = haversineKm(userLat, userLng, marker.latitude, marker.longitude)
            OCMStation(
                id = marker.id,
                name = marker.name,
                latitude = marker.latitude,
                longitude = marker.longitude,
                address = null,
                operatingHours = null,
                pricePerKwh = null,
                rating = marker.rating,
                isOpen = marker.available,
                meta = null,
                distance = distFromUser,
                availableSlots = marker.availableSlots,
                totalSlots = marker.totalSlots,
                connectorTypes = marker.connectorTypes,
                slots = null
            )
        }
    }

    /** Haversine formula — returns distance in kilometres between two lat/lng points. */
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun fetchStationDetail(id: Long, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val detail = repository.getStationDetail(id, lat, lng)
                _selectedStationDetail.value = detail
            } catch (e: Exception) {
                _selectedStationDetail.value = null
            }
        }
    }

    fun clearSelectedStationDetail() {
        _selectedStationDetail.value = null
    }

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

    fun fetchNearbyStationsDebounced(location: LatLng, radius: Double) {
        // Only fetch if moved more than ~500 meters
        val distanceMoved = lastFetchedLocation?.let { 
            calculateDistance(it, location)
        } ?: Float.MAX_VALUE

        if (distanceMoved < 500f) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(1500) // 1.5 second debounce
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
