package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.*
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.di.userRoleColumnAdapter
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseType
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

class KtorExerciseRepositoryTest {
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
                roleAdapter = userRoleColumnAdapter
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

        database.appDatabaseQueries.insertLesson(
            id = "lesson-1",
            courseId = "course-1",
            title = "Test Lesson",
            theoryContent = "Theory content"
        )
    }

    @Test
    fun `getExercisesByLesson fetches from API and saves to DB`() = runTest {
        val mockExercises = listOf(
            Exercise(
                id = "ex-1",
                lessonId = "lesson-1",
                question = "What is Kotlin?",
                options = listOf("A language", "A tool", "A framework"),
                correctAnswer = "A language",
                type = ExerciseType.MULTIPLE_CHOICE
            ),
            Exercise(
                id = "ex-2",
                lessonId = "lesson-1",
                question = "Is Kotlin null safe?",
                options = listOf("Yes", "No"),
                correctAnswer = "Yes",
                type = ExerciseType.TRUE_FALSE
            )
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/lessons/lesson-1/exercises" -> respond(
                    content = json.encodeToString(mockExercises),
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

        val api = ExerciseApi(httpClient, apiConfig)
        val repository = KtorExerciseRepository(api, database)

        val exercises = repository.getExercisesByLesson("lesson-1")

        assertEquals(2, exercises.size)
        assertEquals("What is Kotlin?", exercises[0].question)

        val dbExercises = database.appDatabaseQueries.selectExercisesByLessonId("lesson-1").executeAsList()
        assertEquals(2, dbExercises.size)
        assertEquals("What is Kotlin?", dbExercises[0].question)
    }

    @Test
    fun `createExercise posts to API and returns created exercise`() = runTest {
        val newExercise = Exercise(
            id = "new-ex",
            lessonId = "lesson-1",
            question = "New Question",
            options = listOf("Option A", "Option B", "Option C"),
            correctAnswer = "Option A",
            type = ExerciseType.MULTIPLE_CHOICE
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/exercises" -> respond(
                    content = json.encodeToString(newExercise),
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

        val api = ExerciseApi(httpClient, apiConfig)
        val repository = KtorExerciseRepository(api, database)

        val created = repository.createExercise(newExercise)

        assertEquals("New Question", created.question)
        assertEquals("lesson-1", created.lessonId)

        val dbExercise = database.appDatabaseQueries.selectExerciseById("new-ex").executeAsOneOrNull()
        assertEquals("New Question", dbExercise?.question)
        assertEquals("Option A", dbExercise?.correctAnswer)
    }

    @Test
    fun `updateExercise puts to API and returns updated exercise`() = runTest {
        val existingExercise = Exercise(
            id = "existing-ex",
            lessonId = "lesson-1",
            question = "Original Question",
            options = listOf("A", "B", "C"),
            correctAnswer = "A",
            type = ExerciseType.MULTIPLE_CHOICE
        )

        database.appDatabaseQueries.insertExercise(
            id = existingExercise.id,
            lessonId = existingExercise.lessonId,
            question = existingExercise.question,
            correctAnswer = existingExercise.correctAnswer,
            type = existingExercise.type,
            options = "A,B,C"
        )

        val updatedExercise = existingExercise.copy(question = "Updated Question", correctAnswer = "B")

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/exercises/${updatedExercise.id}" -> respond(
                    content = json.encodeToString(updatedExercise),
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

        val api = ExerciseApi(httpClient, apiConfig)
        val repository = KtorExerciseRepository(api, database)

        val result = repository.updateExercise(updatedExercise)

        assertEquals("Updated Question", result.question)
        assertEquals("B", result.correctAnswer)

        val dbExercise = database.appDatabaseQueries.selectExerciseById("existing-ex").executeAsOneOrNull()
        assertEquals("Updated Question", dbExercise?.question)
        assertEquals("B", dbExercise?.correctAnswer)
    }

    @Test
    fun `deleteExercise deletes from API`() = runTest {
        val exerciseId = "to-delete-ex"
        var deleteCalled = false

        database.appDatabaseQueries.insertExercise(
            id = exerciseId,
            lessonId = "lesson-1",
            question = "Question to delete",
            correctAnswer = "Answer",
            type = ExerciseType.MULTIPLE_CHOICE,
            options = "A,B,C"
        )

        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/exercises/$exerciseId" -> {
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

        val api = ExerciseApi(httpClient, apiConfig)
        val repository = KtorExerciseRepository(api, database)

        repository.deleteExercise(exerciseId)

        assertEquals(true, deleteCalled)

        val dbExercise = database.appDatabaseQueries.selectExerciseById(exerciseId).executeAsOneOrNull()
        assertEquals(null, dbExercise)
    }
}
