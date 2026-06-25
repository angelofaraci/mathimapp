package com.example.proyectofinal

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.AdminCourseResponse
import com.example.proyectofinal.models.PageResponse
import com.example.proyectofinal.models.RoleUpdateRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.Security
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminIntegrationTest {
    private fun testDbUrl(): String =
        "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"

    private fun setupTestDatabase() {
        System.setProperty("jwt.secret", "test-jwt-secret")
        DatabaseFactory.init(
            url = testDbUrl(),
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
    }

    // ── GET /admin/users ──────────────────────────────────────────

    @Test
    fun `admin can list users with pagination`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val response = client.get("/admin/users?page=0&size=10") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val page = response.body<PageResponse<User>>()
        assertEquals(4, page.totalElements)
        assertEquals(1, page.totalPages)
        assertEquals(0, page.page)
        assertEquals(10, page.size)
        assertEquals(4, page.items.size)
    }

    @Test
    fun `admin can search users by name or email`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val nameSearch = client.get("/admin/users?query=Alice") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, nameSearch.status)
        val namePage = nameSearch.body<PageResponse<User>>()
        assertEquals(1, namePage.totalElements)
        assertEquals("Alice", namePage.items.single().name)

        val emailSearch = client.get("/admin/users?query=bob@") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, emailSearch.status)
        val emailPage = emailSearch.body<PageResponse<User>>()
        assertEquals(1, emailPage.totalElements)
        assertEquals("bob@example.com", emailPage.items.single().email)

        val emptySearch = client.get("/admin/users?query=nonexistent") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, emptySearch.status)
        val emptyPage = emptySearch.body<PageResponse<User>>()
        assertEquals(0, emptyPage.totalElements)
        assertTrue(emptyPage.items.isEmpty())
    }

    @Test
    fun `non-admin cannot access user list`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val studentToken = Security.generateToken("student-1", UserRole.STUDENT.name)

        val response = client.get("/admin/users") {
            bearerAuth(studentToken)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `user list rejects missing token`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val response = client.get("/admin/users")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ── GET /admin/courses ────────────────────────────────────────

    @Test
    fun `admin can list all courses with creator name and enrollment count`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertCourseFixtures()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val response = client.get("/admin/courses") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val courses = response.body<List<AdminCourseResponse>>()
        assertEquals(2, courses.size)

        val mathCourse = courses.find { it.id == "course-math" }!!
        assertEquals("Math 101", mathCourse.title)
        assertEquals("Teacher John", mathCourse.creatorName)
        assertEquals(2, mathCourse.enrollmentCount)
        assertEquals(true, mathCourse.isOfficial)

        val artCourse = courses.find { it.id == "course-art" }!!
        assertEquals("Art Class", artCourse.title)
        assertEquals(0, artCourse.enrollmentCount)
        assertEquals(false, artCourse.isOfficial)
    }

    @Test
    fun `empty course list returns 200 with empty array`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        transaction {
            Users.insert {
                it[id] = "admin-1"
                it[name] = "Admin"
                it[email] = "admin@example.com"
                it[passwordHash] = "hash"
                it[role] = "ADMIN"
            }
        }

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val response = client.get("/admin/courses") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.body<List<AdminCourseResponse>>().isEmpty())
    }

    @Test
    fun `non-admin cannot access course list`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val teacherToken = Security.generateToken("teacher-1", UserRole.TEACHER.name)

        val response = client.get("/admin/courses") {
            bearerAuth(teacherToken)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    // ── PUT /admin/users/{id}/role ─────────────────────────────────

    @Test
    fun `admin can update user role`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val response = client.put("/admin/users/student-1/role") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RoleUpdateRequest(role = "TEACHER"))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val updated = response.body<User>()
        assertEquals("student-1", updated.id)
        assertEquals(UserRole.TEACHER, updated.role)

        transaction {
            val persisted = Users.selectAll().where { Users.id eq "student-1" }.single()
            assertEquals("TEACHER", persisted[Users.role])
        }
    }

    @Test
    fun `role update returns 404 for nonexistent user`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val response = client.put("/admin/users/nonexistent-id/role") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RoleUpdateRequest(role = "TEACHER"))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `role update returns 400 for invalid role`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val response = client.put("/admin/users/student-1/role") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RoleUpdateRequest(role = "SUPERHERO"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `non-admin cannot update roles`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val studentToken = Security.generateToken("student-1", UserRole.STUDENT.name)

        val response = client.put("/admin/users/student-1/role") {
            bearerAuth(studentToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RoleUpdateRequest(role = "ADMIN"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `role update rejects missing token`() = testApplication {
        setupTestDatabase()

        application {
            module(initDatabase = false, seedData = false)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        insertUserFixtures()

        val response = client.put("/admin/users/student-1/role") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(RoleUpdateRequest(role = "TEACHER"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ── Fixtures ──────────────────────────────────────────────────

    private fun insertUserFixtures() {
        transaction {
            Users.insert {
                it[id] = "admin-1"
                it[name] = "Admin"
                it[email] = "admin@example.com"
                it[passwordHash] = "hash"
                it[role] = "ADMIN"
            }
            Users.insert {
                it[id] = "teacher-1"
                it[name] = "Teacher John"
                it[email] = "teacher@example.com"
                it[passwordHash] = "hash"
                it[role] = "TEACHER"
            }
            Users.insert {
                it[id] = "student-1"
                it[name] = "Alice"
                it[email] = "alice@example.com"
                it[passwordHash] = "hash"
                it[role] = "STUDENT"
            }
            Users.insert {
                it[id] = "student-2"
                it[name] = "Bob"
                it[email] = "bob@example.com"
                it[passwordHash] = "hash"
                it[role] = "STUDENT"
            }
        }
    }

    private fun insertCourseFixtures() {
        transaction {
            Users.insert {
                it[id] = "admin-1"
                it[name] = "Admin"
                it[email] = "admin@example.com"
                it[passwordHash] = "hash"
                it[role] = "ADMIN"
            }
            Users.insert {
                it[id] = "teacher-1"
                it[name] = "Teacher John"
                it[email] = "teacher@example.com"
                it[passwordHash] = "hash"
                it[role] = "TEACHER"
            }
            Users.insert {
                it[id] = "student-1"
                it[name] = "Alice"
                it[email] = "alice@example.com"
                it[passwordHash] = "hash"
                it[role] = "STUDENT"
            }
            Users.insert {
                it[id] = "student-2"
                it[name] = "Bob"
                it[email] = "bob@example.com"
                it[passwordHash] = "hash"
                it[role] = "STUDENT"
            }

            Courses.insert {
                it[id] = "course-math"
                it[title] = "Math 101"
                it[description] = "Basic math"
                it[creatorId] = "teacher-1"
                it[isOfficial] = true
                it[schoolYear] = 3
                it[joinCode] = "MATH1"
            }
            Courses.insert {
                it[id] = "course-art"
                it[title] = "Art Class"
                it[description] = "Creative art"
                it[creatorId] = "teacher-1"
                it[isOfficial] = false
                it[schoolYear] = 0
                it[joinCode] = null
            }

            EnrolledCourses.insert {
                it[userId] = "student-1"
                it[courseId] = "course-math"
            }
            EnrolledCourses.insert {
                it[userId] = "student-2"
                it[courseId] = "course-math"
            }
        }
    }
}
