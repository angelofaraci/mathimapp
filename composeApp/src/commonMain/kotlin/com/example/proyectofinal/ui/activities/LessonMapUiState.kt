package com.example.proyectofinal.ui.activities

import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.Lesson

enum class LessonNodeState {
    Locked,
    Unlocked,
    Completed,
    Current
}

data class LessonMapLesson(
    val lesson: Lesson,
    val exercises: List<Exercise>
)

data class LessonMapNodeUiModel(
    val exercise: Exercise,
    val index: Int,
    val title: String,
    val summary: String,
    val state: LessonNodeState
)

data class LessonMapUiState(
    val isLoading: Boolean = true,
    val lessonMap: LessonMapLesson? = null,
    val nodes: List<LessonMapNodeUiModel> = emptyList(),
    val selectedExerciseId: String? = null,
    val activeExerciseId: String? = null,
    val selectedAnswer: String? = null,
    val isSubmittingAnswer: Boolean = false,
    val exerciseFeedbackMessage: String? = null,
    val selectedTheoryLesson: Lesson? = null,
    val errorMessage: String? = null
) {
    val activeNode: LessonMapNodeUiModel?
        get() = nodes.firstOrNull { it.exercise.id == selectedExerciseId }
            ?: nodes.firstOrNull { it.state == LessonNodeState.Unlocked || it.state == LessonNodeState.Current }

    val activeExercise: Exercise?
        get() = lessonMap?.exercises?.firstOrNull { it.id == activeExerciseId }

    val isTheoryAvailable: Boolean
        get() = lessonMap != null
}
