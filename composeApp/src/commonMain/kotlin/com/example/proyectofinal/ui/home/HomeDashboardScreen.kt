package com.example.proyectofinal.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proyectofinal.ui.MainRouterViewModel
import com.example.proyectofinal.ui.primitives.MButton
import com.example.proyectofinal.ui.primitives.MButtonStyle
import com.example.proyectofinal.ui.primitives.MCard
import com.example.proyectofinal.ui.primitives.MLinearProgressIndicator
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.primitives.MTextField
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.achievement_placeholder
import proyectofinal.composeapp.generated.resources.home_join_course_action_join
import proyectofinal.composeapp.generated.resources.home_join_course_action_joining
import proyectofinal.composeapp.generated.resources.home_join_course_code_label
import proyectofinal.composeapp.generated.resources.home_join_course_code_placeholder
import proyectofinal.composeapp.generated.resources.home_join_course_description
import proyectofinal.composeapp.generated.resources.home_join_course_title
import proyectofinal.composeapp.generated.resources.home_level
import proyectofinal.composeapp.generated.resources.home_continue_learning_illustration_description
import proyectofinal.composeapp.generated.resources.home_action_go
import proyectofinal.composeapp.generated.resources.home_action_go_to_map
import proyectofinal.composeapp.generated.resources.home_action_open_lesson_map
import proyectofinal.composeapp.generated.resources.home_continue_learning_empty_description
import proyectofinal.composeapp.generated.resources.home_continue_learning_empty_title
import proyectofinal.composeapp.generated.resources.home_continue_learning_title
import proyectofinal.composeapp.generated.resources.home_course_progress_percent
import proyectofinal.composeapp.generated.resources.home_dashboard_subtitle
import proyectofinal.composeapp.generated.resources.home_greeting_wave
import proyectofinal.composeapp.generated.resources.home_in_progress_courses_header
import proyectofinal.composeapp.generated.resources.home_streak_days
import proyectofinal.composeapp.generated.resources.home_xp_progress
import proyectofinal.composeapp.generated.resources.ic_flame

@Composable
fun HomeDashboardScreen(
    router: MainRouterViewModel,
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
            DashboardHeader(uiState.greeting, uiState.streak)
            ProgressSummaryCard(uiState.level, uiState.currentXp, uiState.xpForNextLevel)
            if (uiState.hasEnrolledCourse) {
                if (uiState.inProgressCourses.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.home_in_progress_courses_header),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    uiState.inProgressCourses.forEach { course ->
                        CourseProgressCard(course = course, onOpenLessonMap = onOpenLessonMap)
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.home_continue_learning_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    ContinueLearningCard(onContinueLearning)
                }
                MButton(
                    onClick = onOpenLessonMap,
                    modifier = Modifier.fillMaxWidth(),
                    style = MButtonStyle.Outline
                ) {
                    Text(stringResource(Res.string.home_action_open_lesson_map))
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
private fun DashboardHeader(greeting: String, streak: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.home_greeting_wave, greeting),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            if (streak > 0) {
                StreakPill(streak)
            }
        }
        Text(
            text = stringResource(Res.string.home_dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreakPill(streak: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("homeStreakPill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_flame),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(Res.string.home_streak_days, streak),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ProgressSummaryCard(level: Int, currentXp: Int, xpForNextLevel: Int) {
    MCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.home_level, level), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(Res.string.home_xp_progress, currentXp, xpForNextLevel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            MLinearProgressIndicator(
                progress = { if (xpForNextLevel > 0) currentXp / xpForNextLevel.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun CourseProgressCard(course: HomeCourseProgress, onOpenLessonMap: () -> Unit) {
    MCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "÷",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(Res.string.home_course_progress_percent, course.progressPercent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                onClick = onOpenLessonMap,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = stringResource(Res.string.home_action_go),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
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
                contentDescription = stringResource(Res.string.home_continue_learning_illustration_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(Res.string.home_continue_learning_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.home_continue_learning_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            MButton(onClick = onContinueLearning, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.home_action_go_to_map))
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
                text = stringResource(Res.string.home_join_course_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(Res.string.home_join_course_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MTextField(
                value = joinCode,
                onValueChange = onJoinCodeChange,
                singleLine = true,
                label = { Text(stringResource(Res.string.home_join_course_code_label)) },
                placeholder = { Text(stringResource(Res.string.home_join_course_code_placeholder)) }
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
                Text(
                    if (isJoiningCourse) {
                        stringResource(Res.string.home_join_course_action_joining)
                    } else {
                        stringResource(Res.string.home_join_course_action_join)
                    }
                )
            }
        }
    }
}
