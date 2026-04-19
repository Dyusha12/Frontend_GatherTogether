package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val creatorCode: String,
    val dateCreation: String,
    val description: String?,
    val eventCode: String,
    val location: EventLocation,
    val minimumAge: Int?,
    val nameEvent: String,
    val type: TypeEvent,
    val rating: Double
)