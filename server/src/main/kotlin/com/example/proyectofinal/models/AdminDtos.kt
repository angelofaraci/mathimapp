package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

@Serializable
data class AdminUserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole
)

@Serializable
data class AdminCourseResponse(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val enrollmentCount: Int,
    val isOfficial: Boolean,
    val schoolYear: Int
)

@Serializable
data class RoleUpdateRequest(
    val role: String
)
