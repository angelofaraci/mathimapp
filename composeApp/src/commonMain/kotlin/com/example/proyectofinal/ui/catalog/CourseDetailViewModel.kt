package com.example.proyectofinal.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.UserProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val course: Course? = null,
    val cta: CourseDetailCta = CourseDetailCta.Start,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

enum class CourseDetailCta {
    Continue,
    Enroll,
    Start
}

class CourseDetailViewModel(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    fun load(courseId: String) {
        viewModelScope.launch {
            _uiState.value = CourseDetailUiState(isLoading = true)

            try {
                val course = courseRepository.getCourseById(courseId) ?: error("Course not found")
                val sessionUser = authRepository.session.value.user
                val progressResult = sessionUser?.let { runCatching { userRepository.getUserProgress(it.id) } }

                _uiState.value = CourseDetailUiState(
                    isLoading = false,
                    course = course,
                    cta = resolveCta(course, progressResult?.getOrNull()),
                    errorMessage = when {
                        sessionUser == null -> "Authenticated user not available"
                        progressResult?.exceptionOrNull() != null ->
                            progressResult.exceptionOrNull()?.message ?: "Unable to load user progress"
                        else -> null
                    }
                )
            } catch (error: Exception) {
                _uiState.value = CourseDetailUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unknown error"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onPrimaryAction() {
        val currentState = _uiState.value
        val course = currentState.course ?: return

        if (currentState.isSubmitting) {
            return
        }

        when (currentState.cta) {
            CourseDetailCta.Continue,
            CourseDetailCta.Start -> Unit

            CourseDetailCta.Enroll -> enroll(course)
        }
    }

    private fun enroll(course: Course) {
        viewModelScope.launch {
            val sessionUser = authRepository.session.value.user

            if (sessionUser == null) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Authenticated user not available"
                    )
                }
                return@launch
            }

            val joinCode = course.joinCode
            if (joinCode.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        cta = CourseDetailCta.Start,
                        isSubmitting = false,
                        errorMessage = "Course enrollment is unavailable"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null
                )
            }

            try {
                val joinedCourse = courseRepository.joinCourseByCode(sessionUser.id, joinCode)
                    ?: error("Unable to enroll in this course")
                val progress = userRepository.getUserProgress(sessionUser.id)
                val resolvedCourse = if (joinedCourse.id == course.id) joinedCourse else course

                _uiState.update {
                    it.copy(
                        course = resolvedCourse,
                        cta = resolveCta(resolvedCourse, progress),
                        isSubmitting = false,
                        errorMessage = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        cta = CourseDetailCta.Enroll,
                        errorMessage = error.message ?: "Unable to enroll in this course"
                    )
                }
            }
        }
    }

    private fun resolveCta(course: Course, progress: UserProgress?): CourseDetailCta = when {
        progress?.enrolledCourseIds?.contains(course.id) == true -> CourseDetailCta.Continue
        !course.joinCode.isNullOrBlank() -> CourseDetailCta.Enroll
        else -> CourseDetailCta.Start
    }
}
