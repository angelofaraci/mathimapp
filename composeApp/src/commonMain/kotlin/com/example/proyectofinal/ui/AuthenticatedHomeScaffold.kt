package com.example.proyectofinal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.proyectofinal.ui.catalog.CourseCatalogScreen
import com.example.proyectofinal.ui.catalog.CourseDetailScreen
import com.example.proyectofinal.ui.home.HomeDashboardScreen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import proyectofinal.composeapp.generated.resources.Res
import proyectofinal.composeapp.generated.resources.tab_activities
import proyectofinal.composeapp.generated.resources.tab_home
import proyectofinal.composeapp.generated.resources.tab_profile
import proyectofinal.composeapp.generated.resources.tab_progress

@Composable
fun AuthenticatedHomeScaffold(
    onLogout: () -> Unit,
    router: MainRouter = remember { MainRouter() }
) {
    val selectedTab by router.target.collectAsState()
    var selectedCourseId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                mainDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedTab == destination.tab,
                        onClick = { router.select(destination.tab) },
                        icon = {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.HOME -> HomeDashboardScreen(router = router, onLogout = onLogout)
                MainTab.ACTIVITIES -> {
                    val courseId = selectedCourseId

                    if (courseId == null) {
                        CourseCatalogScreen(onCourseSelected = { selectedCourseId = it })
                    } else {
                        CourseDetailScreen(
                            courseId = courseId,
                            onBack = { selectedCourseId = null }
                        )
                    }
                }
                MainTab.PROGRESS -> PlaceholderScreen(title = "Progreso")
                MainTab.PROFILE -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}

private data class MainDestination(
    val tab: MainTab,
    val label: String,
    val icon: DrawableResource
)

private val mainDestinations = listOf(
    MainDestination(MainTab.HOME, "Inicio", Res.drawable.tab_home),
    MainDestination(MainTab.ACTIVITIES, "Actividades", Res.drawable.tab_activities),
    MainDestination(MainTab.PROGRESS, "Progreso", Res.drawable.tab_progress),
    MainDestination(MainTab.PROFILE, "Perfil", Res.drawable.tab_profile)
)
