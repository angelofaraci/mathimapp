package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.CompleteExerciseRequest
import com.example.proyectofinal.models.ExerciseCompletionResponse
import com.example.proyectofinal.models.User
import com.example.proyectofinal.models.UserProgress
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(
    private val client: HttpClient,
    private val apiConfig: ApiConfig
) {

    private val baseUrl: String = apiConfig.baseUrl

    suspend fun fetchUser(userId: String): User {
        return client.get("$baseUrl/users/$userId").body()
    }

    suspend fun updateUser(user: User) {
        client.put("$baseUrl/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }
    }

    suspend fun fetchUserProgress(userId: String): UserProgress {
        return client.get("$baseUrl/progress/$userId").body()
    }

    suspend fun completeExercise(exerciseId: String, score: Int = 0): ExerciseCompletionResponse {
        return client.post("$baseUrl/exercises/$exerciseId/complete") {
            contentType(ContentType.Application.Json)
            setBody(CompleteExerciseRequest(exerciseId = exerciseId, score = score))
        }.body()
    }
}
