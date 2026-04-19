package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class ReviewEvent(
    val reviewCodeEvent: String,
    val typeCode: String,
    val user: User,
    val rating: Int,
    val comment: String?,
    val dateCreation: String,
    val eventCode: String
)
