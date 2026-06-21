package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.*
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.CompleteExerciseRequest
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
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
            CourseEntityAdapter = CourseEntity.Adapter(
                schoolYearAdapter = intAdapter
            ),
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
    fun `completeExercise posts CompleteExerciseRequest to completion endpoint`() = runTest {
        val exerciseId = "exercise-progress"
        val requestBody = CompleteExerciseRequest(
            exerciseId = exerciseId,
            score = 42
        )
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        var capturedRequest: CompleteExerciseRequest? = null

        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            capturedRequest = json.decodeFromString(request.body.toByteArray().decodeToString())

            respond(
                content = json.encodeToString(
                    ExerciseCompletionResponse(
                        exerciseId = exerciseId,
                        lessonId = "lesson-progress",
                        lessonCompleted = false,
                        progress = UserProgress(
                            userId = "user-progress",
                            completedExerciseIds = setOf(exerciseId),
                            totalScore = 42
                        )
                    )
                ),
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

        api.completeExercise(exerciseId = exerciseId, score = requestBody.score)

        assertEquals("/exercises/$exerciseId/complete", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertEquals(requestBody, capturedRequest)
    }

    @Test
    fun `getUserProgress merges completed exercises locally`() = runTest {
        val userId = "user-progress"
        val progress = UserProgress(
            userId = userId,
            completedLessonIds = setOf("lesson-1"),
            completedExerciseIds = setOf("exercise-1"),
            totalScore = 15,
            enrolledCourseIds = setOf("course-1")
        )

        database.appDatabaseQueries.insertUser(
            id = userId,
            name = "Progress User",
            email = "progress@example.com",
            role = UserRole.LEARNER
        )
        database.appDatabaseQueries.insertCourse(
            id = "course-1",
            title = "Course",
            description = "Description",
            creatorId = "teacher-1",
            isOfficial = true,
            schoolYear = 1,
            joinCode = null
        )
        database.appDatabaseQueries.insertLesson(
            id = "lesson-1",
            courseId = "course-1",
            title = "Lesson",
            theoryContent = "Theory"
        )
        database.appDatabaseQueries.insertExercise(
            id = "exercise-1",
            lessonId = "lesson-1",
            question = "Question",
            correctAnswer = "Answer",
            type = com.example.proyectofinal.models.ExerciseType.MULTIPLE_CHOICE,
            options = "A,B"
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/progress/$userId" -> respond(
                    content = json.encodeToString(progress),
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

        val repository = KtorUserRepository(UserApi(httpClient, apiConfig), database)

        val syncedProgress = repository.getUserProgress(userId)

        assertEquals(progress, syncedProgress)
        assertEquals(progress.totalScore, database.appDatabaseQueries.selectProgressByUserId(userId).executeAsOne().totalScore)
        assertEquals(progress.completedLessonIds, database.appDatabaseQueries.selectCompletedLessonsByUserId(userId).executeAsList().toSet())
        assertEquals(progress.completedExerciseIds, database.appDatabaseQueries.selectCompletedExercisesByUserId(userId).executeAsList().toSet())
        assertEquals(progress.enrolledCourseIds, database.appDatabaseQueries.selectEnrolledCoursesByUserId(userId).executeAsList().toSet())
    }

    @Test
    fun `completeExercise duplicate sync preserves single local record`() = runTest {
        val userId = "user-progress"
        val exerciseId = "exercise-1"
        val response = ExerciseCompletionResponse(
            exerciseId = exerciseId,
            lessonId = "lesson-1",
            lessonCompleted = false,
            progress = UserProgress(
                userId = userId,
                completedLessonIds = setOf("lesson-1"),
                completedExerciseIds = setOf(exerciseId),
                totalScore = 10,
                enrolledCourseIds = setOf("course-1")
            )
        )

        database.appDatabaseQueries.insertUser(
            id = userId,
            name = "Progress User",
            email = "progress@example.com",
            role = UserRole.LEARNER
        )
        database.appDatabaseQueries.insertCourse(
            id = "course-1",
            title = "Course",
            description = "Description",
            creatorId = "teacher-1",
            isOfficial = true,
            schoolYear = 1,
            joinCode = null
        )
        database.appDatabaseQueries.insertLesson(
            id = "lesson-1",
            courseId = "course-1",
            title = "Lesson",
            theoryContent = "Theory"
        )
        database.appDatabaseQueries.insertExercise(
            id = exerciseId,
            lessonId = "lesson-1",
            question = "Question",
            correctAnswer = "Answer",
            type = com.example.proyectofinal.models.ExerciseType.MULTIPLE_CHOICE,
            options = "A,B"
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/exercises/$exerciseId/complete" -> respond(
                    content = json.encodeToString(response),
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

        val repository = KtorUserRepository(UserApi(httpClient, apiConfig), database)

        repository.completeExercise(exerciseId = exerciseId, score = 10)
        repository.completeExercise(exerciseId = exerciseId, score = 99)

        assertEquals(listOf(exerciseId), database.appDatabaseQueries.selectCompletedExercisesByUserId(userId).executeAsList())
        assertEquals(10, database.appDatabaseQueries.selectProgressByUserId(userId).executeAsOne().totalScore)
    }
}
