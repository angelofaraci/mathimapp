package com.example.proyectofinal.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Platform-curated, ordered guidance over existing official lessons. */
@Serializable
data class LearningPath(
    val id: String,
    val name: String,
    val description: String,
    val visibleObjective: String,
    val objective: LearningPathObjective,
    val lessons: List<LearningPathLesson> = emptyList()
)

@Serializable
data class LearningPathObjective(
    val type: LearningPathObjectiveType = LearningPathObjectiveType.GRADE_LEVEL,
    val gradeLevel: Int
)

@Serializable
enum class LearningPathObjectiveType {
    @SerialName("grade-level")
    GRADE_LEVEL
}

/** Lightweight lesson data suitable for a path summary or map, never lesson content. */
@Serializable
data class LearningPathLesson(
    val lessonId: String,
    val title: String,
    val orderIndex: Int,
    val completed: Boolean = false
)

@Serializable
data class LearningPathSummary(
    val id: String,
    val name: String,
    val description: String,
    val visibleObjective: String,
    val objective: LearningPathObjective,
    val lessonCount: Int,
    val completedLessonCount: Int = 0,
    val progressPercentage: Int = 0,
    val isDefaultForObjective: Boolean = false
)

/** Server-backed learner selection and re-entry state. */
@Serializable
data class LearningPathState(
    val recommendedPathId: String? = null,
    val selectedPathId: String? = null,
    val lastOpenedPathId: String? = null
)

/** Request for a learner-owned path selection; the authenticated user is always the state owner. */
@Serializable
data class LearningPathIdRequest(
    val pathId: String
)
