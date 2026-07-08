package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateExerciseRequest(
    val id: String,
    val lessonId: String,
    val title: String,
    val type: ExerciseType = ExerciseType.MULTIPLE_CHOICE,
    val payload: ExercisePayload
)

@Serializable
data class UpdateExerciseRequest(
    val title: String? = null,
    val type: ExerciseType? = null,
    val payload: ExercisePayload? = null
)
