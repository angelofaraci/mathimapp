package com.example.proyectofinal.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.Course
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CourseCatalogScreen(
    onCourseSelected: (String) -> Unit,
    viewModel: CourseCatalogViewModel = koinViewModel<CourseCatalogViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()

    CourseCatalogContent(
        uiState = uiState,
        onQueryChange = viewModel::updateQuery,
        onTopicSelected = viewModel::toggleTopic,
        onRetry = viewModel::retry,
        onCourseSelected = onCourseSelected
    )
}

@Composable
internal fun CourseCatalogContent(
    uiState: CourseCatalogUiState,
    onQueryChange: (String) -> Unit,
    onTopicSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onCourseSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Catálogo de cursos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar por nombre") },
            singleLine = true
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.topics) { topic ->
                FilterChip(
                    selected = uiState.selectedTopic == topic,
                    onClick = { onTopicSelected(topic) },
                    label = { Text(topic) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val remoteState = uiState.remoteState) {
                CourseCatalogRemoteState.Loading -> CatalogCenteredState {
                    CircularProgressIndicator()
                }

                is CourseCatalogRemoteState.Error -> CatalogCenteredState {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = remoteState.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onRetry) {
                            Text("Reintentar")
                        }
                    }
                }

                is CourseCatalogRemoteState.Success -> {
                    if (uiState.visibleCourses.isEmpty()) {
                        val message = if (uiState.hasActiveFilters) {
                            "No se encontraron cursos para los filtros seleccionados."
                        } else {
                            "No hay cursos disponibles en este momento."
                        }

                        CatalogCenteredState {
                            Text(
                                text = message,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = uiState.visibleCourses, key = Course::id) { course ->
                                CourseCatalogCard(
                                    course = course,
                                    onClick = { onCourseSelected(course.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCenteredState(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun CourseCatalogCard(course: Course, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course-card-${course.id}")
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(course.description, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(4.dp))

            Text("Tema: ${course.topic ?: "--"}", style = MaterialTheme.typography.bodyMedium)
            Text("Nivel: ${course.difficulty ?: "--"}", style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Duración: ${course.durationMinutes?.let { "$it min" } ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "XP: ${course.xpReward ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
