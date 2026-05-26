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
}
