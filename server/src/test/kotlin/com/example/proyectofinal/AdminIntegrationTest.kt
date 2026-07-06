package com.example.proyectofinal

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.AdminCourseResponse
import com.example.proyectofinal.models.AdminExerciseListResponse
import com.example.proyectofinal.models.AdminExerciseResponse
import com.example.proyectofinal.models.AdminLessonListResponse
import com.example.proyectofinal.models.AdminLessonResponse
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.CreateAdminCourseRequest
import com.example.proyectofinal.models.CreateAdminExerciseRequest
import com.example.proyectofinal.models.CreateAdminLessonRequest
import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.Exercise
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.PageResponse
import com.example.proyectofinal.models.RoleUpdateRequest
import com.example.proyectofinal.models.UpdateAdminCourseRequest
import com.example.proyectofinal.models.UpdateAdminExerciseRequest
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.Security
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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

    @Test
    fun `admin can create update and delete courses through admin endpoints`() = testApplication {
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

        val createdResponse = client.post("/admin/courses") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminCourseRequest(
                    id = "course-science",
                    title = "Science 101",
                    description = "Experiments",
                    isOfficial = true,
                    schoolYear = 6,
                    topic = "Science",
                    difficulty = "Beginner",
                    durationMinutes = 40,
                    xpReward = 120
                )
            )
        }

        assertEquals(HttpStatusCode.OK, createdResponse.status)
        val createdCourse = createdResponse.body<Course>()
        assertEquals("course-science", createdCourse.id)
        assertEquals("admin-1", createdCourse.creatorId)
        assertEquals(true, createdCourse.isOfficial)

        val updatedResponse = client.put("/admin/courses/course-science") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                UpdateAdminCourseRequest(
                    title = "Advanced Science",
                    description = "Labs",
                    isOfficial = false,
                    schoolYear = 7,
                    xpReward = 200
                )
            )
        }

        assertEquals(HttpStatusCode.OK, updatedResponse.status)
        val updatedCourse = updatedResponse.body<Course>()
        assertEquals("Advanced Science", updatedCourse.title)
        assertEquals(false, updatedCourse.isOfficial)
        assertEquals(7, updatedCourse.schoolYear)
        assertEquals(200, updatedCourse.xpReward)

        val missingUpdateResponse = client.put("/admin/courses/missing-course") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                UpdateAdminCourseRequest(
                    title = "Ghost Course"
                )
            )
        }

        assertEquals(HttpStatusCode.NotFound, missingUpdateResponse.status)

        val deleteResponse = client.delete("/admin/courses/course-science") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        transaction {
            assertEquals(0L, Courses.selectAll().where { Courses.id eq "course-science" }.count())
        }
    }

    @Test
    fun `admin course create rejects blank title and non admins are forbidden`() = testApplication {
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
        val teacherToken = Security.generateToken("teacher-1", UserRole.TEACHER.name)

        val invalidResponse = client.post("/admin/courses") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminCourseRequest(
                    id = "invalid-course",
                    title = "",
                    description = "Missing title"
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, invalidResponse.status)

        val forbiddenResponse = client.post("/admin/courses") {
            bearerAuth(teacherToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminCourseRequest(
                    id = "teacher-course",
                    title = "Teacher Course",
                    description = "Should fail"
                )
            )
        }

        assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)
    }

    @Test
    fun `admin lesson routes support filters unassign semantics and creator clear rejection`() = testApplication {
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
        insertLessonFixtures()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)

        val createdLinked = client.post("/admin/lessons") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminLessonRequest(
                    id = "admin-linked-lesson",
                    courseId = "course-math",
                    title = "Admin Linked Lesson",
                    theoryContent = "Linked theory"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, createdLinked.status)
        assertEquals("admin-1", createdLinked.body<AdminLessonResponse>().creatorId)

        val createdStandalone = client.post("/admin/lessons") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminLessonRequest(
                    id = "admin-standalone-lesson",
                    courseId = null,
                    title = "Admin Standalone Lesson",
                    theoryContent = "Standalone theory"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, createdStandalone.status)
        assertEquals("admin-1", createdStandalone.body<AdminLessonResponse>().creatorId)

        val invalidCourseCreate = client.post("/admin/lessons") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminLessonRequest(
                    id = "admin-missing-course-lesson",
                    courseId = "missing-course",
                    title = "Missing Course Lesson",
                    theoryContent = "Should fail"
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, invalidCourseCreate.status)
        assertTrue(invalidCourseCreate.bodyAsText().contains("unknown course"))

        val allLessons = client.get("/admin/lessons") {
            bearerAuth(adminToken)
        }
        val courseLessons = client.get("/admin/lessons?courseId=course-math") {
            bearerAuth(adminToken)
        }
        val standaloneLessons = client.get("/admin/lessons?courseId=") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, allLessons.status)
        assertTrue(allLessons.body<AdminLessonListResponse>().items.any { it.id == "admin-linked-lesson" })
        assertTrue(courseLessons.body<AdminLessonListResponse>().items.all { it.courseId == "course-math" })
        assertTrue(standaloneLessons.body<AdminLessonListResponse>().items.all { it.courseId == null })

        val detachedResponse = client.put("/admin/lessons/admin-linked-lesson") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"courseId":null}""")
        }

        assertEquals(HttpStatusCode.OK, detachedResponse.status)
        val detachedLesson = detachedResponse.body<AdminLessonResponse>()
        assertEquals(null, detachedLesson.courseId)
        assertEquals("admin-1", detachedLesson.creatorId)

        val clearCreatorResponse = client.put("/admin/lessons/admin-linked-lesson") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"creatorId":null}""")
        }

        assertEquals(HttpStatusCode.BadRequest, clearCreatorResponse.status)

        val deleteMissingResponse = client.delete("/admin/lessons/missing-lesson") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.NotFound, deleteMissingResponse.status)
    }

    @Test
    fun `admin exercise routes and public standalone auth flows work end to end`() = testApplication {
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
        insertLessonFixtures()
        insertTeacherTwoFixture()

        val adminToken = Security.generateToken("admin-1", UserRole.ADMIN.name)
        val teacherToken = Security.generateToken("teacher-1", UserRole.TEACHER.name)
        val otherTeacherToken = Security.generateToken("teacher-2", UserRole.TEACHER.name)

        val publicStandaloneCreate = client.post("/lessons") {
            bearerAuth(teacherToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateLessonRequest(
                    id = "teacher-standalone",
                    courseId = null,
                    title = "Teacher Standalone",
                    theoryContent = "Standalone content"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, publicStandaloneCreate.status)
        assertEquals("teacher-1", publicStandaloneCreate.body<Lesson>().creatorId)

        val publicExerciseCreate = client.post("/exercises") {
            bearerAuth(teacherToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateExerciseRequest(
                    id = "teacher-standalone-exercise",
                    lessonId = "teacher-standalone",
                    question = "2 + 2 = ?",
                    options = listOf("3", "4"),
                    correctAnswer = "4",
                    type = ExerciseType.MULTIPLE_CHOICE
                )
            )
        }

        assertEquals(HttpStatusCode.OK, publicExerciseCreate.status)

        val otherTeacherLessonUpdate = client.put("/lessons/teacher-standalone") {
            bearerAuth(otherTeacherToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(UpdateLessonRequest(title = "Hijack"))
        }

        val otherTeacherExerciseCreate = client.post("/exercises") {
            bearerAuth(otherTeacherToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateExerciseRequest(
                    id = "teacher-standalone-exercise-2",
                    lessonId = "teacher-standalone",
                    question = "Blocked",
                    options = listOf("A", "B"),
                    correctAnswer = "A",
                    type = ExerciseType.MULTIPLE_CHOICE
                )
            )
        }

        assertEquals(HttpStatusCode.Forbidden, otherTeacherLessonUpdate.status)
        assertEquals(HttpStatusCode.Forbidden, otherTeacherExerciseCreate.status)

        val adminStandaloneRead = client.get("/lessons/teacher-standalone") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, adminStandaloneRead.status)
        assertEquals("", adminStandaloneRead.body<Lesson>().exercises.single().correctAnswer)

        val adminExerciseCreate = client.post("/admin/exercises") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminExerciseRequest(
                    id = "admin-exercise",
                    lessonId = "linked-lesson",
                    question = "Admin question",
                    options = listOf("1", "2"),
                    correctAnswer = "2",
                    type = ExerciseType.MULTIPLE_CHOICE
                )
            )
        }

        assertEquals(HttpStatusCode.OK, adminExerciseCreate.status)

        val missingLessonExerciseCreate = client.post("/admin/exercises") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                CreateAdminExerciseRequest(
                    id = "missing-lesson-admin-exercise",
                    lessonId = "missing-lesson",
                    question = "Unknown lesson",
                    options = listOf("1", "2"),
                    correctAnswer = "2",
                    type = ExerciseType.MULTIPLE_CHOICE
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, missingLessonExerciseCreate.status)
        assertTrue(missingLessonExerciseCreate.bodyAsText().contains("unknown lesson"))

        val missingQuestionExerciseCreate = client.post("/admin/exercises") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                """{"id":"missing-question-admin-exercise","lessonId":"linked-lesson","options":["1","2"],"correctAnswer":"2"}"""
            )
        }

        assertEquals(HttpStatusCode.BadRequest, missingQuestionExerciseCreate.status)
        assertTrue(missingQuestionExerciseCreate.bodyAsText().contains("Invalid request body"))

        val filteredExercises = client.get("/admin/exercises?lessonId=linked-lesson") {
            bearerAuth(adminToken)
        }

        val allExercises = client.get("/admin/exercises") {
            bearerAuth(adminToken)
        }

        assertEquals(HttpStatusCode.OK, filteredExercises.status)
        assertTrue(filteredExercises.body<AdminExerciseListResponse>().items.any { it.id == "admin-exercise" })
        assertEquals(HttpStatusCode.OK, allExercises.status)
        val allExerciseIds = allExercises.body<AdminExerciseListResponse>().items.map { it.id }.toSet()
        assertTrue(allExerciseIds.contains("standalone-existing-exercise"))
        assertTrue(allExerciseIds.contains("admin-exercise"))

        val updatedExercise = client.put("/admin/exercises/admin-exercise") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                UpdateAdminExerciseRequest(
                    lessonId = "standalone-lesson",
                    question = "Moved question"
                )
            )
        }

        assertEquals(HttpStatusCode.OK, updatedExercise.status)
        assertEquals("standalone-lesson", updatedExercise.body<AdminExerciseResponse>().lessonId)

        val missingLessonExerciseUpdate = client.put("/admin/exercises/admin-exercise") {
            bearerAuth(adminToken)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                UpdateAdminExerciseRequest(
                    lessonId = "missing-lesson"
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, missingLessonExerciseUpdate.status)
        assertTrue(missingLessonExerciseUpdate.bodyAsText().contains("unknown lesson"))

        val deleteMissingExercise = client.delete("/admin/exercises/missing-exercise") {
            bearerAuth(adminToken)
        }

        val forbiddenAdminExercises = client.get("/admin/exercises") {
            bearerAuth(otherTeacherToken)
        }

        assertEquals(HttpStatusCode.NotFound, deleteMissingExercise.status)
        assertEquals(HttpStatusCode.Forbidden, forbiddenAdminExercises.status)
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

    private fun insertLessonFixtures() {
        transaction {
            Lessons.insert {
                it[id] = "linked-lesson"
                it[courseId] = "course-math"
                it[creatorId] = "teacher-1"
                it[title] = "Linked Lesson"
                it[theoryContent] = "Linked theory"
                it[orderIndex] = 0
            }
            Lessons.insert {
                it[id] = "standalone-lesson"
                it[courseId] = null
                it[creatorId] = "teacher-1"
                it[title] = "Standalone Lesson"
                it[theoryContent] = "Standalone theory"
                it[orderIndex] = 1
            }
            Exercises.insert {
                it[id] = "standalone-existing-exercise"
                it[lessonId] = "standalone-lesson"
                it[question] = "1 + 1 = ?"
                it[options] = "1,2"
                it[correctAnswer] = "2"
                it[type] = ExerciseType.MULTIPLE_CHOICE.name
            }
        }
    }

    private fun insertTeacherTwoFixture() {
        transaction {
            Users.insert {
                it[id] = "teacher-2"
                it[name] = "Teacher Two"
                it[email] = "teacher2@example.com"
                it[passwordHash] = "hash"
                it[role] = "TEACHER"
            }
        }
    }
}
