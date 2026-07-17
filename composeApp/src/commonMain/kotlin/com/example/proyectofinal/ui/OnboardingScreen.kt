package com.example.proyectofinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.data.SchoolYearOption
import com.example.proyectofinal.domain.StudentTrack
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.theme.AppThemeDefaults

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onCompleted()
        }
    }

    OnboardingContent(
        state = state,
        onProvinceSelected = viewModel::selectProvince,
        onSchoolYearSelected = viewModel::selectSchoolYear,
        onTrackSelected = viewModel::selectTrack,
        onContinue = viewModel::nextStep,
        onBack = viewModel::goBack,
        onComplete = viewModel::completeOnboarding,
        onLogout = onLogout
    )
}

@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    onProvinceSelected: (String) -> Unit,
    onSchoolYearSelected: (Int) -> Unit,
    onTrackSelected: (StudentTrack) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Complete your onboarding",
                style = MaterialTheme.typography.headlineSmall
            )
            MButton(
                onClick = onLogout,
                enabled = !state.isSaving,
                style = MButtonStyle.Outline
            ) {
                Text("Logout")
            }
        }

        StepSummary(state)

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (state.currentStep) {
                OnboardingStep.PROVINCE -> ProvinceStep(
                    provinces = state.provinces,
                    selectedProvince = state.selectedProvince,
                    onProvinceSelected = onProvinceSelected,
                    enabled = !state.isSaving
                )

                OnboardingStep.SCHOOL_YEAR -> SchoolYearStep(
                    schoolYears = state.availableSchoolYears,
                    selectedSchoolYear = state.selectedSchoolYear,
                    onSchoolYearSelected = onSchoolYearSelected,
                    enabled = !state.isSaving
                )

                OnboardingStep.CATEGORY -> CategoryStep(
                    trackOptions = state.trackOptions,
                    selectedTrack = state.selectedTrack,
                    onTrackSelected = onTrackSelected,
                    enabled = !state.isSaving
                )

                OnboardingStep.CONFIRMATION -> ConfirmationStep(
                    state = state,
                    onComplete = onComplete
                )
            }
        }

        if (state.currentStep != OnboardingStep.CONFIRMATION) {
            MButton(
                onClick = onContinue,
                enabled = !state.isSaving && hasCurrentStepSelection(state),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }

        if (state.currentStep != OnboardingStep.PROVINCE) {
            MButton(
                onClick = onBack,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                style = MButtonStyle.Outline
            ) {
                Text("Back")
            }
        }
    }
}

private fun hasCurrentStepSelection(state: OnboardingUiState): Boolean =
    when (state.currentStep) {
        OnboardingStep.PROVINCE -> state.selectedProvince != null
        OnboardingStep.SCHOOL_YEAR -> state.selectedSchoolYear != null
        OnboardingStep.CATEGORY -> state.selectedTrack != null
        OnboardingStep.CONFIRMATION -> false
    }

@Composable
private fun StepSummary(state: OnboardingUiState) {
    val summary = buildList {
        state.selectedProvince?.let { add("Province: $it") }
        state.availableSchoolYears.firstOrNull { option -> option.schoolYear == state.selectedSchoolYear }
            ?.let { add("School year: ${it.label}") }
        state.selectedTrack?.let { add("Category: ${it.displayName}") }
    }

    if (summary.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = summary.joinToString(" • "),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ProvinceStep(
    provinces: List<String>,
    selectedProvince: String?,
    onProvinceSelected: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = "1. Choose your province",
            description = "Your province determines the valid school-year boundaries."
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(provinces) { province ->
                SelectionCard(
                    title = province,
                    selected = selectedProvince == province,
                    enabled = enabled,
                    onClick = { onProvinceSelected(province) }
                )
            }
        }
    }
}

@Composable
private fun SchoolYearStep(
    schoolYears: List<SchoolYearOption>,
    selectedSchoolYear: Int?,
    onSchoolYearSelected: (Int) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = "2. Choose your school year",
            description = "Available years already reflect the selected province structure."
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(schoolYears) { option ->
                SelectionCard(
                    title = option.label,
                    subtitle = allowedTrackSummary(option.allowedTracks),
                    selected = selectedSchoolYear == option.schoolYear,
                    enabled = enabled,
                    onClick = { onSchoolYearSelected(option.schoolYear) }
                )
            }
        }
    }
}

@Composable
private fun CategoryStep(
    trackOptions: List<OnboardingTrackOption>,
    selectedTrack: StudentTrack?,
    onTrackSelected: (StudentTrack) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = "3. Choose your category",
            description = "All four categories are shown. Only the valid ones for the chosen year are enabled."
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trackOptions) { option ->
                SelectionCard(
                    title = option.track.displayName,
                    subtitle = if (option.enabled) null else "Not available for the selected school year",
                    selected = selectedTrack == option.track,
                    enabled = enabled && option.enabled,
                    onClick = { onTrackSelected(option.track) }
                )
            }
        }
    }
}

@Composable
private fun ConfirmationStep(
    state: OnboardingUiState,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepTitle(
            title = "4. Confirm your profile",
            description = "Review the selected province, school year, and category before continuing to courses."
        )

        MCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Province: ${state.selectedProvince.orEmpty()}")
                Text(
                    text = "School year: ${state.availableSchoolYears.firstOrNull { option -> option.schoolYear == state.selectedSchoolYear }?.label.orEmpty()}"
                )
                Text("Category: ${state.selectedTrack?.displayName.orEmpty()}")
            }
        }

        MButton(
            onClick = onComplete,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSaving) "Saving profile..." else "Continue to courses")
        }
    }
}

@Composable
private fun StepTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectionCard(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    MCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun allowedTrackSummary(tracks: Set<StudentTrack>): String =
    tracks.joinToString { track -> track.displayName }
