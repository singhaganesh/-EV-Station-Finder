package com.ganesh.stationfinder.data.network

import com.ganesh.stationfinder.data.model.ApiResponse
import com.ganesh.stationfinder.data.model.OCMStation
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenChargeMapApi {
    
    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<OCMStation>>
}

