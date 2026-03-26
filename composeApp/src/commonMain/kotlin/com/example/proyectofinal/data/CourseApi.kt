package com.example.proyectofinal.data

import com.example.proyectofinal.BASE_URL
import com.example.proyectofinal.domain.Course
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class CourseApi(private val client: HttpClient) {

    suspend fun fetchOfficialCourses(): List<Course> {
        return client.get("$BASE_URL/courses/official").body()
    }

    suspend fun fetchCourseById(id: String): Course {
        return client.get("$BASE_URL/courses/$id").body()
    }

    suspend fun fetchMyCourses(creatorId: String): List<Course> {
        return client.get("$BASE_URL/courses/creator/$creatorId").body()
    }

    suspend fun fetchEnrolledCourses(userId: String): List<Course> {
        return client.get("$BASE_URL/courses/enrolled/$userId").body()
    }

    suspend fun createCourse(course: Course): Course {
        return client.post("$BASE_URL/courses") {
            contentType(ContentType.Application.Json)
            setBody(course)
        }.body()
    }

    suspend fun updateCourse(course: Course): Course {
        return client.put("$BASE_URL/courses/${course.id}") {
            contentType(ContentType.Application.Json)
            setBody(course)
        }.body()
    }

    suspend fun deleteCourse(id: String) {
        client.delete("$BASE_URL/courses/$id")
    }

    suspend fun joinCourse(userId: String, code: String): Course {
        return client.post("$BASE_URL/courses/join") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("userId" to userId, "code" to code))
        }.body()
    }
}
