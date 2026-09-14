package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.ProfilePreferences
import com.example.proyectofinal.models.SupportedLanguage
import com.example.proyectofinal.models.AvatarId
import com.example.proyectofinal.models.UpdateIdentityRequest
import com.example.proyectofinal.models.ChangePasswordRequest
import com.example.proyectofinal.models.DeleteAccountRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.ui.localization.AppLanguage
import com.example.proyectofinal.ui.localization.AppLocaleController
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
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val schoolYear: Int? = null,
    val studentTrack: StudentTrack? = null,
    val level: Int = 0,
    val currentXp: Int = 0,
    val xpForNextLevel: Int = XpPerLevel,
    val streak: Int = 0,
    val completedLessons: Int = 0,
    val achievements: List<ProfileAchievement> = emptyList(),
    val preferences: ProfilePreferences = ProfilePreferences(),
    val isSaving: Boolean = false,
    val accountDeleted: Boolean = false,
    val errorMessage: String? = null
)

data class ProfileAchievement(val id: String, val name: String, val isUnlocked: Boolean)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val learnerProfileRepository: LearnerProfileRepository,
    private val localeController: AppLocaleController
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun updateIdentity(name: String, email: String) = runProfileAction {
        val user = userRepository.updateIdentity(UpdateIdentityRequest(name, email))
        _uiState.value = _uiState.value.copy(displayName = user.name, email = user.email)
    }

    fun changePassword(currentPassword: String, newPassword: String) = runProfileAction {
        userRepository.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    fun updatePreferences(preferences: ProfilePreferences) = runProfileAction {
        _uiState.value = _uiState.value.copy(preferences = userRepository.updateProfilePreferences(preferences))
    }

    fun updateLanguage(language: SupportedLanguage) = runProfileAction {
        val preferences = userRepository.updateProfilePreferences(
            _uiState.value.preferences.copy(language = language)
        )
        val accountId = requireNotNull(authRepository.session.value.user?.id) {
            "Authenticated user not available"
        }
        localeController.apply(accountId, language.toAppLanguage())
        _uiState.value = _uiState.value.copy(preferences = preferences)
    }

    fun updateAvatar(avatarId: AvatarId) = runProfileAction {
        _uiState.value = _uiState.value.copy(preferences = userRepository.updateAvatar(com.example.proyectofinal.models.UpdateAvatarRequest(avatarId)))
    }

    fun deleteAccount(currentPassword: String) = runProfileAction {
        userRepository.deleteAccount(DeleteAccountRequest(currentPassword))
        authRepository.logout()
        _uiState.value = _uiState.value.copy(accountDeleted = true)
    }

    private fun runProfileAction(action: suspend () -> Unit) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        try { action() } catch (error: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Profile update failed")
        } finally {
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }
    private fun loadProfile() = viewModelScope.launch {
        val sessionUser = authRepository.session.value.user
        _uiState.value = ProfileUiState(isLoading = true)

        _uiState.value = try {
            val user = sessionUser ?: error("Authenticated user not available")
            val progress = userRepository.getUserProgress(user.id)
            val profile = learnerProfileRepository.getProfile(user.id)
            val preferences = runCatching { userRepository.getProfilePreferences() }.getOrDefault(ProfilePreferences())
            ProfileUiState(
                isLoading = false,
                displayName = user.name,
                email = user.email,
                role = user.role,
                schoolYear = profile?.schoolYear,
                studentTrack = profile?.studentTrack,
                level = progress.totalScore / XpPerLevel,
                currentXp = progress.totalScore % XpPerLevel,
                xpForNextLevel = XpPerLevel,
                streak = min(progress.completedLessonIds.size, ActivityStreakCap),
                completedLessons = progress.completedLessonIds.size,
                achievements = progress.toAchievements(),
                preferences = preferences
            )
        } catch (error: Exception) {
            ProfileUiState(
                isLoading = false,
                displayName = sessionUser?.name.orEmpty(),
                email = "",
                role = UserRole.STUDENT,
                errorMessage = error.message ?: "Unknown error"
            )
        }
    }
}

private fun SupportedLanguage.toAppLanguage(): AppLanguage = when (this) {
    SupportedLanguage.SPANISH -> AppLanguage.SPANISH
    SupportedLanguage.ENGLISH -> AppLanguage.ENGLISH
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
