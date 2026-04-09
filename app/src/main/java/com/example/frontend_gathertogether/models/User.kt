package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userCode: String,
    val mail: String,
    val phoneNumber: String? = null,
    val password: String,
    val surname: String,
    val name: String,
    val dateBirth: String,
    val rating: Double,
    val dateCreation: String,
    val active: Boolean
)
