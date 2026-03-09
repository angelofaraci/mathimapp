package com.example.proyectofinal.data

import com.example.proyectofinal.domain.Course
import com.example.proyectofinal.domain.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorCourseRepository(
    private val api: CourseApi
) : CourseRepository {

    override suspend fun getOfficialCourses(): List<Course> = withContext(Dispatchers.IO) {
        api.fetchOfficialCourses()
    }

    override suspend fun getCourseById(id: String): Course? = withContext(Dispatchers.IO) {
        try {
            api.fetchCourseById(id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMyCreatedCourses(creatorId: String): List<Course> = withContext(Dispatchers.IO) {
        api.fetchMyCourses(creatorId)
    }

    override suspend fun getEnrolledCourses(userId: String): List<Course> = withContext(Dispatchers.IO) {
        api.fetchEnrolledCourses(userId)
    }

    override suspend fun createCourse(course: Course): Course = withContext(Dispatchers.IO) {
        api.createCourse(course)
    }

    override suspend fun updateCourse(course: Course): Course = withContext(Dispatchers.IO) {
        api.updateCourse(course)
    }

    override suspend fun deleteCourse(id: String) = withContext(Dispatchers.IO) {
        api.deleteCourse(id)
    }

    override suspend fun joinCourseByCode(userId: String, code: String): Course? = withContext(Dispatchers.IO) {
        try {
            api.joinCourse(userId, code)
        } catch (e: Exception) {
            null
        }
    }
}
