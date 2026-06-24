package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val role: UserRole? = null
)
