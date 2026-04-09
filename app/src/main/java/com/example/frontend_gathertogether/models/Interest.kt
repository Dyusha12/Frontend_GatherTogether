package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class Interest(
    val code: String,
    val name: String
)
