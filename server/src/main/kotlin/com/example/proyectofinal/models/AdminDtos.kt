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
data class CreateAdminCourseRequest(
    val id: String,
    val title: String,
    val description: String,
    val isOfficial: Boolean = false,
    val schoolYear: Int = 0,
    val topic: String? = null,
    val difficulty: String? = null,
    val durationMinutes: Int? = null,
    val xpReward: Int? = null
)

@Serializable
data class UpdateAdminCourseRequest(
    val title: String? = null,
    val description: String? = null,
    val isOfficial: Boolean? = null,
    val schoolYear: Int? = null,
    val topic: String? = null,
    val difficulty: String? = null,
    val durationMinutes: Int? = null,
    val xpReward: Int? = null
)

@Serializable
data class CreateAdminLessonRequest(
    val id: String,
    val courseId: String? = null,
    val creatorId: String? = null,
    val title: String,
    val theoryContent: String
)

@Serializable
data class UpdateAdminLessonRequest(
    val title: String? = null,
    val theoryContent: String? = null,
    val courseId: String? = null,
    val creatorId: String? = null
)

@Serializable
data class AdminLessonResponse(
    val id: String,
    val courseId: String?,
    val creatorId: String?,
    val title: String,
    val theoryContent: String
)

@Serializable
data class AdminLessonListResponse(
    val items: List<AdminLessonResponse>
)

@Serializable
data class CreateAdminExerciseRequest(
    val id: String,
    val lessonId: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val type: ExerciseType = ExerciseType.MULTIPLE_CHOICE
)

@Serializable
data class UpdateAdminExerciseRequest(
    val lessonId: String? = null,
    val question: String? = null,
    val options: List<String>? = null,
    val correctAnswer: String? = null,
    val type: ExerciseType? = null
)

@Serializable
data class AdminExerciseResponse(
    val id: String,
    val lessonId: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val type: ExerciseType
)

@Serializable
data class AdminExerciseListResponse(
    val items: List<AdminExerciseResponse>
)

@Serializable
data class RoleUpdateRequest(
    val role: String
)
