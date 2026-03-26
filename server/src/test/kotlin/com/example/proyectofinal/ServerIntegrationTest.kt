package com.example.proyectofinal

import com.example.proyectofinal.database.CompletedLessons
import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.AuthResponse
import com.example.proyectofinal.models.CompleteLessonRequest
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.RegisterRequest
import com.example.proyectofinal.models.UserProgress
import com.example.proyectofinal.models.UserRole
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerIntegrationTest {
    private fun testDbUrl(): String =
        "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"

    private fun setupTestDatabase() {
        DatabaseFactory.init(
            url = testDbUrl(),
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }

    @Test
    fun `register returns token and persisted user`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                RegisterRequest(
                    name = "Test User",
                    email = "test@example.com",
                    password = "secret123",
                    role = UserRole.TEACHER
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val auth = response.body<AuthResponse>()
        assertEquals("Test User", auth.user.name)
        assertEquals("test@example.com", auth.user.email)
        assertEquals(UserRole.TEACHER, auth.user.role)
        assertTrue(auth.token.isNotBlank())

        transaction {
            val savedUser = Users.selectAll().where { Users.email eq "test@example.com" }.single()
            assertEquals("Test User", savedUser[Users.name])
            assertEquals("TEACHER", savedUser[Users.role])
        }
    }

    @Test
    fun `protected courses route rejects missing token`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val response = client.get("/courses/official")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `authorized user can fetch official courses`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerUserAndGetToken(client, email = "courses@example.com")
        seedOfficialCourse()

        val response = client.get("/courses/official") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val courses = response.body<List<Course>>()
        assertEquals(1, courses.size)
        assertEquals("Official Test Course", courses.single().title)
        assertTrue(courses.single().isOfficial)
    }

    @Test
    fun `posting progress updates score and completed lessons`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerUserAndGetToken(client, email = "progress@example.com")
        val userId = transaction { Users.selectAll().where { Users.email eq "progress@example.com" }.single()[Users.id] }

        val updateResponse = client.post("/progress") {
            bearerAuth(token)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CompleteLessonRequest(
                    userId = userId,
                    lessonId = "lesson-1",
                    score = 15
                )
            )
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        val progressResponse = client.get("/progress/$userId") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, progressResponse.status)

        val progress = progressResponse.body<UserProgress>()
        assertEquals(userId, progress.userId)
        assertEquals(15, progress.totalScore)
        assertEquals(setOf("lesson-1"), progress.completedLessonIds)
        assertEquals(emptySet(), progress.enrolledCourseIds)

        transaction {
            val completed = CompletedLessons.selectAll().where { CompletedLessons.userId eq userId }.single()
            assertEquals("lesson-1", completed[CompletedLessons.lessonId])
        }
    }

    private suspend fun registerUserAndGetToken(
        client: io.ktor.client.HttpClient,
        email: String
    ): String {
        val response = client.post("/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                RegisterRequest(
                    name = "Integration User",
                    email = email,
                    password = "secret123",
                    role = UserRole.LEARNER
                )
            )
        }

        return response.body<AuthResponse>().token
    }

    private fun seedOfficialCourse() {
        transaction {
            if (Users.selectAll().where { Users.id eq "admin-test" }.firstOrNull() == null) {
                Users.insert {
                    it[id] = "admin-test"
                    it[name] = "Admin"
                    it[email] = "admin-test@example.com"
                    it[passwordHash] = "hash"
                    it[role] = "ADMIN"
                }
            }

            Courses.insert {
                it[id] = "course-1"
                it[title] = "Official Test Course"
                it[description] = "Basic test course"
                it[creatorId] = "admin-test"
                it[isOfficial] = true
                it[joinCode] = "JOIN123"
            }
        }
    }
}
