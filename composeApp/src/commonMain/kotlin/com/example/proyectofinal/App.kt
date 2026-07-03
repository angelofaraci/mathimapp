package com.example.proyectofinal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyectofinal.di.appModule
import com.example.proyectofinal.di.rememberPlatformModule
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.LearnerProfileRepository
import com.example.proyectofinal.domain.auth.SessionHydrationResult
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

private sealed interface AuthGateSessionState {
    data object Ready : AuthGateSessionState

    data object Loading : AuthGateSessionState

    data class Error(val message: String) : AuthGateSessionState
}

@Composable
fun App() {
    val platformModule = rememberPlatformModule()

    KoinApplication(application = {
        modules(platformModule, appModule)
    }) {
        Surface(modifier = Modifier.fillMaxSize()) {
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
    var hydrationRetryKey by remember(session.token) { mutableStateOf(0) }
    val sessionState by produceState<AuthGateSessionState>(
        initialValue = if (session.isAuthenticated && session.user == null) AuthGateSessionState.Loading else AuthGateSessionState.Ready,
        session.token,
        session.user,
        hydrationRetryKey
    ) {
        value = when {
            !session.isAuthenticated -> AuthGateSessionState.Ready
            session.user != null -> AuthGateSessionState.Ready
            else -> {
                value = AuthGateSessionState.Loading
                when (val result = authRepository.hydrateSessionIfNeeded()) {
                    SessionHydrationResult.Skipped,
                    SessionHydrationResult.ClearedInvalidSession,
                    is SessionHydrationResult.Hydrated -> AuthGateSessionState.Ready

                    is SessionHydrationResult.Failed -> AuthGateSessionState.Error(
                        result.message.ifBlank { "Unable to restore session" }
                    )
                }
            }
        }
    }
    val onboardingComplete by produceState<Boolean?>(
        initialValue = if (session.isAuthenticated && sessionState is AuthGateSessionState.Ready) null else false,
        session.isAuthenticated,
        session.token,
        session.user,
        sessionState,
        onboardingRefreshKey
    ) {
        value =
            if (session.isAuthenticated && sessionState is AuthGateSessionState.Ready) {
                learnerProfileRepository.isOnboardingComplete()
            }
            else false
    }

    if (session.isAuthenticated) {
        when (val currentSessionState = sessionState) {
            AuthGateSessionState.Loading -> {
                AuthGateLoading()
                return
            }

            is AuthGateSessionState.Error -> {
                AuthGateRestoreError(
                    message = currentSessionState.message,
                    onRetry = { hydrationRetryKey += 1 }
                )
                return
            }

            AuthGateSessionState.Ready -> {
                if (onboardingComplete == null) {
                    AuthGateLoading()
                    return
                }
            }
        }
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

@Composable
private fun AuthGateLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthGateRestoreError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(message)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                Text("Retry")
            }
        }
    }
}
