package com.example.proyectofinal.data

import com.example.proyectofinal.domain.Lesson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class LessonApi(private val client: HttpClient) {
    private val baseUrl = "https://api.yourbackend.com"

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

    suspend fun deleteLesson(lessonId: String) {
        client.delete("$baseUrl/lessons/$lessonId")
    }
}
