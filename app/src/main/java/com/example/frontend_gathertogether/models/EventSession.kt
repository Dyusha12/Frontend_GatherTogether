package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class EventSession(
    val sessionCode: String,
    val eventCode: String,
    val dateEvent: String,
    val numberPeople: Int,
    val status: String
)