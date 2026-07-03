package com.example.proyectofinal.data

import com.example.proyectofinal.di.TokenStore
import com.example.proyectofinal.domain.AuthRepository
import com.example.proyectofinal.domain.AuthSession
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.domain.auth.SessionHydrationResult
import com.example.proyectofinal.models.AuthResponse
import com.example.proyectofinal.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KtorAuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val userRepository: UserRepository
) : AuthRepository {

    private val _session = MutableStateFlow(
        tokenStore.accessToken
            ?.takeIf { it.isNotBlank() }
            ?.let { AuthSession(token = it) }
            ?: AuthSession()
    )

    override val session: StateFlow<AuthSession> = _session.asStateFlow()

    override suspend fun login(email: String, password: String): Result<User> =
        authenticate { authApi.login(email = email, password = password) }

    override suspend fun register(name: String, email: String, password: String): Result<User> =
        authenticate { authApi.register(name = name, email = email, password = password) }

    override suspend fun hydrateSessionIfNeeded(): SessionHydrationResult {
        val currentSession = _session.value
        val token = currentSession.token?.takeIf { it.isNotBlank() }
            ?: return SessionHydrationResult.Skipped

        if (currentSession.user != null) {
            return SessionHydrationResult.Skipped
        }

        return try {
            val user = userRepository.getCurrentUser()
                ?: error("Authenticated user not available")
            _session.value = AuthSession(token = token, user = user)
            SessionHydrationResult.Hydrated(user)
        } catch (_: UnauthorizedSessionException) {
            logout()
            SessionHydrationResult.ClearedInvalidSession
        } catch (error: Exception) {
            SessionHydrationResult.Failed(
                error.message ?: "Unable to restore session"
            )
        }
    }

    override fun logout() {
        tokenStore.accessToken = null
        _session.value = AuthSession()
    }

    private suspend fun authenticate(request: suspend () -> AuthResponse): Result<User> =
        runCatching { request() }
            .map { response ->
                tokenStore.accessToken = response.token
                _session.value = AuthSession(token = response.token, user = response.user)
                response.user
            }
}
