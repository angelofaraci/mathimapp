package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.*
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorUserRepositoryTest {
    private lateinit var database: AppDatabase
    private val apiConfig = ApiConfig("https://example.test")
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        val driver = createTestDriver()

        val intAdapter = object : ColumnAdapter<Int, Long> {
            override fun decode(databaseValue: Long): Int = databaseValue.toInt()
            override fun encode(value: Int): Long = value.toLong()
        }

        database = AppDatabase(
            driver = driver,
            ExerciseEntityAdapter = ExerciseEntity.Adapter(
                typeAdapter = EnumColumnAdapter()
            ),
            UserProgressEntityAdapter = UserProgressEntity.Adapter(
                totalScoreAdapter = intAdapter
            ),
            UserEntityAdapter = UserEntity.Adapter(
                roleAdapter = EnumColumnAdapter()
            )
        )
    }

    @Test
    fun `getCurrentUser fetches from API and returns user`() = runTest {
        val mockUser = User(
            id = "user-1",
            name = "John Doe",
            email = "john@example.com",
            role = UserRole.LEARNER
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users/current-user-id" -> respond(
                    content = json.encodeToString(mockUser),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database)

        val user = repository.getCurrentUser()

        assertEquals("John Doe", user?.name)
        assertEquals("john@example.com", user?.email)
        assertEquals(UserRole.LEARNER, user?.role)

        val dbUser = database.appDatabaseQueries.selectUserById("user-1").executeAsOneOrNull()
        assertEquals("John Doe", dbUser?.name)
        assertEquals("john@example.com", dbUser?.email)
    }

    @Test
    fun `getUserRole fetches user role from API`() = runTest {
        val userId = "user-teacher"
        val mockUser = User(
            id = userId,
            name = "Teacher User",
            email = "teacher@example.com",
            role = UserRole.TEACHER
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users/$userId" -> respond(
                    content = json.encodeToString(mockUser),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database)

        val role = repository.getUserRole(userId)

        assertEquals(UserRole.TEACHER, role)
    }

    @Test
    fun `getUserRole returns LEARNER as default on failure`() = runTest {
        val userId = "unknown-user"

        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database)

        val role = repository.getUserRole(userId)

        assertEquals(UserRole.LEARNER, role)
    }

    @Test
    fun `updateUser puts to API and updates locally`() = runTest {
        val user = User(
            id = "user-update",
            name = "Original Name",
            email = "original@example.com",
            role = UserRole.LEARNER
        )

        database.appDatabaseQueries.insertUser(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role
        )

        val updatedUser = user.copy(name = "Updated Name", email = "updated@example.com")
        var updateCalled = false

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/users/${updatedUser.id}" -> {
                    updateCalled = true
                    respond(
                        content = "",
                        status = HttpStatusCode.NoContent,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)
        val repository = KtorUserRepository(api, database)

        repository.updateUser(updatedUser)

        assertEquals(true, updateCalled)

        val dbUser = database.appDatabaseQueries.selectUserById("user-update").executeAsOneOrNull()
        assertEquals("Updated Name", dbUser?.name)
        assertEquals("updated@example.com", dbUser?.email)
    }

    @Test
    fun `saveUserProgress posts CompleteLessonRequest to progress endpoint`() = runTest {
        val requestBody = CompleteLessonRequest(
            userId = "user-progress",
            lessonId = "lesson-progress",
            score = 42
        )
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        var capturedRequest: CompleteLessonRequest? = null

        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            capturedRequest = json.decodeFromString(request.body.toByteArray().decodeToString())

            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = UserApi(httpClient, apiConfig)

        api.saveUserProgress(requestBody)

        assertEquals("/progress", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertEquals(requestBody, capturedRequest)
    }
}
