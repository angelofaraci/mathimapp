package com.example.proyectofinal.data

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import com.example.proyectofinal.db.*
import com.example.proyectofinal.domain.Course
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorCourseRepositoryTest {
    private lateinit var database: AppDatabase
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        val driver = createTestDriver()
        
        // Adapter for Int fields (SQLDelight INTEGER defaults to Long)
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
            )
        )
    }

    @Test
    fun `getOfficialCourses fetches from API and saves to DB`() = runTest {
        val mockCourse = Course(
            id = "test-1",
            title = "Test Course",
            description = "Description",
            creatorId = "admin",
            isOfficial = true
        )

        val mockEngine = MockEngine { _ ->
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

        val api = CourseApi(httpClient)
        val repository = KtorCourseRepository(api, database)

        // 1. Fetch courses
        val courses = repository.getOfficialCourses()

        // 2. Verify network result
        assertEquals(1, courses.size)
        assertEquals("Test Course", courses[0].title)

        // 3. Verify it was saved to the local database
        val dbCourse = database.appDatabaseQueries.selectCourseById("test-1").executeAsOne()
        assertEquals("Test Course", dbCourse.title)
        assertEquals(true, dbCourse.isOfficial)
    }
}
