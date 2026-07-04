package com.example.proyectofinal.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.Course
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CourseDetailScreen(
    courseId: String,
    onBack: () -> Unit,
    viewModel: CourseDetailViewModel = koinViewModel<CourseDetailViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(courseId) {
        viewModel.load(courseId)
    }

    CourseDetailContent(
        uiState = uiState,
        onBack = onBack,
        onPrimaryAction = viewModel::onPrimaryAction,
        onDismissError = viewModel::dismissError
    )
}

@Composable
internal fun CourseDetailContent(
    uiState: CourseDetailUiState,
    onBack: () -> Unit,
    onPrimaryAction: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }

            uiState.errorMessage?.let {
                ErrorMessageCard(message = it, onDismissError = onDismissError)
            }

            val course = uiState.course
            if (course == null) {
                Text(
                    text = "Course unavailable",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Try going back to the catalog and opening the course again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CourseDetailBody(
                    course = course,
                    cta = uiState.cta,
                    isSubmitting = uiState.isSubmitting,
                    onPrimaryAction = onPrimaryAction
                )
            }
        }
    }
}

@Composable
private fun CourseDetailBody(
    course: Course,
    cta: CourseDetailCta,
    isSubmitting: Boolean,
    onPrimaryAction: () -> Unit
) {
    Text(
        text = course.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold
    )

    DetailMetaCard(course)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = course.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Button(
        onClick = onPrimaryAction,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSubmitting
    ) {
        Text(if (isSubmitting) "Loading..." else cta.label)
    }
}

@Composable
private fun DetailMetaCard(course: Course) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow(label = "Topic", value = course.topic ?: "Not provided")
            DetailRow(label = "Difficulty", value = course.difficulty ?: "Not provided")
            DetailRow(
                label = "Duration",
                value = course.durationMinutes?.let { "$it min" } ?: "Not provided"
            )
            DetailRow(label = "XP reward", value = course.xpReward?.toString() ?: "Not provided")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ErrorMessageCard(message: String, onDismissError: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onDismissError, modifier = Modifier.align(Alignment.End)) {
                Text("Dismiss")
            }
        }
    }
}

private val CourseDetailCta.label: String
    get() = when (this) {
        CourseDetailCta.Continue -> "Continue"
        CourseDetailCta.Enroll -> "Enroll"
        CourseDetailCta.Start -> "Start"
    }
