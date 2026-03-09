package com.example.proyectofinal.data

import com.example.proyectofinal.domain.Course
import com.example.proyectofinal.domain.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class KtorCourseRepository(
    private val api: CourseApi
) : CourseRepository {

    override suspend fun getCourses(): List<Course> = withContext(Dispatchers.IO) {
        api.fetchCourses()
    }

    override suspend fun getCourseById(id: String): Course? = withContext(Dispatchers.IO) {
        try {
            api.fetchCourseById(id)
        } catch (e: Exception) {
            null
        }
    }
}
