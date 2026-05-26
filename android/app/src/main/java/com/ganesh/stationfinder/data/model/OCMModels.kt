package com.ganesh.stationfinder.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T
)

data class OCMStation(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("address") val address: String?,
    @SerializedName("operatingHours") val operatingHours: String?,
    @SerializedName("pricePerKwh") val pricePerKwh: Double?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("isOpen") val isOpen: Boolean?,
    @SerializedName("meta") val meta: String?,
    @SerializedName("distance") val distance: Double?,
    @SerializedName("availableSlots") val availableSlots: Int?,
    @SerializedName("totalSlots") val totalSlots: Int?,
    @SerializedName("connectorTypes") val connectorTypes: List<String>?,
    @SerializedName("slots") val slots: List<SlotInfo>?
) {
    val operatorName: String
        get() {
            if (meta.isNullOrEmpty()) return "Independent Operator"
            val match = Regex("\"ocm_operator\":\"([^\"]+)\"").find(meta)
            return match?.groupValues?.get(1) ?: "Independent Operator"
        }
}

data class SlotInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("label") val label: String?,
    @SerializedName("connectorType") val connectorType: String?,
    @SerializedName("powerKw") val powerKw: Double?,
    @SerializedName("isAvailable") val isAvailable: Boolean?
)

