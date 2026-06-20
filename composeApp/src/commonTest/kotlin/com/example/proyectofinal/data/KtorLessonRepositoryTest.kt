package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.*
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.TheoryUpdateRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.OutgoingContent
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorLessonRepositoryTest {
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

        database.appDatabaseQueries.insertCourse(
            id = "course-1",
            title = "Test Course",
            description = "Description",
            creatorId = "admin",
            isOfficial = true,
            schoolYear = 0,
            joinCode = null
        )
    }

    @Test
    fun `getLessonsByCourse fetches from API and saves to DB`() = runTest {
        val mockLessons = listOf(
            Lesson(
                id = "lesson-1",
                courseId = "course-1",
                title = "Introduction",
                theoryContent = "Welcome to the course"
            ),
            Lesson(
                id = "lesson-2",
                courseId = "course-1",
                title = "Variables",
                theoryContent = "Understanding variables"
            )
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/courses/course-1/lessons" -> respond(
                    content = json.encodeToString(mockLessons),
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

        val api = LessonApi(httpClient, apiConfig)
        val repository = KtorLessonRepository(api, database)

        val lessons = repository.getLessonsByCourse("course-1")

        assertEquals(2, lessons.size)
        assertEquals("Introduction", lessons[0].title)

        val dbLessons = database.appDatabaseQueries.selectLessonsByCourseId("course-1").executeAsList()
        assertEquals(2, dbLessons.size)
        assertEquals("Introduction", dbLessons[0].title)
    }

    @Test
    fun `getLessonById fetches single lesson from API`() = runTest {
        val mockLesson = Lesson(
            id = "lesson-1",
            courseId = "course-1",
            title = "Single Lesson",
            theoryContent = "Theory content here"
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/lessons/lesson-1" -> respond(
                    content = json.encodeToString(mockLesson),
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

        val api = LessonApi(httpClient, apiConfig)
        val repository = KtorLessonRepository(api, database)

        val lesson = repository.getLessonById("lesson-1")

        assertEquals("Single Lesson", lesson?.title)
        assertEquals("Theory content here", lesson?.theoryContent)

        val dbLesson = database.appDatabaseQueries.selectLessonById("lesson-1").executeAsOneOrNull()
        assertEquals("Single Lesson", dbLesson?.title)
        assertEquals("Theory content here", dbLesson?.theoryContent)
    }

    @Test
    fun `createLesson posts to API and returns created lesson`() = runTest {
        val newLesson = Lesson(
            id = "new-lesson",
            courseId = "course-1",
            title = "New Lesson",
            theoryContent = "New theory content"
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/lessons" -> respond(
                    content = json.encodeToString(newLesson),
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

        val api = LessonApi(httpClient, apiConfig)
        val repository = KtorLessonRepository(api, database)

        val created = repository.createLesson(newLesson)

        assertEquals("New Lesson", created.title)
        assertEquals("course-1", created.courseId)

        val dbLesson = database.appDatabaseQueries.selectLessonById("new-lesson").executeAsOneOrNull()
        assertEquals("New Lesson", dbLesson?.title)
        assertEquals("New theory content", dbLesson?.theoryContent)
    }

    @Test
    fun `updateLesson puts to API and returns updated lesson`() = runTest {
        val existingLesson = Lesson(
            id = "existing-lesson",
            courseId = "course-1",
            title = "Original Title",
            theoryContent = "Original Content"
        )

        database.appDatabaseQueries.insertLesson(
            id = existingLesson.id,
            courseId = existingLesson.courseId,
            title = existingLesson.title,
            theoryContent = existingLesson.theoryContent
        )

        val updatedLesson = existingLesson.copy(title = "Updated Title", theoryContent = "Updated Content")

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/lessons/${updatedLesson.id}" -> respond(
                    content = json.encodeToString(updatedLesson),
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

        val api = LessonApi(httpClient, apiConfig)
        val repository = KtorLessonRepository(api, database)

        val result = repository.updateLesson(updatedLesson)

        assertEquals("Updated Title", result.title)
        assertEquals("Updated Content", result.theoryContent)

        val dbLesson = database.appDatabaseQueries.selectLessonById("existing-lesson").executeAsOneOrNull()
        assertEquals("Updated Title", dbLesson?.title)
        assertEquals("Updated Content", dbLesson?.theoryContent)
    }

    @Test
    fun `deleteLesson deletes from API`() = runTest {
        val lessonId = "to-delete-lesson"
        var deleteCalled = false

        database.appDatabaseQueries.insertLesson(
            id = lessonId,
            courseId = "course-1",
            title = "Lesson to Delete",
            theoryContent = "Content"
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/lessons/$lessonId" -> {
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

        val api = LessonApi(httpClient, apiConfig)
        val repository = KtorLessonRepository(api, database)

        repository.deleteLesson(lessonId)

        assertEquals(true, deleteCalled)

        val dbLesson = database.appDatabaseQueries.selectLessonById(lessonId).executeAsOneOrNull()
        assertEquals(null, dbLesson)
    }

    @Test
    fun `updateTheory sends shared request and refreshes lesson cache`() = runTest {
        val existingLesson = Lesson(
            id = "lesson-1",
            courseId = "course-1",
            title = "Original Title",
            theoryContent = "Original Content"
        )

        database.appDatabaseQueries.insertLesson(
            id = existingLesson.id,
            courseId = existingLesson.courseId,
            title = existingLesson.title,
            theoryContent = existingLesson.theoryContent
        )

        val expectedRequest = TheoryUpdateRequest(
            lessonId = "lesson-1",
            theoryContent = "Updated Theory"
        )
        val updatedLesson = existingLesson.copy(theoryContent = expectedRequest.theoryContent)

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/lessons/lesson-1/theory" -> {
                    assertEquals(HttpMethod.Put, request.method)
                    assertEquals(expectedRequest, request.decodeBody())
                    respond(
                        content = json.encodeToString(updatedLesson),
                        status = HttpStatusCode.OK,
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

        val api = LessonApi(httpClient, apiConfig)
        val repository = KtorLessonRepository(api, database)

        val result = repository.updateTheory("lesson-1", "Updated Theory")

        assertEquals("Updated Theory", result.theoryContent)

        val dbLesson = database.appDatabaseQueries.selectLessonById("lesson-1").executeAsOneOrNull()
        assertEquals("Updated Theory", dbLesson?.theoryContent)
    }

    private suspend fun HttpRequestData.decodeBody(): TheoryUpdateRequest {
        val requestBody = body as? OutgoingContent.ByteArrayContent
            ?: error("Expected ByteArrayContent but was ${body::class.simpleName}")
        return json.decodeFromString(requestBody.bytes().decodeToString())
    }
}
