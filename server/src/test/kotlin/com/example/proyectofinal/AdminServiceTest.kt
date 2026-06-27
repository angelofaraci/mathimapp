package com.example.proyectofinal

import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.service.CourseService
import com.example.proyectofinal.service.UserService
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminServiceTest {
    @BeforeTest
    fun setUp() {
        initAdminServiceTestDatabase()
    }

    @Test
    fun `list users applies pagination and search filters`() {
        insertAdminServiceUser(id = "admin-1", role = UserRole.ADMIN, name = "Zoe Admin", email = "zoe@ops.io")
        insertAdminServiceUser(id = "student-1", role = UserRole.STUDENT, name = "Alice Student", email = "alice@academy.io")
        insertAdminServiceUser(id = "teacher-1", role = UserRole.TEACHER, name = "Bob Teacher", email = "bob@academy.io")
        insertAdminServiceUser(id = "student-2", role = UserRole.STUDENT, name = "Aaron Learner", email = "aaron@school.io")

        val service = UserService()

        val firstPage = service.listUsers(page = 0, size = 2)
        assertEquals(4L, firstPage.totalElements)
        assertEquals(2, firstPage.totalPages)
        assertEquals(listOf("Aaron Learner", "Alice Student"), firstPage.items.map { it.name })

        val secondPage = service.listUsers(page = 1, size = 2)
        assertEquals(4L, secondPage.totalElements)
        assertEquals(2, secondPage.totalPages)
        assertEquals(listOf("Bob Teacher", "Zoe Admin"), secondPage.items.map { it.name })

        val searchPage = service.listUsers(query = "academy", page = 0, size = 10)
        assertEquals(2L, searchPage.totalElements)
        assertEquals(1, searchPage.totalPages)
        assertEquals(listOf("Alice Student", "Bob Teacher"), searchPage.items.map { it.name })
    }

    @Test
    fun `get all courses admin returns creator names and enrollment counts`() {
        insertAdminServiceUser(id = "teacher-1", role = UserRole.TEACHER, name = "Teacher One")
        insertAdminServiceUser(id = "teacher-2", role = UserRole.TEACHER, name = "Teacher Two")
        insertAdminServiceUser(id = "student-1", role = UserRole.STUDENT)
        insertAdminServiceUser(id = "student-2", role = UserRole.STUDENT)
        insertAdminServiceUser(id = "student-3", role = UserRole.STUDENT)

        insertAdminServiceCourse(
            id = "course-math",
            creatorId = "teacher-1",
            title = "Math 101",
            description = "Numbers",
            isOfficial = true,
            schoolYear = 3
        )
        insertAdminServiceCourse(
            id = "course-art",
            creatorId = "teacher-2",
            title = "Art Workshop",
            description = "Colors",
            schoolYear = 0
        )
        enrollAdminServiceUser(userId = "student-1", courseId = "course-math")
        enrollAdminServiceUser(userId = "student-2", courseId = "course-math")
        enrollAdminServiceUser(userId = "student-3", courseId = "course-art")

        val courses = CourseService().getAllCoursesAdmin()
        assertEquals(2, courses.size)

        val mathCourse = courses.single { it.id == "course-math" }
        assertEquals("Teacher One", mathCourse.creatorName)
        assertEquals(2, mathCourse.enrollmentCount)
        assertEquals(true, mathCourse.isOfficial)
        assertEquals(3, mathCourse.schoolYear)

        val artCourse = courses.single { it.id == "course-art" }
        assertEquals("Teacher Two", artCourse.creatorName)
        assertEquals(1, artCourse.enrollmentCount)
        assertEquals(false, artCourse.isOfficial)
        assertEquals(0, artCourse.schoolYear)
    }
}

private fun initAdminServiceTestDatabase() {
    DatabaseFactory.init(
        url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
}

private fun insertAdminServiceUser(
    id: String,
    role: UserRole,
    name: String = id,
    email: String = "$id@example.com",
    passwordHash: String = "hash"
) {
    transaction {
        Users.insert {
            it[Users.id] = id
            it[Users.name] = name
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.role] = role.name
        }
    }
}

private fun insertAdminServiceCourse(
    id: String,
    creatorId: String,
    title: String = id,
    description: String = id,
    isOfficial: Boolean = false,
    schoolYear: Int = 0
) {
    transaction {
        Courses.insert {
            it[Courses.id] = id
            it[Courses.title] = title
            it[Courses.description] = description
            it[Courses.creatorId] = creatorId
            it[Courses.isOfficial] = isOfficial
            it[Courses.schoolYear] = schoolYear
            it[Courses.joinCode] = null
        }
    }
}

private fun enrollAdminServiceUser(userId: String, courseId: String) {
    transaction {
        EnrolledCourses.insert {
            it[EnrolledCourses.userId] = userId
            it[EnrolledCourses.courseId] = courseId
        }
    }
}
