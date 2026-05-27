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

    suspend fun getReviews(stationId: Long): List<com.ganesh.stationfinder.data.model.Review> {
        return try {
            val response = api.getReviews(stationId)
            if (response.success) response.data else emptyList()
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Error getting reviews", e)
            emptyList()
        }
    }

    suspend fun addReview(stationId: Long, reviewerName: String, rating: Double, comment: String): com.ganesh.stationfinder.data.model.Review? {
        return try {
            val body = mapOf(
                "reviewerName" to reviewerName,
                "rating" to rating,
                "comment" to comment
            )
            val response = api.addReview(stationId, body)
            if (response.success) response.data else null
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Error adding review", e)
            null
        }
    }

    suspend fun getStationsAlongRoute(waypoints: String, connectorType: String?): List<OCMStation> {
        return try {
            val response = api.getStationsAlongRoute(waypoints, connectorType)
            if (response.success) response.data else emptyList()
        } catch (e: Exception) {
            android.util.Log.e("Repository", "Error getting stations along route", e)
            emptyList()
        }
    }
}
