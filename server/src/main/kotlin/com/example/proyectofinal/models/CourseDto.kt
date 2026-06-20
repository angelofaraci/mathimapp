package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateCourseRequest(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val isOfficial: Boolean = false,
    val joinCode: String? = null,
    val schoolYear: Int = 0
)

@Serializable
data class UpdateCourseRequest(
    val title: String? = null,
    val description: String? = null,
    val joinCode: String? = null,
    val schoolYear: Int? = null
)

@Serializable
data class JoinCourseRequest(
    val userId: String,
    val code: String
)
