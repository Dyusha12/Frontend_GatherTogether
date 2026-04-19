package com.example.frontend_gathertogether.models

import kotlinx.serialization.Serializable

@Serializable
data class TypeEvent(
    val typeCode: String,
    val name: String
)
