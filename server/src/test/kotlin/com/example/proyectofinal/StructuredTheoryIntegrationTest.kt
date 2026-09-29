package com.example.proyectofinal

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.AdminLessonResponse
import com.example.proyectofinal.models.CreateAdminLessonRequest
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.LessonTheoryResponse
import com.example.proyectofinal.models.ReplaceLessonTheorySectionsRequest
import com.example.proyectofinal.models.TheorySectionInput
import com.example.proyectofinal.models.TheorySectionType
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.plugins.Security
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StructuredTheoryIntegrationTest {
    private fun setupDatabase() {
        System.setProperty("jwt.secret", "test-jwt-secret")
        DatabaseFactory.init("jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "org.h2.Driver", "sa", "")
        transaction {
            Users.insert { it[id] = "admin"; it[name] = "Admin"; it[email] = "admin@example.com"; it[passwordHash] = "hash"; it[role] = "ADMIN" }
            Users.insert { it[id] = "student"; it[name] = "Student"; it[email] = "student@example.com"; it[passwordHash] = "hash"; it[role] = "STUDENT" }
            Courses.insert { it[id] = "course"; it[title] = "Math"; it[description] = "Math"; it[creatorId] = "admin"; it[isOfficial] = true; it[schoolYear] = 3 }
            EnrolledCourses.insert { it[userId] = "student"; it[courseId] = "course" }
            Lessons.insert { it[id] = "legacy"; it[courseId] = "course"; it[creatorId] = "admin"; it[title] = "Legacy"; it[theoryContent] = "Legacy fallback"; it[orderIndex] = 0 }
        }
    }

    @Test
    fun `admin can create replace and learners read structured theory`() = testApplication {
        setupDatabase()
        application { module(initDatabase = false, seedData = false) }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val admin = Security.generateToken("admin", UserRole.ADMIN.name)
        val student = Security.generateToken("student", UserRole.STUDENT.name)
        val original = listOf(
            TheorySectionInput(TheorySectionType.CONCEPT, "Concept", "Fractions can name the same value."),
            TheorySectionInput(TheorySectionType.EXAMPLE, "Example", "2/3 = 4/6")
        )

        val created = client.post("/admin/lessons") {
            bearerAuth(admin); header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(CreateAdminLessonRequest("structured", "course", title = "Equivalent fractions", theoryContent = "Fallback", theorySections = original))
        }
        assertEquals(HttpStatusCode.OK, created.status)
        assertEquals("structured", created.body<AdminLessonResponse>().id)

        val adminRead = client.get("/admin/lessons/structured/theory-sections") { bearerAuth(admin) }
        assertEquals(HttpStatusCode.OK, adminRead.status)
        assertEquals(original.map { it.type }, adminRead.body<LessonTheoryResponse>().sections.map { it.type })
        assertEquals(listOf(0, 1), adminRead.body<LessonTheoryResponse>().sections.map { it.position })

        val learnerRead = client.get("/lessons/structured/theory") { bearerAuth(student) }
        assertEquals(HttpStatusCode.OK, learnerRead.status)
        assertEquals(original.map { it.content }, learnerRead.body<LessonTheoryResponse>().sections.map { it.content })

        val replacement = listOf(TheorySectionInput(TheorySectionType.WARNING, null, "Multiply both terms by the same number."))
        val replaced = client.put("/admin/lessons/structured/theory-sections") {
            bearerAuth(admin); header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(ReplaceLessonTheorySectionsRequest(replacement))
        }
        assertEquals(HttpStatusCode.OK, replaced.status)
        assertEquals(replacement.map { it.content }, replaced.body<LessonTheoryResponse>().sections.map { it.content })

        val learnerAfterReplacement = client.get("/lessons/structured/theory") { bearerAuth(student) }
        assertEquals(replacement.map { it.content }, learnerAfterReplacement.body<LessonTheoryResponse>().sections.map { it.content })
    }

    @Test
    fun `rejected replacement preserves sections and legacy lessons expose their fallback`() = testApplication {
        setupDatabase()
        application { module(initDatabase = false, seedData = false) }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val admin = Security.generateToken("admin", UserRole.ADMIN.name)
        val student = Security.generateToken("student", UserRole.STUDENT.name)
        val unstructuredLegacyTheory = client.get("/lessons/legacy/theory") { bearerAuth(student) }
        assertEquals(HttpStatusCode.OK, unstructuredLegacyTheory.status)
        assertTrue(unstructuredLegacyTheory.body<LessonTheoryResponse>().sections.isEmpty())

        val existing = listOf(TheorySectionInput(TheorySectionType.CONCEPT, null, "Saved content"))
        suspend fun theoryWrite(sections: List<TheorySectionInput>) = client.put("/admin/lessons/legacy/theory-sections") {
            bearerAuth(admin); header(HttpHeaders.ContentType, ContentType.Application.Json.toString()); setBody(ReplaceLessonTheorySectionsRequest(sections))
        }

        assertEquals(HttpStatusCode.OK, theoryWrite(existing).status)
        assertEquals(HttpStatusCode.BadRequest, theoryWrite(listOf(TheorySectionInput(TheorySectionType.EXAMPLE, null, "  "))).status)
        val preserved = client.get("/admin/lessons/legacy/theory-sections") { bearerAuth(admin) }
        assertEquals(existing.map { it.content }, preserved.body<LessonTheoryResponse>().sections.map { it.content })

        val legacyLesson = client.get("/lessons/legacy") { bearerAuth(student) }
        assertEquals(HttpStatusCode.OK, legacyLesson.status)
        assertEquals("Legacy fallback", legacyLesson.body<Lesson>().theoryContent)
        assertTrue(client.get("/lessons/legacy/theory") { bearerAuth(student) }.body<LessonTheoryResponse>().sections.map { it.content } == listOf("Saved content"))
    }
}