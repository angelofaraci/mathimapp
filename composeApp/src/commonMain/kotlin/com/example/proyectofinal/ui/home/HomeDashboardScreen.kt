package com.example.proyectofinal.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.ui.MainRouter
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.primitives.MTextField
import com.example.proyectofinal.ui.theme.AppThemeDefaults
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.painterResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.achievement_placeholder

@Composable
fun HomeDashboardScreen(
    router: MainRouter,
    onLogout: () -> Unit,
    viewModel: HomeDashboardViewModel = koinViewModel<HomeDashboardViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeDashboardContent(
        uiState = uiState,
        onContinueLearning = { viewModel.openActivities(router::showActivities) },
        onOpenLessonMap = { viewModel.openActivities(router::showActivities) },
        onJoinCourse = { code -> viewModel.joinCourse(code, router::showActivities) },
        onLogout = onLogout
    )
}

@Composable
internal fun HomeDashboardContent(
    uiState: HomeDashboardUiState,
    onContinueLearning: () -> Unit,
    onOpenLessonMap: () -> Unit,
    onJoinCourse: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var joinCode by remember { mutableStateOf("") }

    LaunchedEffect(uiState.hasEnrolledCourse) {
        if (uiState.hasEnrolledCourse) {
            joinCode = ""
        }
    }

    when {
        uiState.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MProgressIndicator()
        }

        uiState.errorMessage != null -> Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DashboardHeader(uiState.greeting, uiState.schoolYearLabel)
            ProgressSummaryCard(uiState.level, uiState.activityCount, uiState.completedLessons)
            if (uiState.hasEnrolledCourse) {
                Text(
                    text = "Continuar aprendiendo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                ContinueLearningCard(onContinueLearning)
                MButton(
                    onClick = onOpenLessonMap,
                    modifier = Modifier.fillMaxWidth(),
                    style = MButtonStyle.Outline
                ) {
                    Text("Abrir mapa de lecciones")
                }
            } else {
                JoinCourseCard(
                    joinCode = joinCode,
                    onJoinCodeChange = { joinCode = it },
                    isJoiningCourse = uiState.isJoiningCourse,
                    joinCourseMessage = uiState.joinCourseMessage,
                    onJoinCourse = { onJoinCourse(joinCode) }
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(greeting: String, schoolYearLabel: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(greeting, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            text = "Tu inicio reúne tu progreso y el siguiente paso recomendado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        schoolYearLabel?.let {
            Surface(
                shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ProgressSummaryCard(level: Int, activityCount: Int, completedLessons: Int) {
    MCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Surface(
                shape = RoundedCornerShape(AppThemeDefaults.shapes.pill),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Nivel $level • Actividad $activityCount",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = "$completedLessons lecciones completadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContinueLearningCard(onContinueLearning: () -> Unit) {
    MCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(Res.drawable.achievement_placeholder),
                contentDescription = "Learning placeholder illustration",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Todavía no tienes una actividad en curso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Explora el mapa de lecciones para avanzar una actividad a la vez y repasar teoría cuando lo necesites.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            MButton(onClick = onContinueLearning, modifier = Modifier.fillMaxWidth()) {
                Text("Ir al mapa")
            }
        }
    }
}

@Composable
private fun JoinCourseCard(
    joinCode: String,
    onJoinCodeChange: (String) -> Unit,
    isJoiningCourse: Boolean,
    joinCourseMessage: String?,
    onJoinCourse: () -> Unit
) {
    MCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Join a course to unlock activities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Enter the code your teacher shared to start your lesson map.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MTextField(
                value = joinCode,
                onValueChange = onJoinCodeChange,
                singleLine = true,
                label = { Text("Course code") },
                placeholder = { Text("Example: FRACTIONS-7A") }
            )
            joinCourseMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            MButton(
                onClick = onJoinCourse,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isJoiningCourse
            ) {
                Text(if (isJoiningCourse) "Joining course..." else "Join course")
            }
        }
    }
}
