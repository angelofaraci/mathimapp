package com.example.proyectofinal.data

import com.example.proyectofinal.domain.Course
import com.example.proyectofinal.domain.UserProgress
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class CourseApi(private val client: HttpClient) {
    private val baseUrl = "https://api.yourbackend.com" // TODO: replace with actual URL

    suspend fun fetchCourses(): List<Course> {
        return client.get("$baseUrl/courses").body()
    }

    suspend fun fetchCourseById(id: String): Course {
        return client.get("$baseUrl/courses/$id").body()
    }

    suspend fun fetchUserProgress(userId: String): UserProgress {
        return client.get("$baseUrl/progress/$userId").body()
    }

    suspend fun saveUserProgress(progress: UserProgress) {
        client.post("$baseUrl/progress") {
            contentType(ContentType.Application.Json)
            setBody(progress)
        }
    }
}
