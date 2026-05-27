package com.ganesh.stationfinder.util

import android.content.Context
import android.content.SharedPreferences

object FavoriteManager {
    private const val PREFS_NAME = "favorite_stations"
    private const val KEY_FAVORITES = "favorite_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFavorites(context: Context): Set<Long> {
        val stringSet = getPrefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun isFavorite(context: Context, stationId: Long): Boolean {
        return getFavorites(context).contains(stationId)
    }

    fun toggleFavorite(context: Context, stationId: Long): Boolean {
        val current = getFavorites(context).toMutableSet()
        val isFavNow = if (current.contains(stationId)) {
            current.remove(stationId)
            false
        } else {
            current.add(stationId)
            true
        }
        val stringSet = current.map { it.toString() }.toSet()
        getPrefs(context).edit().putStringSet(KEY_FAVORITES, stringSet).apply()
        return isFavNow
    }

    // EV Profile Helpers
    fun getVehicleModel(context: Context): String = getPrefs(context).getString("vehicle_model", "Tata Nexon EV") ?: "Tata Nexon EV"
    fun setVehicleModel(context: Context, model: String) = getPrefs(context).edit().putString("vehicle_model", model).apply()

    fun getBatteryCapacity(context: Context): String = getPrefs(context).getString("battery_capacity", "40.5 kWh") ?: "40.5 kWh"
    fun setBatteryCapacity(context: Context, battery: String) = getPrefs(context).edit().putString("battery_capacity", battery).apply()

    fun getRange(context: Context): String = getPrefs(context).getString("range", "312 km") ?: "312 km"
    fun setRange(context: Context, range: String) = getPrefs(context).edit().putString("range", range).apply()

    fun getPreferredConnector(context: Context): String = getPrefs(context).getString("preferred_connector", "CCS2") ?: "CCS2"
    fun setPreferredConnector(context: Context, connector: String) = getPrefs(context).edit().putString("preferred_connector", connector).apply()

    fun getMinPower(context: Context): Int = getPrefs(context).getInt("min_power", 22)
    fun setMinPower(context: Context, power: Int) = getPrefs(context).edit().putInt("min_power", power).apply()

    fun getOnlyOpen(context: Context): Boolean = getPrefs(context).getBoolean("only_open", true)
    fun setOnlyOpen(context: Context, onlyOpen: Boolean) = getPrefs(context).edit().putBoolean("only_open", onlyOpen).apply()

    fun getOnlyAvailable(context: Context): Boolean = getPrefs(context).getBoolean("only_available", false)
    fun setOnlyAvailable(context: Context, onlyAvailable: Boolean) = getPrefs(context).edit().putBoolean("only_available", onlyAvailable).apply()
}
