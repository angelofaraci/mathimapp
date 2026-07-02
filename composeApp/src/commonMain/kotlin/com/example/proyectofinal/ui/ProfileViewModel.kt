package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.UserProgress
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal const val XpPerLevel = 100
internal const val ActivityStreakCap = 7
private const val FirstLessonThreshold = 1
private const val Score100Threshold = 100
private const val Lessons5Threshold = 5
private const val Lessons10Threshold = 10

data class ProfileUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val schoolYearLabel: String? = null,
    val level: Int = 0,
    val currentXp: Int = 0,
    val xpForNextLevel: Int = XpPerLevel,
    val streak: Int = 0,
    val completedLessons: Int = 0,
    val achievements: List<ProfileAchievement> = emptyList(),
    val errorMessage: String? = null
)

data class ProfileAchievement(val id: String, val name: String, val isUnlocked: Boolean)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val learnerProfileRepository: LearnerProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() = viewModelScope.launch {
        val sessionUser = authRepository.session.value.user
        _uiState.value = ProfileUiState(isLoading = true)

        _uiState.value = try {
            val user = sessionUser ?: error("Authenticated user not available")
            val progress = userRepository.getUserProgress(user.id)
            val profile = learnerProfileRepository.getProfile()
            ProfileUiState(
                isLoading = false,
                displayName = user.name,
                schoolYearLabel = profile?.let { "Year ${it.schoolYear} • ${it.studentTrack.displayName}" },
                level = progress.totalScore / XpPerLevel,
                currentXp = progress.totalScore % XpPerLevel,
                xpForNextLevel = XpPerLevel,
                streak = min(progress.completedLessonIds.size, ActivityStreakCap),
                completedLessons = progress.completedLessonIds.size,
                achievements = progress.toAchievements()
            )
        } catch (error: Exception) {
            ProfileUiState(
                isLoading = false,
                displayName = sessionUser?.name.orEmpty(),
                errorMessage = error.message ?: "Unknown error"
            )
        }
    }
}

private fun UserProgress.toAchievements(): List<ProfileAchievement> {
        val completedLessons = completedLessonIds.size
    return listOf(
        ProfileAchievement("first_lesson", "First Lesson", completedLessons >= FirstLessonThreshold),
        ProfileAchievement("score_100", "Score 100", totalScore >= Score100Threshold),
        ProfileAchievement("lessons_5", "5 Lessons Completed", completedLessons >= Lessons5Threshold),
        ProfileAchievement("lessons_10", "10 Lessons Completed", completedLessons >= Lessons10Threshold)
    )
}
