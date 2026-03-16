package com.example.proyectofinal.data

import com.example.proyectofinal.domain.Exercise
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ExerciseApi(private val client: HttpClient) {
    private val baseUrl = "https://api.yourbackend.com"

    suspend fun fetchExercisesByLesson(lessonId: String): List<Exercise> {
        return client.get("$baseUrl/lessons/$lessonId/exercises").body()
    }

    suspend fun createExercise(exercise: Exercise): Exercise {
        return client.post("$baseUrl/exercises") {
            contentType(ContentType.Application.Json)
            setBody(exercise)
        }.body()
    }

    suspend fun updateExercise(exercise: Exercise): Exercise {
        return client.put("$baseUrl/exercises/${exercise.id}") {
            contentType(ContentType.Application.Json)
            setBody(exercise)
        }.body()
    }

    suspend fun deleteExercise(exerciseId: String) {
        client.delete("$baseUrl/exercises/$exerciseId")
    }
}
