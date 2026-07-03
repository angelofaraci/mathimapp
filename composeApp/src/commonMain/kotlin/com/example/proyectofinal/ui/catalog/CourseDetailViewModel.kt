package com.example.proyectofinal.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val isLoading: Boolean = true,
    val course: Course? = null,
    val isEnrolled: Boolean = false,
    val completedLessonIds: Set<String> = emptySet(),
    val errorMessage: String? = null
) {
    val totalLessons: Int
        get() = course?.lessons?.size ?: 0

    val completedCourseLessonIds: Set<String>
        get() = course
            ?.lessons
            .orEmpty()
            .map { it.id }
            .filterTo(linkedSetOf()) { it in completedLessonIds }

    val completedLessonsCount: Int
        get() = completedCourseLessonIds.size
}

class CourseDetailViewModel(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private var currentCourseId: String? = null

    fun load(courseId: String, force: Boolean = false) {
        if (!force && currentCourseId == courseId && !_uiState.value.isLoading && _uiState.value.errorMessage == null) {
            return
        }

        currentCourseId = courseId

        viewModelScope.launch {
            _uiState.value = CourseDetailUiState(isLoading = true)

            _uiState.value = try {
                val user = authRepository.session.value.user ?: error("Authenticated user not available")
                val course = courseRepository.getCourseById(courseId) ?: error("Unable to load course")
                val progress = userRepository.getUserProgress(user.id)

                CourseDetailUiState(
                    isLoading = false,
                    course = course,
                    isEnrolled = course.id in progress.enrolledCourseIds,
                    completedLessonIds = progress.completedLessonIds
                )
            } catch (error: Exception) {
                CourseDetailUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load course"
                )
            }
        }
    }

    fun retry() {
        currentCourseId?.let { load(it, force = true) }
    }

    fun refresh() {
        currentCourseId?.let { load(it, force = true) }
    }
}
