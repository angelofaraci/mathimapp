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
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.ui.CourseUiState
import com.example.proyectofinal.ui.CourseViewModel
import org.koin.compose.KoinApplication
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
                CourseScreen()
            }
        }
    }
}

@Composable
private fun CourseScreen(viewModel: CourseViewModel = koinViewModel<CourseViewModel>()) {
    val uiState by viewModel.uiState.collectAsState()
    CourseContent(uiState)
}

@Composable
private fun CourseContent(uiState: CourseUiState) {
    when (val state = uiState) {
        is CourseUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is CourseUiState.Success -> {
            CourseList(state.courses)
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
        val viewModel = remember { CourseViewModel(repository) }
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
        CourseList(emptyList())
    }
}

@Composable
fun CourseList(courses: List<Course>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Welcome to MathApp!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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
