package com.ganesh.stationfinder.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.maps.model.LatLng

object LocationHelper {
    
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, callback: (LatLng?) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        fusedLocationClient.lastLocation.addOnCompleteListener { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                val location = task.result
                callback(LatLng(location.latitude, location.longitude))
            } else {
                callback(null)
            }
        }
    }
}
