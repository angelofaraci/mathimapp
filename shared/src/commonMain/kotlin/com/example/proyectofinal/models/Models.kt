package com.example.proyectofinal.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.LEARNER
)

@Serializable
enum class UserRole {
    ADMIN, TEACHER, LEARNER
}

@Serializable
data class Course(
    val id: String,
    val title: String,
    val description: String,
    val creatorId: String,
    val isOfficial: Boolean = false,
    val joinCode: String? = null,
    val lessons: List<Lesson> = emptyList(),
    val schoolYear: Int = 0
)

@Serializable
data class Lesson(
    val id: String,
    val courseId: String,
    val title: String,
    val theoryContent: String,
    val exercises: List<Exercise> = emptyList()
)

@Serializable
data class Exercise(
    val id: String,
    val lessonId: String,
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

@Serializable
data class UserProgress(
    val userId: String,
    val completedLessonIds: Set<String> = emptySet(),
    val completedExerciseIds: Set<String> = emptySet(),
    val totalScore: Int = 0,
    val enrolledCourseIds: Set<String> = emptySet()
)

@Serializable
data class CompleteExerciseRequest(
    val exerciseId: String,
    val score: Int = 0
)

@Serializable
data class ExerciseCompletionResponse(
    val exerciseId: String,
    val lessonId: String,
    val lessonCompleted: Boolean,
    val progress: UserProgress
)
@Serializable
data class CompleteLessonRequest(
    val userId: String,
    val lessonId: String,
    val score: Int = 0
)

@Serializable
data class TheoryUpdateRequest(
    val lessonId: String,
    val theoryContent: String
)
