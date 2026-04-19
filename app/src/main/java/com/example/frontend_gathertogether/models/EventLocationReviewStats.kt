package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class EventLocationReviewStats(
    val averageRating: Double,
    val totalReviews: Int,
    val fiveStars: Int,
    val fourStars: Int,
    val threeStars: Int,
    val twoStars: Int,
    val oneStars: Int
)