package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProgressRequest(
    val lessonId: String? = null,
    val courseId: String? = null,
    val scoreToAdd: Int = 0,
    val completedLessonId: String? = null
)
