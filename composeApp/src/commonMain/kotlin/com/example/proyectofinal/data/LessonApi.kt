package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.Lesson
import com.example.proyectofinal.models.TheoryUpdateRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class LessonApi(
    private val client: HttpClient,
    private val apiConfig: ApiConfig
) {

    private val baseUrl: String = apiConfig.baseUrl

    suspend fun fetchLessonsByCourse(courseId: String): List<Lesson> {
        return client.get("$baseUrl/courses/$courseId/lessons").body()
    }

    suspend fun fetchLesson(lessonId: String): Lesson {
        return client.get("$baseUrl/lessons/$lessonId").body()
    }

    suspend fun createLesson(lesson: Lesson): Lesson {
        return client.post("$baseUrl/lessons") {
            contentType(ContentType.Application.Json)
            setBody(lesson)
        }.body()
    }

    suspend fun updateLesson(lesson: Lesson): Lesson {
        return client.put("$baseUrl/lessons/${lesson.id}") {
            contentType(ContentType.Application.Json)
            setBody(lesson)
        }.body()
    }

    suspend fun updateTheory(request: TheoryUpdateRequest): Lesson {
        return client.put("$baseUrl/lessons/${request.lessonId}/theory") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteLesson(lessonId: String) {
        client.delete("$baseUrl/lessons/$lessonId")
    }
}
