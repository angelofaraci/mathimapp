package com.example.proyectofinal

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.proyectofinal.database.Courses
import com.example.proyectofinal.database.DatabaseFactory
import com.example.proyectofinal.database.EnrolledCourses
import com.example.proyectofinal.database.Exercises
import com.example.proyectofinal.database.Lessons
import com.example.proyectofinal.database.Users
import com.example.proyectofinal.models.CreateCourseRequest
import com.example.proyectofinal.models.CreateExerciseRequest
import com.example.proyectofinal.models.CreateLessonRequest
import com.example.proyectofinal.models.ExerciseType
import com.example.proyectofinal.models.UpdateCourseRequest
import com.example.proyectofinal.models.UpdateExerciseRequest
import com.example.proyectofinal.models.UpdateLessonRequest
import com.example.proyectofinal.models.UserRole
import com.example.proyectofinal.service.AuthService
import com.example.proyectofinal.service.CourseService
import com.example.proyectofinal.service.ExerciseService
import com.example.proyectofinal.service.LessonService
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CourseServiceTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
    }

    @Test
    fun `course service query methods return persisted data`() {
        insertUser(id = "admin-1", role = UserRole.ADMIN)
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertUser(id = "student-1", role = UserRole.LEARNER)
        insertCourse(id = "official-course", creatorId = "admin-1", isOfficial = true)
        insertCourse(id = "teacher-course", creatorId = "teacher-1", joinCode = "JOIN123")
        insertCourse(id = "teacher-course-2", creatorId = "teacher-1", joinCode = "JOIN456")
        insertLesson(id = "lesson-2", courseId = "teacher-course", orderIndex = 1)
        insertLesson(id = "lesson-1", courseId = "teacher-course", orderIndex = 0)
        enrollUser(userId = "student-1", courseId = "teacher-course")

        val service = CourseService()

        assertEquals(listOf("official-course"), service.getOfficialCourses().map { it.id })
        assertEquals(
            setOf("teacher-course", "teacher-course-2"),
            service.getCoursesByCreator("teacher-1").map { it.id }.toSet()
        )
        assertEquals(listOf("teacher-course"), service.getEnrolledCourses("student-1").map { it.id })

        val course = service.getCourseById("teacher-course")
        assertNotNull(course)
        assertEquals(listOf("lesson-1", "lesson-2"), course.lessons.map { it.id })
        assertEquals("teacher-1", service.getCreatorId("teacher-course"))
    }

    @Test
    fun `course service mutation methods persist changes`() {
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertUser(id = "student-1", role = UserRole.LEARNER)

        val service = CourseService()
        val created = service.createCourse(
            CreateCourseRequest(
                id = "created-course",
                title = "Created Course",
                description = "Created description",
                creatorId = "teacher-1",
                joinCode = "JOIN123"
            )
        )

        assertEquals("created-course", created.id)

        val updated = service.updateCourse(
            id = "created-course",
            request = UpdateCourseRequest(
                title = "Updated Course",
                description = "Updated description",
                joinCode = "NEWCODE"
            )
        )
        assertEquals("Updated Course", updated?.title)
        assertEquals("Updated description", updated?.description)

        val joined = service.joinCourse(userId = "student-1", code = "NEWCODE")
        assertEquals("created-course", joined?.id)

        transaction {
            assertEquals(
                1L,
                EnrolledCourses.selectAll()
                    .where { EnrolledCourses.userId eq "student-1" }
                    .count()
            )
        }

        assertTrue(service.deleteCourse("created-course"))
        assertNull(service.getCourseById("created-course"))
    }
}

class AuthServiceTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
    }

    @Test
    fun `find user by email returns persisted auth record`() {
        val passwordHash = BCrypt.withDefaults().hashToString(12, "secret123".toCharArray())
        insertUser(
            id = "teacher-1",
            role = UserRole.TEACHER,
            email = "teacher@example.com",
            passwordHash = passwordHash
        )

        val user = AuthService().findUserByEmail("teacher@example.com")

        assertNotNull(user)
        assertEquals("teacher-1", user.id)
        assertEquals("teacher@example.com", user.email)
        assertEquals(passwordHash, user.passwordHash)
    }

    @Test
    fun `validate credentials accepts matching password and rejects other paths`() {
        val passwordHash = BCrypt.withDefaults().hashToString(12, "secret123".toCharArray())
        insertUser(
            id = "teacher-1",
            role = UserRole.TEACHER,
            email = "teacher@example.com",
            passwordHash = passwordHash
        )

        val service = AuthService()

        assertEquals("teacher-1", service.validateCredentials("teacher@example.com", "secret123")?.id)
        assertNull(service.validateCredentials("teacher@example.com", "wrong-password"))
        assertNull(service.validateCredentials("missing@example.com", "secret123"))
    }
}

class LessonExerciseServiceTest {
    @BeforeTest
    fun setUp() {
        initServiceTestDatabase()
    }

    @Test
    fun `lesson service supports list lookup update and delete flows`() {
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertCourse(id = "course-1", creatorId = "teacher-1")

        val service = LessonService()
        service.createLesson(CreateLessonRequest("lesson-1", "course-1", "First", "Theory 1"))
        service.createLesson(CreateLessonRequest("lesson-2", "course-1", "Second", "Theory 2"))
        insertExercise(id = "exercise-1", lessonId = "lesson-1", correctAnswer = "4")

        assertEquals(listOf("lesson-1", "lesson-2"), service.getLessonsByCourseId("course-1").map { it.id })

        val lesson = service.getLessonById("lesson-1", hideAnswers = false)
        assertNotNull(lesson)
        assertEquals("4", lesson.exercises.single().correctAnswer)

        val updated = service.updateLesson(
            id = "lesson-1",
            request = UpdateLessonRequest(
                title = "Updated lesson",
                theoryContent = "Updated theory"
            )
        )
        assertEquals("Updated lesson", updated?.title)
        assertEquals("teacher-1", service.getCourseCreatorId("course-1"))
        assertEquals("teacher-1", service.getCreatorId("lesson-1"))

        assertTrue(service.deleteLesson("lesson-2"))
        assertEquals(listOf("lesson-1"), service.getLessonsByCourseId("course-1").map { it.id })
    }

    @Test
    fun `exercise service supports list create update and delete flows`() {
        insertUser(id = "teacher-1", role = UserRole.TEACHER)
        insertCourse(id = "course-1", creatorId = "teacher-1")
        insertLesson(id = "lesson-1", courseId = "course-1")

        val service = ExerciseService()
        service.createExercise(
            CreateExerciseRequest(
                id = "exercise-1",
                lessonId = "lesson-1",
                question = "2 + 2 = ?",
                options = listOf("3", "4"),
                correctAnswer = "4",
                type = ExerciseType.MULTIPLE_CHOICE
            )
        )

        val hiddenExercises = service.getExercisesByLessonId("lesson-1", hideAnswers = true)
        assertEquals(1, hiddenExercises.size)
        assertEquals("", hiddenExercises.single().correctAnswer)

        val updated = service.updateExercise(
            id = "exercise-1",
            request = UpdateExerciseRequest(
                question = "3 + 3 = ?",
                options = listOf("5", "6"),
                correctAnswer = "6"
            )
        )
        assertEquals("3 + 3 = ?", updated?.question)
        assertEquals(listOf("5", "6"), updated?.options)
        assertEquals("teacher-1", service.getLessonCreatorId("lesson-1"))
        assertEquals("teacher-1", service.getCreatorId("exercise-1"))

        assertTrue(service.deleteExercise("exercise-1"))
        assertTrue(service.getExercisesByLessonId("lesson-1", hideAnswers = false).isEmpty())
    }
}

private fun initServiceTestDatabase() {
    DatabaseFactory.init(
        url = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
}

private fun insertUser(
    id: String,
    role: UserRole,
    email: String = "$id@example.com",
    passwordHash: String = "hash",
    name: String = id
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

private fun insertCourse(
    id: String,
    creatorId: String,
    isOfficial: Boolean = false,
    joinCode: String? = null,
    title: String = id,
    description: String = id
) {
    transaction {
        Courses.insert {
            it[Courses.id] = id
            it[Courses.title] = title
            it[Courses.description] = description
            it[Courses.creatorId] = creatorId
            it[Courses.isOfficial] = isOfficial
            it[Courses.joinCode] = joinCode
        }
    }
}

private fun insertLesson(
    id: String,
    courseId: String,
    orderIndex: Int = 0,
    title: String = id,
    theoryContent: String = id
) {
    transaction {
        Lessons.insert {
            it[Lessons.id] = id
            it[Lessons.courseId] = courseId
            it[Lessons.title] = title
            it[Lessons.theoryContent] = theoryContent
            it[Lessons.orderIndex] = orderIndex
        }
    }
}

private fun insertExercise(
    id: String,
    lessonId: String,
    question: String = id,
    options: String = "a,b",
    correctAnswer: String = "a"
) {
    transaction {
        Exercises.insert {
            it[Exercises.id] = id
            it[Exercises.lessonId] = lessonId
            it[Exercises.question] = question
            it[Exercises.options] = options
            it[Exercises.correctAnswer] = correctAnswer
            it[Exercises.type] = ExerciseType.MULTIPLE_CHOICE.name
        }
    }
}

private fun enrollUser(userId: String, courseId: String) {
    transaction {
        EnrolledCourses.insert {
            it[EnrolledCourses.userId] = userId
            it[EnrolledCourses.courseId] = courseId
        }
    }
}
