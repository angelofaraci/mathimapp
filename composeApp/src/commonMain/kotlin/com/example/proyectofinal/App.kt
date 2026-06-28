package com.example.proyectofinal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.data.MockCourseRepository
import com.example.proyectofinal.di.appModule
import com.example.proyectofinal.di.rememberPlatformModule
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.ui.CourseViewModel
import com.example.proyectofinal.ui.AuthGateRouter
import com.example.proyectofinal.ui.AuthView
import com.example.proyectofinal.ui.CourseUiState
import com.example.proyectofinal.ui.LoginScreen
import com.example.proyectofinal.ui.LoginViewModel
import com.example.proyectofinal.ui.OnboardingScreen
import com.example.proyectofinal.ui.OnboardingViewModel
import com.example.proyectofinal.ui.RegisterScreen
import com.example.proyectofinal.ui.RegisterViewModel
import com.example.proyectofinal.ui.resolveAuthView
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App() {
    val platformModule = rememberPlatformModule()

    KoinApplication(application = {
        modules(platformModule, appModule)
    }) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            MaterialTheme {
                AuthGate()
            }
        }
    }
}

@Composable
private fun AuthGate() {
    val authRepository = koinInject<AuthRepository>()
    val learnerProfileRepository = koinInject<LearnerProfileRepository>()
    val session by authRepository.session.collectAsState()
    val router = remember { AuthGateRouter() }
    val target by router.target.collectAsState()
    var onboardingRefreshKey by remember(session.token) { mutableStateOf(0) }
    val onboardingComplete by produceState<Boolean?>(
        initialValue = if (session.isAuthenticated) null else false,
        key1 = session.isAuthenticated,
        key2 = session.token,
        key3 = onboardingRefreshKey
    ) {
        value =
            if (session.isAuthenticated) learnerProfileRepository.isOnboardingComplete()
            else false
    }

    if (session.isAuthenticated && onboardingComplete == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    when (resolveAuthView(session, target, onboardingComplete = onboardingComplete ?: false)) {
        AuthView.COURSE -> CourseScreen(
            onLogout = { authRepository.logout() }
        )

        AuthView.LOGIN -> LoginScreen(
            viewModel = koinViewModel<LoginViewModel>(),
            onSwitchToRegister = router::switchToRegister
        )

        AuthView.REGISTER -> RegisterScreen(
            viewModel = koinViewModel<RegisterViewModel>(),
            onSwitchToLogin = router::switchToLogin
        )

        AuthView.ONBOARDING -> OnboardingScreen(
            viewModel = koinViewModel<OnboardingViewModel>(),
            onCompleted = { onboardingRefreshKey += 1 },
            onLogout = authRepository::logout
        )
    }
}

@Composable
private fun CourseScreen(
    onLogout: () -> Unit,
    viewModel: CourseViewModel = koinViewModel<CourseViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()
    CourseContent(uiState, onLogout)
}

@Composable
private fun CourseContent(uiState: CourseUiState, onLogout: () -> Unit = {}) {
    when (val state = uiState) {
        is CourseUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is CourseUiState.Success -> {
            CourseList(state.courses, onLogout)
        }
        is CourseUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PreviewAppContent() {
    MaterialTheme {
        val repository = remember { MockCourseRepository() }
        val learnerProfileRepository = remember {
            object : LearnerProfileRepository {
                override suspend fun getProfile() = null

                override suspend fun isOnboardingComplete(): Boolean = false

                override suspend fun upsertProfile(profile: com.example.proyectofinal.domain.LearnerProfile) = Unit
            }
        }
        val viewModel = remember { CourseViewModel(repository, learnerProfileRepository) }
        val uiState by viewModel.uiState.collectAsState()
        CourseContent(uiState)
    }
}

@Composable
@Preview
private fun AppPreview() {
    PreviewAppContent()
}

@Composable
@Preview
private fun CourseListEmptyPreview() {
    MaterialTheme {
        CourseList(emptyList()) {}
    }
}

@Composable
fun CourseList(courses: List<Course>, onLogout: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome to MathApp!",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (courses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No courses available yet.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(courses) { course ->
                    CourseCard(course)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCard(course: Course) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { /* Handle Click */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = course.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = course.description, style = MaterialTheme.typography.bodyMedium)
            
            if (course.lessons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${course.lessons.size} Lessons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
