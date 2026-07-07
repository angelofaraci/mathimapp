package com.example.proyectofinal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.proyectofinal.di.appModule
import com.example.proyectofinal.di.rememberPlatformModule
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.ui.primitives.MProgressIndicator
import com.example.proyectofinal.ui.theme.AppTheme
import com.example.proyectofinal.ui.AuthGateRouter
import com.example.proyectofinal.ui.AuthView
import com.example.proyectofinal.ui.AuthenticatedHomeScaffold
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

@Composable
fun App() {
    val platformModule = rememberPlatformModule()

    KoinApplication(application = {
        modules(platformModule, appModule)
    }) {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
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
            MProgressIndicator()
        }
        return
    }

    when (resolveAuthView(session, target, onboardingComplete = onboardingComplete ?: false)) {
        AuthView.COURSE -> AuthenticatedHomeScaffold(onLogout = authRepository::logout)

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
