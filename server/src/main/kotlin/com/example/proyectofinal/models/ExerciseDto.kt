package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateExerciseRequest(
    val id: String,
    val lessonId: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val type: ExerciseType = ExerciseType.MULTIPLE_CHOICE
)

@Serializable
data class UpdateExerciseRequest(
    val question: String? = null,
    val options: List<String>? = null,
    val correctAnswer: String? = null,
    val type: ExerciseType? = null
)
