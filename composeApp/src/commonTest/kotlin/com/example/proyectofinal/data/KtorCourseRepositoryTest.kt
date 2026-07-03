package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.*
import com.example.proyectofinal.di.InMemoryTokenStore
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.di.userRoleColumnAdapter
import com.example.proyectofinal.createHttpClient
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.UserProgress
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class KtorCourseRepositoryTest {
    private lateinit var database: AppDatabase
    private val apiConfig = ApiConfig("https://example.test")
    private val json = Json { ignoreUnknownKeys = true }
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val driver = createTestDriver()
        
        // Adapter for Int fields (SQLDelight INTEGER defaults to Long)
        val intAdapter = object : ColumnAdapter<Int, Long> {
            override fun decode(databaseValue: Long): Int = databaseValue.toInt()
            override fun encode(value: Int): Long = value.toLong()
        }

        database = AppDatabase(
            driver = driver,
            CourseEntityAdapter = CourseEntity.Adapter(
                schoolYearAdapter = intAdapter,
                durationMinutesAdapter = intAdapter,
                xpRewardAdapter = intAdapter
            ),
            ExerciseEntityAdapter = ExerciseEntity.Adapter(
                typeAdapter = EnumColumnAdapter()
            ),
            UserProgressEntityAdapter = UserProgressEntity.Adapter(
                totalScoreAdapter = intAdapter
            ),
            UserEntityAdapter = UserEntity.Adapter(
                roleAdapter = userRoleColumnAdapter
            )
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getOfficialCourses sends schoolYear filter and saves it to DB`() = runTest {
        val mockCourse = Course(
            id = "test-1",
            title = "Test Course",
            description = "Description",
            creatorId = "admin",
            isOfficial = true,
            schoolYear = 3
        )

        val mockEngine = MockEngine { request ->
            assertEquals("3", request.url.parameters["schoolYear"])
            respond(
                content = json.encodeToString(listOf(mockCourse)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val courses = repository.getOfficialCourses(3)

        assertEquals(1, courses.size)
        assertEquals("Test Course", courses[0].title)
        assertEquals(3, courses[0].schoolYear)

        val dbCourse = database.appDatabaseQueries.selectCourseById("test-1").executeAsOne()
        assertEquals("Test Course", dbCourse.title)
        assertEquals(true, dbCourse.isOfficial)
        assertEquals(3, dbCourse.schoolYear)
    }

    @Test
    fun `getOfficialCourses caches discovery fields in SQLDelight`() = runTest {
        val mockCourse = Course(
            id = "discovery-1",
            title = "Fractions Basics",
            description = "Learn equivalent fractions",
            creatorId = "admin",
            isOfficial = true,
            schoolYear = 4,
            topic = "Fracciones",
            difficulty = "Fácil",
            durationMinutes = 15,
            xpReward = 50
        )

        val mockEngine = MockEngine { request ->
            assertEquals("4", request.url.parameters["schoolYear"])
            respond(
                content = json.encodeToString(listOf(mockCourse)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val courses = repository.getOfficialCourses(4)

        assertEquals("Fracciones", courses.single().topic)
        assertEquals("Fácil", courses.single().difficulty)
        assertEquals(15, courses.single().durationMinutes)
        assertEquals(50, courses.single().xpReward)

        val dbCourse = database.appDatabaseQueries.selectCourseById("discovery-1").executeAsOne()
        assertEquals("Fracciones", dbCourse.topic)
        assertEquals("Fácil", dbCourse.difficulty)
        assertEquals(15, dbCourse.durationMinutes)
        assertEquals(50, dbCourse.xpReward)
    }

    @Test
    fun `getCourseById fetches from API and saves to DB`() = runTest {
        val mockCourse = Course(
            id = "course-1",
            title = "Kotlin Basics",
            description = "Learn Kotlin fundamentals",
            creatorId = "admin",
            isOfficial = true
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/courses/course-1" -> respond(
                    content = json.encodeToString(mockCourse),
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

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val course = repository.getCourseById("course-1")

        assertEquals("Kotlin Basics", course?.title)
        val dbCourse = database.appDatabaseQueries.selectCourseById("course-1").executeAsOne()
        assertEquals("Kotlin Basics", dbCourse.title)
    }

    @Test
    fun `getMyCreatedCourses fetches user's created courses`() = runTest {
        val creatorId = "user-123"
        val mockCourses = listOf(
            Course(id = "c1", title = "My Course 1", description = "Desc 1", creatorId = creatorId, isOfficial = false),
            Course(id = "c2", title = "My Course 2", description = "Desc 2", creatorId = creatorId, isOfficial = false)
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/courses/creator/$creatorId" -> respond(
                    content = json.encodeToString(mockCourses),
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

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val courses = repository.getMyCreatedCourses(creatorId)

        assertEquals(2, courses.size)
        assertEquals("My Course 1", courses[0].title)

        val dbCourses = database.appDatabaseQueries.selectCoursesByCreatorId(creatorId).executeAsList()
        assertEquals(2, dbCourses.size)
        assertEquals("My Course 1", dbCourses[0].title)
    }

    @Test
    fun `getEnrolledCourses fetches user's enrolled courses`() = runTest {
        val userId = "user-456"
        val mockCourses = listOf(
            Course(id = "e1", title = "Enrolled Course 1", description = "Desc", creatorId = "admin", isOfficial = true)
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/courses/enrolled/$userId" -> respond(
                    content = json.encodeToString(mockCourses),
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

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val courses = repository.getEnrolledCourses(userId)

        assertEquals(1, courses.size)
        assertEquals("Enrolled Course 1", courses[0].title)

        val dbCourse = database.appDatabaseQueries.selectCourseById("e1").executeAsOneOrNull()
        assertEquals("Enrolled Course 1", dbCourse?.title)
    }

    @Test
    fun `createCourse posts to API and saves to DB`() = runTest {
        val newCourse = Course(
            id = "new-course",
            title = "New Course",
            description = "New Description",
            creatorId = "user-123",
            isOfficial = false
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/courses" -> respond(
                    content = json.encodeToString(newCourse),
                    status = HttpStatusCode.Created,
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

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val created = repository.createCourse(newCourse)

        assertEquals("New Course", created.title)
        val dbCourse = database.appDatabaseQueries.selectCourseById("new-course").executeAsOne()
        assertEquals("New Course", dbCourse.title)
    }

    @Test
    fun `updateCourse puts to API and updates DB`() = runTest {
        val existingCourse = Course(
            id = "existing-course",
            title = "Original Title",
            description = "Original Desc",
            creatorId = "user-123",
            isOfficial = false
        )
        database.appDatabaseQueries.insertCourse(
            id = existingCourse.id,
            title = existingCourse.title,
            description = existingCourse.description,
            creatorId = existingCourse.creatorId,
            isOfficial = existingCourse.isOfficial,
            schoolYear = existingCourse.schoolYear,
            joinCode = existingCourse.joinCode,
            topic = existingCourse.topic,
            difficulty = existingCourse.difficulty,
            durationMinutes = existingCourse.durationMinutes,
            xpReward = existingCourse.xpReward
        )

        val updatedCourse = existingCourse.copy(title = "Updated Title")

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/courses/${updatedCourse.id}" -> respond(
                    content = json.encodeToString(updatedCourse),
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

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val result = repository.updateCourse(updatedCourse)

        assertEquals("Updated Title", result.title)
        val dbCourse = database.appDatabaseQueries.selectCourseById("existing-course").executeAsOne()
        assertEquals("Updated Title", dbCourse.title)
    }

    @Test
    fun `deleteCourse deletes from API and removes from DB`() = runTest {
        val courseId = "to-delete"
        database.appDatabaseQueries.insertCourse(
            id = courseId,
            title = "Course to Delete",
            description = "Description",
            creatorId = "user-123",
            isOfficial = false,
            schoolYear = 0,
            joinCode = null,
            topic = null,
            difficulty = null,
            durationMinutes = null,
            xpReward = null
        )

        var deleteCalled = false
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/courses/$courseId" -> {
                    deleteCalled = true
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

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        repository.deleteCourse(courseId)

        assertEquals(true, deleteCalled)
        val deletedCourse = database.appDatabaseQueries.selectCourseById(courseId).executeAsOneOrNull()
        assertEquals(null, deletedCourse)
    }

    @Test
    fun `joinCourseByCode joins course with code and saves to DB`() = runTest {
        val userId = "user-789"
        val joinCode = "KOTLIN123"
        val joinedCourse = Course(
            id = "joined-course",
            title = "Joined Course",
            description = "Description",
            creatorId = "admin",
            isOfficial = true,
            joinCode = joinCode
        )

        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath == "/courses/join" && request.method.value == "POST" -> respond(
                    content = json.encodeToString(joinedCourse),
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

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val result = repository.joinCourseByCode(userId, joinCode)

        assertEquals("Joined Course", result?.title)
        val dbCourse = database.appDatabaseQueries.selectCourseById("joined-course").executeAsOne()
        assertEquals("KOTLIN123", dbCourse.joinCode)
    }

    @Test
    fun `enroll posts to enrollment endpoint with bearer token and syncs progress locally`() = runTest {
        val expectedProgress = UserProgress(
            userId = "student-1",
            totalScore = 25,
            enrolledCourseIds = setOf("official-course"),
            completedLessonIds = setOf("lesson-1"),
            completedExerciseIds = setOf("exercise-1")
        )
        var capturedAuthorization: String? = null
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null

        val mockEngine = MockEngine { request ->
            capturedAuthorization = request.headers[HttpHeaders.Authorization]
            capturedMethod = request.method
            capturedPath = request.url.encodedPath

            respond(
                content = json.encodeToString(expectedProgress),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = createHttpClient(
            tokenStore = InMemoryTokenStore().apply { accessToken = "token-123" },
            engine = mockEngine
        )

        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val result = repository.enroll("official-course")

        assertEquals(expectedProgress, result)
        assertEquals("Bearer token-123", capturedAuthorization)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertEquals("/courses/official-course/enroll", capturedPath)
        assertEquals(
            listOf("official-course"),
            database.appDatabaseQueries.selectEnrolledCoursesByUserId("student-1").executeAsList()
        )
        assertEquals(
            expectedProgress.totalScore,
            database.appDatabaseQueries.selectProgressByUserId("student-1").executeAsOneOrNull()?.totalScore
        )
    }

    @Test
    fun `enroll propagates remote failures`() = runTest {
        val mockEngine = MockEngine { error("Network unavailable") }
        val httpClient = createHttpClient(
            tokenStore = InMemoryTokenStore().apply { accessToken = "token-123" },
            engine = mockEngine
        )
        val api = CourseApi(httpClient, apiConfig)
        val repository = KtorCourseRepository(api, database)

        val error = assertFailsWith<IllegalStateException> {
            repository.enroll("official-course")
        }

        assertEquals("Network unavailable", error.message)
    }
}
