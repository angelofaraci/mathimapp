package com.example.proyectofinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.proyectofinal.ui.home.HomeDashboardContent
import com.example.proyectofinal.ui.home.HomeDashboardUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    MaterialTheme {
        HomeDashboardContent(
            uiState = HomeDashboardUiState(
                isLoading = false,
                greeting = "Buenos días, María",
                schoolYearLabel = "Year 7 • Secondary",
                level = 2,
                activityCount = 5,
                completedLessons = 5
            ),
            onContinueLearning = {},
            onOpenCatalog = {},
            onLogout = {}
        )
    }
}
