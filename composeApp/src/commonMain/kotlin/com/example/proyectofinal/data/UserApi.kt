package com.example.proyectofinal.data

import com.example.proyectofinal.di.ApiConfig
import com.example.proyectofinal.models.ExerciseAttemptRequest
import com.example.proyectofinal.models.ExerciseAttemptResponse
import com.example.proyectofinal.models.ExerciseSubmission
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

    suspend fun attemptExercise(
        exerciseId: String,
        submission: ExerciseSubmission,
        score: Int = 100
    ): ExerciseAttemptResponse {
        return client.post("$baseUrl/exercises/$exerciseId/attempt") {
            contentType(ContentType.Application.Json)
            setBody(
                ExerciseAttemptRequest(
                    exerciseId = exerciseId,
                    submission = submission,
                    score = score
                )
            )
        }.body()
    }
}
