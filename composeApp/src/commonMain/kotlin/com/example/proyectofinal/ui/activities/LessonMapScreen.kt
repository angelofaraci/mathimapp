package com.example.proyectofinal.ui.activities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LessonMapScreen(
    onShowHome: () -> Unit = {},
    viewModel: LessonMapViewModel = koinViewModel<LessonMapViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()

    LessonMapContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onShowHome = onShowHome,
        onExerciseSelected = viewModel::selectExercise,
        onDismissActiveExercise = viewModel::dismissActiveExercise,
        onAnswerSelected = viewModel::selectAnswer,
        onSubmitAnswer = viewModel::submitAnswer,
        onOpenTheory = viewModel::openTheory,
        onDismissTheory = viewModel::dismissTheory
    )
}

@Composable
internal fun LessonMapContent(
    uiState: LessonMapUiState,
    onRetry: () -> Unit,
    onShowHome: () -> Unit,
    onExerciseSelected: (String) -> Unit,
    onDismissActiveExercise: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onOpenTheory: () -> Unit,
    onDismissTheory: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MProgressIndicator()
        }

        uiState.errorMessage != null -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                MButton(onClick = onRetry) {
                    Text("Retry")
                }
                MButton(onClick = onShowHome, style = MButtonStyle.Outline) {
                    Text("Go to Home")
                }
            }
        }

        else -> {
            val lessonMap = uiState.lessonMap ?: return
            val activeExercise = uiState.activeExercise

            if (activeExercise != null) {
                ExercisePlayerContent(
                    exercise = activeExercise,
                    exerciseNumber = uiState.activeNode?.index,
                    selectedAnswer = uiState.selectedAnswer,
                    isSubmitting = uiState.isSubmittingAnswer,
                    feedbackMessage = uiState.exerciseFeedbackMessage,
                    onBack = onDismissActiveExercise,
                    onAnswerSelected = onAnswerSelected,
                    onSubmitAnswer = onSubmitAnswer,
                    modifier = modifier
                )
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    LessonMapHeader(
                        title = lessonMap.lesson.title,
                        totalExercises = lessonMap.exercises.size,
                        completedExercises = uiState.nodes.count { it.state == LessonNodeState.Completed }
                    )

                    MButton(
                        onClick = onOpenTheory,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isTheoryAvailable,
                        style = MButtonStyle.Outline
                    ) {
                        Text("View theory")
                    }

                    uiState.exerciseFeedbackMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    uiState.activeNode?.let { activeNode ->
                        ActiveExerciseCard(activeNode)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        uiState.nodes.forEachIndexed { index, node ->
                            LessonMapNode(
                                node = node,
                                onClick = { onExerciseSelected(node.exercise.id) }
                            )

                            if (index < uiState.nodes.lastIndex) {
                                LessonMapConnector(
                                    isOpenPath = node.state != LessonNodeState.Locked
                                )
                            }
                        }
                    }
                }
            }

            uiState.selectedTheoryLesson?.let { lesson ->
                TheorySheet(
                    lesson = lesson,
                    onDismiss = onDismissTheory
                )
            }
        }
    }
}

@Composable
private fun ExercisePlayerContent(
    exercise: Exercise,
    exerciseNumber: Int?,
    selectedAnswer: String?,
    isSubmitting: Boolean,
    feedbackMessage: String?,
    onBack: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MButton(
            onClick = onBack,
            style = MButtonStyle.Outline
        ) {
            Text("Back to lesson map")
        }

        MCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = exerciseNumber?.let { "Exercise $it" } ?: "Exercise",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = exercise.question,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            exercise.options.forEach { option ->
                AnswerOptionCard(
                    option = option,
                    isSelected = selectedAnswer == option,
                    onClick = { onAnswerSelected(option) }
                )
            }
        }

        feedbackMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        MButton(
            onClick = onSubmitAnswer,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedAnswer != null && !isSubmitting
        ) {
            Text(if (isSubmitting) "Saving progress..." else "Submit answer")
        }
    }
}

@Composable
private fun AnswerOptionCard(
    option: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    MCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = isSelected, onClick = onClick)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = option,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun optionLabel(option: String): String = option.firstOrNull()?.uppercase() ?: "?"

@Composable
private fun LessonMapHeader(
    title: String,
    totalExercises: Int,
    completedExercises: Int
) {
    MCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Activities",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Move one exercise at a time. Theory stays available for the whole lesson.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$completedExercises of $totalExercises exercises completed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ActiveExerciseCard(node: LessonMapNodeUiModel) {
    MCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (node.state == LessonNodeState.Current) "Current exercise" else "Next available exercise",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = node.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun LessonMapConnector(isOpenPath: Boolean) {
    Box(
        modifier = Modifier
            .padding(start = 21.dp)
            .width(2.dp)
            .height(18.dp)
            .background(
                color = if (isOpenPath) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
    )
}
