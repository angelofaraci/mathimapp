package com.example.proyectofinal.data

import com.example.proyectofinal.domain.CourseRepository
import com.example.proyectofinal.models.Course
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.UserProgress
import kotlin.random.Random

class MockCourseRepository : CourseRepository {
    private val enrolledCourseIds = mutableSetOf<String>()
    private val mockCourses = mutableListOf(
        Course(
            id = "math-101",
            title = "Basic Arithmetic",
            description = "Master addition, subtraction, multiplication, and division.",
            creatorId = "admin",
            isOfficial = true,
            joinCode = null,
            lessons = listOf(
                Lesson(
                    id = "lesson-1",
                    courseId = "math-101", // FIXED: Added courseId
                    title = "Introduction to Addition",
                    theoryContent = "# Addition",
                    exercises = emptyList()
                )
            )
        )
    )

    override suspend fun getOfficialCourses(schoolYear: Int?): List<Course> =
        mockCourses.filter { course ->
            course.isOfficial && (schoolYear == null || course.schoolYear == schoolYear)
        }

    override suspend fun getCourseById(id: String): Course? = mockCourses.find { it.id == id }

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = 
        mockCourses.filter { it.creatorId == creatorId && !it.isOfficial }

    override suspend fun getEnrolledCourses(userId: String): List<Course> = 
        mockCourses.filter { it.creatorId != "admin" && it.creatorId != userId }

    override suspend fun createCourse(course: Course): Course {
        val newCourse = course.copy(
            id = Random.nextInt(1000, 9999).toString(),
            isOfficial = false,
            joinCode = generateJoinCode()
        )
        mockCourses.add(newCourse)
        return newCourse
    }

    override suspend fun updateCourse(course: Course): Course {
        val index = mockCourses.indexOfFirst { it.id == course.id }
        if (index != -1) {
            mockCourses[index] = course
            return course
        }
        throw Exception("Course not found")
    }

    override suspend fun deleteCourse(id: String) {
        mockCourses.removeAll { it.id == id && !it.isOfficial }
    }

    override suspend fun joinCourseByCode(userId: String, code: String): Course? {
        return mockCourses.find { it.joinCode == code }
    }

    override suspend fun enroll(courseId: String): UserProgress {
        val course = mockCourses.find { it.id == courseId && it.isOfficial }
            ?: error("Course not found")

        enrolledCourseIds += course.id

        return UserProgress(
            userId = "mock-user",
            enrolledCourseIds = enrolledCourseIds.toSet()
        )
    }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
