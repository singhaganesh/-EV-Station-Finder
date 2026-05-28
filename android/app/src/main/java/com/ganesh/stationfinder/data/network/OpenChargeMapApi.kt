package com.ganesh.stationfinder.data.network

import com.ganesh.stationfinder.data.model.ApiResponse
import com.ganesh.stationfinder.data.model.OCMStation
import com.ganesh.stationfinder.data.model.Review
import com.ganesh.stationfinder.data.model.StationMarker
import retrofit2.http.*

interface OpenChargeMapApi {
    
    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<OCMStation>>

    @GET("api/stations/{id}/reviews")
    suspend fun getReviews(
        @Path("id") id: Long
    ): ApiResponse<List<Review>>

    @POST("api/stations/{id}/reviews")
    suspend fun addReview(
        @Path("id") id: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<Review>

    @GET("api/stations/route")
    suspend fun getStationsAlongRoute(
        @Query("waypoints") waypoints: String,
        @Query("connectorType") connectorType: String?,
        @Query("bufferKm") bufferKm: Double = 10.0
    ): ApiResponse<List<OCMStation>>

    @GET("api/stations/viewport")
    suspend fun getStationsInViewport(
        @Query("neLat") neLat: Double,
        @Query("neLng") neLng: Double,
        @Query("swLat") swLat: Double,
        @Query("swLng") swLng: Double
    ): ApiResponse<List<StationMarker>>

    @GET("api/stations/{id}/detail")
    suspend fun getStationDetail(
        @Path("id") id: Long,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): ApiResponse<OCMStation>
}

