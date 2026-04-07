package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val userCode: String,
    val mail: String,
    val phoneNumber: String?,
    val password: String,
    val surname: String,
    val name: String,
    val dateBirth: String,
    val rating: Double,
    val dateCreation: String,
    val active: Boolean
)
