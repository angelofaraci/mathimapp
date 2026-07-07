package com.example.proyectofinal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.User
import com.example.proyectofinal.ui.ActivityStreakCap
import com.example.proyectofinal.ui.XpPerLevel
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val MorningHours = 5..11
private val AfternoonHours = 12..18

data class HomeDashboardUiState(
    val isLoading: Boolean = true,
    val greeting: String = "",
    val schoolYearLabel: String? = null,
    val level: Int = 0,
    val activityCount: Int = 0,
    val completedLessons: Int = 0,
    val hasEnrolledCourse: Boolean = false,
    val isJoiningCourse: Boolean = false,
    val joinCourseMessage: String? = null,
    val errorMessage: String? = null
)

class HomeDashboardViewModel(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val learnerProfileRepository: LearnerProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState: StateFlow<HomeDashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun openActivities(onOpen: () -> Unit) {
        onOpen()
    }

    fun joinCourse(code: String, onJoined: () -> Unit = {}) = viewModelScope.launch {
        val user = resolveCurrentUser() ?: run {
            _uiState.value = _uiState.value.copy(
                isJoiningCourse = false,
                joinCourseMessage = "Your session could not be restored. Please sign in again.",
                errorMessage = null
            )
            return@launch
        }

        val normalizedCode = code.trim()
        if (normalizedCode.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isJoiningCourse = false,
                joinCourseMessage = "Enter a course code to continue.",
                errorMessage = null
            )
            return@launch
        }

        _uiState.value = _uiState.value.copy(
            isJoiningCourse = true,
            joinCourseMessage = null,
            errorMessage = null
        )

        val joinedCourse = courseRepository.joinCourseByCode(user.id, normalizedCode)
        if (joinedCourse == null) {
            _uiState.value = _uiState.value.copy(
                isJoiningCourse = false,
                joinCourseMessage = "We couldn't find a course with that code.",
                errorMessage = null
            )
            return@launch
        }

        _uiState.value = buildDashboardState(user).copy(joinCourseMessage = null)
        onJoined()
    }

    private fun loadDashboard() = viewModelScope.launch {
        val previousState = _uiState.value
        _uiState.value = previousState.copy(isLoading = true, errorMessage = null)

        _uiState.value = try {
            val user = resolveCurrentUser() ?: error("Authenticated user not available")
            buildDashboardState(user).copy(joinCourseMessage = previousState.joinCourseMessage)
        } catch (error: Exception) {
            HomeDashboardUiState(
                isLoading = false,
                greeting = greetingFor(resolveCurrentUser()?.name),
                joinCourseMessage = previousState.joinCourseMessage,
                errorMessage = error.message ?: "Unknown error"
            )
        }
    }

    private suspend fun resolveCurrentUser(): User? =
        authRepository.session.value.user ?: userRepository.getCurrentUser()

    private suspend fun buildDashboardState(user: User): HomeDashboardUiState {
        val progress = userRepository.getUserProgress(user.id)
        val profile = learnerProfileRepository.getProfile()
        val completedLessons = progress.completedLessonIds.size
        return HomeDashboardUiState(
            isLoading = false,
            greeting = greetingFor(user.name),
            schoolYearLabel = profile?.let { "Year ${it.schoolYear} • ${it.studentTrack.displayName}" },
            level = progress.totalScore / XpPerLevel,
            activityCount = min(completedLessons, ActivityStreakCap),
            completedLessons = completedLessons,
            hasEnrolledCourse = progress.enrolledCourseIds.isNotEmpty()
        )
    }
}

internal fun salutation(hour: Int = currentLocalHour()): String = when (hour) {
    in MorningHours -> "Buenos días"
    in AfternoonHours -> "Buenas tardes"
    else -> "Buenas noches"
}

internal fun greetingFor(displayName: String?, hour: Int = currentLocalHour()): String {
    val trimmedName = displayName?.trim().orEmpty()
    return if (trimmedName.isBlank()) salutation(hour) else "${salutation(hour)}, $trimmedName"
}
