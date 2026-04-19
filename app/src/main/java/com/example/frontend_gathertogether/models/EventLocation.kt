package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class EventLocation(
    val locationCode: String,
    val nameLocation: String,
    val address: String,
    val rating: Double,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String? = null
)
