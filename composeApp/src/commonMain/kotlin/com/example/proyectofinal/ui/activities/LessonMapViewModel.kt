package com.example.proyectofinal.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.ExerciseRepository
import com.example.proyectofinal.domain.LessonRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LessonMapViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val lessonRepository: LessonRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LessonMapUiState())
    val uiState: StateFlow<LessonMapUiState> = _uiState.asStateFlow()

    private var completedExerciseIds: Set<String> = emptySet()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = LessonMapUiState(isLoading = true)

        _uiState.value = try {
            val user = authRepository.session.value.user
                ?: userRepository.getCurrentUser()
                ?: error("Unable to restore the authenticated user")
            val progress = userRepository.getUserProgress(user.id)
            val lessonMap = loadLessonMap(progress.enrolledCourseIds)
            completedExerciseIds = progress.completedExerciseIds

            createLoadedState(lessonMap = lessonMap, completedExerciseIds = completedExerciseIds)
        } catch (error: Exception) {
            LessonMapUiState(
                isLoading = false,
                errorMessage = error.message ?: "Unknown error"
            )
        }
    }

    fun selectExercise(exerciseId: String) {
        val currentState = _uiState.value
        val lessonMap = currentState.lessonMap ?: return
        val node = currentState.nodes.firstOrNull { it.exercise.id == exerciseId } ?: return

        if (node.state != LessonNodeState.Unlocked && node.state != LessonNodeState.Current) {
            return
        }

        _uiState.update {
            it.copy(
                selectedExerciseId = exerciseId,
                activeExerciseId = exerciseId,
                selectedAnswer = null,
                exerciseFeedbackMessage = null,
                nodes = buildLessonMapNodes(
                    exercises = lessonMap.exercises,
                    completedExerciseIds = completedExerciseIds,
                    selectedExerciseId = exerciseId
                )
            )
        }
    }

    fun dismissActiveExercise() {
        _uiState.update {
            it.copy(
                activeExerciseId = null,
                selectedAnswer = null,
                isSubmittingAnswer = false,
                exerciseFeedbackMessage = null
            )
        }
    }

    fun selectAnswer(answer: String) {
        _uiState.update {
            it.copy(
                selectedAnswer = answer,
                exerciseFeedbackMessage = null
            )
        }
    }

    fun submitAnswer() = viewModelScope.launch {
        val currentState = _uiState.value
        val lessonMap = currentState.lessonMap ?: return@launch
        val exercise = currentState.activeExercise ?: return@launch
        val selectedAnswer = currentState.selectedAnswer ?: return@launch

        if (currentState.isSubmittingAnswer) {
            return@launch
        }

        if (selectedAnswer != exercise.correctAnswer) {
            _uiState.update { it.copy(exerciseFeedbackMessage = "Incorrect answer. Try again.") }
            return@launch
        }

        _uiState.update {
            it.copy(
                isSubmittingAnswer = true,
                exerciseFeedbackMessage = null
            )
        }

        try {
            val completion = userRepository.completeExercise(exercise.id, score = 100)
            completedExerciseIds = completion.progress.completedExerciseIds
            _uiState.value = createLoadedState(
                lessonMap = lessonMap,
                completedExerciseIds = completedExerciseIds,
                exerciseFeedbackMessage = "Exercise completed. Keep going."
            )
        } catch (error: Exception) {
            _uiState.update {
                it.copy(
                    isSubmittingAnswer = false,
                    exerciseFeedbackMessage = error.message ?: "Unable to save your progress"
                )
            }
        }
    }

    fun openTheory() {
        val lesson = _uiState.value.lessonMap?.lesson ?: return
        _uiState.update { it.copy(selectedTheoryLesson = lesson) }
    }

    fun dismissTheory() {
        _uiState.update { it.copy(selectedTheoryLesson = null) }
    }

    private suspend fun loadLessonMap(enrolledCourseIds: Set<String>): LessonMapLesson {
        val courseId = enrolledCourseIds.sorted().firstOrNull()
            ?: error("Activities are unavailable until you join a course.")
        val lesson = lessonRepository.getLessonsByCourse(courseId).firstOrNull()
            ?: error("No lessons are available for your current course yet.")
        val exercises = lesson.exercises.ifEmpty {
            exerciseRepository.getExercisesByLesson(lesson.id)
        }

        if (exercises.isEmpty()) {
            error("No exercises are available for this lesson yet.")
        }

        return LessonMapLesson(
            lesson = lesson,
            exercises = exercises
        )
    }

    private fun createLoadedState(
        lessonMap: LessonMapLesson,
        completedExerciseIds: Set<String>,
        exerciseFeedbackMessage: String? = null
    ): LessonMapUiState {
        return LessonMapUiState(
            isLoading = false,
            lessonMap = lessonMap,
            nodes = buildLessonMapNodes(
                exercises = lessonMap.exercises,
                completedExerciseIds = completedExerciseIds
            ),
            exerciseFeedbackMessage = exerciseFeedbackMessage
        )
    }
}

internal fun buildLessonMapNodes(
    exercises: List<Exercise>,
    completedExerciseIds: Set<String>,
    selectedExerciseId: String? = null
): List<LessonMapNodeUiModel> {
    val firstAvailableExerciseId = exercises.firstOrNull { it.id !in completedExerciseIds }?.id

    return exercises.mapIndexed { index, exercise ->
        val state = when {
            exercise.id in completedExerciseIds -> LessonNodeState.Completed
            firstAvailableExerciseId == null -> LessonNodeState.Completed
            exercise.id == firstAvailableExerciseId && selectedExerciseId == exercise.id -> LessonNodeState.Current
            exercise.id == firstAvailableExerciseId -> LessonNodeState.Unlocked
            else -> LessonNodeState.Locked
        }

        LessonMapNodeUiModel(
            exercise = exercise,
            index = index + 1,
            title = "Exercise ${index + 1}",
            summary = exercise.question,
            state = state
        )
    }
}
