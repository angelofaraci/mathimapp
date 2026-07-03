package com.example.proyectofinal.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.Lesson
import org.koin.compose.viewmodel.koinViewModel

data class CourseEnrollmentUiState(
    val isEnrolling: Boolean = false,
    val errorMessage: String? = null
)

@Composable
fun CourseDetailScreen(
    courseId: String,
    onBack: () -> Unit,
    onEnroll: (String) -> Unit,
    localEnrolledCourseIds: Set<String> = emptySet(),
    enrollmentUiState: CourseEnrollmentUiState = CourseEnrollmentUiState(),
    viewModel: CourseDetailViewModel = koinViewModel<CourseDetailViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(courseId) {
        viewModel.load(courseId)
    }

    LaunchedEffect(courseId, localEnrolledCourseIds) {
        if (courseId in localEnrolledCourseIds) {
            viewModel.refresh()
        }
    }

    CourseDetailContent(
        uiState = uiState,
        enrollmentUiState = enrollmentUiState,
        localEnrolledCourseIds = localEnrolledCourseIds,
        onBack = onBack,
        onRetry = viewModel::retry,
        onEnroll = { onEnroll(courseId) }
    )
}

@Composable
internal fun CourseDetailContent(
    uiState: CourseDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEnroll: () -> Unit,
    enrollmentUiState: CourseEnrollmentUiState,
    localEnrolledCourseIds: Set<String>,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        uiState.errorMessage != null -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) {
                        Text("Volver")
                    }
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }

        else -> {
            val course = requireNotNull(uiState.course)
            val isEnrolled = uiState.isEnrolled || course.id in localEnrolledCourseIds
            val progress = if (uiState.totalLessons == 0) 0f else uiState.completedLessonsCount / uiState.totalLessons.toFloat()

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedButton(onClick = onBack) {
                        Text("Volver al catálogo")
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = course.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CourseDetailMetricChip(text = "${course.lessons.size} lecciones")
                            course.difficulty?.takeIf { it.isNotBlank() }?.let {
                                CourseDetailMetricChip(text = it)
                            }
                            course.xpReward?.let {
                                CourseDetailMetricChip(text = "$it XP")
                            }
                        }

                        Button(
                            onClick = onEnroll,
                            enabled = !isEnrolled && !enrollmentUiState.isEnrolling,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    isEnrolled -> "Ya estás inscripto"
                                    enrollmentUiState.isEnrolling -> "Inscribiendo..."
                                    else -> "Inscribirse en este curso"
                                }
                            )
                        }

                        enrollmentUiState.errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (isEnrolled) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tu progreso",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${uiState.completedLessonsCount}/${uiState.totalLessons} lecciones",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Lecciones del curso",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(items = course.lessons, key = Lesson::id) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        isCompleted = lesson.id in uiState.completedCourseLessonIds
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseDetailMetricChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun LessonCard(lesson: Lesson, isCompleted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCompleted) "✓" else "→",
                style = MaterialTheme.typography.titleLarge,
                color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${lesson.exerciseCount} ejercicios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
