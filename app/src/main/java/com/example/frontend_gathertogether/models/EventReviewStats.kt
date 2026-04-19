package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class EventReviewStats(
    val rating: Double,
    val reviewCount: Long
)
