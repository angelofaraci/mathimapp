package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.AppDatabase
import com.example.proyectofinal.db.CourseEntity
import com.example.proyectofinal.db.ExerciseEntity
import com.example.proyectofinal.db.UserEntity
import com.example.proyectofinal.db.UserProgressEntity
import com.example.proyectofinal.db.createTestDriver
import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.di.userRoleColumnAdapter
import com.example.proyectofinal.models.ChoiceOption
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.InputValuePayload
import com.example.proyectofinal.models.MultiSelectPayload
import com.example.proyectofinal.models.MultipleChoicePayload
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
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

        database.appDatabaseQueries.insertCourse(
            id = "course-1",
            title = "Test Course",
            description = "Description",
            creatorId = "admin",
            isOfficial = true,
            schoolYear = 0,
            joinCode = null,
            topic = null,
            difficulty = null,
            durationMinutes = null,
            xpReward = null
        )

        database.appDatabaseQueries.insertLesson(
            id = "lesson-1",
            courseId = "course-1",
            creatorId = null,
            title = "Test Lesson",
            theoryContent = "Theory content"
        )
    }

    @Test
    fun `getExercisesByLesson fetches from API and saves payload JSON to DB`() = runTest {
        val mockExercises = listOf(
            multipleChoiceExercise(id = "ex-1"),
            multiSelectExercise(id = "ex-2")
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

        val repository = KtorExerciseRepository(ExerciseApi(httpClient(mockEngine), apiConfig), database)

        val exercises = repository.getExercisesByLesson("lesson-1")

        assertEquals(2, exercises.size)
        assertEquals("What is Kotlin?", exercises[0].title)

        val dbExercises = database.appDatabaseQueries.selectExercisesByLessonId("lesson-1").executeAsList()
        assertEquals(2, dbExercises.size)
        assertEquals("What is Kotlin?", dbExercises[0].title)
        assertEquals(mockExercises[0].payload, ExercisePayloadJson.decode(dbExercises[0].payload))
        assertEquals(mockExercises[1].payload, ExercisePayloadJson.decode(dbExercises[1].payload))
    }

    @Test
    fun `createExercise posts to API and stores typed payload locally`() = runTest {
        val newExercise = inputValueExercise(id = "new-ex")

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

        val repository = KtorExerciseRepository(ExerciseApi(httpClient(mockEngine), apiConfig), database)

        val created = repository.createExercise(newExercise)

        assertEquals("New Question", created.title)
        assertEquals("lesson-1", created.lessonId)

        val dbExercise = database.appDatabaseQueries.selectExerciseById("new-ex").executeAsOneOrNull()
        assertEquals("New Question", dbExercise?.title)
        assertEquals(newExercise.payload, dbExercise?.payload?.let(ExercisePayloadJson::decode))
    }

    @Test
    fun `updateExercise puts to API and returns updated exercise`() = runTest {
        val existingExercise = multipleChoiceExercise(id = "existing-ex")

        database.appDatabaseQueries.insertExercise(
            id = existingExercise.id,
            lessonId = existingExercise.lessonId,
            title = existingExercise.title,
            type = existingExercise.type,
            payload = ExercisePayloadJson.encode(existingExercise)
        )

        val updatedExercise = existingExercise.copy(
            title = "Updated Question",
            payload = MultipleChoicePayload(
                options = (existingExercise.payload as MultipleChoicePayload).options,
                correctOptionId = "tool"
            )
        )

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

        val repository = KtorExerciseRepository(ExerciseApi(httpClient(mockEngine), apiConfig), database)

        val result = repository.updateExercise(updatedExercise)

        assertEquals("Updated Question", result.title)

        val dbExercise = database.appDatabaseQueries.selectExerciseById("existing-ex").executeAsOneOrNull()
        assertEquals("Updated Question", dbExercise?.title)
        assertEquals(updatedExercise.payload, dbExercise?.payload?.let(ExercisePayloadJson::decode))
    }

    @Test
    fun `payload JSON round trips through local DB storage`() = runTest {
        val exercise = multiSelectExercise(id = "payload-round-trip")

        database.appDatabaseQueries.insertExercise(
            id = exercise.id,
            lessonId = exercise.lessonId,
            title = exercise.title,
            type = exercise.type,
            payload = ExercisePayloadJson.encode(exercise)
        )

        val storedExercise = database.appDatabaseQueries.selectExerciseById("payload-round-trip").executeAsOne()

        assertEquals(exercise.title, storedExercise.title)
        assertEquals(ExerciseType.MULTI_SELECT, storedExercise.type)
        assertEquals(exercise.payload, ExercisePayloadJson.decode(storedExercise.payload))
    }

    @Test
    fun `deleteExercise deletes from API`() = runTest {
        val exerciseId = "to-delete-ex"
        var deleteCalled = false

        database.appDatabaseQueries.insertExercise(
            id = exerciseId,
            lessonId = "lesson-1",
            title = "Question to delete",
            type = ExerciseType.MULTIPLE_CHOICE,
            payload = ExercisePayloadJson.legacyPayloadJson(
                type = ExerciseType.MULTIPLE_CHOICE,
                optionsCsv = "A,B,C",
                correctAnswer = "A"
            )
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

        val repository = KtorExerciseRepository(ExerciseApi(httpClient(mockEngine), apiConfig), database)

        repository.deleteExercise(exerciseId)

        assertEquals(true, deleteCalled)

        val dbExercise = database.appDatabaseQueries.selectExerciseById(exerciseId).executeAsOneOrNull()
        assertEquals(null, dbExercise)
    }

    private fun httpClient(mockEngine: MockEngine): HttpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(json)
        }
    }
}

private fun multipleChoiceExercise(id: String) = Exercise(
    id = id,
    lessonId = "lesson-1",
    title = "What is Kotlin?",
    payload = MultipleChoicePayload(
        options = listOf(
            ChoiceOption(id = "language", text = "A language"),
            ChoiceOption(id = "tool", text = "A tool"),
            ChoiceOption(id = "framework", text = "A framework")
        ),
        correctOptionId = "language"
    )
)

private fun inputValueExercise(id: String) = Exercise(
    id = id,
    lessonId = "lesson-1",
    title = "New Question",
    type = ExerciseType.INPUT_VALUE,
    payload = InputValuePayload(
        placeholder = "Type your answer",
        correctValue = "42"
    )
)

private fun multiSelectExercise(id: String) = Exercise(
    id = id,
    lessonId = "lesson-1",
    title = "Select every true statement.",
    type = ExerciseType.MULTI_SELECT,
    payload = MultiSelectPayload(
        options = listOf(
            ChoiceOption(id = "a", text = "A"),
            ChoiceOption(id = "b", text = "B"),
            ChoiceOption(id = "c", text = "C")
        ),
        correctOptionIds = listOf("a", "c")
    )
)
