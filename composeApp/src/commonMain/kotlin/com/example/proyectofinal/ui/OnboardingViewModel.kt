package com.example.proyectofinal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyectofinal.data.ProvinceSchoolCatalog
import com.example.proyectofinal.data.SchoolYearOption
import com.example.proyectofinal.domain.LearnerProfile
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.StudentTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep {
    PROVINCE,
    SCHOOL_YEAR,
    CATEGORY,
    CONFIRMATION
}

data class OnboardingTrackOption(
    val track: StudentTrack,
    val enabled: Boolean
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.PROVINCE,
    val provinces: List<String> = ProvinceSchoolCatalog.provinces,
    val availableSchoolYears: List<SchoolYearOption> = emptyList(),
    val trackOptions: List<OnboardingTrackOption> = StudentTrack.entries.map {
        OnboardingTrackOption(track = it, enabled = false)
    },
    val selectedProvince: String? = null,
    val selectedSchoolYear: Int? = null,
    val selectedTrack: StudentTrack? = null,
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class OnboardingViewModel(
    private val learnerProfileRepository: LearnerProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectProvince(province: String) {
        val schoolYears = ProvinceSchoolCatalog.schoolYearOptionsFor(province)
        if (schoolYears.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Select a valid province")
            return
        }

        _uiState.value = _uiState.value.copy(
            availableSchoolYears = schoolYears,
            trackOptions = defaultTrackOptions(),
            selectedProvince = province,
            selectedSchoolYear = null,
            selectedTrack = null,
            errorMessage = null
        )
    }

    fun selectSchoolYear(schoolYear: Int) {
        val state = _uiState.value
        val selectedOption = state.availableSchoolYears.firstOrNull { option ->
            option.schoolYear == schoolYear
        }

        if (selectedOption == null) {
            _uiState.value = state.copy(errorMessage = "Select a valid school year")
            return
        }

        _uiState.value = state.copy(
            selectedSchoolYear = schoolYear,
            selectedTrack = null,
            trackOptions = buildTrackOptions(selectedOption.allowedTracks),
            errorMessage = null
        )
    }

    fun selectTrack(track: StudentTrack) {
        val state = _uiState.value
        val enabledTrack = state.trackOptions.firstOrNull { option ->
            option.track == track && option.enabled
        }

        if (enabledTrack == null) {
            _uiState.value = state.copy(
                errorMessage = "Selected category is not available for this school year"
            )
            return
        }

        _uiState.value = state.copy(
            selectedTrack = track,
            errorMessage = null
        )
    }

    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            OnboardingStep.PROVINCE -> {
                val province = state.selectedProvince
                if (province == null || ProvinceSchoolCatalog.schoolYearOptionsFor(province).isEmpty()) {
                    _uiState.value = state.copy(errorMessage = "Select a valid province")
                    return
                }

                _uiState.value = state.copy(
                    currentStep = OnboardingStep.SCHOOL_YEAR,
                    errorMessage = null
                )
            }

            OnboardingStep.SCHOOL_YEAR -> {
                val schoolYear = state.selectedSchoolYear
                if (schoolYear == null || state.availableSchoolYears.none { it.schoolYear == schoolYear }) {
                    _uiState.value = state.copy(errorMessage = "Select a valid school year")
                    return
                }

                _uiState.value = state.copy(
                    currentStep = OnboardingStep.CATEGORY,
                    errorMessage = null
                )
            }

            OnboardingStep.CATEGORY -> {
                val track = state.selectedTrack
                if (track == null || state.trackOptions.none { it.track == track && it.enabled }) {
                    _uiState.value = state.copy(
                        errorMessage = "Selected category is not available for this school year"
                    )
                    return
                }

                _uiState.value = state.copy(
                    currentStep = OnboardingStep.CONFIRMATION,
                    errorMessage = null
                )
            }

            OnboardingStep.CONFIRMATION -> Unit
        }
    }

    fun goBack() {
        val state = _uiState.value
        _uiState.value = when (state.currentStep) {
            OnboardingStep.PROVINCE -> state
            OnboardingStep.SCHOOL_YEAR -> state.copy(
                currentStep = OnboardingStep.PROVINCE,
                errorMessage = null
            )

            OnboardingStep.CATEGORY -> state.copy(
                currentStep = OnboardingStep.SCHOOL_YEAR,
                selectedTrack = null,
                errorMessage = null
            )

            OnboardingStep.CONFIRMATION -> state.copy(
                currentStep = OnboardingStep.CATEGORY,
                errorMessage = null
            )
        }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val province = state.selectedProvince
        val schoolYear = state.selectedSchoolYear
        val track = state.selectedTrack

        if (province == null || schoolYear == null || track == null) {
            _uiState.value = state.copy(
                errorMessage = "Complete every onboarding step before continuing"
            )
            return
        }

        if (!ProvinceSchoolCatalog.isValidSelection(province, schoolYear, track)) {
            _uiState.value = state.copy(
                errorMessage = "Selected category does not match the chosen school year"
            )
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            runCatching {
                learnerProfileRepository.upsertProfile(
                    LearnerProfile(
                        province = province,
                        schoolYear = schoolYear,
                        studentTrack = track,
                        onboardingComplete = true
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isCompleted = true,
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "Unable to save onboarding"
                )
            }
        }
    }

    private fun defaultTrackOptions(): List<OnboardingTrackOption> = buildTrackOptions(emptySet())

    private fun buildTrackOptions(allowedTracks: Set<StudentTrack>): List<OnboardingTrackOption> =
        StudentTrack.entries.map { track ->
            OnboardingTrackOption(track = track, enabled = track in allowedTracks)
        }
}
