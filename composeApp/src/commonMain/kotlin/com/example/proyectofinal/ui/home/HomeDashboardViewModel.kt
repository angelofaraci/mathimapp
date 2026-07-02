package com.example.proyectofinal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.UserRepository
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
    val errorMessage: String? = null
)

class HomeDashboardViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val learnerProfileRepository: LearnerProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState: StateFlow<HomeDashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun openActivities(onOpen: () -> Unit) {
        onOpen()
    }

    private fun loadDashboard() = viewModelScope.launch {
        val sessionUser = authRepository.session.value.user
        _uiState.value = HomeDashboardUiState(isLoading = true)

        _uiState.value = try {
            val user = sessionUser ?: error("Authenticated user not available")
            val progress = userRepository.getUserProgress(user.id)
            val profile = learnerProfileRepository.getProfile()
            val completedLessons = progress.completedLessonIds.size
            HomeDashboardUiState(
                isLoading = false,
                greeting = greetingFor(user.name),
                schoolYearLabel = profile?.let { "Year ${it.schoolYear} • ${it.studentTrack.displayName}" },
                level = progress.totalScore / XpPerLevel,
                activityCount = min(completedLessons, ActivityStreakCap),
                completedLessons = completedLessons
            )
        } catch (error: Exception) {
            HomeDashboardUiState(
                isLoading = false,
                greeting = greetingFor(sessionUser?.name),
                errorMessage = error.message ?: "Unknown error"
            )
        }
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
