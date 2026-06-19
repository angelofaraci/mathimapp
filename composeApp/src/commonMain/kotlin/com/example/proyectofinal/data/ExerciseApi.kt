package com.example.proyectofinal.data

import com.example.proyectofinal.BASE_URL
import com.example.proyectofinal.domain.Exercise
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ExerciseApi(private val client: HttpClient) {

    suspend fun fetchExercisesByLesson(lessonId: String): List<Exercise> {
        return client.get("$BASE_URL/lessons/$lessonId/exercises").body()
    }

    suspend fun createExercise(exercise: Exercise): Exercise {
        return client.post("$BASE_URL/exercises") {
            contentType(ContentType.Application.Json)
            setBody(exercise)
        }.body()
    }

    suspend fun updateExercise(exercise: Exercise): Exercise {
        return client.put("$BASE_URL/exercises/${exercise.id}") {
            contentType(ContentType.Application.Json)
            setBody(exercise)
        }.body()
    }

    suspend fun deleteExercise(exerciseId: String) {
        client.delete("$BASE_URL/exercises/$exerciseId")
    }
}
