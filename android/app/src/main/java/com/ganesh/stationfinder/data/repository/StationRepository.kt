package com.ganesh.stationfinder.data.repository

import com.ganesh.stationfinder.data.model.OCMStation
import com.ganesh.stationfinder.data.network.RetrofitClient

class StationRepository {
    
    private val api = RetrofitClient.api

    suspend fun getNearbyStations(lat: Double, lng: Double, distance: Double = 20.0): List<OCMStation> {
        android.util.Log.d("Repository", "Fetching stations at Lat: $lat, Lng: $lng (Radius: ${distance}km)")
        return try {
            val response = api.getNearbyStations(
                lat = lat,
                lng = lng,
                radius = distance
            )
            if (response.success) {
                android.util.Log.d("Repository", "API returned ${response.data.size} stations: ${response.message}")
                response.data
            } else {
                android.util.Log.e("Repository", "API returned failure: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Error fetching stations", e)
            emptyList()
        }
    }
}
