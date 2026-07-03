package com.example.proyectofinal.domain.auth

import com.example.proyectofinal.data.AuthApi
import com.example.proyectofinal.data.KtorAuthRepository
import com.example.proyectofinal.data.UnauthorizedSessionException
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.di.InMemoryTokenStore
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SessionHydrationTest {
    private val apiConfig = ApiConfig("https://example.test")
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hydrateSessionIfNeeded fetches current user when token exists without user`() = runTest {
        val expectedUser = User(
            id = "student-1",
            name = "Alice",
            email = "alice@example.com",
            role = UserRole.STUDENT
        )
        val userRepository = FakeHydrationUserRepository(currentUser = expectedUser)
        val repository = createRepository(
            tokenStore = InMemoryTokenStore().apply { accessToken = "token-123" },
            userRepository = userRepository
        )

        val result = repository.hydrateSessionIfNeeded()

        assertIs<SessionHydrationResult.Hydrated>(result)
        assertEquals(expectedUser, repository.session.value.user)
        assertEquals(1, userRepository.currentUserCalls)
    }

    @Test
    fun `hydrateSessionIfNeeded skips network call when session is already hydrated`() = runTest {
        val expectedUser = User(
            id = "student-2",
            name = "Bob",
            email = "bob@example.com",
            role = UserRole.STUDENT
        )
        val userRepository = FakeHydrationUserRepository(currentUser = expectedUser)
        val repository = createRepository(
            tokenStore = InMemoryTokenStore().apply { accessToken = "token-456" },
            userRepository = userRepository
        )

        repository.hydrateSessionIfNeeded()
        val result = repository.hydrateSessionIfNeeded()

        assertEquals(SessionHydrationResult.Skipped, result)
        assertEquals(1, userRepository.currentUserCalls)
    }

    @Test
    fun `hydrateSessionIfNeeded clears invalid token after unauthorized response`() = runTest {
        val tokenStore = InMemoryTokenStore().apply { accessToken = "expired-token" }
        val repository = createRepository(
            tokenStore = tokenStore,
            userRepository = FakeHydrationUserRepository(error = UnauthorizedSessionException())
        )

        val result = repository.hydrateSessionIfNeeded()

        assertEquals(SessionHydrationResult.ClearedInvalidSession, result)
        assertNull(tokenStore.accessToken)
        assertNull(repository.session.value.token)
        assertNull(repository.session.value.user)
    }

    @Test
    fun `hydrateSessionIfNeeded preserves token and returns retryable failure on network error`() = runTest {
        val tokenStore = InMemoryTokenStore().apply { accessToken = "token-789" }
        val repository = createRepository(
            tokenStore = tokenStore,
            userRepository = FakeHydrationUserRepository(error = IllegalStateException("Network unavailable"))
        )

        val result = repository.hydrateSessionIfNeeded()

        assertEquals(
            SessionHydrationResult.Failed("Network unavailable"),
            result
        )
        assertEquals("token-789", tokenStore.accessToken)
        assertEquals("token-789", repository.session.value.token)
        assertNull(repository.session.value.user)
    }

    private fun createRepository(
        tokenStore: InMemoryTokenStore,
        userRepository: UserRepository
    ): KtorAuthRepository {
        val authApi = AuthApi(HttpClient(MockEngine { error("Auth API should not be called") }), apiConfig)
        return KtorAuthRepository(authApi, tokenStore, userRepository)
    }

    private class FakeHydrationUserRepository(
        private val currentUser: User? = null,
        private val error: Exception? = null
    ) : UserRepository {
        var currentUserCalls: Int = 0
            private set

        override suspend fun getCurrentUser(): User? {
            currentUserCalls += 1
            error?.let { throw it }
            return currentUser
        }

        override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

        override suspend fun updateUser(user: User) = Unit

        override suspend fun getUserProgress(userId: String): UserProgress = UserProgress(userId = userId)

        override suspend fun completeExercise(exerciseId: String, score: Int): ExerciseCompletionResponse {
            error("Not used in hydration tests")
        }
    }
}
