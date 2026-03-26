package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateLessonRequest(
    val id: String,
    val courseId: String,
    val title: String,
    val theoryContent: String
)

@Serializable
data class UpdateLessonRequest(
    val title: String? = null,
    val theoryContent: String? = null
)
