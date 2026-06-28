package com.example.proyectofinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.data.SchoolYearOption
import com.example.proyectofinal.domain.StudentTrack

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
        onBack = viewModel::goBack,
        onComplete = viewModel::completeOnboarding,
        onLogout = onLogout
    )
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    onProvinceSelected: (String) -> Unit,
    onSchoolYearSelected: (Int) -> Unit,
    onTrackSelected: (StudentTrack) -> Unit,
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
            OutlinedButton(onClick = onLogout, enabled = !state.isSaving) {
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

        if (state.currentStep != OnboardingStep.PROVINCE) {
            OutlinedButton(
                onClick = onBack,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
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
        Text(
            text = summary.joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProvinceStep(
    provinces: List<String>,
    selectedProvince: String?,
    onProvinceSelected: (String) -> Unit,
    enabled: Boolean
) {
    StepTitle(
        title = "1. Choose your province",
        description = "Your province determines the valid school-year boundaries."
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun SchoolYearStep(
    schoolYears: List<SchoolYearOption>,
    selectedSchoolYear: Int?,
    onSchoolYearSelected: (Int) -> Unit,
    enabled: Boolean
) {
    StepTitle(
        title = "2. Choose your school year",
        description = "Available years already reflect the selected province structure."
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun CategoryStep(
    trackOptions: List<OnboardingTrackOption>,
    selectedTrack: StudentTrack?,
    onTrackSelected: (StudentTrack) -> Unit,
    enabled: Boolean
) {
    StepTitle(
        title = "3. Choose your category",
        description = "All four categories are shown. Only the valid ones for the chosen year are enabled."
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ConfirmationStep(
    state: OnboardingUiState,
    onComplete: () -> Unit
) {
    StepTitle(
        title = "4. Confirm your profile",
        description = "Review the selected province, school year, and category before continuing to courses."
    )

    Card(modifier = Modifier.fillMaxWidth()) {
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

    Button(
        onClick = onComplete,
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Continue to courses")
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
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
