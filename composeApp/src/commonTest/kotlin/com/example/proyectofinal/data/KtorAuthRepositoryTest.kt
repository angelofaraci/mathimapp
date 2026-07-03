package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.di.InMemoryTokenStore
import com.example.proyectofinal.domain.UserRepository
import com.example.proyectofinal.models.AuthResponse
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KtorAuthRepositoryTest {
    private val apiConfig = ApiConfig("https://example.test")
    private val json = Json { ignoreUnknownKeys = true }
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
    fun `login posts shared payload and stores authenticated session`() = runTest {
        val expectedUser = User(
            id = "user-1",
            name = "Alice",
            email = "alice@example.com",
            role = UserRole.STUDENT
        )
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null

        val repository = createRepository(
            engine = MockEngine { request ->
                capturedPath = request.url.encodedPath
                capturedMethod = request.method

                respond(
                    content = json.encodeToString(
                        AuthResponse(
                            token = "token-123",
                            user = expectedUser
                        )
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )

        val result = repository.repository.login(
            email = "alice@example.com",
            password = "secret"
        )

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        assertEquals("/auth/login", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertEquals("token-123", repository.tokenStore.accessToken)
        assertEquals(
            "token-123",
            repository.repository.session.value.token
        )
        assertEquals(expectedUser, repository.repository.session.value.user)
        assertTrue(repository.repository.session.value.isAuthenticated)
    }

    @Test
    fun `register posts shared payload and stores authenticated student session`() = runTest {
        val expectedUser = User(
            id = "user-2",
            name = "Bob",
            email = "bob@example.com",
            role = UserRole.STUDENT
        )
        var capturedPath: String? = null

        val repository = createRepository(
            engine = MockEngine { request ->
                capturedPath = request.url.encodedPath

                respond(
                    content = json.encodeToString(
                        AuthResponse(
                            token = "token-456",
                            user = expectedUser
                        )
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )

        val result = repository.repository.register(
            name = "Bob",
            email = "bob@example.com",
            password = "top-secret"
        )

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        assertEquals("/auth/register", capturedPath)
        assertEquals(UserRole.STUDENT, repository.repository.session.value.user?.role)
        assertEquals("token-456", repository.tokenStore.accessToken)
        assertTrue(repository.repository.session.value.isAuthenticated)
    }

    @Test
    fun `register surfaces raw server error text and preserves anonymous session`() = runTest {
        val repository = createRepository(
            engine = MockEngine {
                respond(
                    content = "Email already registered",
                    status = HttpStatusCode.Conflict,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        )

        val result = repository.repository.register(
            name = "Taken",
            email = "taken@example.com",
            password = "secret"
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Email already registered",
            result.exceptionOrNull()?.message
        )
        assertNull(repository.tokenStore.accessToken)
        assertNull(repository.repository.session.value.token)
        assertNull(repository.repository.session.value.user)
        assertFalse(repository.repository.session.value.isAuthenticated)
    }

    @Test
    fun `logout clears token and session even when already anonymous`() = runTest {
        val expectedUser = User(
            id = "user-3",
            name = "Carol",
            email = "carol@example.com",
            role = UserRole.STUDENT
        )

        val repository = createRepository(
            engine = MockEngine {
                respond(
                    content = json.encodeToString(
                        AuthResponse(
                            token = "token-789",
                            user = expectedUser
                        )
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        )

        repository.repository.login(
            email = "carol@example.com",
            password = "secret"
        )

        repository.repository.logout()

        assertNull(repository.tokenStore.accessToken)
        assertNull(repository.repository.session.value.token)
        assertNull(repository.repository.session.value.user)
        assertFalse(repository.repository.session.value.isAuthenticated)

        repository.repository.logout()

        assertNull(repository.tokenStore.accessToken)
        assertFalse(repository.repository.session.value.isAuthenticated)
    }

    private fun createRepository(engine: MockEngine): AuthRepositoryFixture {
        val tokenStore = InMemoryTokenStore()
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val api = AuthApi(client, apiConfig)

        return AuthRepositoryFixture(
            repository = KtorAuthRepository(api, tokenStore, FakeUserRepository()),
            tokenStore = tokenStore
        )
    }

    private data class AuthRepositoryFixture(
        val repository: KtorAuthRepository,
        val tokenStore: InMemoryTokenStore
    )

    private class FakeUserRepository : UserRepository {
        override suspend fun getCurrentUser(): User? = null

        override suspend fun getUserRole(userId: String): UserRole = UserRole.STUDENT

        override suspend fun updateUser(user: User) = Unit

        override suspend fun getUserProgress(userId: String): UserProgress = UserProgress(userId = userId)

        override suspend fun completeExercise(exerciseId: String, score: Int): ExerciseCompletionResponse {
            error("Not used in auth tests")
        }
    }
}
