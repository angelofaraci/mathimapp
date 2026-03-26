package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole = UserRole.LEARNER
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val role: UserRole? = null
)
