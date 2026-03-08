package com.example.proyectofinal.domain

import kotlinx.serialization.Serializable

@Serializable
data class Course(
    val id: String,
    val title: String,
    val description: String,
    val lessons: List<Lesson> = emptyList()
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val theoryContent: String,
    val exercises: List<Exercise> = emptyList()
)

@Serializable
data class Exercise(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val type: ExerciseType = ExerciseType.MULTIPLE_CHOICE
)

@Serializable
enum class ExerciseType {
    MULTIPLE_CHOICE, 
    TRUE_FALSE, 
    INPUT_VALUE
}

/**
 * Tracks the user's journey. 
 * This is the part your app will "Update" as the user learns.
 */
@Serializable
data class UserProgress(
    val userId: String,
    val completedLessonIds: Set<String> = emptySet(),
    val totalScore: Int = 0
)
